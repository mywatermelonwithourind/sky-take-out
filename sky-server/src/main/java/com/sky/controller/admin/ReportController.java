package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Api(tags = "数据统计相关接口")
@RestController
@RequestMapping("/admin/report")
@Slf4j
public class ReportController {

    @Autowired
    ReportService reportService;

    /**
     * 营业数据统计
     * @param begin
     * @param end
     * @return
     */
    @ApiOperation("营业数据统计")
    @GetMapping("/turnoverStatistics")
    public Result<TurnoverReportVO>  turnoverStatistics(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin ,
                                                        @DateTimeFormat(pattern = "yyyy-MM-dd")LocalDate end){
        log.info("营业数据统计，begin = {}, end = {}", begin, end);
        TurnoverReportVO vo=reportService.turnoverStatistics(begin, end);
        return Result.success(vo);
    }

    /**
     * 用户统计
     * @param begin
     * @param end
     * @return
     */
    @ApiOperation("用户统计")
    @GetMapping("/userStatistics")
    public Result<UserReportVO> userStatistics(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin ,
                                               @DateTimeFormat(pattern = "yyyy-MM-dd")LocalDate end){
        log.info("用户统计，begin = {}, end = {}", begin, end);
        UserReportVO vo=reportService.userStatistics(begin, end);
        return Result.success(vo);
    }

    /**
     * 订单统计
     * @return
     */
    @ApiOperation("订单统计")
    @GetMapping("/ordersStatistics")
    public Result<OrderReportVO> ordersStatistics(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin ,
                                                  @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){
        log.info("订单统计，begin = {}, end = {}", begin, end);
        OrderReportVO vo=reportService.ordersStatistics(begin, end);
        return Result.success(vo);
    }

    /**
     * 销售前十排行
     * @param begin
     * @param end
     * @return
     */
    @ApiOperation("销售前十排行")
    @GetMapping("/top10")
    public Result<SalesTop10ReportVO> top10(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin ,
                                            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){
        log.info("销售前十排行，begin = {}, end = {}", begin, end);
        SalesTop10ReportVO vo=reportService.salesTop10(begin, end);
        return Result.success(vo);
    }
}
