package com.peng.ding.controller;


import com.peng.ding.dao.UserDao;
import com.peng.ding.pojo.Result;
import com.peng.ding.pojo.User;
import com.peng.ding.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    public UserDao userDao;

    @Autowired
    public AdminService adminService;

    @PostMapping("/login")
    @ResponseBody
    public Result login(String username,String password){
        Result result = new Result();
        User user = userDao.findByUsername(username);
        //使用md5对前台传过来的密码进行加密，并和保存在数据库中的密文密码进行比对
        String passwd = DigestUtils.md5DigestAsHex(password.getBytes());
        if (user!=null){
            if (user.getPower().equals("admin")){
                if (user.getPassword().equals(passwd)){
                    result.setCode(0);
                    result.setMsg("登录成功");
                    result.setObject(user);
                }else {
                    result.setCode(1);
                    result.setMsg("密码错误，请重试");
                }
            }else {
                result.setCode(1);
                result.setMsg("没有此管理员");
            }
        }else {
            result.setCode(1);
            result.setMsg("没有此管理员");
        }
        return result;
    }


    @GetMapping("/getUsers")
    @ResponseBody
    public Object getUsers(){
        return adminService.getUsers();
    }

    @GetMapping("/warn")
    @ResponseBody
    public Result warn(long userId){
        return adminService.warn(userId);
    }

    @GetMapping("/forceOffline")
    @ResponseBody
    public Result forceOffline(long userId){
        return adminService.forceOffline(userId);
    }

    @GetMapping("/isDisable")
    @ResponseBody
    public Result isDisable(long userId,boolean isdisable){
//        System.out.println(userId+" "+isdisable);
        return adminService.isDisable(userId,isdisable);
//        return null;

    }

    //changeManagerPassword

    @PostMapping("/changeManagerPassword")
    @ResponseBody
    public Result changeManagerPassword(long userId,String oldPassword,String password){
//        System.out.println(userId+" "+oldPassword+" "+password);
        return adminService.changeManagerPassword(userId,oldPassword,password);
    }

    @PostMapping("/changeManagerInfo")
    @ResponseBody
    public Result changeManagerInfo(long userId,String username){
//        System.out.println(userId+" "+oldPassword+" "+password);
        return adminService.changeManagerInfo(userId,username);
    }

//    sendAnn
    @PostMapping("/sendAnn")
    @ResponseBody
    public Result sendAnn(String str){
    //        System.out.println(userId+" "+oldPassword+" "+password);
        return adminService.sendAnn(str);
    }

}
