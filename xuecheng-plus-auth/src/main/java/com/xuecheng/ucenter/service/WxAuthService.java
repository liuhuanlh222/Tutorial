package com.xuecheng.ucenter.service;

import com.xuecheng.ucenter.model.po.XcUser;

/**
 * @author LH
 * @version 1.0
 * @description 微信扫码接入
 * @date 2024/08/20 14:58
 */
public interface WxAuthService {


    /**
     * 微信扫码认证，申请令牌，携带令牌查询用户信息、保存用户信息到数据库
     *
     * @param code 授权码
     * @return
     */
    public XcUser wxAuth(String code);
}
