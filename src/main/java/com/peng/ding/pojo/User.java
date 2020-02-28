package com.peng.ding.pojo;


import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Data
@Entity
@Table(name = "im_user")
public class User {

    @Id@GeneratedValue  //自增的组件
    private Long id;  //用户id

    private String username;  //用户昵称（姓名）

    private String password;  //密码

    private String sign;  //签名

    private String status;  //用户状态

    private String avatar;  //用户头像

    private Date created;  //创建时间

    private String isdisable;  //是否封禁

    private String power;  //权限，本系统中由于采用userid来进行websocket session 的绑定，而在管理系统中要用到这个websocket所以管理员和普通用户采用一个库



}
