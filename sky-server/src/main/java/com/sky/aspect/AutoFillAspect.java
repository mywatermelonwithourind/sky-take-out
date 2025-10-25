package com.sky.aspect;

import com.sky.anno.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;


/**
 * 公共字段自动填充切面类
 */
@Slf4j
@Component
@Aspect
public class AutoFillAspect {


    @Before("@annotation(com.sky.anno.AutoFill)")
    public void autoFill(JoinPoint joinPoint){
        log.info("开始进行公共字段自动填充");
        //1.拿到注解判断里面的值是update或者insert
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();//获取方法对象
        AutoFill autoFill = method.getAnnotation(AutoFill.class);
        OperationType value = autoFill.value();
        //2.获取目标方法的参数对象
        Object[] args = joinPoint.getArgs();

        if(args==null || args.length==0){
            return;
        }

        Object arg=args[0]; //拿到的是各种实体的对象

        //3.判断注解中的苏醒值如果是insert就补充四个字段否则补充两个
        try {
            if(value==OperationType.INSERT){
            //通过反射去补充属性值
                Method setCreateTime = arg.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                Method setUpdateTime = arg.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setCreateUser = arg.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                Method setUpdateUser= arg.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
                setCreateTime.invoke(arg,LocalDateTime.now());
                setUpdateTime.invoke(arg,LocalDateTime.now());
                setCreateUser.invoke(arg, BaseContext.getCurrentId());
                setUpdateUser.invoke(arg,BaseContext.getCurrentId());


            }else if(value==OperationType.UPDATE){
                Method setUpdateTime = arg.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser= arg.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
                setUpdateTime.invoke(arg,LocalDateTime.now());
                setUpdateUser.invoke(arg,BaseContext.getCurrentId());
         }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
