package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService {
    /**
     * 新增菜品
     * @param dishDTO
     */
    void addDish(DishDTO dto);

    /**
     * 分页菜品查询
     * @param dto
     * @return
     */
    PageResult page(DishPageQueryDTO dto);

    /**
     * 删除菜品
     * @param ids
     */
    void delete(List<Long> ids);

    /**
     * 根据id查询菜品及其口味信息
     * @param id
     * @return
     */
    DishVO getById(Long id);

    /**
     * 修改菜品
     * @param dto
     */
    void update(DishDTO dto);

    /**
     * 修改菜品状态
     * @param status
     * @param id
     */
    void updateStatus(Integer status, Long id);
}
