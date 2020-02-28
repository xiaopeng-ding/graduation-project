package com.peng.ding.controller;

import com.peng.ding.dao.UserDao;
import com.peng.ding.pojo.Result;
import com.peng.ding.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/public")
public class PublicController {

    @Autowired
    public UserDao userDao;


    /**
     * 登录
     * @param username 前台参数用户昵称
     * @param password 前台参数密码
     * @param session 设置的session
     * @return 返回登录成功或者错误代码及信息
     */
    @PostMapping("/login")
    @ResponseBody
    public Result login(String username, String password, HttpSession session){
        Result result = new Result();
        User user = userDao.findByUsername(username);
        //使用md5对前台传过来的密码进行加密，并和保存在数据库中的密文密码进行比对
        String passwd = DigestUtils.md5DigestAsHex(password.getBytes());
        if (user==null){
            result.setCode(1);
            result.setMsg("没有此用户");
            return result; //登录失败
        }
        if (!user.getPassword().equals(passwd)){
            result.setCode(1);
            result.setMsg("密码错误");
            return result; //登录失败
        }
        if (user.getIsdisable().equals("yes")){
            result.setCode(1);
            result.setMsg("您由于违规次数过多已被封号，请联系管理员（QQ：1516066048）");
            return result; //登录失败
        }
        if (user.getPower().equals("admin")){  //检查出来的是管理员的话，直接显示没有此用户
            result.setCode(1);
            result.setMsg("此用户是管理员，无法登录聊天APP");
            return result; //登录失败
        }
        session.setAttribute("userInfo",user);
        result.setCode(0);
        result.setMsg("登录成功");
        result.setObject(user);
        return result; //登录成功，跳到首页
    }
}
