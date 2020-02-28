package com.peng.ding.service;

import com.peng.ding.pojo.Result;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

public interface UserService {

    Object init(Long userid);

    Object createUser(String username, String password);

    Result addFriend(int userId, String friendName);

    Result addFriendGroup(long userId, String friendGroupName);

    Result findFriend(long userId, String friendName);

    Result changeFriendGroup(long mineId, String friendName, String groupName);

    Result delFriend(long mineId, String friendName);

    Result upLoad(MultipartFile img,long mineId);

    Result addGroupChat(long userId, String groupChatName);

    Result findGroupChat(long groupChatId);

    Object getGroupChatMember(Long groupChatId);

    Object getFriends(Long userId);

    Result addNewMember(long groupChatId, long friendId);

    Result getFriendGroupsInfo(Long userId);

    Result changeFriendGroupName(long userId, long friendGroupId, String friendGroupName);

    Result exitGroupChat(long userId, long groupChatId);

    Object friendByUsernameOrId(String username,long userId);

}
