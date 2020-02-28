package com.peng.ding.service;

import com.peng.ding.pojo.Result;

public interface AdminService {
    Object getUsers();

    Result warn(long userId);

    Result forceOffline(long userId);

    Result isDisable(long userId,boolean isdisable);

    Result changeManagerPassword(long userId,String oldPassword,String password);

    Result changeManagerInfo(long userId, String username);

    Result sendAnn(String str);
}
