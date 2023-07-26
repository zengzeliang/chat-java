//package com.brother.chatgpt.controller;
//
//import com.alibaba.fastjson.JSON;
//import com.brother.chatgpt.bean.Channel;
//import com.brother.chatgpt.config.InitConfig;
//import com.brother.chatgpt.enums.RecordMessageTypeEnum;
//import com.plexpt.chatgpt.entity.chat.ChatCompletion;
//import com.plexpt.chatgpt.entity.chat.ChatCompletionResponse;
//import com.plexpt.chatgpt.entity.chat.Message;
//import com.plexpt.chatgpt.listener.SseStreamListener;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.logging.log4j.util.Strings;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.redis.core.HashOperations;
//import org.springframework.data.redis.core.ListOperations;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
//
//import javax.servlet.http.HttpServletRequest;
//import java.text.SimpleDateFormat;
//import java.util.*;
//import java.util.concurrent.TimeUnit;
//
//@RestController
//@Slf4j
//public class ChatController2 {
//    @Autowired
//    private InitConfig initConfig;
//
//    @Autowired
//    private RedisTemplate<String, Object> redisTemplate;
//
//    private static final String DEFAULT_CHANNEL_ID = "default";
//    private static final String DEFAULT_CHANNEL_NAME = "默认对话";
//
//    private static final String DEFAULT_CHANNEL_MODE = "continuous";
//
//    // 存储用户每个通道的消息 {"zzl": {"default": [{"role": "角色", "content": "回答内容"}], "channel1": [{"role": "角色", "content": "回答内容"}]}, "zze": {"default": [{"role": "juese", "": ""}]}}
//    private static final String USER_CHAT_ID_MESSAGE_PREFIX = "chat_id_message";
//
//    // {"zzl": [{"id": "角色", "name": "回答内容", "mode": "模式"}, {"id": "角色", "name": "回答内容", "mode": "模式"}]}
//    private static final String USER_CHAT_ID_CHANNEL_PREFIX = "chat_id_channel";
//
//    private static final String USER_SELECT_CHANNEL_PREFIX = "select_channel";
//
//    // 非流式 单次聊天调用
//    @GetMapping("/chat")
//    @CrossOrigin
//    public Object chat(String prompt){
//        Message message = Message.of(prompt);
//
//        ChatCompletion chatCompletion = ChatCompletion.builder()
//                .model(ChatCompletion.Model.GPT_3_5_TURBO.getName())
//                .messages(Arrays.asList(message))
//                .maxTokens(3000)
//                .temperature(0.9)
//                .build();
//        ChatCompletionResponse response = initConfig.getChatGPT().chatCompletion(chatCompletion);
//        Message res = response.getChoices().get(0).getMessage();
//
//        Map<String, Object> result = new HashMap<>();
//        result.put("role", res.getRole());
//        result.put("content", res.getContent());
//        return result;
//    }
//
//    // 加载
//    @GetMapping("/loadChats")
//    @CrossOrigin
//    public Object loadChats(String userId){
//        Map<String, Object> result = new HashMap<>();
//        List<Map> data = new ArrayList<>();
//
//        // 用户校验不通过，返回空聊天窗口
//        if(!checkUser(userId)){
//            result.put("code", 201);
//            result.put("data", data);
//            result.put("message", "用户校验失败");
//            return result;
//        }
//        log.info("{}进入loadChats", userId);
//
//        // 获取用户所有的chat channel
//        String selectedChannelId = (String) redisTemplate.opsForValue().get(USER_SELECT_CHANNEL_PREFIX + userId);
//        if(Strings.isEmpty(selectedChannelId)){
//            selectedChannelId = DEFAULT_CHANNEL_ID;
//        }
//
//        // {"zzl": [{"id": "角色", "name": "回答内容", "mode": "模式"}, {}]}
//        ListOperations<String, Object> listOps = redisTemplate.opsForList();
//        List<Object> channelInfoList = listOps.range(USER_CHAT_ID_CHANNEL_PREFIX + userId, 0, -1);
//
//        HashOperations<String, String, List<Map<String, String>>> hashOps = redisTemplate.opsForHash();
//
//        for (Object info : channelInfoList){
//            String channelInfoJson = (String)info;
//            Channel channel = JSON.parseObject(channelInfoJson, Channel.class);
//            Map<String, Object> chatsInfo = new HashMap<>();
//            chatsInfo.put("id", channel.getId());
//            chatsInfo.put("name", channel.getName());
//            chatsInfo.put("selected", selectedChannelId.equals(channel.getId()));
//            chatsInfo.put("mode", channel.getMode());
//
//            // 存储用户每个通道的消息 {"zzl": {"default": [{"role": "角色", "content": "回答内容"}], "channel1": [{"role": "角色", "content": "回答内容"}]}, "zze": {"default": [{"role": "juese", "": ""}]}}
//
//            List<Map<String, String>> maps = hashOps.get(USER_CHAT_ID_MESSAGE_PREFIX + userId, channel.getId());
//            if(maps == null){
//                maps = new ArrayList<>();
//            }
//
//            chatsInfo.put("messages_total", maps.size());
//            data.add(chatsInfo);
//        }
//
//        result.put("code", 200);
//        result.put("data", data);
//        result.put("message", "success");
//        return result;
//    }
//
//    // 创建新的聊天窗口
//    @GetMapping("/newChat")
//    @CrossOrigin
//    public Object newChat(String name, String userId){
//        log.info("进入newChat name: [{}], userId: [{}]", name, userId);
//        Map<String, Object> result = new HashMap<>();
//
//        // 用户校验不通过
//        if(!checkUser(userId)){
//            result.put("code", 201);
//            result.put("message", "请先创建或输入已有用户id");
//            return result;
//        }
//
//        // 生成聊天channel id
//        String channelId = UUID.randomUUID().toString();
//        setChannelInfo(userId, channelId, name, DEFAULT_CHANNEL_MODE);
//
//        HashOperations<String, String, List<Map<String, String>>> hashOps = redisTemplate.opsForHash();
//        Map<String, Object> data = new HashMap<>();
//        data.put("name", name);
//        data.put("id", channelId);
//        data.put("selected", true);
//        data.put("messages_total", hashOps.get(USER_CHAT_ID_MESSAGE_PREFIX + userId, channelId).size());
//
//        result.put("code", 200);
//        result.put("message", "success");
//        result.put("data", data);
//        return result;
//    }
//
//    // 选择chat channel
//    @GetMapping("/selectChat")
//    @CrossOrigin
//    public Object selectChat(String userId, String id) {
//        log.info("selectChat userId: [{}]", userId);
//        Map<String, Object> result = new HashMap<>();
//
//        if(!checkUser(userId)){
//            result.put("code", 201);
//            result.put("message", "请先创建或输入已有用户id");
//            return result;
//        }
//        // 设置默认选择的channelId
//        redisTemplate.opsForValue().set(USER_SELECT_CHANNEL_PREFIX + userId, id);
//        result.put("code", 200);
//        result.put("message", "success");
//        return result;
//    }
//
//    // 加载
//    @GetMapping("/loadHistory")
//    @CrossOrigin
//    public Object loadHistory(String userId){
//        log.info("loadHistory userId: [{}]", userId);
//        Map<String, Object> result = new HashMap<>();
//
//        // 用户校验不通过
//        if(!checkUser(userId)){
//            result.put("code", 201);
//            result.put("message", "请先创建或输入已有用户id");
//            return result;
//        }
//        // 获取当前选择的聊天channel
//        String selectedChannelId = (String) redisTemplate.opsForValue().get(USER_SELECT_CHANNEL_PREFIX + userId);
//
//        HashOperations<String, String, List<Map<String, String>>> hashOps = redisTemplate.opsForHash();
//
//        List<Map<String, String>> messages = hashOps.get(USER_CHAT_ID_MESSAGE_PREFIX + userId, selectedChannelId);
//
//        result.put("code", 200);
//        result.put("message", "success");
//        result.put("data", messages);
//        return result;
//    }
//
//    // 设置用户id
//    @PostMapping("/setUserId")
//    @CrossOrigin
//    public Object userId(@RequestBody Map<String, Object> param) {
//        log.info("进入setUserId, {}", param);
//
//        String userId = (String) param.get("userId");
//
//        if(Strings.isEmpty(userId)){
//            Map<String, Object> result = new HashMap<>();
//            result.put("code", 202);
//            result.put("message", "userId不能为空");
//            return result;
//        }
//
//        boolean userExist = redisTemplate.hasKey(USER_CHAT_ID_MESSAGE_PREFIX + userId);
//
//        if(userExist){
//            Map<String, Object> result = new HashMap<>();
//            result.put("code", 201);
//            result.put("message", "userId已经存在");
//            return result;
//        }else{
//            // 设置默认channel
//            setChannelInfo(userId, DEFAULT_CHANNEL_ID, DEFAULT_CHANNEL_NAME, DEFAULT_CHANNEL_MODE);
//            Map<String, Object> result = new HashMap<>();
//            result.put("code", 200);
//            result.put("message", "success");
//            return result;
//        }
//    }
//
//    // 删除聊天
//    @GetMapping("/deleteHistory")
//    @CrossOrigin
//    public Object deleteHistory(String userId) {
//        Map<String, Object> result = new HashMap<>();
//
//        if (!checkUser(userId)) {
//            result.put("code", 201);
//            result.put("message", "删除历史记录失败");
//            return result;
//        }
//        // 默认channel不能删除，获取当前选择的channel
//        String channelId = (String) redisTemplate.opsForValue().get(USER_SELECT_CHANNEL_PREFIX + userId);
//
//        if(DEFAULT_CHANNEL_ID.equals(channelId)){
//            // 是默认channel 只清空聊天记录
//            HashOperations<String, String, List<Map<String, String>>> hashOps = redisTemplate.opsForHash();
//            hashOps.put(USER_CHAT_ID_MESSAGE_PREFIX + userId, channelId, new ArrayList<>());
//        }else{
//            // 设置默认channel id
//            redisTemplate.opsForValue().set(USER_SELECT_CHANNEL_PREFIX + userId, DEFAULT_CHANNEL_ID);
//
//            // 删除对应的key，消息对应的key
//            HashOperations<String, String, List<Map<String, String>>> hashOps = redisTemplate.opsForHash();
//            hashOps.delete(USER_CHAT_ID_MESSAGE_PREFIX + userId, channelId);
//
//            // 删除channel对应的消息
//            ListOperations<String, Object> listOps = redisTemplate.opsForList();
//            List<Object> channels = listOps.range(USER_CHAT_ID_CHANNEL_PREFIX + userId, 0, -1);
//
//            if(channels != null){
//                for(Object info : channels){
//                    String channelInfoJson = (String)info;
//                    Channel channel = JSON.parseObject(channelInfoJson, Channel.class);
//                    if (channelId.equals(channel.getId())){
//                        listOps.remove(USER_CHAT_ID_CHANNEL_PREFIX + userId, 1, info);
//                    }
//                }
//            }
//        }
//        result.put("code", 200);
//        result.put("message", "success");
//        return result;
//    }
//
//    // 流式 单次聊天调用
//    @PostMapping("/sseChat")
//    @CrossOrigin
//    public Object sseChat1(@RequestBody Map<String, Object> param, HttpServletRequest request) {
//        // mode: normal 普通对话, continuous连续对话
//        // 参数{"userId": "zzl", "content": "消息内容", "channelId": "cedfaff", "mode": "normal"}
//        long curTime = System.currentTimeMillis();
//        // 定义日期和时间格式
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        // 格式化时间戳
//        String formattedTime = sdf.format(new Date(curTime));
//        param.put("time", formattedTime);
//        String channelId = (String) param.get("channelId");
//        String userId = (String) param.get("userId");
//        String prompt = (String) param.get("content");
//
//        String clientIP = getClientIP(request);
//        log.info("user[{}], ip[{}]进入sseChat", userId, clientIP);
//        log.info("{}进入sseChat channelId: {}: question: {}, param: {}", userId, channelId, prompt, param);
//
//        if (!checkUser(userId)) {
//            return "error";
//        }
//
//        if("UNAUTHORIZED".equals(userId)){
//            // 单独处理
//            List<Message> messages = new ArrayList<>();
//            List<Map<String, String>> data = (List<Map<String, String>>) redisTemplate.opsForValue().get("zzl");
//
//
//            ListOperations<String, Object> listOps = redisTemplate.opsForList();
//            List<Object> redisPreMessages = listOps.range(USER_CHAT_ID_MESSAGE_PREFIX + clientIP, 0, -1);
//            if(redisPreMessages != null){
//                List<Object> lastConfigNum = redisPreMessages.subList(Math.max(redisPreMessages.size() - initConfig.getMaxPreContextNum() + 1, 0), redisPreMessages.size());
//                for(Object obj : lastConfigNum){
//                    Map<String, String> map = (Map) obj;
//                    if ("assistant".equals(map.get("role"))){
//                        messages.add(Message.ofSystem(map.getOrDefault("content", "")));
//                    }else if("user".equals(map.get("role"))){
//                        messages.add(Message.of(map.getOrDefault("content", "")));
//                    }
//                }
//            }else{
//                redisPreMessages = new ArrayList<>();
//            }
//
//            messages.add(Message.of(prompt));
//            SseEmitter sseEmitter = new SseEmitter(-1L);
//            SseStreamListener listener = new SseStreamListener(sseEmitter);
//            List<Object> finalRedisPreMessages = redisPreMessages;
//            listener.setOnComplate(msg -> {
//                // 回答完成，可以做一些事情
//                sseEmitter.complete();
//                // 记录用户问题以及给出的消息
//                Map<String, String> userMessage = new HashMap<>();
//                userMessage.put("role", "user");
//                userMessage.put("content", prompt);
//
//                Map<String, String> assistantMessage = new HashMap<>();
//                assistantMessage.put("role", "user");
//                assistantMessage.put("content", msg);
//
//                finalRedisPreMessages.add(userMessage);
//                finalRedisPreMessages.add(assistantMessage);
//
//                listOps.rightPush(USER_CHAT_ID_MESSAGE_PREFIX + clientIP, userMessage);
//
//                redisTemplate.opsForValue().set(USER_CHAT_ID_MESSAGE_PREFIX + clientIP, finalRedisPreMessages, 1, TimeUnit.DAYS);
//                log.info("{}，{}回答完成: {}", param.get("userId"), channelId, msg);
//                return;
//            });
//            initConfig.getChatGPTStream().streamChatCompletion(messages, listener);
//            return sseEmitter;
//        }
//
//        List<Message> messages = new ArrayList<>();
//
//        // 记得修改channel的mode
//        String mode = (String) param.get("mode");
//
//        ListOperations<String, Object> listOps = redisTemplate.opsForList();
//        List<Object> channelInfoList = listOps.range(USER_CHAT_ID_CHANNEL_PREFIX + userId, 0, -1);
//
//        // 找到这个channle，并且修改mode设置
//        int index;
//        Channel targetChannel = null;
//
//        for (index = 0; index < channelInfoList.size(); index++) {
//            Object info = channelInfoList.get(index);
//            String channelInfoJson = (String) info;
//            targetChannel = JSON.parseObject(channelInfoJson, Channel.class);
//            if (channelId.equals(targetChannel.getId())){
//                break;
//            }
//        }
//
//        if(targetChannel == null){
//            targetChannel = new Channel(DEFAULT_CHANNEL_ID, DEFAULT_CHANNEL_NAME, DEFAULT_CHANNEL_MODE);
//            targetChannel.setMode(mode);
//            listOps.set(USER_CHAT_ID_CHANNEL_PREFIX + userId, 0, JSON.toJSONString(targetChannel));
//        }else{
//            targetChannel.setMode(mode);
//            listOps.set(USER_CHAT_ID_CHANNEL_PREFIX + userId, index, JSON.toJSONString(targetChannel));
//        }
//
//        if("normal".equals(mode)){
//            messages.add(Message.of(prompt));
//        }else if("continuous".equals(mode)){
//            // 存储用户每个通道的消息 {"zzl": {"default": [{"role": "角色", "content": "回答内容"}], "channel1": [{"role": "角色", "content": "回答内容"}]}, "zze": {"default": [{"role": "juese", "": ""}]}}
//            HashOperations<String, String, List<Map<String, String>>> hashOps = redisTemplate.opsForHash();
//            List<Map<String, String>> preMessages = hashOps.get(USER_CHAT_ID_MESSAGE_PREFIX + userId, channelId);
//
//            // 有两种情况，1. 是redis获取超时，一次未获取到。2. 在设置user的时候超时，未设置成功存储消息的list
//            int retryTime = 2;
//            while (retryTime > 0 && preMessages == null){
//                preMessages = hashOps.get(USER_CHAT_ID_MESSAGE_PREFIX + userId, channelId);
//                retryTime --;
//            }
//
//            // 还是为空，单独给用户id设置消息
//            if(preMessages == null){
//                preMessages = new ArrayList<>();
//                hashOps.put(USER_CHAT_ID_MESSAGE_PREFIX + userId, channelId, preMessages);
//            }
//
//            List<Map<String, String>> lastConfigNum = preMessages.subList(Math.max(preMessages.size() - initConfig.getMaxPreContextNum() + 1, 0), preMessages.size());
//            for(Map map : lastConfigNum){
//                if ("assistant".equals(map.get("role"))){
//                    messages.add(Message.ofSystem((String) map.getOrDefault("content", "")));
//                }else if("user".equals(map.get("role"))){
//                    messages.add(Message.of((String) map.getOrDefault("content", "")));
//                }
//            }
//            // 加入当前消息
//            messages.add(Message.of(prompt));
//        }
//        SseEmitter sseEmitter = new SseEmitter(-1L);
//        SseStreamListener listener = new SseStreamListener(sseEmitter);
//        listener.setOnComplate(msg -> {
//            // 回答完成，可以做一些事情
//            sseEmitter.complete();
//
//            // 记录用户问题以及给出的消息
//            recordMessageInfo(userId, channelId, prompt, RecordMessageTypeEnum.USER_TYPE);
//            recordMessageInfo(userId, channelId, msg, RecordMessageTypeEnum.ASSISTANT);
//
//            log.info("{}，{}回答完成: {}", param.get("userId"), channelId, msg);
//            return;
//        });
//        initConfig.getChatGPTStream().streamChatCompletion(messages, listener);
//
//        return sseEmitter;
//    }
//
//    // 流式 单次聊天调用
//    @PostMapping("/recordMessage")
//    @CrossOrigin
//    public Object recordMessage(@RequestBody Map<Object, Object> param) {
//        String userId = (String) param.get("userId");
//
//        String channelId = (String) param.get("channelId");
//
//        String msg = (String) param.get("msg");
//
//        log.info("{}进入recordMessage, msg{}", userId, msg);
//
//        Integer messageType = (Integer) param.get("messageType");
//
//        RecordMessageTypeEnum enumValue = null;
//        for (RecordMessageTypeEnum member : RecordMessageTypeEnum.values()) {
//            if (member.getType().equals(messageType)) {
//                enumValue = member;
//                break;
//            }
//        }
//        recordMessageInfo(userId, channelId, msg, enumValue);
//
//        Map<String, Object> result = new HashMap<>();
//        result.put("code", 200);
//        result.put("message", "success");
//        return result;
//    }
//
//    private void recordMessageInfo(String userId, String channelId, String msg, RecordMessageTypeEnum msgType) {
//
//        HashOperations<String, String, List<Map<String, String>>> hashOps = redisTemplate.opsForHash();
//        List<Map<String, String>> messages = hashOps.get(USER_CHAT_ID_MESSAGE_PREFIX + userId, channelId);
//        if(messages == null){
//            return;
//        }
//
//        int excess = messages.size() - initConfig.getStoreMessage();
//
//        // 超过100个移除元素
//        for(int i = 0; i <= excess; i++){
//            messages.remove(0);
//        }
//
//        Map<String, String> msgMap = new HashMap<>();
//
//        if (msgType == RecordMessageTypeEnum.ASSISTANT){
//            msgMap.put("role", "assistant");
//        }else if(msgType == RecordMessageTypeEnum.USER_TYPE){
//            msgMap.put("role", "user");
//        }else if(msgType == RecordMessageTypeEnum.WEB_SYSTEM){
//            msgMap.put("role", "web-system");
//        }
//
//        msgMap.put("content", msg);
//        messages.add(msgMap);
//
//        // 往redis中添加消息
//        hashOps.put(USER_CHAT_ID_MESSAGE_PREFIX + userId, channelId, messages);
//    }
//
//    // 设置channel信息
//    private void setChannelInfo(String userId, String channelId, String channelName, String mode) {
//
//        redisTemplate.opsForValue().set(USER_SELECT_CHANNEL_PREFIX + userId, channelId);
//        HashOperations<String, String, List<Map<String, String>>> hashOps = redisTemplate.opsForHash();
//
//        // 将当前channel消息设置为空
//        hashOps.put(USER_CHAT_ID_MESSAGE_PREFIX + userId, channelId, new ArrayList<>());
//
//        // {"zzl": [{"id": "角色", "name": "回答内容", "mode": "模式"}, {}]}
//        ListOperations<String, Object> listOps = redisTemplate.opsForList();
//        Channel channel = new Channel();
//        channel.setId(channelId);
//        channel.setName(channelName);
//        channel.setMode(mode);
//        String channelStr = JSON.toJSONString(channel);
//
//        listOps.rightPush(USER_CHAT_ID_CHANNEL_PREFIX + userId, channelStr);
//    }
//
//    // 流式 单次聊天调用
//    @PostMapping("/sseChat1")
//    @CrossOrigin
//    public SseEmitter sseChat(@RequestBody Map<String, Object> param){
//        String prompt = (String) ((Map)((ArrayList)param.get("messages")).get(0)).get("content");
//        Message message = Message.of(prompt);
//
//        SseEmitter sseEmitter = new SseEmitter(-1L);
//
//        SseStreamListener listener = new SseStreamListener(sseEmitter);
//
//        listener.setOnComplate(msg -> {
//            // 回答完成，可以做一些事情
//            sseEmitter.complete();
//            log.info("回答完成: {}", msg);
//            return;
//        });
//        initConfig.getChatGPTStream().streamChatCompletion(Arrays.asList(message), listener);
//
//        return sseEmitter;
//    }
//
//    private boolean checkUser(String userId) {
//
//        if (Strings.isEmpty(userId)){
//            return false;
//        }
//        return true;
//    }
//
//    private static String getClientIP(HttpServletRequest request) {
//        String ipAddress = request.getHeader("X-Forwarded-For");
//        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
//            ipAddress = request.getHeader("Proxy-Client-IP");
//        }
//        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
//            ipAddress = request.getHeader("WL-Proxy-Client-IP");
//        }
//        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
//            ipAddress = request.getHeader("HTTP_CLIENT_IP");
//        }
//        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
//            ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
//        }
//        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
//            ipAddress = request.getRemoteAddr();
//        }
//        return ipAddress;
//    }
//
//}
