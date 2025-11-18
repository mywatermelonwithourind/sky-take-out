package com.sky.mapper;


import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {
    /**
     * 根据购物车信息查询购物车
     * @param cart
     */
    List<ShoppingCart> selectBy(ShoppingCart cart);

    /**
     * 根据修改购物车中的数量
     * @param cart
     */
    @Update("update shopping_cart set number = #{number} where id = #{id}")
    void update(ShoppingCart cart);

    /**
     * 添加购物车记录
     * @param cart
     */
    void insert(ShoppingCart cart);

    /**
     * 根据用户id查询购物车列表
     * @param currentId
     */
    @Delete("delete from shopping_cart where user_id = #{userId}")
    void clean(Long userId);
}
