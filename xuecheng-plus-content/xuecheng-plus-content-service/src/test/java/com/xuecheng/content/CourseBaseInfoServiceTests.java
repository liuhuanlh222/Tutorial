package com.xuecheng.content;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.service.CourseBaseInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class CourseBaseInfoServiceTests {

    @Autowired
    CourseBaseInfoService courseBaseInfoService;

    @Test
    public void testCourseBaseInfoService() {

        //查询条件
        QueryCourseParamsDto courseParamDto = new QueryCourseParamsDto();
        courseParamDto.setCourseName("java");//课程名称查询条件
        courseParamDto.setAuditStatus("202004");//202004表示课程审核未通过
        //分页参数对象
        PageParams pageParams = new PageParams();
        pageParams.setPageSize(2L);
        pageParams.setPageNo(2L);

        PageResult<CourseBase> courseBasePageResult = courseBaseInfoService.queryCourseBaseList(null, pageParams, courseParamDto);
        System.out.println(courseBasePageResult);

    }

}
