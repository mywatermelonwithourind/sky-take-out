package com.sky.mapper;

import com.sky.anno.AutoFill;
import com.sky.entity.DishFlavor;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DishFlavorMapper {
    /**
     * 批量插入口味数据
     * @param flavors
     */

    void insertBatch(List<DishFlavor> flavors);

    /**
     * 根据菜品ids删除对应的口味数据
     * @param ids
     */
    void deleteByDishIds(List<Long> dishIds);

    /**
     * 根据菜品id查询对应的口味列表
     * @param id
     * @return
     */

    List<DishFlavor> listByDishId(Long id);
}
