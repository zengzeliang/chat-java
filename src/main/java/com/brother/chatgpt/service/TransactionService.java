package com.brother.chatgpt.service;

import com.alibaba.fastjson.JSONObject;
import com.brother.chatgpt.bean.UserInfo;

public interface TransactionService {
    void saveUserInfoAndOrderInfo(UserInfo userInfo, String outTradeNo, JSONObject jsonObject);

    void updateUserAndOrderStatus(String outTradeNo, Integer userId);

    void updateUserInfoAndOrderInfo(UserInfo userInfo, String outTradeNo, JSONObject jsonObject, UserInfo existUserInfo);
}
