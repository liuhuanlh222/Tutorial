//package com.xuecheng.media;
//
//import io.minio.*;
//import io.minio.errors.*;
//import org.junit.jupiter.api.Test;
//
//import java.io.IOException;
//import java.security.InvalidKeyException;
//import java.security.NoSuchAlgorithmException;
//import java.util.List;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;
//
///**
// * @author LH
// * @version 1.0
// * @description 测试minio的sdk
// * @date 2024/07/23 15:39
// */
//public class MinioTest {
//
//    MinioClient minioClient =
//            MinioClient.builder()
//                    .endpoint("http://192.168.101.65:9000")
//                    .credentials("minioadmin", "minioadmin")
//                    .build();
//
//    //删除文件
//    @Test
//    public void test_delete() throws Exception {
//        //RemoveObjectArgs
//        RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder().bucket("testbucket").object("dahai.jpg").build();
//        //删除文件
//        minioClient.removeObject(removeObjectArgs);
//    }
//
//    //将分块文件上传至minio
//    @Test
//    public void uploadChunk() throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
//        for (int i = 0; i < 24; i++) {
//            //上传文件的参数信息
//            UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
//                    .bucket("testbucket")//桶
//                    .filename("C:\\Users\\17407\\Videos\\屏幕录制\\chunk\\" + i)//指定本地文件路径
//                    .object("chunk/" + i)//对象名 放在子目录下面
//                    .build();
//
//            //上传文件
//            minioClient.uploadObject(uploadObjectArgs);
//            System.out.println("上传分块" + i + "成功");
//        }
//    }
//
//    //调用minio接口合并分块
//    @Test
//    public void testMerge() throws Exception {
//
////        List<ComposeSource> sources = new ArrayList<>();
////        for (int i = 0; i < 4; i++) {
////            //指定分块文件的信息
////            ComposeSource composeSource = ComposeSource.builder().bucket("testbucket").object("chunk/" + i).build();
////            sources.add(composeSource);
////        }
//
//        List<ComposeSource> sources = Stream.iterate(0, i -> ++i).limit(24).map(i -> ComposeSource.builder().bucket("testbucket").object("chunk/" + i).build()).collect(Collectors.toList());
//        //指定合并后的objectName等信息
//        ComposeObjectArgs composeObjectArgs = ComposeObjectArgs.builder()
//                .bucket("testbucket")
//                .object("merge01.mp4")
//                .sources(sources)//指定源文件
//                .build();
//        //合并文件
//        //报错size 1048576 must be greater than 5242880 minio默认的分块文件大小为5M
//        minioClient.composeObject(composeObjectArgs);
//    }
//
//
//    //批量清理分块文件
//
//}
