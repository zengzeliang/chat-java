package com.brother.chatgpt.enums;

public enum ProxyTypeEnum {
    PROXY_TYPE_HTTP(1),
    PROXY_TYPE_SOCKETS(2);
    private Integer type;

    ProxyTypeEnum(Integer type) {
        this.type = type;
    }

    public Integer getType() {
        return type;
    }
}
