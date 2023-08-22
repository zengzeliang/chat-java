package com.brother.chatgpt.service.impl;

import com.brother.chatgpt.bean.OrderInfo;
import com.brother.chatgpt.bean.UserInfo;
import com.brother.chatgpt.service.UserInfoService;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

@Service
public class UserInfoServiceImpl implements UserInfoService {

    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 保存用户信息
     * @param userInfo
     */
    @Override
    public Integer saveUserInfo(UserInfo userInfo) {

        KeyHolder keyHolder = new GeneratedKeyHolder();
        String sql = "INSERT INTO userInfo (userId, password, buy, createTime, state) VALUES (?, ?, ?, ?, ?)";
        PreparedStatementCreator preparedStatementCreator = con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, userInfo.getUserId());
            ps.setString(2, userInfo.getPassword());
            ps.setInt(3, userInfo.getBuy());
            ps.setString(4, userInfo.getCreateTime());
            ps.setInt(5, userInfo.getState());
            return ps;
        };
        jdbcTemplate.update(preparedStatementCreator, keyHolder);
        Number generatedId = keyHolder.getKey();
        if(generatedId != null){
            return generatedId.intValue();
        }
        return -1;
    }

    /**
     * 根据用户号查询订单信息
     * @param userId
     */
    @Override
    public UserInfo getUserInfoByUserId(String userId) {
        String sql = "select * from userInfo where userId = ?";
        List<UserInfo> userInfoList = jdbcTemplate.query(sql, preparedStatement -> {
            preparedStatement.setString(1, userId); // 设置第一个占位符的值为 18
        }, new BeanPropertyRowMapper<>(UserInfo.class));

        if(userInfoList == null || userInfoList.size() == 0){
            return null;
        }
        return userInfoList.get(0);
    }

    /**
     * 根据id去更新状态
     * @param userId
     * @param buy
     */
    @Override
    public void updateUserBuyStatus(Integer userId, int buy) {
        String sql = "UPDATE userInfo SET buy = ? WHERE id = ?";
        jdbcTemplate.update(sql, buy, userId);

    }

    /**
     * 根据用户id和密码判断是否已经购买
     * @param userId
     * @param password
     * @return
     */
    @Override
    public UserInfo getUserInfoByUserIdAndPass(String userId, String password) {
        String sql = "select * from userInfo where userId = ? and password = ? and state = 1 and buy = 1";
        List<UserInfo> userInfoList = jdbcTemplate.query(sql, (PreparedStatementSetter) preparedStatement -> {
            preparedStatement.setString(1, userId); // 设置第一个占位符的值为
            preparedStatement.setString(2, password); // 设置第二个占位符的值为
        }, new BeanPropertyRowMapper<>(UserInfo.class));

        if(userInfoList == null || userInfoList.size() == 0){
            return null;
        }
        return userInfoList.get(0);

    }

    @Override
    public void updateUserInfoById(UserInfo userInfo, Integer id) {
        String updateSql = "UPDATE userInfo SET userId = ?, password = ?, buy = ?, createTime = ?, state = ? WHERE id = ?";
        jdbcTemplate.update(updateSql, userInfo.getUserId(), userInfo.getPassword(), userInfo.getBuy(), userInfo.getCreateTime(), userInfo.getState(), id);
    }
}
