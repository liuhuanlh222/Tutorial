package com.xuecheng.content.feignclient;

import org.springframework.web.multipart.MultipartFile;

/**
 * @author LH
 * @version 1.0
 * @description TODO
 * @date 2024/08/06 11:04
 */
public class MediaServiceClientFallback implements MediaServiceClient {

    @Override
    public String upload(MultipartFile filedata, String objectName) {
        return "";
    }
}
