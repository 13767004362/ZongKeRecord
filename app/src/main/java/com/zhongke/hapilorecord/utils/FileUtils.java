package com.zhongke.hapilorecord.utils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by ${xinGen} on 2018/2/9.
 */

public class FileUtils {
    public static  void deleteFileOrFileDirectory(String filePath){
        File file=new File(filePath);
        if (file==null&&!file.exists()){
            return;
        }
        if (file.isDirectory()){
            deleteFileDirectory(file);
        }else{
            deleteFile(file);
        }
    }
    public static void deleteFile(File file){
        if (file==null&&!file.exists()&&!file.isFile()){
            return;
        }
        file.delete();
    }
    public static  void deleteFileDirectory(File dir){
        if (dir == null || !dir.exists() || !dir.isDirectory())
            return;
        for (File file : dir.listFiles()) {
            if (file.isFile())
                file.delete(); // 删除所有文件
            else if (file.isDirectory())
                deleteFileDirectory(file); // 递规的方式删除文件夹
        }
        // 删除目录本身
        dir.delete();
    }

    /**
     * 获取Amr文件的时长
     *
     * @param file
     * @return 文件时长
     * @throws IOException
     */
    public static long getAmrDuration(File file) throws IOException {
        long duration = -1;
        int[] packedSize = {12, 13, 15, 17, 19, 20, 26, 31, 5, 0, 0, 0, 0, 0,
                0, 0};
        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(file, "rw");
            long length = file.length();// 文件的长度
            int pos = 6;// 设置初始位置
            int frameCount = 0;// 初始帧数
            int packedPos = -1;

            byte[] datas = new byte[1];// 初始数据值
            while (pos <= length) {
                randomAccessFile.seek(pos);
                if (randomAccessFile.read(datas, 0, 1) != 1) {
                    duration = length > 0 ? ((length - 6) / 650) : 0;
                    break;
                }
                packedPos = (datas[0] >> 3) & 0x0F;
                pos += packedSize[packedPos] + 1;
                frameCount++;
            }

            duration += frameCount * 20;// 帧数*20
        } finally {
            if (randomAccessFile != null) {
                randomAccessFile.close();
            }
        }
        return duration;
    }


}
