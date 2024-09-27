package com.xuecheng.content;

import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * @author LH
 * @version 1.0
 * @description 测试远程调用媒资服务
 * @date 2024/08/05 17:02
 */
@SpringBootTest
public class FeignUploadTest {

    @Autowired
    MediaServiceClient mediaServiceClient;

    @Test
    public void test() {

        //将file转为MultipartFile
        File file = new File("D:\\learning\\xuecheng\\project\\test.html");
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);
        //远程调用得到返回值
        String upload = mediaServiceClient.upload(multipartFile, "course/test.html");
        if (upload == null) {
            System.out.println("走了降级逻辑");
        }
    }
}
