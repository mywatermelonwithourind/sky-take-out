package com.sky.controller.user;

import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.Result;
import com.sky.service.OrdersService;
import com.sky.vo.OrderSubmitVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "订单相关接口")
@Slf4j
@RequestMapping("/user/order")
@RestController
public class OrdersController {

    @Autowired
    private OrdersService ordersService;

    /**
     * 订单提交
     * @param dto
     * @return
     */
    @ApiOperation("订单提交")
    @PostMapping("/submit")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO dto){
        log.info("订单提交: {}", dto);
        OrderSubmitVO orderSubmitVO = ordersService.submit(dto);
        return Result.success(orderSubmitVO);
    }
}
