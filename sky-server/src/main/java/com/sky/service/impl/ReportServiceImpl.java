package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        // 1.计算dateList
        // 判断区间合法性
        if (begin.isAfter(end))
            return new TurnoverReportVO();

        // 存放从begin到end内每天的日期
        List<LocalDate> dateList = getDateList(begin, end);

        // 2.统计turnoverList
        List<Double> turnoverList = new ArrayList<>();

        for (LocalDate date : dateList) {
            // 查询日期对应的营业额数据（状态为“已完成[5]”的订单金额[amount]的合计)）
            Map<String, Object> map = new HashMap<>();
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            map.put("status", Orders.COMPLETED);
            map.put("begin", beginTime);
            map.put("end", endTime);
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }



        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    /**
     * 用户统计
     * @param begin
     * @param end
     * @return
     */
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        // 1.计算dateList
        // 判断区间合法性
        if (begin.isAfter(end))
            return new UserReportVO();

        // 存放从begin到end内每天的日期
        List<LocalDate> dateList = getDateList(begin, end);

        // 2.统计totalUserList和newUserList
        List<Integer> totalUserList = new ArrayList<>();
        List<Integer> newUserList = new ArrayList<>();

        for (LocalDate date : dateList) {
            // 查询日期对应的用户数量
            Map<String, Object> map = new HashMap<>();
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            map.put("end", endTime);
            // 总用户数量
            Integer totalUser = userMapper.countByMap(map);

            map.put("begin", beginTime);
            // 新增用户数量
            Integer newUser = userMapper.countByMap(map);

            totalUserList.add(totalUser);
            newUserList.add(newUser);
        }



        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
    }

    /**
     * 订单统计
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        // 1.计算dateList
        // 判断区间合法性
        if (begin.isAfter(end))
            return new OrderReportVO();

        // 存放从begin到end内每天的日期
        List<LocalDate> dateList = getDateList(begin, end);

        // 2.统计orderCountList, validOrderCountList
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();

        for (LocalDate date : dateList) {
            // 查询日期对应的订单数量
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            // 每天的订单总数
            Integer orderCount = getOrderCount(beginTime, endTime, null);

            // 每天的有效订单总数（已完成）
            Integer validOrderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);

            orderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);
        }

        // 3.计算总订单数，有效订单数量，完成率
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get(); // list求和
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get(); // list求和
        Double orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;


        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 销量排名统计
     * @param begin
     * @param end
     * @return
     */
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> goodsSalesDTOList = orderMapper.getSalesTop10(beginTime, endTime);

        // 获取nameList
        String nameList = StringUtils.join(
                goodsSalesDTOList.stream()
                        .map(GoodsSalesDTO::getName)
                        .collect(Collectors.toList()), ",");
        // 获取numberList
        String numberList = StringUtils.join(
                goodsSalesDTOList.stream()
                        .map(GoodsSalesDTO::getNumber)
                        .collect(Collectors.toList()), ",");

        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }

    /**
     * 导出运营数据报表
     * @param response
     */
    public void exportBusinessData(HttpServletResponse response) {
        // 1.车讯数据率，获取营业数据（最近30天）
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        // 查询概览数据
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(
                LocalDateTime.of(dateBegin, LocalTime.MIN),
                LocalDateTime.of(dateEnd, LocalTime.MAX));

        // 2.通过POI将数据写入到Excel文件中

        // 获取类路径下的模板文件输入流
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        try {
            // 基于模板文件创建新的Excel
            XSSFWorkbook excel = new XSSFWorkbook(in);

            // 获取表格标签页
            XSSFSheet sheet = excel.getSheet("Sheet1");
            // 填充数据--时间
            sheet.getRow(1).getCell(1).setCellValue("时间："+dateBegin+"至"+dateEnd);

            // 获得第4行
            XSSFRow row = sheet.getRow(3);
            // 填充数据
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());

            // 获得第5行
            row = sheet.getRow(4);
            // 填充数据
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());

            // 填充每日明细
            for (int i=0; i<30; i++) {
                LocalDate date = dateBegin.plusDays(i);
                businessDataVO = workspaceService.getBusinessData(
                        LocalDateTime.of(date, LocalTime.MIN),
                        LocalDateTime.of(date, LocalTime.MAX));

                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessDataVO.getTurnover());
                row.getCell(3).setCellValue(businessDataVO.getValidOrderCount());
                row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessDataVO.getUnitPrice());
                row.getCell(6).setCellValue(businessDataVO.getNewUsers());
            }

            // 3.通过输出流将文件下载到客户端浏览器中
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);
            // 关闭资源
            out.flush();
            out.close();
            excel.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据时间区间统计指定状态的订单数量
     * @param beginTime
     * @param endTime
     * @param status
     * @return
     */
    private Integer getOrderCount(LocalDateTime beginTime, LocalDateTime endTime, Integer status) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", status);
        map.put("begin", beginTime);
        map.put("end", endTime);
        return orderMapper.countByMap(map);
    }

    List<LocalDate> getDateList(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);

        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        return dateList;
    }
}
