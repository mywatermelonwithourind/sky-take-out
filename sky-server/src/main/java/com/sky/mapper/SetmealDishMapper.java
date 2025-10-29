package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 根据菜品ids查询套餐中关联的菜品数量
     * @param dishIds
     * @return
     */
    int countByDishId(List<Long> dishIds);
}
