package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private SetmealMapper setmealMapper;


    @Override
    @Transactional //开启事物(涉及到多张表的增删改操作需要开事务)
    public void addDish(DishDTO dto) {
        //1.构造菜品基本数据将其存入dish表中
        Dish dish=new Dish();
        BeanUtils.copyProperties(dto,dish);
        dishMapper.insert(dish);
        log.info("菜品id:{}",dish.getId());

        //2.构造菜品口味列表数据将其存入dish_flavor表中
        List<DishFlavor> flavors=dto.getFlavors();
        for (DishFlavor flavor : flavors) {
            flavor.setDishId(dish.getId());
        }

        //批量插入口味列表数据
        dishFlavorMapper.insertBatch(flavors);
    }

    @Override
    public PageResult page(DishPageQueryDTO dto) {
        //设置分页参数
        PageHelper.startPage(dto.getPage(), dto.getPageSize());
        //调用mapper方法
        Page<DishVO> page=dishMapper.list(dto);
        return new PageResult(page.getTotal(),page.getResult());
    }

    @Override
    @Transactional //开启事务
    public void delete(List<Long> ids) {
        //1.删除菜品之前判断菜品是否起售起售中的不允许删除
        ids.forEach(id->{
            Dish dish=dishMapper.selectById(id);
            if(dish.getStatus()== StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        });
        //2.判断菜品是否被套餐关联，关联的不允许删除
        int count=setmealDishMapper.countByDishId(ids);
        if(count>0){
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        //3.删除菜品表中的数据
        dishMapper.deleteByIds(ids);
        //4.删除菜品口味表中的数据
        dishFlavorMapper.deleteByDishIds(ids);

    }

    /**
     * 根据id查询菜品及其口味信息
     * @param id
     * @return
     */
    @Override
    public DishVO getById(Long id) {
        //1.根据id查询菜品表
        Dish dish=dishMapper.selectById(id);
        //2.根据菜品id查询口味表
        List<DishFlavor> flavors=dishFlavorMapper.listByDishId(id);
        //3.构造返回对象
        DishVO dishVO=new DishVO();
        BeanUtils.copyProperties(dish,dishVO);
        dishVO.setFlavors(flavors);

        return dishVO;
    }

    @Override
    @Transactional
    public void update(DishDTO dto) {
        //1.更新菜品表中的基本信息
        Dish dish=new Dish();
        BeanUtils.copyProperties(dto,dish);
        dishMapper.update(dish);

        //2.更新口味表中的信息
        //2.1先删除原有口味数据
        dishFlavorMapper.deleteByDishIds(List.of(dish.getId()));
        //2.2再插入新的口味数据
        List<DishFlavor> flavors=dto.getFlavors();

        if(flavors!=null&& !flavors.isEmpty()){
            for (DishFlavor flavor : flavors) {
                flavor.setDishId(dish.getId());
            }
            dishFlavorMapper.insertBatch(flavors);
        }


    }

    /**
     * 修改菜品状态
     * @param status
     * @param id
     */
    @Override
    @Transactional
    public void updateStatus(Integer status, Long id) {

        //1.构造Dish对象
        Dish dish = new Dish();
        dish.setStatus(status);
        dish.setId(id);

        //2.调用Mapper方法，修改菜品状态
        dishMapper.update(dish);

        //3.判断是否是停售如果是修改对应的套餐为停售
        if(status==StatusConstant.DISABLE){
            //3.1 查询对应的套餐id
            List<Long> setmealIds=setmealDishMapper.getById(id);

            if(setmealIds!=null&&setmealIds.isEmpty()){
                for (Long setmealId : setmealIds) {
                    Setmeal setmeal = Setmeal.builder()
                            .id(setmealId)
                            .status(StatusConstant.DISABLE)
                            .build();
                    setmealMapper.update(setmeal);
                }
            }
        }
    }

    @Override
    public List<Dish> list(Long categoryId) {
        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        return dishMapper.listByCategoryId(dish);
    }

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.listBy(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.listByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }
}
