package com.brother.chatgpt.enums;

public enum OrderStatusEnum {

    NONE_ORDER_STATUS(0, "无效"),
    SUCCESS_ORDER_STATUS(1, "支付成功"),
    UNPAID_ORDER_STATUS(2, "待支付"),
    OVERTIME_ORDER_STATUS(3, "超时未支付");

    private Integer type;
    private String name;

    OrderStatusEnum(Integer type, String name){
        this.type = type;
        this.name = name;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
