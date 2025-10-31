package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.SetmealVO;

import java.util.List;

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


    /**
     * 删除套餐
     * @param ids
     */
    void delete(List<Long> ids);


    /**
     * 根据id查询套餐信息
     * @param id
     * @return
     */
    SetmealVO getById(Long id);
}
