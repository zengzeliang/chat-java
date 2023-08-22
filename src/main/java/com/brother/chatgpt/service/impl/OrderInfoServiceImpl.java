package com.brother.chatgpt.service.impl;

import com.brother.chatgpt.bean.OrderInfo;
import com.brother.chatgpt.service.OrderInfoService;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.PreparedStatement;
import java.util.List;

@Service
public class OrderInfoServiceImpl implements OrderInfoService {
    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public void saveOrderInfo(OrderInfo orderInfo) {

        jdbcTemplate.execute((ConnectionCallback<Object>) connection -> {
            PreparedStatement ps = connection.prepareStatement("INSERT INTO orderInfo (orderId, orderStatus, memo, userId, createTime) VALUES(?, ?, ?, ?, ?)");
            ps.setString(1, orderInfo.getOrderId());
            ps.setString(2, orderInfo.getOrderStatus());
            ps.setString(3, orderInfo.getMemo());
            ps.setInt(4, orderInfo.getUserId());
            ps.setString(5, orderInfo.getCreateTime());
            ps.executeUpdate();
            return null;
        });
    }

    /**
     * 根据orderId查询订单信息
     * @param outTradeNo
     * @return
     */
    @Override
    public OrderInfo getOrderByOrderId(String outTradeNo) {

        String sql = "select * from orderInfo where orderId = ?";
        List<OrderInfo> orderInfoList = jdbcTemplate.query(sql, (PreparedStatementSetter) preparedStatement -> {
            preparedStatement.setString(1, outTradeNo); // 设置第一个占位符的值为 18
        }, new BeanPropertyRowMapper<>(OrderInfo.class));

        if(orderInfoList == null || orderInfoList.size() == 0){
            return null;
        }
        return orderInfoList.get(0);
    }

    /**
     * 更新订单状态
     * @param outTradeNo
     * @param status
     */
    @Override
    public void updateOrderStatus(String outTradeNo, String status) {
        String sql = "UPDATE orderInfo SET orderStatus = ? WHERE orderId = ?";
        jdbcTemplate.update(sql, status, outTradeNo);
    }

    @Override
    public void updateOrderInfoByUserId(OrderInfo orderInfo, Integer id) {
        String updateSql = "UPDATE orderInfo SET orderId = ?, orderStatus = ?, memo = ?, createTime = ?  WHERE userId = ?";
        jdbcTemplate.update(updateSql, orderInfo.getOrderId(), orderInfo.getOrderStatus(), orderInfo.getMemo(), orderInfo.getCreateTime(), id);
    }

}
