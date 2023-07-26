package com.brother.chatgpt;

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
import java.util.Arrays;

@SpringBootTest
class ChatgptJavaApplicationTests {

    @Test
    void contextLoads() throws InterruptedException {
        //国内需要代理
//        Proxy proxy = Proxys.http("127.0.0.1", 1081);
        //socks5 代理
         Proxy proxy = Proxys.socks5("127.0.0.1", 10886);

//        ChatGPT chatGPT = ChatGPT.builder()
//                .apiKey("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")
//                .proxy(proxy)
//                .timeout(900)
//                .apiHost("https://api.openai.com/") //反向代理地址
//                .build()
//                .init();

        ChatGPTStream chatGPTStream = ChatGPTStream.builder()
                .timeout(600)
                .apiKey("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")
                .proxy(proxy)
                .apiHost("https://api.openai.com/")
                .build()
                .init();


        ConsoleStreamListener listener = new ConsoleStreamListener();
        Message message = Message.of("写一段七言绝句诗，题目是：火锅！");
        ChatCompletion chatCompletion = ChatCompletion.builder()
                .messages(Arrays.asList(message))
                .build();
        chatGPTStream.streamChatCompletion(chatCompletion, listener);


    }

}
