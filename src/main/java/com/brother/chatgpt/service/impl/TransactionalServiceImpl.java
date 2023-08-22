package com.brother.chatgpt.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.brother.chatgpt.bean.OrderInfo;
import com.brother.chatgpt.bean.UserInfo;
import com.brother.chatgpt.enums.OrderStatusEnum;
import com.brother.chatgpt.service.OrderInfoService;
import com.brother.chatgpt.service.TransactionService;
import com.brother.chatgpt.service.UserInfoService;
import com.brother.chatgpt.util.DateTimeFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionalServiceImpl implements TransactionService {

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private OrderInfoService orderInfoService;

    @Transactional
    public void saveUserInfoAndOrderInfo(UserInfo userInfo, String outTradeNo, JSONObject jsonObject) {
        // 保存用户信息
        Integer userInfoId = userInfoService.saveUserInfo(userInfo);
        // 保存用户信息失败
        if(userInfoId == -1){
            throw new RuntimeException("Error saving user info");
        }

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderId(outTradeNo);
        orderInfo.setOrderStatus(OrderStatusEnum.UNPAID_ORDER_STATUS.getName());
        orderInfo.setCreateTime(DateTimeFormatUtils.dateTimeToStr());
        orderInfo.setMemo(jsonObject.toJSONString());
        orderInfo.setUserId(userInfoId);
        orderInfoService.saveOrderInfo(orderInfo);
    }

    @Override
    @Transactional
    public void updateUserAndOrderStatus(String outTradeNo, Integer userId) {
        orderInfoService.updateOrderStatus(outTradeNo, OrderStatusEnum.SUCCESS_ORDER_STATUS.getName());
        userInfoService.updateUserBuyStatus(userId, 1);
    }

    @Override
    @Transactional
    public void updateUserInfoAndOrderInfo(UserInfo userInfo, String outTradeNo, JSONObject jsonObject, UserInfo existUserInfo) {
        userInfoService.updateUserInfoById(userInfo, existUserInfo.getId());
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderId(outTradeNo);
        orderInfo.setOrderStatus(OrderStatusEnum.UNPAID_ORDER_STATUS.getName());
        orderInfo.setCreateTime(DateTimeFormatUtils.dateTimeToStr());
        orderInfo.setMemo(jsonObject.toJSONString());
        orderInfoService.updateOrderInfoByUserId(orderInfo, existUserInfo.getId());
    }
}
