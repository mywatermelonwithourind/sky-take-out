package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;


@Slf4j
@Api(tags = "店铺操作接口")
@RequestMapping("/admin/shop")
@RestController("adminShopController")
public class ShopController {
    @Autowired
    private RedisTemplate redisTemplate;

    private final static String KEY= "SHOP_STATUS";

    @ApiOperation("修改店铺营业状态")
    @PutMapping("/{status}")
    public Result<String> setStatus(@PathVariable Integer status){
        log.info("修改店铺营业状态: {}"+(status==1?"营业":"打烊"));
        //把店铺的营业状态存入redis中
        redisTemplate.opsForValue().set(KEY,status);
        return Result.success();
    }

    @ApiOperation("获取店铺营业状态")
    @GetMapping("/status")
    public Result<Integer> getStatus(){
        Integer status = (Integer)redisTemplate.opsForValue().get(KEY);
        log.info("店铺营业状态: {}"+(status==1?"营业":"打烊"));

        return Result.success(status);
    }
}
