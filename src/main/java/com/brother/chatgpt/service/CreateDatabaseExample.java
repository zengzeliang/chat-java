package com.brother.chatgpt.service;

import java.sql.*;

public class CreateDatabaseExample {
    public static void main(String[] args) {
        // 数据库连接信息
        String url = "jdbc:mysql://82.156.167.18:3306/chatgpt";
        String username = "root";
        String password = "123456";

        // 加载并注册MySQL驱动程序
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return;
        }


        // 建立数据库连接
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            System.out.println("成功连接到数据库！");

            // 在此处执行您的数据库操作

        } catch (SQLException e) {
            System.out.println("连接数据库时发生错误！");
            e.printStackTrace();
        }
    }
}
