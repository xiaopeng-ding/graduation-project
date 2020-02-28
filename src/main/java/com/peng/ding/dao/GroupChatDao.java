package com.peng.ding.dao;

//import com.peng.ding.pojo.Group;
import com.peng.ding.pojo.Group;
import com.peng.ding.pojo.GroupChat;
import com.peng.ding.pojo.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GroupChatDao extends JpaRepository<GroupChat,Long> {

    GroupChat findGroupChatByNameAndUserId(String groupChatName,long userId);

    @Query("from GroupChat g where g.user.id = ?1")
    List<GroupChat> findGroupChatsByUser(Long userid);

    @Query("select g from GroupChat g left join g.list u where u.id=?1")
    List<GroupChat> findGroupChatsWitchOwnMe(long userId);

    @Query("select g.list from GroupChat g where g.id = ?1")
    List<User> findUsersByGroupChatId(long GroupChatId);
}
