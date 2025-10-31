package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "套餐相关接口")
@Slf4j
@RequestMapping("/admin/setmeal")
@RestController
public class SetmealController {


    @Autowired
    private SetmealService setmealService;

    /**
     * 新增套餐
     * @param dto
     * @return
     */
    @ApiOperation("新增套餐")
    @PostMapping
    public Result<String> addSetmeal(@RequestBody SetmealDTO dto){
        log.info("新增套餐: {}", dto);
        setmealService.addSetmeal(dto);

        return Result.success();
    }


    /**
     * 套餐分页查询
     * @param dto
     * @return
     */
    @ApiOperation("套餐分页查询")
    @GetMapping("/page")
    public Result<PageResult> page(SetmealPageQueryDTO dto){
        log.info("套餐分页查询: {}", dto);
        PageResult pageResult=setmealService.page(dto);
        return Result.success(pageResult);
    }

    /**
     *
     * 删除套餐
     * @param ids
     * @return
     */
    @ApiOperation("删除套餐")
    @DeleteMapping
    public Result<String> delete(@RequestParam List<Long> ids){
        log.info("删除套餐: {}", ids);
        setmealService.delete(ids);
        return Result.success();
    }


}
