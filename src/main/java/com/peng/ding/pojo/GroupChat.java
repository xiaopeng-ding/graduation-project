package com.peng.ding.pojo;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "im_group_chat")
public class GroupChat {

    @Id
    @GeneratedValue  //自增的组件
    private Long id;  //群聊id

    private String name;

    private String avatar;  //群聊头像（“http://39.98.229.7/avatar/defaultGroupChatAvatar.png”）目前默认，不支持修改

    private Date created;

    @ManyToOne()
    @JoinColumn(name = "user_id")
    private User user; //此群聊的主人是谁？ 代替的是user_id（主人的id）

    //利用中间表来对“人-群聊”进行对应
    @ManyToMany()
    @JoinTable(name = "im_group_chat_user",  //中间表
            joinColumns = {@JoinColumn(name = "group_chat_id")}, //当前类（群聊）的id
            inverseJoinColumns = {@JoinColumn(name = "user_id")})  //user的id(好友的id)
    private List<User> list;  //群聊中的好友（很多人）

}
