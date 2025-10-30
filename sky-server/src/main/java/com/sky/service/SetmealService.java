package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;

public interface SetmealService {

    /**
     * 新增套餐
     * @param dto
     */
    void addSetmeal(SetmealDTO dto);

    /**
     * 套餐分页查询
     * @param dto
     * @return
     */
    PageResult page(SetmealPageQueryDTO dto);
}
