package com.xuecheng.learning.service;

import com.xuecheng.base.model.RestResponse;

/**
 * @author LH
 * @version 1.0
 * @description 在线学习相关接口
 * @date 2024/09/15 15:44
 */
public interface LearningService {

    /**
     * @param courseId    课程id
     * @param teachplanId 课程计划id
     * @param mediaId     视频文件id
     * @return com.xuecheng.base.model.RestResponse<java.lang.String>
     * @description 获取教学视频
     * @author Mr.M
     * @date 2022/10/5 9:08
     */
    public RestResponse<String> getVideo(String userId, Long courseId, Long teachplanId, String mediaId);
}
