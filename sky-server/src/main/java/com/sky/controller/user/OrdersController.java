package com.sky.controller.user;

import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.Orders;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrdersService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = ordersService.payment(ordersPaymentDTO);
        log.info("生成预支付交易单：{}", orderPaymentVO);
        return Result.success(orderPaymentVO);
    }

    /**
     * 历史订单查询
     * @param dto
     * @return
     */
    @ApiOperation("历史订单查询")
    @GetMapping("/historyOrders")
    public Result<PageResult> historyOrders(OrdersPageQueryDTO dto){
        log.info("历史订单查询：{}", dto);
        PageResult pageResult = ordersService.historyOrders(dto);
        return Result.success(pageResult);
    }

    /**
     * 订单详情查询
     * @param id
     * @return
     */
    @ApiOperation("订单详情查询")
    @GetMapping("/orderDetail/{id}")
    public Result<OrderVO> orderDetail(@PathVariable("id") Long id){
        log.info("订单详情查询：id= {}", id);
        OrderVO vo=ordersService.orderDetail(id);
        return Result.success(vo);
    }


    /**
     * 订单取消
     * @return
     */
    @ApiOperation("订单取消")
    @PutMapping("/cancel/{id}")
    public Result orderCancel(@PathVariable("id") Long id){
        log.info("订单取消：id= {}", id);
        //取消订单
        ordersService.cancel(id);
        return Result.success();

    }

    /**
     * 订单再来一单
     * @return
     */
    @ApiOperation("订单再来一单")
    @PostMapping("/repetition/{id}")
    public Result repetition(@PathVariable("id") Long id){
        log.info("订单再来一单：id= {}", id);

        ordersService.repetition(id);

        return Result.success();

    }

    /**
     * 订单催单
     * @param id
     */
    @ApiOperation("订单催单")
    @GetMapping("/reminder/{id}")
    public Result reminder(@PathVariable("id" )Long id){

        log.info("订单催单：id= {}", id);
        ordersService.reminder(id);
        return Result.success();
    }
}
