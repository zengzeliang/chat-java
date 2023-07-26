//package com.brother.chatgpt.controller;
//
//import com.brother.chatgpt.bean.Channel;
//import com.brother.chatgpt.config.InitConfig;
//import com.plexpt.chatgpt.entity.chat.ChatCompletion;
//import com.plexpt.chatgpt.entity.chat.ChatCompletionResponse;
//import com.plexpt.chatgpt.entity.chat.Message;
//import com.plexpt.chatgpt.listener.SseStreamListener;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.logging.log4j.util.Strings;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
//
//import java.text.SimpleDateFormat;
//import java.util.*;
//
//@RestController
//@Slf4j
//public class ChatController1 {
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
//    // 后端只存储用户提问以及系统给出回答的消息
//    // 存储用户每个通道的消息 {"zzl": {"default": [{"role": "角色", "content": "回答内容"}], "channel1": [{"role": "角色", "content": "回答内容"}]}, "zze": {"default": [{"role": "juese", "": ""}]}}
//    private Map<String, Map<String, List>> userChatIdMessageMap = new LinkedHashMap<>();
//    // {"zzl": {"default": {"id": "角色", "name": "回答内容", "mode": "模式"}]}
//    private Map<String, Map<String, Channel>> userChatIdChannelMap = new LinkedHashMap<>();
//
//    // 存储用户默认选择的通道，为空就是表示选择的是default的通道
//    private Map<String, String> userSelectChannelId = new HashMap();
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
//        String selectedChannelId = userSelectChannelId.isEmpty() ? DEFAULT_CHANNEL_ID : userSelectChannelId.get(userId);
//        Map<String, Channel> userChannelList = userChatIdChannelMap.get(userId);
//
//        for (String name : userChannelList.keySet()){
//            Map<String, Object> chatsInfo = new HashMap<>();
//            chatsInfo.put("id", name);
//            chatsInfo.put("name", userChannelList.get(name).getName());
//            chatsInfo.put("selected", selectedChannelId.equals(name));
//            chatsInfo.put("mode", userChannelList.get(name).getMode());
//            chatsInfo.put("messages_total", userChatIdMessageMap.get(userId).get(name).size());
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
//        Map<String, Object> data = new HashMap<>();
//        data.put("name", name);
//        data.put("id", channelId);
//        data.put("selected", true);
//        data.put("messages_total", userChatIdMessageMap.get(userId).get(channelId).size());
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
//        userSelectChannelId.put(userId, id);
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
//        String selectedChannelId =  userSelectChannelId.get(userId);
//
//        List messages = userChatIdMessageMap.get(userId).get(selectedChannelId);
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
//        if(userChatIdMessageMap.containsKey(userId)){
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
//        String channelId = userSelectChannelId.get(userId);
//
//        if(DEFAULT_CHANNEL_ID.equals(channelId)){
//            // 是默认channel 只清空聊天记录
//            userChatIdMessageMap.get(userId).put(channelId, new ArrayList());
//        }else{
//            // 设置默认channel id
//            userSelectChannelId.put(userId, DEFAULT_CHANNEL_ID);
//            // 删除对应的key
//            userChatIdMessageMap.get(userId).remove(channelId);
//            userChatIdChannelMap.get(userId).remove(channelId);
//        }
//        result.put("code", 200);
//        result.put("message", "success");
//        return result;
//    }
//
//    // 流式 单次聊天调用
//    @PostMapping("/sseChat")
//    @CrossOrigin
//    public Object sseChat1(@RequestBody Map<String, Object> param) {
//        // mode: normal 普通对话, continuous连续对话
//        // 参数{"userId": "zzl", "content": "消息内容", "channelId": "cedfaff", "mode": "normal"}
//        long curTime = System.currentTimeMillis();
//        // 定义日期和时间格式
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        // 格式化时间戳
//        String formattedTime = sdf.format(new Date(curTime));
//        param.put("time", formattedTime);
//        String userId = (String) param.get("userId");
//        String prompt = (String) param.get("content");
//        log.info("{}进入sseChat: {}, question: {}", userId, param, prompt);
//
//        if (!checkUser(userId)) {
//            return null;
//        }
//        List<Message> messages = new ArrayList<>();
//
//        // 记得修改channel的mode
//        String channelId = (String) param.get("channelId");
//        String mode = (String) param.get("mode");
//        userChatIdChannelMap.get(userId).get(channelId).setMode(mode);
//
//        if("normal".equals(mode)){
//            messages.add(Message.of(prompt));
//        }else if("continuous".equals(mode)){
//            // 存储用户每个通道的消息 {"zzl": {"default": [{"role": "角色", "content": "回答内容"}], "channel1": [{"role": "角色", "content": "回答内容"}]}, "zze": {"default": [{"role": "juese", "": ""}]}}
//            List preMessages = userChatIdMessageMap.get(userId).get(channelId);
//            List<Map> lastConfigNum = preMessages.subList(Math.max(preMessages.size() - initConfig.getMaxPreContextNum() + 1, 0), preMessages.size());
//            for(Map map : lastConfigNum){
//                if ("assistant".equals(map.get("role"))){
//                    messages.add(Message.ofSystem((String) map.get("content")));
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
//            recordMessageInfo(userId, channelId, prompt, msg);
//            log.info("{} 回答完成: {}", param.get("userId"), msg);
//            return;
//        });
//        initConfig.getChatGPTStream().streamChatCompletion(messages, listener);
//
//        return sseEmitter;
//    }
//
//    private void recordMessageInfo(String userId, String channelId, String prompt, String msg) {
//        List messages = userChatIdMessageMap.get(userId).getOrDefault(channelId, new ArrayList());
//
//        int excess = messages.size() - 100;
//
//        // 超过100个移除元素
//        for(int i = 0; i <= excess; i++){
//            messages.remove(0);
//        }
//
//        Map<String, String> userMsg = new HashMap<>();
//        userMsg.put("role", "user");
//        userMsg.put("content", prompt);
//        Map<String, String> assistantMsg = new HashMap<>();
//        assistantMsg.put("role", "assistant");
//        assistantMsg.put("content", msg);
//
//        messages.add(userMsg);
//        messages.add(assistantMsg);
//    }
//
//    // 设置默认channel信息
//    private void setChannelInfo(String userId, String channelId, String channelName, String mode) {
//
//        userSelectChannelId.put(userId, channelId);
//
//        Map<String, List> chatIdMessageMap = userChatIdMessageMap.getOrDefault(userId, new LinkedHashMap<>());
//        chatIdMessageMap.put(channelId, new ArrayList());
//        userChatIdMessageMap.put(userId, chatIdMessageMap);
//
//        Map<String, Channel> chatIdChannelMap = userChatIdChannelMap.getOrDefault(userId, new LinkedHashMap<>());
//        Channel channel = new Channel();
//        channel.setId(channelId);
//        channel.setName(channelName);
//        channel.setMode(mode);
//        chatIdChannelMap.put(channelId, channel);
//        userChatIdChannelMap.put(userId, chatIdChannelMap);
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
//}
