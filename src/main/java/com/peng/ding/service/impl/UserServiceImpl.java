package com.peng.ding.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itshidu.common.ftp.config.FtpPoolConfig;
import com.itshidu.common.ftp.core.FTPClientFactory;
import com.itshidu.common.ftp.core.FTPClientPool;
import com.itshidu.common.ftp.core.FtpClientUtils;
import com.peng.ding.dao.GroupChatDao;
import com.peng.ding.dao.GroupDao;
import com.peng.ding.dao.UserDao;
import com.peng.ding.pojo.Group;
import com.peng.ding.pojo.GroupChat;
import com.peng.ding.pojo.Result;
import com.peng.ding.pojo.User;
import com.peng.ding.service.UserService;
import com.peng.ding.util.RedisReceiver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private GroupDao groupDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private GroupChatDao groupChatDao;

    @Autowired
    private FtpClientUtils ftpUtil;

    @Value("${ftp.host}")
    private String host;  //设置图片服务器的地址

    @Autowired
    private RedisReceiver redisReceiver;

    /**
     * 登录成功的首页数据
     * @param userid 登录人的id
     * @return 返回所需数据
     */
    @Override
    public Object init(Long userid) {

//        ftpUtil.store();
//        try {
//            InputStream in = new FileInputStream("C:/Users/Peng/Desktop/20181205211932_xvslr.jpeg");
//            ftpUtil.store(in,"/avatar/","hello.jpg");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        //上传，参数：输入流in、上传目标目录、上传文件名

        User user  = userDao.findOne(userid); //通过id查是否有此用户
        if (user == null){
            return "error";
        }

        //基本结构
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode root = mapper.createObjectNode(); //基本的结构，{消息代码、消息、实体对象}
        ObjectNode data = mapper.createObjectNode(); //实体对象，{账号主信息、分组信息、群}
        ObjectNode mine = mapper.createObjectNode(); //账号主信息，{账号持有人的id、头像、昵称签名等}
        ArrayNode friend = mapper.createArrayNode(); //分组信息，{分组名称、id、好友们}
        ArrayNode groupChat = mapper.createArrayNode();  //群

        //基本的结构，{消息代码、消息、实体对象}
        root.put("code","0");
        root.put("msg","");
        root.set("data",data);  //set用来设置对象，put用来设置属性

        //实体对象，{账号主信息、好友信息（包含分组信息）、群聊信息}
        data.set("mine",mine);
        data.set("friendDatas",friend);
        data.set("groupChat",groupChat);

        //账号主信息，{账号持有人的id、头像、昵称签名等}
        mine.put("username",user.getUsername());
        mine.put("id",user.getId());
        mine.put("avatar",user.getAvatar());
        mine.put("sign",user.getSign());

        //设置是否在线
        mine.put("status",redisReceiver.isUserOnline(userid)?"online":"offline");

        //分组信息，{分组名称、id、好友们}    一个账号中有多个分组，一个分组有多个好友
        List<Group> groupList = groupDao.findGroupsByUser(userid);  //分组的数组
        groupList.forEach(g->{  //遍历迭代
            ObjectNode gnode = mapper.createObjectNode();
            gnode.put("id",g.getId());
            gnode.put("groupname",g.getName());
            gnode.put("groupname",g.getName());

            //friend分组中的好友
            ArrayNode listNode = mapper.createArrayNode(); //集合，朋友的数组
            gnode.set("friends",listNode);
            g.getList().forEach(u -> {  //遍历迭代
                ObjectNode unode = mapper.createObjectNode();
                unode.put("username",u.getUsername());
                unode.put("id",u.getId());
                unode.put("avatar",u.getAvatar());
                unode.put("sign",u.getSign());

                //设置是否在线
                unode.put("status",redisReceiver.isUserOnline(u.getId())? "online":"offline");
                listNode.add(unode);
            });
            friend.add(gnode);
        });

        //群聊信息

        //第一部分，将自己创建的群聊加入列表，并标识是自己创建的（自己是群主）
        List<GroupChat> groupChatList1 = groupChatDao.findGroupChatsByUser(userid);
        groupChatList1.forEach(gc1->{
            ObjectNode gcnode = mapper.createObjectNode();
            gcnode.put("id",gc1.getId());
            gcnode.put("groupChatName",gc1.getName());
            gcnode.put("groupChatAvatar",gc1.getAvatar());
            gcnode.put("mine",true);
            groupChat.add(gcnode);
        });

        //第二部分，将自己所在的群聊加入列表，并标识不是自己创建的（别人是群主）
        List<GroupChat> groupChatList2 = groupChatDao.findGroupChatsWitchOwnMe(userid);
        groupChatList2.forEach(gc2->{
            if (!gc2.getUser().getId().equals(userid)){
                ObjectNode gcnode = mapper.createObjectNode();
                gcnode.put("id",gc2.getId());
                gcnode.put("groupChatName",gc2.getName());
                gcnode.put("groupChatAvatar",gc2.getAvatar());
                gcnode.put("mine",false);
                groupChat.add(gcnode);
            }
        });

        try {
            return mapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 创建用户
     * @param username 前台参数用户名
     * @param password 前台参数用户密码
     * @return 返回创建成功与否
     */
    @Override
    public Result createUser(String username, String password) {
        Result result = new Result();
        User u = userDao.findByUsername(username);
        if (u!=null){
            result.setCode(1);
            result.setMsg("昵称已被占用");
            return result;
        }
        User user = new User();
        user.setStatus("online");
        user.setCreated(new Date());
        user.setSign("默认签名");
        user.setUsername(username);
        //使用md5对密码进行加密，并保存在数据库中
        user.setPassword(DigestUtils.md5DigestAsHex(password.getBytes()));
        user.setPower("user");
        user.setIsdisable("no");
        //注册的默认头像
        user.setAvatar("http://"+host+"/avatar/defaultAvatar.png");
        userDao.save(user);
        result.setCode(0);
        result.setMsg("注册成功");

        //发送给管理员，给管理员增加一条用户信息（相当于刷新管理员页面的全部显示用户信息）
        redisReceiver.register();

//        result.setObject(user);
        return result;
    }

    /**
     * 添加好友
     * @param userId 登录人id
     * @param friendName 好友昵称
     * @return 返回成功与否
     */
    @Override
    public Result addFriend(int userId, String friendName) {
        Result result = new Result();
        User user  = userDao.findOne((long) userId);
        List<User> users = new ArrayList<>();
        User friend = userDao.findByUsername(friendName);
        if (friend==null){
            result.setCode(1);
            result.setMsg("啊哦，查无此人，请检查好友昵称~");
            result.setObject(null);
            return result;
        }
        Group group = new Group();
        if(user!=null){
            if (user.getId().equals(friend.getId())){
                result.setCode(1);
                result.setMsg("您不能添加自己为好友哦");
                result.setObject(null);
                return result;
            }
            users.add(friend);
            //检查好友列表（是否存在和是否已经是好友）
            List<Group> groups = groupDao.findGroupsByUser(userId); //得到本人的好友列表
            if (groups==null||groups.size()==0){  //本人没有好友列表
//                group = ;
                group.setCreated(new Date());
                group.setUser(user);
                group.setName("我的好友");
                groupDao.save(group);
                group.setList(users);
            }else {
                for (Group g : groups) { //遍历分组列表
                    List<User> users1 = g.getList();  //依次得到每个分组中的好友们
                    for (User u :users1) {
                        if (u.getId().equals(friend.getId())){
                            result.setCode(1);
                            result.setMsg("已经是好友，请勿重复添加");
                            result.setObject(null);
                            return result;
                        }
                    }
                }
                group = groups.get(0);
                List<User> users2 = group.getList();
                users2.add(friend);
                group.setList(users2);
            }
            groupDao.save(group);
            result.setCode(0);
            result.setMsg("添加成功");
            result.setObject(null);
        }else {
            result.setCode(1);
            result.setMsg("出现错误，请重新登录");
            result.setObject(null);
        }
        return result;
    }

    /**
     * 添加好友分组
     * @param userId 登录人id
     * @param friendGroupName 分组名称
     * @return 向前台返回成功与否
     */
    @Override
    public Result addFriendGroup(long userId, String friendGroupName) {
        Result result = new Result();
        User user = userDao.findOne(userId);
        Group group = groupDao.findGroupByNameAndUserId(friendGroupName,userId);
        if(user!=null){
            if (group==null){
                Group group1 = new Group();
                group1.setName(friendGroupName);
                group1.setUser(user);
                group1.setCreated(new Date());
                groupDao.save(group1);
                result.setCode(0);
                result.setMsg("添加成功");
                result.setObject(null);
            }else {
                result.setCode(1);
                result.setMsg("此好友分组已存在");
                result.setObject(null);
            }
        }else{
            result.setCode(1);
            result.setMsg("出现错误，请重新登录");
            result.setObject(null);
        }
        return result;
    }


    /**
     * 搜索用户名下的指定好友
     * @param userId 登录用户id
     * @param friendName 好友昵称
     * @return 返回好友或者失败代码信息
     */
    @Override
    public Result findFriend(long userId, String friendName) {

        Result result = new Result();
        result.setCode(1);
        result.setMsg("出现错误，请重试。");

        User userMine = userDao.findOne(userId);
        User userFriend = userDao.findByUsername(friendName);

        ObjectMapper mapper = new ObjectMapper();  //基本结构：好友信息，好友所在分组
        ObjectNode root = mapper.createObjectNode();  //基本的结构，{我的id、好友id、好友名称、好友签名、好友头像、好友创建时间、好友分组}

        if (userMine!=null){
            if (userFriend!=null){
                List<Group> groups = groupDao.findGroupsByUser(userId);
                if (groups!=null&&groups.size()>0) {
                    for (Group mineGroup : groups) {
                        List<User> friends = mineGroup.getList();
                        if (friends!=null&&friends.size()>0){
                            for (User friend : friends) {
                                if (friend.getUsername().equals(friendName)){
                                    root.put("mineId",userId);
                                    root.put("friendID",userFriend.getId());
                                    root.put("friendName",userFriend.getUsername());
                                    root.put("friendSign",userFriend.getSign());
                                    root.put("friendAvatar",userFriend.getAvatar());
                                    root.put("friendCreated",userFriend.getCreated().getTime());
                                    root.put("friendGroupName",mineGroup.getName());
                                    result.setCode(0);
                                    result.setMsg("Success");
                                    result.setObject(root);
                                }
                            }
                        }
                    }
                }
            }
        }

        return result;
    }


    /**
     * 更改好友分组
     * @param mineId 登录人的id
     * @param friendName 好友昵称
     * @param groupName 要更改的分组名称
     * @return 返回是否成功
     */
    @Override
    public Result changeFriendGroup(long mineId, String friendName, String groupName) {
        Result result = new Result();
        result.setCode(1);
        result.setMsg("出现了错误，请重试！");
        User mineUser = userDao.findOne(mineId);
        User friendUser = userDao.findByUsername(friendName);
        boolean del = false;
        boolean add = false;
        if (mineUser!=null){
            if(friendUser!=null){

                //第一步，删除原好友分组中的该好友
                List<Group> groups = groupDao.findGroupsByUser(mineId);
                if (groups!=null&&groups.size()>0){
                    for (Group group : groups) {
                        List<User> users = group.getList();
                        if (users!=null&&users.size()>0){ //存在好友
                            for (User user : users) {
                                if (user.getUsername().equals(friendName)){
                                    users.remove(user);
                                    group.setList(users);
                                    groupDao.save(group);
                                    del = true;
                                    break;
                                }
                            }
                        }else {  //不存在好友
                            del = true;
                        }
                    }
                    //第二步，往新的好友分组中添加该好友
                    List<Group> groups1 = groupDao.findGroupsByUser(mineId);
                    if (groups1!=null&&groups1.size()>0){
                        for (Group g : groups1) {
                            if (g.getName().equals(groupName)){
                                List<User> users = g.getList();
                                users.add(friendUser);
                                g.setList(users);
                                groupDao.save(g);
                                add = true;
                            }
                        }
                    }
                }
            }
        }

        if (del&&add){
            result.setCode(0);
            result.setMsg("更改成功");
        }

        return result;
    }


    /**
     * 从好友列表中删除好友
     * @param mineId 登录人id
     * @param friendName 好友昵称
     * @return 返回是否成功
     */
    @Override
    public Result delFriend(long mineId, String friendName) {
        Result result = new Result();
        result.setCode(1);
        result.setMsg("出现了错误，请重试！");
        User mineUser = userDao.findOne(mineId);
        User friendUser = userDao.findByUsername(friendName);
        if (mineUser!=null) {
            if (friendUser != null) {
                List<Group> groups = groupDao.findGroupsByUser(mineId);
                if (groups != null && groups.size() > 0) {
                    for (Group group : groups) {
                        List<User> users = group.getList();
                        if (users != null && users.size() > 0) { //存在好友
                            for (User user : users) {
                                if (user.getUsername().equals(friendName)) {
                                    users.remove(user);
                                    group.setList(users);
                                    groupDao.save(group);
                                    result.setCode(0);
                                    result.setMsg("删除成功");
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * 头相上传
     * @param img 头像文件
     * @param mineId 登录人id
     * @return 返回是否成功
     */
    @Override
    public Result upLoad(MultipartFile img,long mineId) {
        //文件上传到ftp服务器上
        User user = userDao.findOne(mineId);
        Result result = new Result();
        result.setCode(1);
        result.setMsg("上传失败，请重试");
        if (user == null) {
            return result;
        } else {
            String name = img.getOriginalFilename();  //文件上传时的原始名称
            String lastName = name.substring(name.lastIndexOf("."));
            String fileName = UUID.randomUUID().toString().replace("-", "") + lastName;
            System.out.println("上传的文件名称为："+fileName);
            try {
                ftpUtil.store(img.getInputStream(), "/avatar/", fileName);
                String oldAvatar = user.getAvatar();
                if (oldAvatar.equals("http://" + host + "/avatar/defaultAvatar.png")||oldAvatar.equals("http://" + host + "/avatar/testAvatar.jpg")){
                    user.setAvatar("http://" + host + "/avatar/" + fileName);
                }else {
                    String deletePath = deletePath("/",oldAvatar,3);//获取删除的文件地址
                    ftpUtil.delete(deletePath);  //为了减少图片服务器端的硬盘使用量，将旧图删除
                    user.setAvatar("http://" + host + "/avatar/" + fileName);
                }
                userDao.save(user);
                result.setCode(0);
                result.setMsg("更改成功");
                return result;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }
    }

    /**
     * 新建群聊
     * @param userId 登录人id
     * @param groupChatName 群聊名称
     * @return 返回是否成功
     */
    @Override
    public Result addGroupChat(long userId, String groupChatName) {  //添加群聊
        Result result = new Result();
        User user = userDao.findOne(userId);
//        GroupChat groupChat = groupChatDao.findGroupChatByNameAndUserId(groupChatName,userId);
        if(user!=null){

            //默认首次创建群聊的时候将自己添加进去（群主），群主有修改群聊名称的权限。
            List<User> userList = new ArrayList<>();
            userList.add(user);

            GroupChat groupChat1 = new GroupChat();
            groupChat1.setName(groupChatName);
            groupChat1.setUser(user);
            groupChat1.setAvatar("http://" + host + "/avatar/defaultGroupChatAvatar.png");
            groupChat1.setCreated(new Date());

            //默认首次创建群聊的时候将自己添加进去（群主），群主有修改群聊名称的权限。
            groupChat1.setList(userList);
            groupChatDao.save(groupChat1);
            result.setCode(0);
            result.setMsg("添加成功");
            result.setObject(null);
        }else{
            result.setCode(1);
            result.setMsg("出现错误，请重新登录");
            result.setObject(null);
        }
        return result;
    }

    /**
     * 查找群聊
     * @param groupChatId 群聊id
     * @return 返回群聊或者失败代码
     */
    @Override
    public Result findGroupChat(long groupChatId) {
        Result result = new Result();
        GroupChat groupChat = groupChatDao.findOne(groupChatId);
        if (groupChat!=null){
            result.setCode(0);
            result.setObject(groupChat);
        }else {
            result.setCode(1);
            result.setMsg("出现错误，请重新登录");
            result.setObject(null);
        }
        return result;
    }

    /**
     * 获取群聊中的成员
     * @param groupChatId 群聊id
     * @return 返回群员或者失败代码
     */
    @Override
    public Object getGroupChatMember(Long groupChatId) {
        GroupChat groupChat = groupChatDao.findOne(groupChatId);
        if (groupChat==null){
            return "error";
        }
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode(); //基本根结构
//        ObjectNode member = mapper.createObjectNode();
        ArrayNode members = mapper.createArrayNode();  //群聊中存在的成员
        root.put("code",0);
        root.put("msg","");
        root.set("members",members);

        List<User> userList = groupChatDao.findUsersByGroupChatId(groupChatId);
        userList.forEach(u->{
            ObjectNode gnode = mapper.createObjectNode();
            gnode.put("id",u.getId());
            gnode.put("avatar",u.getAvatar());
            gnode.put("username",u.getUsername());
            gnode.put("sign",u.getSign());
            gnode.put("status",redisReceiver.isUserOnline(u.getId())?"online":"offline");
            members.add(gnode);
        });

        try {
            return mapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取当前登录人的所有好友
     * @param userId 登录人id
     * @return 返回好友或错误代码
     */
    @Override
    public Object getFriends(Long userId) {
        User user = userDao.findOne(userId);
        if (user==null){
            return "error";
        }
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode(); //基本根结构
//        ObjectNode member = mapper.createObjectNode();
        ArrayNode friends = mapper.createArrayNode();  //群聊中存在的成员
        root.put("code",0);
        root.put("msg","");
        root.set("members",friends);

        List<Group> groupList = groupDao.findGroupsByUser(userId);
        groupList.forEach(g->{
            g.getList().forEach(u->{
                ObjectNode gnode = mapper.createObjectNode();
                gnode.put("id",u.getId());
                gnode.put("avatar",u.getAvatar());
                gnode.put("username",u.getUsername());
                gnode.put("sign",u.getSign());
                gnode.put("status",redisReceiver.isUserOnline(u.getId())?"online":"offline");
                friends.add(gnode);
            });

        });

        try {
            return mapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Result getFriendGroupsInfo(Long userId) {
        Result result = new Result();
        List<Group> groupList = groupDao.findGroupsByUser(userId);
        if (groupList!=null&&groupList.size()>0){
            result.setCode(0);
            result.setMsg("获取成功");
            result.setObject(groupList);
        }else {
            result.setCode(1);
            result.setMsg("获取用户好友列表失败，请返回重试");
        }
        return result;
    }

    @Override
    public Result changeFriendGroupName(long userId, long friendGroupId, String friendGroupName) {
        Result result = new Result();
        Group group = groupDao.findOne(friendGroupId);
        if (group==null){
            result.setCode(1);
            result.setMsg("获取好友分组失败，请重试");
        }else {
            group.setName(friendGroupName);
            groupDao.save(group);
            List<Group> groupList = groupDao.findGroupsByUser(userId);
            if (groupList!=null&&groupList.size()>0){
                result.setCode(0);
                result.setMsg("修改成功");
                result.setObject(groupList);
            }else {
                result.setCode(1);
                result.setMsg("修改成功,但获取好友列表失败，请返回查看");
            }
        }

        return result;
    }

    @Override
    public Result exitGroupChat(long userId, long groupChatId) {
        Result result = new Result();
        result.setCode(1);
        result.setMsg("出现了错误，请重试！");
        GroupChat groupChat = groupChatDao.findOne(groupChatId);
        if (groupChat!=null){
            List<User> userList = groupChat.getList();
            if (userList!=null&&userList.size()>0){
                for (User u :userList) {
                    if (u.getId().equals(userId)){
                        userList.remove(u);
                        groupChat.setList(userList);
                        groupChatDao.save(groupChat);
                        result.setCode(0);
                        result.setMsg("已退出群聊");
                        break;
                    }
                }
            }
        }
        return result;
    }

    /**
     * 向群聊中添加新好友
     * @param groupChatId 群聊id
     * @param friendId 好友id
     * @return 返回成功与否
     */
    @Override
    public Result addNewMember(long groupChatId, long friendId) {
        Result result = new Result();
        result.setCode(1);
        result.setMsg("出现了错误，请重试");
        GroupChat groupChat = groupChatDao.findOne(groupChatId);
        User user = userDao.findOne(friendId);
        if (groupChat!=null&&user!=null){

            List<User> userList = groupChat.getList();
            boolean isExist = false;
            for (User u : userList) {
                if (u.getId().equals(friendId)){
                    isExist = true;
                }
            }
            if (!isExist) {
                userList.add(user);
                groupChat.setList(userList);
                groupChatDao.save(groupChat);
                result.setCode(0);
                result.setMsg("添加成功");
            }else {
                result.setCode(1);
                result.setMsg("该好友已经是群成员，不可重复添加。");
            }
        }
        return result;
    }

    @Override
    public Object friendByUsernameOrId(String username,long userId) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ObjectNode friend = mapper.createObjectNode();
        root.put("code",0);
        root.put("msg","获得信息成功");
        root.set("friend",friend);
        User user;
        if (userId==0){
            user = userDao.findByUsername(username);
        }else{
            user = userDao.findOne(userId);
        }
        if (user!=null){
            if (user.getPower().equals("user")){
                friend.put("id",user.getId());
                friend.put("username",user.getUsername());
                friend.put("avatar",user.getAvatar());
                friend.put("sign",user.getSign());
                friend.put("created",user.getCreated().getTime());
                friend.put("online",redisReceiver.isUserOnline(user.getId()));  //在线true，离线false
                friend.put("isdisable",user.getIsdisable());
            }
        }
        try {
            return mapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 获取要删除的文件目录
     * @param str 子字符串“/”
     * @param fullStr 完整字符串（旧头像完整地址，例子：http://192.168.2.136/avatar/7e8c9279784c4767883d26b4bd1d7486.jpg）
     * @param i 第三次出现
     * @return 删除的文件目录
     */
    public String deletePath(String str,String fullStr,int i){
        Matcher slashMatcher = Pattern.compile(str).matcher(fullStr);
        int mIdx = 0;
        while (slashMatcher.find()) {
            mIdx++;
            //当"/"符号第三次出现的位置
            if (mIdx == i) {
                break;
            }
        }
        int index = slashMatcher.start();
        String deletePath = fullStr.substring(index+1);
        System.out.println("清除旧头像文件："+deletePath);
        return deletePath;
    }

//    public static void main(String[] args) throws Exception {
//        FtpPoolConfig config=new FtpPoolConfig();
//        config.setHost("39.98.229.7");
//        config.setPort(21);
//        config.setUsername("ftpuser");
//        config.setPassword("123456");
//        FTPClientFactory clientFactory=new FTPClientFactory(config); //对象工厂
//        FTPClientPool pool = new FTPClientPool(clientFactory); //连接池对象
//        FtpClientUtils ftp = new FtpClientUtils(pool); //工具对象
//        int i = ftp.store(new File("C:/Users/15160/Desktop/123.jpg"), "/avatar/", "test.jpg");
////        String str = "http://192.168.2.136/avatar/7e8c9279784c4767883d26b4bd1d7486.jpg";
////        Matcher slashMatcher = Pattern.compile("/").matcher(str);
////        int mIdx = 0;
////        while (slashMatcher.find()) {
////            mIdx++;
////            //当"/"符号第三次出现的位置
////            if (mIdx == 3) {
////                break;
////            }
////        }
////        int index = slashMatcher.start();
////        System.out.println(str.substring(index+1));
////        System.out.println(ftp.delete("avatar/3b59a4cc16e24384a4e8d698dec68c6e.jpg"));
////        ftp.
//        System.out.println(i);
//
//    }

//    public static void main(String[] args) {
//        //http://192.168.2.136/avatar/7e8c9279784c4767883d26b4bd1d7486.jpg
//        FtpClientUtils ftpUtil
//        ftpUtil.delete(oldAvatar);
//    }

}
