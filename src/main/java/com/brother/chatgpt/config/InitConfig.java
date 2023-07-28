package com.brother.chatgpt.config;

import com.brother.chatgpt.enums.ProxyTypeEnum;
import com.plexpt.chatgpt.ChatGPT;
import com.plexpt.chatgpt.ChatGPTStream;
import com.plexpt.chatgpt.util.Proxys;
import lombok.Data;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;

@Component
@Data
public class InitConfig {

    private ParamConfig paramConfig;
    private Proxy proxy;
    private String ip;
    private String port;
    private ChatGPTStream chatGPTStream;
    private ChatGPT chatGPT;
    private Integer maxPreContextNum;
    private Integer storeMessage;

    public InitConfig(ParamConfig paramConfig){
        this.paramConfig = paramConfig;

        if(ProxyTypeEnum.PROXY_TYPE_HTTP.getType() == paramConfig.getProxyType()){
            proxy = Proxys.http(paramConfig.getIp(), paramConfig.getPort());
        }else if (ProxyTypeEnum.PROXY_TYPE_SOCKETS.getType() == paramConfig.getProxyType()){
            proxy = Proxys.socks5(paramConfig.getIp(), paramConfig.getPort());
        }else{
            proxy = Proxys.http(paramConfig.getIp(), paramConfig.getPort());
        }

        maxPreContextNum = paramConfig.getMaxPreContextNum();

        chatGPTStream = ChatGPTStream.builder()
                .apiKeyList(paramConfig.getApiKeys())
                .proxy(proxy)
                .apiHost("https://api.openai.com/")
                .timeout(900)
                .build()
                .init();

        chatGPT = ChatGPT.builder()
                .apiKeyList(paramConfig.getApiKeys())
                .proxy(proxy)
                .timeout(900)
                .apiHost("https://api.openai.com/") //反向代理地址
                .build()
                .init();
        this.storeMessage = paramConfig.getStoreMessage();
    }

    @Bean
    public RestTemplate restTemplate() {
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(paramConfig.getIp(), paramConfig.getPort()));
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setProxy(proxy);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        return restTemplate;
    }

    @Bean
    public OkHttpClient okHttpClient(){
        ConnectionPool connectionPool = new ConnectionPool(5, 10, TimeUnit.MINUTES);

        OkHttpClient client = new OkHttpClient().newBuilder().proxy(proxy).connectionPool(connectionPool)
                .build();
        return client;
    }

    @Override
    public String toString() {
        return "InitConfig{" +
                "paramConfig=" + paramConfig +
                ", proxy=" + proxy +
                ", chatGPTStream=" + chatGPTStream +
                ", maxPreContextNum=" + maxPreContextNum +
                '}';
    }
}
