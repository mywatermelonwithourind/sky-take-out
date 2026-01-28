package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrdersMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class OrderTask {
    @Autowired
    OrdersMapper ordersMapper;

    @Scheduled(cron = "0 * * * * ?") //每分钟执行一次
    public void cancelOverTimeOrders() {
        log.info("执行取消超时订单任务: {}", System.currentTimeMillis());

        //1. 筛选出超时订单
        LocalDateTime time=LocalDateTime.now().minusMinutes(15);
        List<Orders> ordersList= ordersMapper.getOrdersByStatusTime(Orders.PENDING_PAYMENT,time);

        //2.修改订单状态
        if(ordersList!=null && !ordersList.isEmpty()){
            for(Orders order:ordersList){
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("超时未支付，系统自动取消");
                order.setCancelTime(LocalDateTime.now());

                ordersMapper.update(order);
            }
        }
    }

    /**
     * 完成派送中订单
     */
    @Scheduled(cron = "0 0 1 * * ?")
//    @Scheduled(cron = "0 56 19 * * ?")  //每天19点56分执行一次（测试用）
    public void completeDeliveryOrders() {
        log.info("执行完成派送中订单任务: {}", System.currentTimeMillis());

        //1. 筛选出超时订单
        LocalDateTime time=LocalDateTime.now().minusHours(1);
        List<Orders> ordersList= ordersMapper.getOrdersByStatusTime(Orders.DELIVERY_IN_PROGRESS,time);

        //2.修改订单状态
        if(ordersList!=null && !ordersList.isEmpty()){
            for(Orders order:ordersList){
                order.setStatus(Orders.COMPLETED);
                order.setDeliveryTime(LocalDateTime.now());

                ordersMapper.update(order);
            }
        }
    }
}
