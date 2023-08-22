package com.brother.chatgpt.service;

import com.brother.chatgpt.bean.UserInfo;

public interface UserInfoService {
     Integer saveUserInfo(UserInfo userInfo);

     UserInfo getUserInfoByUserId(String userId);

     void updateUserBuyStatus(Integer userId, int buy);

     UserInfo getUserInfoByUserIdAndPass(String userId, String password);

     void updateUserInfoById(UserInfo userInfo, Integer id);
}
