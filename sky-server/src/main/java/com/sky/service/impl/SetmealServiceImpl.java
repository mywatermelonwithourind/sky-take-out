package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class SetmealServiceImpl implements SetmealService {
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Override
    @Transactional //操作两张表进行添加操作要开启事务
    public void addSetmeal(SetmealDTO dto) {
        //1.把dto的数据拷贝到setmeal对应的实体对象中因为需要进行自动的公共字段填充
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(dto, setmeal);
        //2.调用mapper方法进行套餐的插入操作
        setmealMapper.insert(setmeal);
        Long setmealId = setmeal.getId();
        log.info("新增套餐的id: {}", setmealId);
        //3.给每个套餐每个的菜品关联菜品id
        List<SetmealDish> setmealDishs=dto.getSetmealDishes();
        if(setmealDishs!=null&&!setmealDishs.isEmpty()){
            for (SetmealDish setmealDish : setmealDishs) {
                setmealDish.setSetmealId(setmealId);
            }
            //4.调用mapper方法进行套餐菜品关联关系的插入操作
            setmealDishMapper.batchInsert(setmealDishs);
        }


    }

    @Override
    public PageResult page(SetmealPageQueryDTO dto) {
        //1.设置分页参数
        PageHelper.startPage(dto.getPage(),dto.getPageSize());

        //2.调用mapper方法进行分页查询
        Page<SetmealVO> page=setmealMapper.list(dto);

        //3.封装并返回分页结果
        return new PageResult(page.getTotal(),page.getResult());
    }

    /**
     * 删除套餐
     * @param ids
     */
    @Override
    @Transactional
    public void delete(List<Long> ids) {
        //1.判断是否可以删除
        ids.forEach(id->{
            Setmeal setmeal=setmealMapper.selectById(id);
            if(setmeal.getStatus()== StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        });
        //2.删除套餐菜品关联表中的数据
        setmealDishMapper.deleteBySetmealIds(ids);
        //3.删除套餐表中的数据
        setmealMapper.deleteByIds(ids);
    }

    @Override
    public SetmealVO getById(Long id) {
        //1.先创建VO对象
        SetmealVO setmealVO=new SetmealVO();
        //2.调用mapper获取套餐实体对象
        Setmeal setmeal = setmealMapper.selectById(id);
        //3.把setmeal对象的数据拷贝到setmealVO对象中
        BeanUtils.copyProperties(setmeal, setmealVO);
        //4.获取套餐对应的菜品数据
        List<SetmealDish> setmealDishes =setmealDishMapper.selectById(id);
        //5.把菜品数据设置到setmealVO对象中
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }

    @Override
    @Transactional
    public void update(SetmealDTO dto) {
        //1.把dto的数据拷贝到setmeal实体对象中
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(dto,setmeal);
        //2.调用mapper方法更新套餐表中的数据
        setmealMapper.update(setmeal);
        Long setmealId = dto.getId();
        //3.删除套餐对应的菜品数据
        setmealDishMapper.deleteBySetmealIds(List.of(setmealId));
        //4.重新添加套餐对应的菜品数据
        List<SetmealDish> setmealDishes=dto.getSetmealDishes();
        if(setmealDishes!=null&&!setmealDishes.isEmpty()){
            for(SetmealDish setmealDish:setmealDishes){
                setmealDish.setSetmealId(setmealId);
            }
            setmealDishMapper.batchInsert(setmealDishes);
        }


    }

    @Override
    public void status(Integer status, Long id) {

        //1.判断是什么状态
        if(status== StatusConstant.ENABLE) {
            List<Dish> dishList=setmealDishMapper.getBySetmealId(id);
            for(Dish dish:dishList){
                if(dish.getStatus()== StatusConstant.DISABLE){
                    throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                }
            }
        }
        //2.先构建setmeal实体对象
        Setmeal setmeal=Setmeal.builder()
                .status(status)
                .id(id)
                .build();
        //3.调用mapper方法更新套餐状态
        setmealMapper.update(setmeal);
    }


}
