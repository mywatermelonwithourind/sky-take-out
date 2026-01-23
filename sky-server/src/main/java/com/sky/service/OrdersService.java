package com.sky.service;

import com.sky.dto.OrdersSubmitDTO;
import com.sky.vo.OrderSubmitVO;

public interface OrdersService {
    /**
     * 订单提交
     * @param dto
     * @return
     */
    OrderSubmitVO submit(OrdersSubmitDTO dto);
}
