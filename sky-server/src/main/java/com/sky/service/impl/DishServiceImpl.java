package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;


    @Override
    @Transactional //开启事物(涉及到多张表的增删改操作需要开事务)
    public void addDish(DishDTO dto) {
        //1.构造菜品基本数据将其存入dish表中
        Dish dish=new Dish();
        BeanUtils.copyProperties(dto,dish);
        dishMapper.insert(dish);
        log.info("菜品id:{}",dish.getId());

        //2.构造菜品口味列表数据将其存入dish_flavor表中
        List<DishFlavor> flavors=dto.getFlavors();
        for (DishFlavor flavor : flavors) {
            flavor.setDishId(dish.getId());
        }

        //批量插入口味列表数据
        dishFlavorMapper.insertBatch(flavors);
    }

    @Override
    public PageResult page(DishPageQueryDTO dto) {
        //设置分页参数
        PageHelper.startPage(dto.getPage(), dto.getPageSize());
        //调用mapper方法
        Page<DishVO> page=dishMapper.list(dto);
        return new PageResult(page.getTotal(),page.getResult());
    }
}
