package com.brother.chatgpt.enums;

public enum RecordMessageTypeEnum {
    ASSISTANT(1),
    USER_TYPE(2),
    WEB_SYSTEM(3);

    private Integer type;

    RecordMessageTypeEnum(Integer type) {
        this.type = type;
    }

    public Integer getType() {
        return type;
    }
}
