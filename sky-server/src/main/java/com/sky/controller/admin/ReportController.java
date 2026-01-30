package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.TurnoverReportVO;
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
}
