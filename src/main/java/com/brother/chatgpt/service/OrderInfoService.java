package com.brother.chatgpt.service;

import com.brother.chatgpt.bean.OrderInfo;

public interface OrderInfoService {
    void saveOrderInfo(OrderInfo orderInfo);

    OrderInfo getOrderByOrderId(String outTradeNo);

    void updateOrderStatus(String outTradeNo, String status);

    void updateOrderInfoByUserId(OrderInfo orderInfo, Integer id);
}
