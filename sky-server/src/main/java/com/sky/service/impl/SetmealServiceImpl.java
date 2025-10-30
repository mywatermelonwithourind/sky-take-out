package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
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
}
