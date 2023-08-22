package com.brother.chatgpt.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTimeFormatUtils {

    static ThreadLocal<SimpleDateFormat> dateStringThreadLocal = ThreadLocal.withInitial(
            () -> new SimpleDateFormat("yyyyMMddHHmmssSSS")
    );

    static ThreadLocal<SimpleDateFormat> dateTimeStringThreadLocal = ThreadLocal.withInitial(
            () -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    );
    /**
     * 将时间类型转换为字符串类型
     * @return
     */
    public static String dateToStr(){
        SimpleDateFormat simpleDateFormat = dateStringThreadLocal.get();
        String dateTimeStr = simpleDateFormat.format(new Date());
        return dateTimeStr;
    }

    /**
     * 将时间类型转换为字符串类型
     * @return
     */
    public static String dateTimeToStr(){
        SimpleDateFormat simpleDateFormat = dateTimeStringThreadLocal.get();
        String dateTimeStr = simpleDateFormat.format(new Date());
        return dateTimeStr;
    }
}
