package com.brother.chatgpt.util;

public class BizNoGenerateUtils {
    // 生成订单id

    public static String generateOrderId(String extendBit){
        String orderId = DateTimeFormatUtils.dateToStr().concat(extendBit);
        return orderId;
    }

}
