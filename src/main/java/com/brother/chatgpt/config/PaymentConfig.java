package com.brother.chatgpt.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "payment")
public class PaymentConfig {

    private String productAmount;
    private String subject;
    private String notifyUrl;
    private String returnUrl;
    private String timeoutExpress;
    private String orderExtendBizPrefix;

}
