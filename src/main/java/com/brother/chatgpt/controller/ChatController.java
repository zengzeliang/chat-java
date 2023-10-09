package com.brother.chatgpt.controller;

import com.alibaba.fastjson.JSON;
import com.brother.chatgpt.bean.Channel;
import com.brother.chatgpt.bean.UserInfo;
import com.brother.chatgpt.config.InitConfig;
import com.brother.chatgpt.enums.RecordMessageTypeEnum;
import com.brother.chatgpt.service.UserInfoService;
import com.brother.chatgpt.util.EncryptUtils;
import com.plexpt.chatgpt.entity.chat.ChatCompletion;
import com.plexpt.chatgpt.entity.chat.ChatCompletionResponse;
import com.plexpt.chatgpt.entity.chat.Message;
import com.plexpt.chatgpt.listener.SseStreamListener;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@RestController
@Slf4j
public class ChatController {
    @Autowired
    private InitConfig initConfig;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private UserInfoService userInfoService;

    @Resource
    private OkHttpClient okHttpClient;

    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    private static final String DEFAULT_CHANNEL_ID = "default";
    private static final String DEFAULT_CHANNEL_NAME = "默认对话";

    private static final String DEFAULT_CHANNEL_MODE = "continuous";

    private static final String DEFAULT_CHANNEL_CHAT_MODE = "text";

    // 存储用户每个通道的消息 {"zzl": {"default": [{"role": "角色", "content": "回答内容"}], "channel1": [{"role": "角色", "content": "回答内容"}]}, "zze": {"default": [{"role": "juese", "": ""}]}}
    private static final String USER_CHAT_ID_MESSAGE_PREFIX = "chat_id_message";

    // {"zzl": [{"id": "角色", "name": "回答内容", "mode": "模式"}, {"id": "角色", "name": "回答内容", "mode": "模式"}]}
    private static final String USER_CHAT_ID_CHANNEL_PREFIX = "chat_id_channel";

    private static final String USER_SELECT_CHANNEL_PREFIX = "select_channel";

    private static final String IMAGE_OPENAI_URL = "https://api.openai.com/v1/images/generations";

    private static final Integer PRE_MYSQL_ID = 123;

    // 非流式 单次聊天调用
    @GetMapping("/chat")
    @CrossOrigin
    public Object chat(String prompt){
        Message message = Message.of(prompt);

        ChatCompletion chatCompletion = ChatCompletion.builder()
                .model(ChatCompletion.Model.GPT_3_5_TURBO.getName())
                .messages(Arrays.asList(message))
                .maxTokens(3000)
                .temperature(0.9)
                .build();
        ChatCompletionResponse response = initConfig.getChatGPT().chatCompletion(chatCompletion);
        Message res = response.getChoices().get(0).getMessage();

        Map<String, Object> result = new HashMap<>();
        result.put("role", res.getRole());
        result.put("content", res.getContent());
        return result;
    }

    // 加载
    @GetMapping("/deleteMessage")
    @CrossOrigin
    public Object deleteMessage(){
        String userId = "zze";
        String channelId = "default";
        HashOperations<String, String, List<Map<String, String>>> hashOps = redisTemplate.opsForHash();

        List<Map<String, String>> messages = hashOps.get(USER_CHAT_ID_MESSAGE_PREFIX + userId, channelId);

        int n = 2;

        for(int i = 0; i < n; i++){
            messages.remove(messages.size() - 1);
        }

        System.out.println(messages);

        hashOps.put(USER_CHAT_ID_MESSAGE_PREFIX + userId, channelId, messages);

        return "success";
    }


        // 加载
    @GetMapping("/loadChats")
    @CrossOrigin
    public Object loadChats(String userId, String password){
        Map<String, Object> result = new HashMap<>();
        List<Map> data = new ArrayList<>();

        // 用户校验不通过，返回空聊天窗口
        if(!checkUser(userId, password)){
            result.put("code", 201);
            result.put("data", data);
            result.put("message", "用户校验失败");
            return result;
        }
        log.info("userId: [{}]进入loadChats", userId);

        // 获取用户所有的chat channel
        String selectedChannelId = (String) redisTemplate.opsForValue().get(USER_SELECT_CHANNEL_PREFIX + userId);
        if(Strings.isEmpty(selectedChannelId)){
            selectedChannelId = DEFAULT_CHANNEL_ID;
        }

        // {"zzl": [{"id": "角色", "name": "回答内容", "mode": "模式"}, {}]}
        ListOperations<String, Object> listOps = redisTemplate.opsForList();
        List<Object> channelInfoList = listOps.range(USER_CHAT_ID_CHANNEL_PREFIX + userId, 0, -1);

        HashOperations<String, String, List<Map<String, String>>> hashOps = redisTemplate.opsForHash();

        for (Object info : channelInfoList){
            String channelInfoJson = (String)info;
            Channel channel = JSON.parseObject(channelInfoJson, Channel.class);
            Map<String, Object> chatsInfo = new HashMap<>();
            chatsInfo.put("id", channel.getId());
            chatsInfo.put("name", channel.getName());
            chatsInfo.put("selected", selectedChannelId.equals(channel.getId()));
            chatsInfo.put("mode", channel.getMode());

            // 存储用户每个通道的消息 {"zzl": {"default": [{"role": "角色", "content": "回答内容"}], "channel1": [{"role": "角色", "content": "回答内容"}]}, "zze": {"default": [{"role": "juese", "": ""}]}}

            List<Map<String, String>> maps = hashOps.get(USER_CHAT_ID_MESSAGE_PREFIX + userId, channel.getId());
            if(maps == null){
                maps = new ArrayList<>();
            }

            chatsInfo.put("messages_total", maps.size());
            data.add(chatsInfo);
        }

        result.put("code", 200);
        result.put("data", data);
        result.put("message", "success");
        return result;
    }

    // 创建新的聊天窗口
    @GetMapping("/newChat")
    @CrossOrigin
    public Object newChat(String name, String userId, String password){
        log.info("userId: [{}]进入newChat name: [{}]", userId, name);
        Map<String, Object> result = new HashMap<>();

        // 用户校验不通过
        if(!checkUser(userId, password)){
            result.put("code", 201);
            result.put("message", "请先创建或输入已有用户id");
            return result;
        }

        // 生成聊天channel id
        String channelId = UUID.randomUUID().toString();
        setChannelInfo(userId, channelId, name, DEFAULT_CHANNEL_MODE, DEFAULT_CHANNEL_CHAT_MODE);

        HashOperations<String, String, List<Map<String, String>>> hashOps = redisTemplate.opsForHash();
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("id", channelId);
        data.put("selected", true);
        data.put("messages_total", hashOps.get(USER_CHAT_ID_MESSAGE_PREFIX + userId, channelId).size());

        result.put("code", 200);
        result.put("message", "success");
        result.put("data", data);
        return result;
    }

    // 选择chat channel
    @GetMapping("/selectChat")
    @CrossOrigin
    public Object selectChat(String userId, String id, String password) {
        log.info("userId: [{}]进入selectChat ", userId);
        Map<String, Object> result = new HashMap<>();

        if(!checkUser(userId, password)){
            result.put("code", 201);
            result.put("message", "请先创建或输入已有用户id");
            return result;
        }
        // 设置默认选择的channelId
        redisTemplate.opsForValue().set(USER_SELECT_CHANNEL_PREFIX + userId, id);
        result.put("code", 200);
        result.put("message", "success");
        return result;
    }

    // 加载
    @GetMapping("/loadHistory")
    @CrossOrigin
    public Object loadHistory(String userId, String password){
        log.info("userId: [{}]进入loadHistory ", userId);
        Map<String, Object> result = new HashMap<>();

        // 用户校验不通过
        if(!checkUser(userId, password)){
            result.put("code", 201);
            result.put("message", "请先创建或输入已有用户id");
            return result;
        }
        // 获取当前选择的聊天channel
        String selectedChannelId = (String) redisTemplate.opsForValue().get(USER_SELECT_CHANNEL_PREFIX + userId);

        if(selectedChannelId == null){
            selectedChannelId = DEFAULT_CHANNEL_ID;
        }

        HashOperations<String, String, List<Map<String, String>>> hashOps = redisTemplate.opsForHash();

        List<Map<String, String>> messages = hashOps.get(USER_CHAT_ID_MESSAGE_PREFIX + userId, selectedChannelId);

        result.put("code", 200);
        result.put("message", "success");
        result.put("data", messages);
        return result;
    }

    // 设置用户id
    @PostMapping("/setUserId")
    @CrossOrigin
    public Object userId(@RequestBody Map<String, Object> param) {
        String userId = (String) param.get("userId");
        String password = (String) param.get("password");
        log.info("进入setUserId, {}", param);
        Map<String, Object> result = new HashMap<>();
        if(Strings.isEmpty(userId)){
            result.put("code", 202);
            result.put("message", "userId不能为空");
            return result;
        }
        // 兼容旧账号，根据userId查一遍
        UserInfo userInfoByUserId = userInfoService.getUserInfoByUserId(userId);
        if(userInfoByUserId == null){
            result.put("code", 201);
            result.put("message", "用户id不存在");
            return result;
        }

        if(userInfoByUserId.getId() >= PRE_MYSQL_ID){
            // 新版账号
            // 根据用户名和密码判断是否已经购买
            UserInfo userInfo = userInfoService.getUserInfoByUserIdAndPass(userId, password);
            boolean userExist = redisTemplate.hasKey(USER_CHAT_ID_MESSAGE_PREFIX + userId);
            if(!userExist){
                // 设置默认channel
                setChannelInfo(userId, DEFAULT_CHANNEL_ID, DEFAULT_CHANNEL_NAME, DEFAULT_CHANNEL_MODE, DEFAULT_CHANNEL_CHAT_MODE);
            }// 设置默认channel
            if(userInfo == null){
                // 查看redis中是否存在
                result.put("code", 201);
                result.put("message", "用户信息未查询到");
            }else{
                // 查看redis中是否存在
                result.put("code", 200);
                result.put("message", "success");
            }
            return result;

        }else{
            // 查看redis中是否存在
            boolean userExist = redisTemplate.hasKey(USER_CHAT_ID_MESSAGE_PREFIX + userId);
            if(!userExist){
                // 设置默认channel
                setChannelInfo(userId, DEFAULT_CHANNEL_ID, DEFAULT_CHANNEL_NAME, DEFAULT_CHANNEL_MODE, DEFAULT_CHANNEL_CHAT_MODE);
            }
            result.put("code", 200);
            result.put("message", "success");
            return result;
        }
    }

    // 删除聊天
    @GetMapping("/deleteHistory")
    @CrossOrigin
    public Object deleteHistory(String userId, String password) {
        Map<String, Object> result = new HashMap<>();

        if (!checkUser(userId, password)) {
            result.put("code", 201);
            result.put("message", "删除历史记录失败");
            return result;
        }
        // 默认channel不能删除，获取当前选择的channel
        String channelId = (String) redisTemplate.opsForValue().get(USER_SELECT_CHANNEL_PREFIX + userId);

        log.info("userId: [{}] channelId: [{}]进入deleteHistory", userId, channelId);

        if(DEFAULT_CHANNEL_ID.equals(channelId)){
            // 是默认channel 只清空聊天记录
            HashOperations<String, String, List<Map<String, String>>> hashOps = redisTemplate.opsForHash();
            hashOps.put(USER_CHAT_ID_MESSAGE_PREFIX + userId, channelId, new ArrayList<>());
        }else{
            // 设置默认channel id
            redisTemplate.opsForValue().set(USER_SELECT_CHANNEL_PREFIX + userId, DEFAULT_CHANNEL_ID);

            // 删除对应的key，消息对应的key
            HashOperations<String, String, List<Map<String, String>>> hashOps = redisTemplate.opsForHash();
            hashOps.delete(USER_CHAT_ID_MESSAGE_PREFIX + userId, channelId);

            // 删除channel对应的消息
            ListOperations<String, Object> listOps = redisTemplate.opsForList();
            List<Object> channels = listOps.range(USER_CHAT_ID_CHANNEL_PREFIX + userId, 0, -1);

            if(channels != null){
                for(Object info : channels){
                    String channelInfoJson = (String)info;
                    Channel channel = JSON.parseObject(channelInfoJson, Channel.class);
                    if (channelId.equals(channel.getId())){
                        listOps.remove(USER_CHAT_ID_CHANNEL_PREFIX + userId, 1, info);
                    }
                }
            }
        }
        result.put("code", 200);
        result.put("message", "success");
        return result;
    }

    // 流式 单次聊天调用
    @PostMapping("/sseChat")
    @CrossOrigin
    public Object sseChat1(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        // mode: normal 普通对话, continuous连续对话
        // 参数{"userId": "zzl", "content": "消息内容", "channelId": "cedfaff", "mode": "normal"}
        long curTime = System.currentTimeMillis();
        // 定义日期和时间格式
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // 格式化时间戳
        String formattedTime = sdf.format(new Date(curTime));
        param.put("time", formattedTime);
        String channelId = (String) param.get("channelId");
        String userId = (String) param.get("userId");
        String prompt = (String) param.get("content");
        String password = (String) param.get("password");

        String clientIP = getClientIP(request);
        log.info("user[{}], ip[{}]进入sseChat, channelId: [{}], param: [{}]", userId, clientIP, channelId, EncryptUtils.encrypt(param.toString()));
        // 是未授权用户或者校验未通过
        if("UNAUTHORIZED".equals(userId) || !checkUser(userId, password)){
            // 单独处理
            SseEmitter sseEmitter = processUnauthorized(userId, channelId, prompt);
            return sseEmitter;
        }

        List<Message> messages = new ArrayList<>();

        // 记得修改channel的mode
        String mode = (String) param.get("mode");

        ListOperations<String, Object> listOps = redisTemplate.opsForList();
        List<Object> channelInfoList = listOps.range(USER_CHAT_ID_CHANNEL_PREFIX + userId, 0, -1);

        if(channelInfoList == null){
            SseEmitter sseEmitter = processUnauthorized(userId, channelId, prompt);
            return sseEmitter;
        }

        // 找到这个channle，并且修改mode设置
        int index;
        Channel targetChannel = null;

        for (index = 0; index < channelInfoList.size(); index++) {
            Object info = channelInfoList.get(index);
            String channelInfoJson = (String) info;
            targetChannel = JSON.parseObject(channelInfoJson, Channel.class);
            if (channelId.equals(targetChannel.getId())){
                break;
            }
        }

        if(targetChannel == null){
            targetChannel = new Channel(DEFAULT_CHANNEL_ID, DEFAULT_CHANNEL_NAME, DEFAULT_CHANNEL_MODE, DEFAULT_CHANNEL_CHAT_MODE);

            try{
                listOps.set(USER_CHAT_ID_CHANNEL_PREFIX + userId, 0, JSON.toJSONString(targetChannel));
            }catch (Exception e){
                log.warn("sseChat 解析出错...");
                listOps.rightPush(USER_CHAT_ID_CHANNEL_PREFIX + userId, JSON.toJSONString(targetChannel));
            }
        }else{
            listOps.set(USER_CHAT_ID_CHANNEL_PREFIX + userId, index, JSON.toJSONString(targetChannel));
        }

        // 授权后没有normal消息
        if("normal".equals(mode)){
            messages.add(Message.of(prompt));
        }else if("continuous".equals(mode)){
            // 存储用户每个通道的消息 {"zzl": {"default": [{"role": "角色", "content": "回答内容"}], "channel1": [{"role": "角色", "content": "回答内容"}]}, "zze": {"default": [{"role": "juese", "": ""}]}}
            HashOperations<String, String, List<Map<String, String>>> hashOps = redisTemplate.opsForHash();
            List<Map<String, String>> preMessages = hashOps.get(USER_CHAT_ID_MESSAGE_PREFIX + userId, channelId);

            // 有两种情况，1. 是redis获取超时，一次未获取到。2. 在设置user的时候超时，未设置成功存储消息的list
            int retryTime = 2;
            while (retryTime > 0 && preMessages == null){
                preMessages = hashOps.get(USER_CHAT_ID_MESSAGE_PREFIX + userId, channelId);
                retryTime --;
            }

            // 还是为空，单独给用户id设置消息
            if(preMessages == null){
                preMessages = new ArrayList<>();
                hashOps.put(USER_CHAT_ID_MESSAGE_PREFIX + userId, channelId, preMessages);
            }

            List<Map<String, String>> lastConfigNum = preMessages.subList(Math.max(preMessages.size() - initConfig.getMaxPreContextNum() + 1, 0), preMessages.size());
            for(Map map : lastConfigNum){
                if ("assistant".equals(map.get("role"))){
                    messages.add(Message.ofSystem((String) map.getOrDefault("content", "")));
                }else if("user".equals(map.get("role"))){
                    messages.add(Message.of((String) map.getOrDefault("content", "")));
                }
            }
            // 加入当前消息
            messages.add(Message.of(prompt));

            // 判断消息是否大于最大长度，用于截断消息
            cutMessageLen(messages);
        }
        SseEmitter sseEmitter = new SseEmitter(-1L);
        SseStreamListener listener = new SseStreamListener(sseEmitter);
        listener.setOnComplate(msg -> {
            // 回答完成，可以做一些事情
            sseEmitter.complete();

            // 记录用户问题以及给出的消息
            recordMessageInfo(userId, channelId, prompt, RecordMessageTypeEnum.USER_TYPE);
            recordMessageInfo(userId, channelId, msg, RecordMessageTypeEnum.ASSISTANT);

            log.info("userId: [{}], channelId[{}]回答完成: {}", userId, channelId, EncryptUtils.encrypt(msg));
            return;
        });
        initConfig.getChatGPTStream().streamChatCompletion(messages, listener);
        return sseEmitter;
    }

    private void cutMessageLen(List<Message> messages) {
        // 获取当前消息的长度，使用线程池计算

        int countTokens = concurrentCaculateMessageToken(messages);

        while (countTokens > initConfig.getParamConfig().getMaxTokenLen()){
            // 需要cut的长度为
            int needCut = countTokens - initConfig.getParamConfig().getMaxTokenLen();
            // 获取最早的message
            Message message = messages.get(0);
            int firstMessageTokens = ChatCompletion.builder().messages(Arrays.asList(message)).build().countTokens();

            if(firstMessageTokens <= needCut){
                // 第一条消息长度还不够减直接remove
                if(!messages.isEmpty()){
                    messages.remove(0);
                }
            }else{
                int temp = firstMessageTokens;
                // 第一条消息长度够减，尝试减10个
                while(firstMessageTokens > (temp -needCut)){
                    String content = message.getContent();
                    int remain = needCut - (temp - firstMessageTokens);
                    if(remain > 4000){
                        content = content.substring(2000);
                    }else if(remain > 2000){
                        content = content.substring(1000);
                    }else if(remain > 1000){
                        content = content.substring(500);
                    }else if(remain > 200){
                        content = content.substring(100);
                    }else{
                        content = content.substring(10);
                    }
                    message.setContent(content);
                    firstMessageTokens = ChatCompletion.builder().messages(Arrays.asList(message)).build().countTokens();
                }
            }
            countTokens = concurrentCaculateMessageToken(messages);
        }
    }

    // 计算消息长度
    private int concurrentCaculateMessageToken(List<Message> messages) {
        // 使用Stream API计算结果的总和
        int sum = messages.stream()
                .map(message -> threadPoolTaskExecutor.submit(() -> ChatCompletion.builder().messages(Arrays.asList(message)).build().countTokens()))
                .mapToInt(future -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        log.error("concurrentCaculateMessageToken messages error: {}", e);
                        return 0;
                    }
                })
                .sum();
        return sum;
    }

    private SseEmitter processUnauthorized(String userId, String channelId, String prompt) {
        List<Message> messages = new ArrayList<>();

        messages.add(Message.of(prompt));
        cutMessageLen(messages);
        SseEmitter sseEmitter = new SseEmitter(-1L);
        SseStreamListener listener = new SseStreamListener(sseEmitter);
        listener.setOnComplate(msg -> {
            // 回答完成，可以做一些事情
            sseEmitter.complete();
            // 记录用户问题以及给出的消息
            log.info("userId: [{}], channelId[{}]回答完成: {}", userId, channelId, EncryptUtils.encrypt(msg));
            return;
        });
        initConfig.getChatGPTStream().streamChatCompletion(messages, listener);
        return sseEmitter;
    }

    // 流式 单次聊天调用
    @PostMapping("/recordMessage")
    @CrossOrigin
    public Object recordMessage(@RequestBody Map<Object, Object> param) {
        String userId = (String) param.get("userId");
        String msg = (String) param.get("msg");
        log.info("{}进入recordMessage, msg{}", userId, EncryptUtils.encrypt(msg));

        Map<String, Object> result = new HashMap<>();
        // 获取用户信息
        UserInfo userInfoByUserId = userInfoService.getUserInfoByUserId(userId);

        if(userInfoByUserId == null || userInfoByUserId.getBuy() == 0){
            result.put("code", 200);
            result.put("message", "用户Id不存在或者用户未购买");
            return result;
        }

        String channelId = (String) param.get("channelId");

        Integer messageType = (Integer) param.get("messageType");

        RecordMessageTypeEnum enumValue = null;
        for (RecordMessageTypeEnum member : RecordMessageTypeEnum.values()) {
            if (member.getType().equals(messageType)) {
                enumValue = member;
                break;
            }
        }
        recordMessageInfo(userId, channelId, msg, enumValue);

        result.put("code", 200);
        result.put("message", "success");
        return result;
    }

    private void recordMessageInfo(String userId, String channelId, String msg, RecordMessageTypeEnum msgType) {

        HashOperations<String, String, List<Map<String, String>>> hashOps = redisTemplate.opsForHash();
        List<Map<String, String>> messages = hashOps.get(USER_CHAT_ID_MESSAGE_PREFIX + userId, channelId);
        if(messages == null){
            return;
        }

        int excess = messages.size() - initConfig.getStoreMessage();

        // 超过100个移除元素
        for(int i = 0; i <= excess; i++){
            messages.remove(0);
        }

        Map<String, String> msgMap = new HashMap<>();

        if (msgType == RecordMessageTypeEnum.ASSISTANT){
            msgMap.put("role", "assistant");
        }else if(msgType == RecordMessageTypeEnum.USER_TYPE){
            msgMap.put("role", "user");
        }else if(msgType == RecordMessageTypeEnum.WEB_SYSTEM){
            msgMap.put("role", "web-system");
        }

        msgMap.put("content", msg);
        messages.add(msgMap);

        // 往redis中添加消息
        hashOps.put(USER_CHAT_ID_MESSAGE_PREFIX + userId, channelId, messages);
    }

    // 设置channel信息
    private void setChannelInfo(String userId, String channelId, String channelName, String mode, String chatMode) {

        redisTemplate.opsForValue().set(USER_SELECT_CHANNEL_PREFIX + userId, channelId);
        HashOperations<String, String, List<Map<String, String>>> hashOps = redisTemplate.opsForHash();

        // 将当前channel消息设置为空
        hashOps.put(USER_CHAT_ID_MESSAGE_PREFIX + userId, channelId, new ArrayList<>());

        // {"zzl": [{"id": "角色", "name": "回答内容", "mode": "模式"}, {}]}
        ListOperations<String, Object> listOps = redisTemplate.opsForList();
        Channel channel = new Channel();
        channel.setId(channelId);
        channel.setName(channelName);
        channel.setMode(mode);
        channel.setChatMode(chatMode);
        String channelStr = JSON.toJSONString(channel);

        if(channelId == DEFAULT_CHANNEL_ID){
            List<Object> channelList = listOps.range(USER_CHAT_ID_CHANNEL_PREFIX + userId, 0, -1);

            if(channelList == null || channelList.size() == 0){
                listOps.rightPush(USER_CHAT_ID_CHANNEL_PREFIX + userId, channelStr);
            }else{
                listOps.set(USER_CHAT_ID_CHANNEL_PREFIX + userId, 0, channelStr);
            }
        }else{
            listOps.rightPush(USER_CHAT_ID_CHANNEL_PREFIX + userId, channelStr);
        }

    }

    // 流式 单次聊天调用
    @PostMapping("/sseChat1")
    @CrossOrigin
    public SseEmitter sseChat(@RequestBody Map<String, Object> param){
        String prompt = (String) ((Map)((ArrayList)param.get("messages")).get(0)).get("content");
        Message message = Message.of(prompt);

        SseEmitter sseEmitter = new SseEmitter(-1L);

        SseStreamListener listener = new SseStreamListener(sseEmitter);

        listener.setOnComplate(msg -> {
            // 回答完成，可以做一些事情
            sseEmitter.complete();
            log.info("回答完成: {}", msg);
            return;
        });
        initConfig.getChatGPTStream().streamChatCompletion(Arrays.asList(message), listener);

        return sseEmitter;
    }

    // 流式 单次聊天调用
    @PostMapping("/image")
    @CrossOrigin
    public Object image(@RequestBody Map<Object, Object> param, HttpServletRequest servletRequest) throws IOException {
        String prompt = (String)param.get("msg");
        String userId = (String)param.get("userId");
        String clientIp = getClientIP(servletRequest);

        log.info("userId: [{}], ip: [{}], param: [{}]进入image", userId, clientIp, EncryptUtils.encrypt(prompt));

        // 设置您的OpenAI API密钥
        String apiKey = initConfig.getParamConfig().getApiKeys().get(0);

        // 构建请求头
        Headers headers = new Headers.Builder()
                .add("Authorization", "Bearer " + apiKey)  // 添加Accept-Language头
                .build();

        // 构建请求体
        String requestBodyContent = "{\"prompt\": \"" + prompt.trim() + "\", \"n\": 1, \"size\":\"1024x1024\"}"; // 根据实际需求替换为您的请求体内容
        log.info(EncryptUtils.encrypt(requestBodyContent));
        okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(requestBodyContent, MediaType.parse("application/json"));
        Request request = new Request.Builder().url(IMAGE_OPENAI_URL).headers(headers).post(requestBody).build();
        okhttp3.Response response = okHttpClient.newCall(request).execute();
        String responseString = response.body().string();
        return responseString;
    }

    private boolean checkUser(String userId, String password) {
        UserInfo userInfoByUserId = userInfoService.getUserInfoByUserId(userId);
        if(userInfoByUserId == null){
            return false;
        }
        if(userInfoByUserId.getId() >= PRE_MYSQL_ID){
            UserInfo userInfo = userInfoService.getUserInfoByUserIdAndPass(userId, password);
            if (userInfo == null){
                return false;
            }
        }
        return true;
    }

    private static String getClientIP(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

}
