package com.sky.mapper;

import com.sky.anno.AutoFill;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 根据菜品ids查询套餐中关联的菜品数量
     * @param dishIds
     * @return
     */
    int countByDishId(List<Long> dishIds);

    /**
     * 根据菜品id查询套餐id
     * @param dishId
     * @return
     */
    @Select("select setmeal_id from setmeal_dish where dish_id = #{dishId}")
    List<Long> getById(Long dishId);


    /**
     * 批量插入套餐菜品关联数据
     * @param setmealDishs
     */
    void batchInsert(List<SetmealDish> setmealDishes);
}
