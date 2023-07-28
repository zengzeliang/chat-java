package com.brother.chatgpt;

import com.brother.chatgpt.util.EncryptUtils;
import com.plexpt.chatgpt.ChatGPT;
import com.plexpt.chatgpt.ChatGPTStream;
import com.plexpt.chatgpt.entity.chat.ChatCompletion;
import com.plexpt.chatgpt.entity.chat.ChatCompletionResponse;
import com.plexpt.chatgpt.entity.chat.Message;
import com.plexpt.chatgpt.listener.ConsoleStreamListener;
import com.plexpt.chatgpt.util.Proxys;
import org.apache.tomcat.jni.Time;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.Proxy;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

@SpringBootTest
class ChatgptJavaApplicationTests {

    @Test
    void contextLoads() throws InterruptedException {

        String hello = EncryptUtils.encrypt("Hello World");

        System.out.println(hello);
    }

}
