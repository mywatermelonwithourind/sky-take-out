package com.sky.controller.admin;

import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrdersService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    /**
     * 接单
     * @param ordersConfirmDTO
     * @return
     */
    @ApiOperation("接单")
    @PutMapping("/confirm")
    public Result confirm(@RequestBody OrdersConfirmDTO ordersConfirmDTO){
        log.info("接单：id= {}", ordersConfirmDTO);
        ordersService.confirm(ordersConfirmDTO);
        return Result.success();
    }

    /**
     * 拒单
     * @param dto
     * @return
     */
    @ApiOperation("拒单")
    @PutMapping("/rejection")
    public Result rejection(@RequestBody OrdersRejectionDTO dto){

        log.info("拒单：id= {}", dto);
        ordersService.rejection(dto);
        return Result.success();
    }

    /**
     * 订单取消
     * @param dto
     * @return
     */
    @ApiOperation("订单取消")
    @PutMapping("/cancel")
    public Result cancel(@RequestBody OrdersCancelDTO dto){
        log.info("订单取消：dto= {}", dto);
        ordersService.adminCancel(dto);
        return Result.success();
    }
}
