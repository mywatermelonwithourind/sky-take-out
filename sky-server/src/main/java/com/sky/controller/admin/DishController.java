package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.mapper.DishMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Api(tags = "菜品相关接口")
@RestController
@RequestMapping("/admin/dish")
public class DishController {

    @Autowired
    private DishService dishService;
    @Autowired
    private DishMapper dishMapper;

    /**
     * 新增菜品
     * @param dto
     * @return
     */
    @ApiOperation("新增菜品")
    @PostMapping
    public Result addDish(@RequestBody DishDTO dto) {
        log.info("新增菜品: {}", dto);

        dishService.addDish(dto);


        return Result.success();
    }

    /**
     * 分页查询菜品列表
     * @param dto
     * @return
     */
    @ApiOperation("分页查询菜品列表")
    @GetMapping("/page")
    public Result<PageResult> page( DishPageQueryDTO dto){
        log.info("分页查询菜品列表:{}",dto);
        PageResult pageResult= dishService.page(dto);

        return Result.success(pageResult);
    }

    /**
     * 删除菜品
     * @param ids
     * @return
     */
    @ApiOperation("删除菜品")
    @DeleteMapping
    public Result<String> delete(@RequestParam List<Long> ids){
        log.info("删除菜品:{}",ids);

        dishService.delete(ids);

        return Result.success();
    }

    /**
     * 根据id查询菜品及其口味信息
     * @param id
     * @return
     */
    @ApiOperation("根据id回显修改数据")
    @GetMapping("/{id}")
    public Result<DishVO>  getById(@PathVariable Long id){
        log.info("根据id查询菜品:{}",id);
        DishVO dish=dishService.getById(id);
        return Result.success(dish);
    }

    /**
     * 修改菜品
     * @param dto
     * @return
     */
    @ApiOperation("修改菜品")
    @PutMapping
    public Result<String> update(@RequestBody DishDTO dto){
        log.info("修改菜品:{}",dto);

        dishService.update(dto);

        return Result.success();
    }

    /**
     * 修改菜品状态
     * @param status
     * @param id
     * @return
     */
    @ApiOperation("修改菜品状态")
    @PostMapping("/status/{status}")
    public Result<String> updateStatus(@PathVariable Integer status,@RequestParam Long id){
        log.info("修改菜品状态: {},{}",status,id);
        dishService.updateStatus(status,id);

        return Result.success();
    }

}
