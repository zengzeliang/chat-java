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

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.net.Proxy;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

@SpringBootTest
class ChatgptJavaApplicationTests {

    @Test
    void contextLoads() {

        String serict = EncryptUtils.encrypt("hello");

        String a = "8m+HjdLZKCDWvR1lUsQvq1rBIlj1jETvUxsKRhhjaQb05InIOPPO2Q731btxqknRPrzCUEo6t7q3UuachUOB8c1+Jbk+ROC9c8KVouk0IhnhWZq9kIVo/m4kg3tg5R7Uu/DQ0POtIb+n4WWBdTMXeh1+5cU+GmksktZCS6E1eEafMjFQRnE0jQp7xE/LABoR+cBlq+fU86jH/BgYokPyZUU0uohqaRt/eR6DLLLh+4mBxThXN8qhYYv1V4j8966T3hovBjU6+MebHsNhcCOvbHY0/a+X4rRWe+F61vL0NXBWrSHLY8xYlq+vEvnqU8LzfsvVzUNJKYrcP3Nryaf5hG9NGAU1fjPQlwqv7GhcWoJhc/kKzy+jdvefKc4yMGLFT6yxi4eseagT39oV/iPkCg";

        String ans = EncryptUtils.decrypt(a);


        System.out.println(serict);
        System.out.println(ans);

    }

}
