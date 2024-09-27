//package com.xuecheng.media;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.util.DigestUtils;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.io.RandomAccessFile;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.List;
//
///**
// * @author LH
// * @version 1.0
// * @description 测试大文件上传方法
// * @date 2024/07/29 14:08
// */
//public class BigFileTest {
//
//    //分块测试
//    @Test
//    public void testChunk() throws IOException {
//        //源文件
//        File sourceFile = new File("C:\\Users\\17407\\Videos\\屏幕录制\\屏幕录制 2024-06-23 200707.mp4");
//        //分块文件存储路径
//        String chunkFilePath = "C:\\Users\\17407\\Videos\\屏幕录制\\chunk\\";
//        //分块文件大小
//        int chunkSize = 1024 * 1024 * 5;
//        //分块文件个数
//        int chunkNum = (int) Math.ceil(sourceFile.length() * 1.0 / chunkSize);
//        //使用流从源文件读数据，向分块文件中写数据
//        RandomAccessFile raf_r = new RandomAccessFile(sourceFile, "r");
//        //缓存区
//        byte[] bytes = new byte[1024];
//        for (int i = 0; i < chunkNum; i++) {
//            File chunkFile = new File(chunkFilePath + i);
//            //分块文件写入流
//            RandomAccessFile raf_rw = new RandomAccessFile(chunkFile, "rw");
//            int len = -1;
//            while ((len = raf_r.read(bytes)) != -1) {
//                raf_rw.write(bytes, 0, len);
//                if (chunkFile.length() >= chunkSize) {
//                    break;
//                }
//            }
//            raf_rw.close();
//        }
//        raf_r.close();
//    }
//
//    //将分块进行合并
//    @Test
//    public void testMerge() throws IOException {
//        //块文件目录
//        File chunkFolder = new File("C:\\Users\\17407\\Videos\\屏幕录制\\chunk");
//        //源文件
//        File sourceFile = new File("C:\\Users\\17407\\Videos\\屏幕录制\\屏幕录制 2024-06-23 194707.mp4");
//        //合并后的文件
//        File mergeFile = new File("C:\\Users\\17407\\Videos\\屏幕录制\\1.mp4");
//
//        //取出所有的分块文件
//        File[] files = chunkFolder.listFiles();
//        //将数组转为list
//        List<File> fileList = Arrays.asList(files);
//        //对分块文件排序
//        Collections.sort(fileList, new Comparator<File>() {
//            @Override
//            public int compare(File o1, File o2) {
//                return Integer.parseInt(o1.getName()) - Integer.parseInt(o2.getName());
//            }
//        });
//        //向合并文件写的流
//        RandomAccessFile raf_rw = new RandomAccessFile(mergeFile, "rw");
//        //缓存区
//        byte[] bytes = new byte[1024];
//        //遍历分块文件，向合并的文件写
//        for (File file : fileList) {
//            //读分块的流
//            RandomAccessFile raf_r = new RandomAccessFile(file, "r");
//            int len = -1;
//            while ((len = raf_r.read(bytes)) != -1) {
//                raf_rw.write(bytes, 0, len);
//            }
//            raf_r.close();
//        }
//        raf_rw.close();
//        //合并文件完成后对合并的文件校验
//        FileInputStream fileInputStream_merge = new FileInputStream(mergeFile);
//        FileInputStream fileInputStream_source = new FileInputStream(sourceFile);
//        String md5_merge = DigestUtils.md5DigestAsHex(fileInputStream_merge);
//        String md5_source = DigestUtils.md5DigestAsHex(fileInputStream_source);
//        if (md5_merge.equals(md5_source)) {
//            System.out.println("合并文件完成");
//        }
//    }
//}
