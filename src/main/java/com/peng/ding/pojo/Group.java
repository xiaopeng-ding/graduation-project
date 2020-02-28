package com.peng.ding.pojo;


import lombok.Data;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "im_group")
public class Group {

    @Id
    @GeneratedValue  //自增的组件
    private Long id;  //好友分组id

    private String name;

    private Date created;

    @ManyToOne()
    @JoinColumn(name = "user_id")
    private User user; //此分组的主人是谁？ 代替的是user_id（主人的id）

    //利用中间表来对“人-组”进行对应
    @ManyToMany()
    @JoinTable(name = "im_group_user",  //中间表
            joinColumns = {@JoinColumn(name = "group_id")}, //当前类的id
            inverseJoinColumns = {@JoinColumn(name = "user_id")})  //user的id(好友的id)
    private List<User> list;  //分组中的好友（很多人）

}
