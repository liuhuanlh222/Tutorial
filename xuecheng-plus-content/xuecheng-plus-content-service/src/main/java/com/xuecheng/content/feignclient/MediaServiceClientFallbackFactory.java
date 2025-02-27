package com.xuecheng.content.feignclient;

import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author LH
 * @version 1.0
 * @description TODO
 * @date 2024/08/06 11:14
 */
@Component
@Slf4j
public class MediaServiceClientFallbackFactory implements FallbackFactory<MediaServiceClient> {
    //拿到了熔断的异常信息throwable
    @Override
    public MediaServiceClient create(Throwable throwable) {
        return new MediaServiceClient() {
            //发生熔断，上游服务就会调用此方法执行降级处理
            @Override
            public String upload(MultipartFile filedata, String objectName) {
                log.debug("远程调用上传文件的接口发生熔断:{}", throwable.toString(), throwable);
                return null;
            }
        };
    }
}
