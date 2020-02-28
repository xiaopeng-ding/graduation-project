package com.peng.ding.controller;

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
import org.springframework.stereotype.Controller;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserDao userDao;

    @Autowired
    private GroupDao groupDao;

    @Autowired
    private GroupChatDao groupChatDao;

    @Autowired
    private RedisReceiver redisReceiver;

    @GetMapping("/getUser")
    @ResponseBody
    public Object init(Long userId){
        return userService.init(userId);
    }

    @GetMapping("/statusTell")
    @ResponseBody
    public Object statusTell(Long userId){
        return userDao.findWhoOwnedMe(userId);
    }


    @PostMapping("/register")
    @ResponseBody
    public Object createUser(String username, String password){
        return userService.createUser(username,password);
    }

    @PostMapping("/addFriend")
    @ResponseBody
    public Result addFriend(int userId, String friendName){
        return userService.addFriend(userId,friendName);
    }

    @PostMapping("/user")
    @ResponseBody
    public Result getUserByName(String username){
        Result result = new Result();
        User user = userDao.findByUsername(username);
        if (user!=null){
            result.setCode(0);
            result.setMsg("success");
            result.setObject(user);
        }else {
            result.setCode(1);
            result.setMsg("failed");
            result.setObject(null);
        }
        return result;
    }

    @PostMapping("/getUserById")
    @ResponseBody
    public Result getUserById(long userId){
        Result result = new Result();
        User user = userDao.findOne(userId);
        if (user!=null){
            result.setCode(0);
            result.setMsg("success");
            result.setObject(user);
        }else {
            result.setCode(1);
            result.setMsg("failed");
            result.setObject(null);
        }
        return result;
    }

    /**
     * 修改个人信息
     * @param userId
     * @param username
     * @param sign
     * @return
     */
    @PostMapping("/changeInfo")
    @ResponseBody
    public Result changeInfo(long userId,String username, String sign){
        Result result = new Result();
        //检查新的用户名是否重名
        User checkUser = userDao.findByUsername(username);
        User user = userDao.findOne(userId);
        if (user==null){
            result.setCode(1);
            result.setMsg("发生错误，无法获取当前登录人，请重新登录。");
        }else {
            user.setUsername(username);
            user.setSign(sign);
            if (checkUser == null){  //新的用户名在数据库中不存在，直接更新
                userDao.save(user);
                result.setCode(0);
                result.setMsg("修改成功");
                User userChanged = userDao.findByUsername(user.getUsername());
                result.setObject(userChanged);

                //修改成功后通过redisReceiver给客户端发生消息
                redisReceiver.changeInfoStatus(user.getId(),
                        "updateInfo",
                        user.getId(),
                        user.getUsername(),
                        user.getSign());

            }else{  //新的用户名在数据库中存在，判断如果是自己（没有修改用户名），更新，不是自己（重名），修改失败
                long checkId = checkUser.getId();
                if (checkId == userId){  //如果是自己的话，则两者的id是一样的
                    userDao.save(user);
                    result.setCode(0);
                    result.setMsg("修改成功");
                    User userChanged = userDao.findByUsername(user.getUsername());
                    result.setObject(userChanged);
                    //修改成功后通过redisReceiver给客户端发生消息
                    redisReceiver.changeInfoStatus(user.getId(),
                            "updateInfo",
                            user.getId(),
                            user.getUsername(),
                            user.getSign());
                }else{  //如果是不是自己的话，则两者的id是不同的
                    result.setCode(1);
                    result.setMsg("修改失败，用户名已存在");
                }
            }
        }
        return result;
    }

    @PostMapping("/changePassword")
    @ResponseBody
    public Result changePassword(long userId,String oldPassword,String password){
        Result result = new Result();
        User user = userDao.findOne(userId);
        //使用md5对前台传过来的密码进行加密
        String oldPasswd = DigestUtils.md5DigestAsHex(oldPassword.getBytes());
        String passwd = DigestUtils.md5DigestAsHex(password.getBytes());
        if (user==null){
            result.setCode(1);
            result.setMsg("发生错误，无法获取当前登录人，请重新登录。");
        }else {
            if (oldPassword.equals(passwd)){
                result.setCode(1);
                result.setMsg("新旧密码相同，请重试");
            }else if (user.getPassword().equals(oldPasswd)) {
                //将密文密码存储在数据库中
                user.setPassword(passwd);
                userDao.save(user);
                result.setCode(0);
                result.setMsg("修改成功，请重新登录");
            }else {
                result.setCode(1);
                result.setMsg("旧密码错误，请重试");
            }
        }
        return result;
    }

    @PostMapping("/addFriendGroup")
    @ResponseBody
    public Result addFriendGroup(long userId,String friendGroupName){
        return userService.addFriendGroup(userId,friendGroupName);
    }

    @PostMapping("/findFriendInfo")
    @ResponseBody
    public Result findFriendInfo(long mineId,String friendName){
        return userService.findFriend(mineId,friendName);
    }

    @PostMapping("/findFriendGroups")
    @ResponseBody
    public Result findFriendGroups(long mineId){
        Result result = new Result();
        result.setCode(1);
        result.setMsg("没有获取到好友分组信息，请重试");
        List<Group> groups = groupDao.findGroupsByUser(mineId);
        if (groups!=null&&groups.size()>0){
            result.setCode(0);
            result.setMsg("success");
            result.setObject(groups);
        }
        return result;
    }

    @PostMapping("/changeFriendGroup")
    @ResponseBody
    public Result changeFriendGroup(long mineId,String friendName,String groupName){
        return userService.changeFriendGroup(mineId,friendName,groupName);
    }

    @PostMapping("/delFriend")
    @ResponseBody
    public Result delFriend(long mineId,String friendName){
        return userService.delFriend(mineId,friendName);
    }


    @PostMapping("/avatarUpload")
    @ResponseBody
    public Result avatarUpload(MultipartFile img,long mineId){
//        System.out.println(img);
//        System.out.println(mineId);
        return userService.upLoad(img,mineId);
    }

    @PostMapping("/addGroupChat")
    @ResponseBody
    public Result addGroupChat(long userId,String groupChatName){
        return userService.addGroupChat(userId,groupChatName);
    }

    @PostMapping("/findGroupChat")
    @ResponseBody
    public Result findGroupChat(long groupChatId){
        return userService.findGroupChat(groupChatId);
    }

    @PostMapping("/changeGroupChatInfo")
    @ResponseBody
    public Result changeGroupChatInfo(long groupChatId,String groupChatName){
        Result result = new Result();
        GroupChat groupChat = groupChatDao.findOne(groupChatId);
        if (groupChat!=null){
            groupChat.setName(groupChatName);
            groupChatDao.save(groupChat);
            result.setCode(0);
            result.setMsg("修改成功");
        }else {
            result.setCode(1);
            result.setMsg("修改失败……没有找到群聊信息，请重新登录后重试");
        }
        return userService.findGroupChat(groupChatId);
    }

    @GetMapping("/getGroupChatMember")
    @ResponseBody
    public Object getGroupChatMember(Long groupChatId){
        return userService.getGroupChatMember(groupChatId);
    }

    @GetMapping("/getFriends")
    @ResponseBody
    public Object getFriends(Long userId){
        return userService.getFriends(userId);
    }

    @PostMapping("/addNewMember")
    @ResponseBody
    public Result addNewMember(long groupChatId,long friendId){
        return userService.addNewMember(groupChatId,friendId);
    }

    @GetMapping("/getFriendGroupsInfo")
    @ResponseBody
    public Result getFriendGroupsInfo(Long userId){
        return userService.getFriendGroupsInfo(userId);
    }

    @PostMapping("/changeFriendGroupName")
    @ResponseBody
    public Result changeFriendGroupName(long userId,long friendGroupId,String friendGroupName){
        return userService.changeFriendGroupName(userId,friendGroupId,friendGroupName);
    }

    @PostMapping("/exitGroupChat")
    @ResponseBody
    public Result exitGroupChat(long userId,long groupChatId){
        return userService.exitGroupChat(userId,groupChatId);
    }

    @GetMapping("/friendByUsernameOrId")
    @ResponseBody
    public Object friendByUsernameOrId(String username,long userId){
        return userService.friendByUsernameOrId(username,userId);
    }

}
