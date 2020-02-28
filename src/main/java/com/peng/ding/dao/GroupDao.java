package com.peng.ding.dao;

import com.peng.ding.pojo.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GroupDao extends JpaRepository<Group,Long> {

    @Query("from Group g where g.user.id = ?1")
    List<Group> findGroupsByUser(long userid);

    Group findGroupByNameAndUserId(String name, long userId);
}
