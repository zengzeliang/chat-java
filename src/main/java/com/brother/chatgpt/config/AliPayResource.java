package com.brother.chatgpt.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "alipay")
public class AliPayResource {

    private String appId;
    private String merchantPrivateKey;
    private String alipayPublicKey;

    private String signType;
    private String charset;
    private String gatewayUrl;
}
