package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.ucenter.mapper.XcMenuMapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcMenu;
import com.xuecheng.ucenter.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author LH
 * @version 1.0
 * @description TODO
 * @date 2024/08/11 10:11
 */
@Component
@Slf4j
public class UserServiceImpl implements UserDetailsService {

    @Autowired
    XcUserMapper xcUserMapper;

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    XcMenuMapper xcMenuMapper;

    //传入的请求认证的参数就是AuthParamsDto
    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        //将传入的json转为AuthParamsDto对象
        AuthParamsDto authParamsDto = null;
        try {
            authParamsDto = JSON.parseObject(s, AuthParamsDto.class);
        } catch (Exception e) {
            throw new RuntimeException("请求认证参数不符合要求");
        }

        //认证类型，有password，wx...
        String authType = authParamsDto.getAuthType();

        //根据认证类型从spring容器取出指定的bean
        String beanName = authType + "_authservice";
        AuthService authService = applicationContext.getBean(beanName, AuthService.class);
        //调用统一的execute方法完成认证
        XcUserExt xcUserExt = authService.execute(authParamsDto);
        //封装xcUserExt用户信息为UserDetails
        //根据UserDetails对象生成令牌
        UserDetails userPrincipal = getUserPrincipal(xcUserExt);
        return userPrincipal;
    }

    /**
     * @param xcUser 用户id，主键
     * @return com.xuecheng.ucenter.model.po.XcUser 用户信息
     * @description 查询用户信息
     * @author Mr.M
     * @date 2022/9/29 12:19
     */
    public UserDetails getUserPrincipal(XcUserExt xcUser) {
        String password = xcUser.getPassword();
        //权限
        String[] authorities = {"test"};
        //根据用户id查询用户的权限
        List<XcMenu> xcMenus = xcMenuMapper.selectPermissionByUserId(xcUser.getId());
        if (xcMenus.size() > 0) {
            ArrayList<String> permissions = new ArrayList<>();
            xcMenus.forEach(m -> {
                //拿到了用户拥有的权限标志符
                permissions.add(m.getCode());
            });
            //将permisson转为数组
            authorities = permissions.toArray(new String[0]);
        }

        xcUser.setPassword(null);
        //将用户信息转为json
        String userJson = JSON.toJSONString(xcUser);
        UserDetails userDetails = User.withUsername(userJson).password(password).authorities(authorities).build();
        return userDetails;
    }
}
