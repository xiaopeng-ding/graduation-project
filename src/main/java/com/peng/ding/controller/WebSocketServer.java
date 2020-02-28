package com.peng.ding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peng.ding.dao.UserDao;
import com.peng.ding.util.RedisReceiver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

/**
 * 本次系统核心组件webSocket
 */
@EnableScheduling
@ServerEndpoint(value = "/websocket/{userid}")
@Component
public class WebSocketServer {

//    public static List<SessionInfo> list = new LinkedList<>();

    @Autowired
    private UserDao userDao;


    static RedisReceiver redisReceiver;
    @Autowired
    void setRedisReceiver(RedisReceiver redisReceiver){
        WebSocketServer.redisReceiver = redisReceiver;
    }

////    @Autowired
//    private static StringRedisTemplate stringRedisTemplate;
//
//    @Resource(name = "stringRedisTemplate")
//    public void setStringRedisTemplate(StringRedisTemplate srt){
//        stringRedisTemplate=srt;
//    }
//
//    public static class SessionInfo{  //静态内部类，用来绑定userid和WebSocketSession
//        public Session session;
//        public Long userid;
//
//        public SessionInfo(Session session, Long userid) {
//            this.session = session;
//            this.userid = userid;
//
//        }
//
//    }

//    ObjectMapper objectMapper = new ObjectMapper();

    //静态变量，记录当前在线链接数
//    private static int onlineCount = 0;
//
//    private static CopyOnWriteArraySet<WebSocketServer> webSocketSet = new CopyOnWriteArraySet<WebSocketServer>();
//
//    private Session session;

    /**
     * webSocket
     * @param userid
     * @param session
     */
    @OnOpen
    public void onOpen(@PathParam("userid") Long userid, Session session){  //@@PathParam("userid") 里边的userid必须写
//        SessionInfo info = new SessionInfo(session,userid);
//        synchronized (list){
//            list.add(info);
//        }
//        list.add(session);
        redisReceiver.onOpen(session,userid);
//        System.out.println("有人上线了，目前在线人数："+list.size());
    }

    /**
     * @param session
     */
    @OnClose
    public void onClose(Session session){
//        synchronized (list) {
//            Iterator<SessionInfo> it = list.iterator();
//            while (it.hasNext()) {
//                SessionInfo info = it.next();
//                if (info.session.getId().equals(session.getId())) {
//                    list.remove(info);
//                    break;  //删除后停止循环遍历
//                }
//            }
//        }
//        System.out.println("有人下线了，目前在线人数："+list.size());
        redisReceiver.onClose(session);
    }

    @OnMessage
    public void onMessage(String message,Session session){
        System.out.println("来自客户端的消息："+message);
//        try {
//
//            JsonNode root = objectMapper.readTree(message);
//            long to = root.get("friend").get("friendId").asLong();
//            long from = root.get("mine").get("mineId").asLong();
//            ObjectNode data = (ObjectNode) root.get("data");
//            data.put("timestamp",new Date().getTime());  //拼凑时间戳
//            String str = objectMapper.writeValueAsString(root);
//            list.forEach(info -> {
//                if (info.userid==to){
//                    send(info.session,message);
//                }
//                if(info.userid==from){
//                    send(session,message);
//                }
//            });
//            stringRedisTemplate.convertAndSend("chat",message);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        redisReceiver.onMessage(message,session);
    }

//    public static void send(Session session,String message){
//        System.out.println("发送给客户端的消息："+message);
//        try {
//            synchronized(session){
//                session.getBasicRemote().sendText(message);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    @OnError
    public void onError(Session session,Throwable error){
        System.out.println("发生错误");
        error.printStackTrace();
    }

//    @Scheduled(fixedRate = 2000)
//    public void send(){
//        list.forEach(session -> {
//            try {
//                session.getBasicRemote().sendText("helloClient,I am server,"+ UUID.randomUUID().toString());
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        });
//    }
}
