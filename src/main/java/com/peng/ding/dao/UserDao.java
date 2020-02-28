package com.peng.ding.dao;

import com.peng.ding.pojo.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserDao extends JpaRepository<User,Long> {

    User findByUsername(String username);



    /**
     * 谁的好友列表中有userid所对应的人(上线下线该通知谁)
     * @param userid userid
     * @return
     */
    @Query("select g.user from Group g left join g.list u where u.id=?1")
    List<User> findWhoOwnedMe(long userid);

    List<User> findAllByPower(String power);

    User findUsersByPower(String power);


}
