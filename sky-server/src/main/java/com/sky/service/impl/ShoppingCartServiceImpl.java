package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    @Override
    public void add(ShoppingCartDTO dto) {
        //1.拷贝数据
        ShoppingCart cart = new ShoppingCart();
        BeanUtils.copyProperties(dto, cart);
        //2.补充数据
        cart.setUserId(BaseContext.getCurrentId());

        //3.查询购物车有没有相关数据
        List<ShoppingCart> list= shoppingCartMapper.selectBy(cart);

        //4.判断是否为空
        if(list.isEmpty()){
            //如果为空向购物车添加数据
            if(cart.getDishId()!=null){
                Dish dish = dishMapper.selectById(cart.getDishId());
                cart.setName(dish.getName());
                cart.setImage(dish.getImage());
                cart.setAmount(dish.getPrice());
            }else{
                Setmeal setmeal = setmealMapper.selectById(cart.getSetmealId());
                cart.setName(setmeal.getName());
                cart.setImage(setmeal.getImage());
                cart.setAmount(setmeal.getPrice());
            }
            cart.setNumber(1);
            cart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insert(cart);
        }else{
            //不为空修改购物车的数量
            ShoppingCart existingCart = list.get(0);
            existingCart.setNumber(existingCart.getNumber()+1);
            shoppingCartMapper.update(existingCart);
        }
    }

    @Override
    public List<ShoppingCart> list() {
        ShoppingCart cart=new ShoppingCart();
        cart.setUserId(BaseContext.getCurrentId());
        List<ShoppingCart> shoppingCarts = shoppingCartMapper.selectBy(cart);
        return shoppingCarts;
    }
}
