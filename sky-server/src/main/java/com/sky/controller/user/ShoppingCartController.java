package com.sky.controller.user;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "C端-购物车接口")
@Slf4j
@RequestMapping("/user/shoppingCart")
@RestController
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    @ApiOperation("添加购物车")
    @PostMapping("/add")
    private Result add(@RequestBody ShoppingCartDTO dto){
        log.info("添加购物车: {}", dto);
        shoppingCartService.add(dto);
        return Result.success();
    }

    /**
     * 查看购物车列表
     * @return
     */
    @ApiOperation("查看购物车列表")
    @GetMapping("/list")
    private Result<List<ShoppingCart>> list(){
        log.info("查看购物车列表");
        List<ShoppingCart> shoppingCart=  shoppingCartService.list();

        return Result.success(shoppingCart);
    }

    /**
     * 清空购物车
     * @return
     */
    @ApiOperation("清空购物车")
    @DeleteMapping("/clean")
    private Result clear(){
        log.info("清空购物车");
        shoppingCartService.clean();
        return Result.success();
    }
}
