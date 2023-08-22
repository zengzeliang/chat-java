package com.brother.chatgpt.bean;

import lombok.Data;

import java.util.Date;

@Data
public class UserInfo {
    private Integer id;
    private String userId;
    private String password;
    private Integer buy;
    private String createTime;
    private Integer state;


}
