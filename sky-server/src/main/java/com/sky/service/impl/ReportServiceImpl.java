package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrdersMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    OrdersMapper ordersMapper;

    @Autowired
    UserMapper userMapper;

    @Override
    public TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end) {
        //1.构造dateList数据
        List<LocalDate> dateList = getLocalDate(begin, end);
        //2.构造turnoverList数据
        List<Double> turnoverList=new ArrayList<>();
        for(LocalDate date:dateList){
            //构造开始时间和结束时间
            LocalDateTime beginDate = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endDate = LocalDateTime.of(date, LocalTime.MAX);
            //根据日期区间和订单状态查询营业额
            Map map=new HashMap();
            map.put("beginDate",beginDate);
            map.put("endDate",endDate);
            map.put("status", Orders.COMPLETED);
            Double turnover= ordersMapper.sumByMap(map);
            //因为可能某一天没有营业额，所以要做个非空判断（sum会返回空的数据）
            if(turnover==null) turnover=0.0;
            //添加到turnoverList中
            turnoverList.add(turnover);
        }
        //3.构造并返回TurnoverReportVO对象
        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList,','))
                .turnoverList(StringUtils.join(turnoverList,','))
                .build();
    }

    private static List<LocalDate> getLocalDate(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList=new ArrayList<>();
        while (!begin.isAfter(end)){
            dateList.add(begin);
            begin = begin.plusDays(1);
        }
        return dateList;
    }


    @Override
    public UserReportVO userStatistics(LocalDate begin, LocalDate end) {
        //1.构造dateList数据
        List<LocalDate> dateList = getLocalDate(begin, end);
        //2.构造totalUserList数据
        List<Integer> totalUserList=new ArrayList<>();
        //3.构造newUserList数据
        List<Integer> newUserList=new ArrayList<>();

        for(LocalDate date:dateList){
            //构造开始时间和结束时间
            LocalDateTime beginDate = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endDate = LocalDateTime.of(date, LocalTime.MAX);
            //构造查询根据日期区间查询用户总量
            Map map=new HashMap();

            map.put("endDate",endDate);

            //根据日期区间查询用户总量
            Integer totalUser= userMapper.countUserByMap(map);
            totalUserList.add(totalUser);
            //根据日期区间查询新增用户
            map.put("beginDate",beginDate);
            Integer newUser= userMapper.countUserByMap(map);
            newUserList.add(newUser);
        }
        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList,','))
                .newUserList(StringUtils.join(newUserList,','))
                .totalUserList(StringUtils.join(totalUserList,','))
                .build();
    }
}
