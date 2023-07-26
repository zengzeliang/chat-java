package com.brother.chatgpt.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "gpt")
@Data
public class ParamConfig {

    // 配置的api个数
    private List<String> apiKeys;

    // 上次会话次数
    private Integer maxPreContextNum;

    // 1 表示http代理， 2表示socketType
    private Integer proxyType;

    private String ip;

    private Integer port;

    private Integer storeMessage;
}
