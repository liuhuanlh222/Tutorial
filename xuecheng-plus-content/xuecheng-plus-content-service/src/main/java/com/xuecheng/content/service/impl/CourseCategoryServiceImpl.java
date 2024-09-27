package com.xuecheng.content.service.impl;

import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.service.CourseCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CourseCategoryServiceImpl implements CourseCategoryService {

    @Autowired
    CourseCategoryMapper courseCategoryMapper;

    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes(String id) {
        //调用mapper递归查询出分类信息
        List<CourseCategoryTreeDto> courseCategoryTreeDtos = courseCategoryMapper.selectTreeNodes(id);

        //找到每个结点的子结点，最终封装成List<CourseCategoryTreeDto>
        //先将List转成map，key就是结点的id，value就是CourseCategoryTreeDto对象，目的就是为了方便从map获取结点
        Map<String, CourseCategoryTreeDto> mapTemp = courseCategoryTreeDtos.stream().filter(item -> !id.equals(item.getId())).collect(Collectors.toMap(key -> key.getId(), value -> value, (key1, key2) -> key2));
        //定义一个List作为最终返回的list
        ArrayList<CourseCategoryTreeDto> courseCategoryList = new ArrayList<>();
        //从头遍历List<CourseCategoryTreeDto>，一边遍历一边找子节点放在父节点的childrenTreeNodes
        courseCategoryTreeDtos.stream().filter(item -> !id.equals(item.getId())).forEach(item -> {
            if (item.getParentid().equals(id)) {
                courseCategoryList.add(item);
            }
            //找到结点的父节点
            CourseCategoryTreeDto courseCategoryTreeDto = mapTemp.get(item.getParentid());
            if (courseCategoryTreeDto != null) {
                if (courseCategoryTreeDto.getChildrenTreeNodes() == null) {
                    //如果该父节点的ChildrenTreeNodes属性为null，则要new一个集合，因为要向该集合中放它的子节点
                    courseCategoryTreeDto.setChildrenTreeNodes(new ArrayList<>());
                }
                //找到每个结点的子节点放在父节点的childrenTreeNodes属性中
                courseCategoryTreeDto.getChildrenTreeNodes().add(item);
            }

        });
        return courseCategoryList;
    }
}
