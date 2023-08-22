package com.brother.chatgpt.controller;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConstants;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.brother.chatgpt.bean.OrderInfo;
import com.brother.chatgpt.bean.UserInfo;
import com.brother.chatgpt.config.AliPayResource;
import com.brother.chatgpt.config.PaymentConfig;
import com.brother.chatgpt.enums.OrderStatusEnum;
import com.brother.chatgpt.service.OrderInfoService;
import com.brother.chatgpt.service.TransactionService;
import com.brother.chatgpt.service.UserInfoService;
import com.brother.chatgpt.util.BizNoGenerateUtils;
import com.brother.chatgpt.util.DateTimeFormatUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@CrossOrigin
@RequestMapping("/payment")
public class PayController {

    @Autowired
    private PaymentConfig paymentConfig;

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private AliPayResource aliPayResource;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private OrderInfoService orderInfoService;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private TransactionService transactionService;

    private static final double EPSILON = 0.000001;

    @PostMapping("/goAlipay")
    public Object aliPay(@RequestBody Map<String, String> param) {

        log.info("进入aliPay, param: {}", param);
        Map<String, Object> result = new HashMap<>();

        // 构造调用接口请求类
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();

        alipayRequest.setReturnUrl(paymentConfig.getReturnUrl());

        alipayRequest.setNotifyUrl(paymentConfig.getNotifyUrl());
        // 商户订单号，商户网站订单系统重唯一订单号
        Long orderNo = redisTemplate.opsForValue().increment(paymentConfig.getOrderExtendBizPrefix());
        String formattedNumber = String.format("%04d", orderNo % 10000);
        String outTradeNo = BizNoGenerateUtils.generateOrderId(formattedNumber);
        String totalAmount = paymentConfig.getProductAmount();
        // 商品描述body可空, subject订单名称
        String subject = paymentConfig.getSubject();
        String timeoutExpress = paymentConfig.getTimeoutExpress();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("out_trade_no", outTradeNo);
        jsonObject.put("total_amount", totalAmount);
        jsonObject.put("subject", subject);
        jsonObject.put("timeout_express", timeoutExpress);
        jsonObject.put("product_code", "FAST_INSTANT_TRADE_PAY");
        alipayRequest.setBizContent(jsonObject.toJSONString());
        //请求
        String alipayForm = "";
        try {
            alipayForm = alipayClient.pageExecute(alipayRequest).getBody();
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        // 保存用户以及订单信息
        String userId = param.get("buyPhone");
        String password = param.get("buyPassword");

        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(userId);
        userInfo.setBuy(0);
        userInfo.setPassword(password);
        userInfo.setState(1);
        userInfo.setCreateTime(DateTimeFormatUtils.dateTimeToStr());

        UserInfo existUserInfo = userInfoService.getUserInfoByUserId(userId);

        if(existUserInfo == null){
            // 保存用户和订单信息
            transactionService.saveUserInfoAndOrderInfo(userInfo, outTradeNo, jsonObject);
        }else{
            // 已经付款了
            if(existUserInfo.getBuy() == 1){
                result.put("code", 201);
                result.put("message", "手机号已成功支付，请更换手机号购买");
                return result;
            }else{
                // 更新订单信息
                transactionService.updateUserInfoAndOrderInfo(userInfo, outTradeNo, jsonObject, existUserInfo);
            }
        }

        log.info("支付宝支付 - 前往支付页面, alipayForm: \n{}", alipayForm);

        result.put("code", 200);
        result.put("message", "success");
        result.put("data", alipayForm);

        return result;
    }

    /**
     * 支付宝异步通知回调接口
     * @return
     */
    @PostMapping("/notify")
    public String alipayNotify(@RequestParam Map<String, String> params) {
        log.info("支付通知正在执行, param: {}", params);
        String result = "failure";
        try {
            //异步通知验签
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    aliPayResource.getAlipayPublicKey(),
                    AlipayConstants.CHARSET_UTF8,
                    AlipayConstants.SIGN_TYPE_RSA2); //调用SDK验证签名

            if(!signVerified){
                //验签失败则记录异常日志，并在response中返回failure.
                log.error("支付成功异步通知验签失败！");
                return result;
            }

            // 验签成功后
            log.info("支付成功异步通知验签成功！");

            //按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，
            //1 商户需要验证该通知数据中的 out_trade_no 是否为商户系统中创建的订单号
            String outTradeNo = params.get("out_trade_no");
            OrderInfo order = orderInfoService.getOrderByOrderId(outTradeNo);
            if(order == null){
                log.error("订单不存在");
                return result;
            }

            //2 判断 total_amount 是否确实为该订单的实际金额（即商户订单创建时的金额）
            String totalAmount = params.get("total_amount");
            double totalAmountD = Double.parseDouble(totalAmount);
            String productAmount = paymentConfig.getProductAmount();
            double productAmountD = Double.parseDouble(productAmount);

            if(Math.abs(totalAmountD - productAmountD) > EPSILON){
                log.error("金额校验失败");
                return result;
            }

            //3 验证 app_id 是否为该商户本身
            String appId = params.get("app_id");
            String appIdProperty = aliPayResource.getAppId();
            if(!appId.equals(appIdProperty)){
                log.error("appid校验失败");
                return result;
            }

            //在支付宝的业务通知中，只有交易通知状态为 TRADE_SUCCESS时，
            // 支付宝才会认定为买家付款成功。
            String tradeStatus = params.get("trade_status");
            if(!"TRADE_SUCCESS".equals(tradeStatus)){
                log.error("支付未成功");
                return result;
            }
            //处理业务 修改订单状态 记录支付日志
            transactionService.updateUserAndOrderStatus(outTradeNo, order.getUserId());
            //校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            result = "success";
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        return result;
    }

}
