package com.peng.ding.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.peng.ding.dao.GroupChatDao;
import com.peng.ding.dao.UserDao;
import com.peng.ding.pojo.GroupChat;
import com.peng.ding.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.websocket.Session;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Component
public class RedisReceiver {



    public static class SessionInfo{  //静态内部类，用来绑定userid和WebSocketSession
        public Session session;
        public Long userid;
        public SessionInfo(Session session, Long userid) {
            this.session = session;
            this.userid = userid;
        }

    }

    public List<SessionInfo> list = new LinkedList<>();

    ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    private UserDao userDao;

    @Autowired
    private GroupChatDao groupChatDao;


    /**
     * 有人上线的时候的处理逻辑
     * @param session
     * @param userId
     */
    public void onOpen(Session session, Long userId){
        if(isUserOnline(userId)){
                //1.向登录的那个人和准备登录的人推送已经在线的信息
                //2.将已经登录的人强制下线，准备登录的人登录失败
            ObjectMapper sendStatus = new ObjectMapper();
            ObjectNode data = sendStatus.createObjectNode();
            data.put("type","forceOffline");
            data.put("message","您的账号在其他地方登录，为了您的安全请重新登录并修改密码");
            try {
                send(session, sendStatus.writeValueAsString(data));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            for (SessionInfo info : list) {
                if (info.userid.equals(userId)) {
                    try {
//                        onClose(info.session);  //下线
                        send(info.session, sendStatus.writeValueAsString(data));
                        stringRedisTemplate.opsForValue().setBit("online",info.userid,false);
                        list.remove(info);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                }
            }
//            return;
        }else {
            synchronized (list) {
                list.add(new SessionInfo(session, userId));
                System.out.println("有人上线了，目前在线人数：" + list.size());
                stringRedisTemplate.opsForValue().setBit("online", userId, true); // 设定在线状态(上线)
                //通知所有列表中有userId的用户，此人已下线
                changeOnlineStatus(userId, "online");
            }
        }
    }

    /**
     * 有人下线的处理逻辑
     * @param session
     */
    public void onClose(Session session){
        synchronized (list) {
            for (int i=0;i<list.size();i++){
                SessionInfo info = list.get(i);
                if (info.session.getId().equals(session.getId())) {
                    stringRedisTemplate.opsForValue().setBit("online",info.userid,false); // 设定在线状态(下线)
                    //通知所有列表中有userId的用户，此人已下线
                    changeOnlineStatus(info.userid,"offline");
                    list.remove(info);
//                    break;  //删除后停止循环遍历
                }
            }
            System.out.println("有人下线了，目前在线人数："+list.size());
        }
    }

    /**
     * //通知所有列表中有userId的用户，此人已下线(上线)
     * @param userId  userId
     * @param status 下线(上线)
     */
    public void changeOnlineStatus(long userId,String status){
        List<User> userList = userDao.findWhoOwnedMe(userId);
        User admin = userDao.findUsersByPower("admin");
        userList.add(admin);

//        userDao.findWhoOwnedMe(userId).forEach(user -> {  //遍历有此人的用户
        userList.forEach(user -> {  //遍历有此人的用户（包含管理员）
            //找到在线用户中有此人的用户
            if(!isUserOnline(user.getId())){
                return;  //结束当前一次的操作
            }
            for (SessionInfo si : list) {
                if (si.userid.equals(user.getId())) {
                    ObjectMapper sendStatus = new ObjectMapper();
                    ObjectNode data = sendStatus.createObjectNode();
                    data.put("type","changeOnlineStatus");
                    data.put("status", status);
                    data.put("userId",userId);
                    try {
                        send(si.session, sendStatus.writeValueAsString(data));
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }


    /**
     * //通知所有列表中有userId的用户，此人已修改个人信息
     * @param userId
     * @param type
     * @param mineId
     * @param mineName
     * @param sign
     */
    public void changeInfoStatus(long userId,String type,long mineId,String mineName,String sign){
        List<User> userList = userDao.findWhoOwnedMe(userId);
        User admin = userDao.findUsersByPower("admin");
        userList.add(admin);
//        userDao.findWhoOwnedMe(userId).forEach(user -> {  //遍历有此人的用户
        userList.forEach(user -> {  //遍历有此人的用户（包含管理员）
            //找到在线用户中有此人的用户
            if(!isUserOnline(user.getId())){
                return;  //结束当前一次的操作
            }
            for (SessionInfo si : list) {
                if (si.userid.equals(user.getId())) {
                    ObjectMapper sendStatus = new ObjectMapper();
                    ObjectNode data = sendStatus.createObjectNode();
                    data.put("type", type);
                    data.put("userId",userId);
                    data.put("mineId",mineId);
                    data.put("mineName",mineName);
                    data.put("sign",sign);
                    try {
                        send(si.session, sendStatus.writeValueAsString(data));
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void warn(long userId) {
        for (SessionInfo si: list) {
            if (si.userid.equals(userId)){
                ObjectMapper sendWarning = new ObjectMapper();
                ObjectNode data = sendWarning.createObjectNode();
                data.put("type","warn");
                data.put("message","请规范操作，否则将对您进行强制下线和封禁操作。");
                try {
                    send(si.session, sendWarning.writeValueAsString(data));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void forceOffline(long userId) {

        ObjectMapper sendStatus = new ObjectMapper();
        ObjectNode data = sendStatus.createObjectNode();
        data.put("type","forceOffline");
        data.put("message","您违规次数过多，已被强制下线，如无视强制措施，将被封号。");
        for (SessionInfo info : list) {
            if (info.userid.equals(userId)) {
                try {
//                        onClose(info.session);  //下线
                    send(info.session, sendStatus.writeValueAsString(data));
                    stringRedisTemplate.opsForValue().setBit("online",info.userid,false);
//                    list.remove(info);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void sendAnn(String str) {
        ObjectMapper sendStatus = new ObjectMapper();
        ObjectNode data = sendStatus.createObjectNode();
        data.put("type","ann");
        data.put("message",str);
        for (SessionInfo info : list) {
            try {
                send(info.session, sendStatus.writeValueAsString(data));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }


    public void register() {
        ObjectMapper sendRegister = new ObjectMapper();
        ObjectNode data = sendRegister.createObjectNode();
        ArrayNode users = sendRegister.createArrayNode();
        List<User> userList = userDao.findAllByPower("user");
        userList.forEach(u->{
            ObjectNode unode = sendRegister.createObjectNode();
            unode.put("id",u.getId());
            unode.put("username",u.getUsername());
            unode.put("sign",u.getSign());
            unode.put("created",u.getCreated().getTime());
            unode.put("isdisable",u.getIsdisable());
            unode.put("status",isUserOnline(u.getId())?"online":"offline");
            users.add(unode);
        });

        data.put("type","register");
        data.put("message","有新的用户");
        data.set("users",users);

        long adminId = userDao.findUsersByPower("admin").getId();
        for (SessionInfo si:list) {
            if (si.userid.equals(adminId)){
                try {
                    send(si.session, sendRegister.writeValueAsString(data));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
//        data.set();
    }


    /**
     * 判断用户是否在线
     * @param userId
     * @return
     */
    public boolean isUserOnline(long userId){
        return stringRedisTemplate.opsForValue().getBit("online",userId);
    }


    public void onMessage(String message,Session session){
//        System.out.println("来自客户端的消息："+message);
        try {
            stringRedisTemplate.convertAndSend("chat",message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 消息接的方法
     * @param message
     */
    public void receiveMessage(String message){
        System.out.println("接收到一条消息"+message);

        try {
            JsonNode root = objectMapper.readTree(message);
            String type = root.get("type").asText();
            if (type.equals("friend")){
                long to = root.get("friend").get("friendId").asLong();
                synchronized (list){  //list同步
                    list.forEach(info -> {
                        if (info.userid==to){
                            send(info.session,message);
                        }
                    });
                }
            }else if(type.equals("groupChat")){
                long groupChatId = root.get("groupChat").get("groupChatId").asLong();
                long from = root.get("from").asLong();
                GroupChat groupChat = groupChatDao.findOne(groupChatId);
                if (groupChat!=null){
                    //遍历群中的好友
                    groupChatDao.findUsersByGroupChatId(groupChatId).forEach(u->{
                        //找到此群聊中的在线用户
                        if (!isUserOnline(u.getId())){
                            return;
                        }
                        for (SessionInfo si :list) {
                            if (si.userid.equals(u.getId())&&from!=u.getId()){
                                send(si.session,message);
                            }
                        }
                    });
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void send(Session session,String message){
        System.out.println("发送给客户端的消息："+message);
        try {
            synchronized(session){
                session.getBasicRemote().sendText(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
