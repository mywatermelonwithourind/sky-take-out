package com.sky.controller.admin;

import com.sky.dto.OrdersPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrdersService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Api(tags = "订单管理接口")
@RestController("adminOrdersController")
@RequestMapping("/admin/order")
public class OrdersController {
    @Autowired
    OrdersService ordersService;


    /**
     * 订单搜索
     * @return
     */
    @ApiOperation("订单搜索")
    @GetMapping("/conditionSearch")
    public Result<PageResult> conditionSearch(OrdersPageQueryDTO dto){
        log.info("订单搜索：{}", dto);
        PageResult pageResult=ordersService.conditionSearch(dto);
        return Result.success(pageResult);
    }

    /**
     * 各个状态的订单数量统计
     * @return
     */
    @ApiOperation("各个状态的订单数量统计")
    @GetMapping("/statistics")
    public Result<OrderStatisticsVO> statistics(){

        log.info("各个状态的订单数量统计");
        OrderStatisticsVO vo=ordersService.statistics();
        return Result.success(vo);
    }

    /**
     * 查询订单详情
     * @return
     */
    @ApiOperation("查询订单详情")
    @GetMapping("/details/{id}")
    public Result<OrderVO> details(@PathVariable("id") Long id){
        log.info("查询订单详情：id= {}", id);
        OrderVO orderVO = ordersService.orderDetail(id);
        return Result.success(orderVO);

    }
}
