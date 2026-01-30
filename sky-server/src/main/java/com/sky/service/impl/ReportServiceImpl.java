package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrdersMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
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

    @Override
    public TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end) {
        //1.构造dateList数据
        List<LocalDate> dateList=new ArrayList<>();
        while (!begin.isAfter(end)){
            dateList.add(begin);
            begin=begin.plusDays(1);
        }
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
}
