package com.sky.mapper;

import com.sky.anno.AutoFill;
import com.sky.entity.Dish;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DishMapper {

    /**
     * 根据分类id查询菜品数量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    /**
     * 新增菜品
     * @param dish
     */
    @AutoFill(OperationType.INSERT)
//    @Options(useGeneratedKeys = true, keyProperty = "id") //获取自增主键
//    @Insert("insert into dish values (null, #{name}, #{categoryId}, #{price}, #{image}, #{description}" +
//            ", #{status}, #{createTime}, #{updateTime}, #{createUser},#{updateUser})")
    void insert(Dish dish);
}
