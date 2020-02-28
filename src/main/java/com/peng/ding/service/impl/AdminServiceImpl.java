package com.peng.ding.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.peng.ding.dao.UserDao;
import com.peng.ding.pojo.Result;
import com.peng.ding.pojo.User;
import com.peng.ding.service.AdminService;
import com.peng.ding.util.RedisReceiver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.List;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private RedisReceiver redisReceiver;

    @Override
    public Object getUsers() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode(); //基本根结构
        ArrayNode users = mapper.createArrayNode();
        root.put("code",0);
        root.put("msg","");
        root.set("users",users);
        List<User> userList = userDao.findAllByPower("user");
        userList.forEach(u->{
            ObjectNode unode = mapper.createObjectNode();
            unode.put("id",u.getId());
            unode.put("username",u.getUsername());
            unode.put("sign",u.getSign());
            unode.put("created",u.getCreated().getTime());
            unode.put("isdisable",u.getIsdisable());
            unode.put("status",redisReceiver.isUserOnline(u.getId())?"online":"offline");
            users.add(unode);
        });
        try {
            return mapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Result warn(long userId) {
        Result result = new Result();
        redisReceiver.warn(userId);
        result.setCode(0);
        result.setMsg("已将警告信息发送给此用户。");
        return result;
    }

    @Override
    public Result forceOffline(long userId) {
        Result result = new Result();
        redisReceiver.forceOffline(userId);
        result.setCode(0);
        result.setMsg("已将该用户强制下线。");
        return result;
    }

    @Override
    public Result isDisable(long userId,boolean isdisable) {
        Result result = new Result();
        User user = userDao.findOne(userId);
        if (user!=null){
            if (isdisable){  //ture 表示正常状态，也就是没有封号
                user.setIsdisable("no");
                result.setMsg("该用户已解封");
            }else {
                user.setIsdisable("yes");
                result.setMsg("该用户已被封禁");
            }
            userDao.save(user);
            result.setCode(0);
        }else{
            result.setCode(1);
            result.setMsg("获取用户信息失败，请重新登录后重试。");
        }
        return result;
    }

    @Override
    public Result changeManagerPassword(long userId,String oldPassword,String password) {
        Result result = new Result();
        User user = userDao.findOne(userId);
        //使用md5对前台传过来的密码进行加密
        String oldPasswd = DigestUtils.md5DigestAsHex(oldPassword.getBytes());
        String passwd = DigestUtils.md5DigestAsHex(password.getBytes());
        if (user!=null){
            //和保存在数据库中的密文密码进行比对
            if (user.getPassword().equals(oldPasswd)){
                user.setPassword(passwd);
                userDao.save(user);
                result.setCode(0);
                result.setMsg("密码修改成功");
            }else {
                result.setCode(1);
                result.setMsg("旧密码错误");
            }
        }else {
            result.setCode(1);
            result.setMsg("获取管理员信息失败，请重新登录后重试");
        }
        return result;
    }

    @Override
    public Result changeManagerInfo(long userId, String username) {
        Result result = new Result();
        User user = userDao.findOne(userId);
        User checkUser = userDao.findByUsername(username);
        if (user!=null){
            user.setUsername(username);
            if (checkUser==null){
                userDao.save(user);
                result.setCode(0);
                result.setMsg("修改成功");
                User userChanged = userDao.findOne(userId);
                result.setObject(userChanged);
            }else {
                long checkId = checkUser.getId();
                if (checkId == userId){
                    userDao.save(user);
                    result.setCode(0);
                    result.setMsg("修改成功");
                    User userChanged = userDao.findOne(userId);
                    result.setObject(userChanged);
                }else {
                    result.setCode(1);
                    result.setMsg("修改失败，与普通用户发生冲突");
                }
            }
        }else {
            result.setCode(1);
            result.setMsg("发生错误，无法获取管理员新息，请重新登录。");
        }
        return result;
    }

    @Override
    public Result sendAnn(String str) {
        Result result = new Result();
        redisReceiver.sendAnn(str);
        result.setCode(0);
        result.setMsg("公告发布成功");
        return result;
    }
}
