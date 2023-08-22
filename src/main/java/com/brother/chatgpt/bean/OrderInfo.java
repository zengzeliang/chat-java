package com.brother.chatgpt.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderInfo {
    private Integer id;
    private String orderId;
    private String orderStatus;
    private String memo;
    private Integer userId;
    private String createTime;

}
