package com.zhongke.hapilorecord.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by ${xinGen} on 2018/2/8.
 */

public class TimeUtils {
    /**
     * 播放时间格式化
     *
     * @param time
     * @return
     */
    public static String getDate(long time) {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("GMT+0"));
        Date data = new Date(time);
        return format.format(data);
    }


    public static  String setName(long time) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        Date data = new Date(time);
        return format.format(data);
    }
    public static String getTimeToLong(String time){
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        try {
        return   String.valueOf(  format.parse(time).getTime());
        }catch (Exception e){
            e.printStackTrace();
            return String.valueOf(new Date().getTime());
        }
    }
}
