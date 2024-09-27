package com.xuecheng.content.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author LH
 * @version 1.0
 * @description Freemarker入门程序
 * @date 2024/08/03 09:01
 */
@Controller
public class FreemarkerController {

    @GetMapping("/testfreemarker")
    public ModelAndView test() {

        ModelAndView modelAndView = new ModelAndView();
        //指定模型
        modelAndView.addObject("name", "小明");
        //指定模板
        modelAndView.setViewName("test");//更具视图名称加.ftl找到模板
        return modelAndView;
    }
}
