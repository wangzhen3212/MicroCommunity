package com.java110.job.export.adapt;

import com.alibaba.fastjson.JSONObject;
import com.java110.dto.data.ExportDataDto;
import com.java110.dto.parking.ParkingAreaDto;
import com.java110.dto.report.QueryStatisticsDto;
import com.java110.intf.community.IParkingAreaV1InnerServiceSMO;
import com.java110.intf.report.IReportFeeStatisticsInnerServiceSMO;
import com.java110.job.export.IExportDataAdapt;
import com.java110.utils.util.Assert;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * 收费情况（停车费）导出，按停车场统计
 */
@Service("dataReportParkingFeeStatistics")
public class DataReportParkingFeeStatisticsAdapt implements IExportDataAdapt {

    @Autowired
    private IParkingAreaV1InnerServiceSMO parkingAreaV1InnerServiceSMOImpl;

    @Autowired
    private IReportFeeStatisticsInnerServiceSMO reportFeeStatisticsInnerServiceSMOImpl;

    @Override
    public SXSSFWorkbook exportData(ExportDataDto exportDataDto) {
        JSONObject reqJson = exportDataDto.getReqJson();
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
        String startDate = reqJson.getString("startDate");
        String endDate = reqJson.getString("endDate");
        if (!StringUtil.isEmpty(startDate) && !startDate.contains(":")) {
            startDate += " 00:00:00";
            reqJson.put("startDate", startDate);
        }
        if (!StringUtil.isEmpty(endDate) && !endDate.contains(":")) {
            endDate += " 23:59:59";
            reqJson.put("endDate", endDate);
        }

        SXSSFWorkbook workbook = new SXSSFWorkbook();  //工作簿
        workbook.setCompressTempFiles(false);

        Sheet sheet = workbook.createSheet("收费情况");
        Row row = sheet.createRow(0);
        row.createCell(0).setCellValue("停车场");
        row.createCell(1).setCellValue("车位数");
        row.createCell(2).setCellValue("收费车位数");
        row.createCell(3).setCellValue("历史欠费");
        row.createCell(4).setCellValue("总欠费");
        row.createCell(5).setCellValue("本日已交户数");
        row.createCell(6).setCellValue("本日已交金额");
        row.createCell(7).setCellValue("历史欠费清缴户");
        row.createCell(8).setCellValue("历史欠费清缴金额");
        row.createCell(9).setCellValue("本月已收户数");
        row.createCell(10).setCellValue("剩余车位数");
        row.createCell(11).setCellValue("已收占比");
        row.createCell(12).setCellValue("当月已收金额");
        row.createCell(13).setCellValue("当月剩余未收");
        row.createCell(14).setCellValue("收费率");

        ParkingAreaDto parkingAreaDto = new ParkingAreaDto();
        parkingAreaDto.setCommunityId(reqJson.getString("communityId"));
        String paIds = reqJson.getString("paIds");
        if (!StringUtil.isEmpty(paIds)) {
            parkingAreaDto.setPaIds(paIds.split(","));
        }
        List<ParkingAreaDto> parkingAreaDtos = parkingAreaV1InnerServiceSMOImpl.queryParkingAreas(parkingAreaDto);
        if (parkingAreaDtos == null || parkingAreaDtos.size() < 1) {
            return workbook;
        }

        String monthFastDate = DateUtil.getFormatTimeStringB(DateUtil.getFirstDate(reqJson.getString("startDate")));
        String monthLastDate = DateUtil.getFormatTimeStringB(DateUtil.getNextMonthFirstDate(reqJson.getString("startDate")));
        String todayStartDate = reqJson.getString("startDate");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(DateUtil.getDateFromStringB(todayStartDate));
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        String todayEndDate = DateUtil.getFormatTimeStringB(calendar.getTime());

        List<Map> infos = null;
        int seq = 0;
        for (ParkingAreaDto tmpParkingAreaDto : parkingAreaDtos) {
            QueryStatisticsDto queryStatisticsDto = new QueryStatisticsDto();
            queryStatisticsDto.setCommunityId(reqJson.getString("communityId"));
            queryStatisticsDto.setPaId(tmpParkingAreaDto.getPaId());
            queryStatisticsDto.setFeeTypeCd(reqJson.getString("feeTypeCd"));
            queryStatisticsDto.setStartDate(reqJson.getString("startDate"));
            queryStatisticsDto.setEndDate(reqJson.getString("endDate"));
            queryStatisticsDto.setTodayStartDate(todayStartDate);
            queryStatisticsDto.setTodayEndDate(todayEndDate);
            queryStatisticsDto.setHisDate(monthFastDate);
            queryStatisticsDto.setMonthEndDate(monthLastDate);
            infos = reportFeeStatisticsInnerServiceSMOImpl.getParkingAreaFeeStatistics(queryStatisticsDto);
            if (infos == null || infos.size() < 1) {
                continue;
            }
            for (Map info : infos) {
                appendData(info, sheet, seq);
                seq++;
            }
        }

        return workbook;
    }

    /**
     * 封装数据到Excel中
     *
     * @param data
     * @param sheet
     */
    private void appendData(Map data, Sheet sheet, int seq) {

        Row row = sheet.createRow(seq + 1);

        row.createCell(0).setCellValue(getValue(data, "paName"));
        double psCount = getDoubleValue(data, "psCount");
        row.createCell(1).setCellValue(psCount);
        double feePsCount = getDoubleValue(data, "feePsCount");
        row.createCell(2).setCellValue(feePsCount);
        row.createCell(3).setCellValue(getDoubleValue(data, "hisMonthOweFee"));
        row.createCell(4).setCellValue(getDoubleValue(data, "oweFee"));
        row.createCell(5).setCellValue(getDoubleValue(data, "todayReceivedRoomCount"));
        row.createCell(6).setCellValue(getDoubleValue(data, "todayReceivedRoomAmount"));
        row.createCell(7).setCellValue(getDoubleValue(data, "hisOweReceivedRoomCount"));
        row.createCell(8).setCellValue(getDoubleValue(data, "hisOweReceivedRoomAmount"));
        double monthReceivedRoomCount = getDoubleValue(data, "monthReceivedRoomCount");
        row.createCell(9).setCellValue(monthReceivedRoomCount);
        row.createCell(10).setCellValue(new BigDecimal(feePsCount)
                .subtract(new BigDecimal(monthReceivedRoomCount)).setScale(0, BigDecimal.ROUND_HALF_UP).doubleValue());
        if (feePsCount <= 0) {
            row.createCell(11).setCellValue("0%");
        } else {
            BigDecimal paidRatio = new BigDecimal(monthReceivedRoomCount)
                    .divide(new BigDecimal(feePsCount), 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(new BigDecimal(100)).setScale(2, BigDecimal.ROUND_HALF_UP);
            row.createCell(11).setCellValue(paidRatio.doubleValue() + "%");
        }
        double monthReceivedRoomAmount = getDoubleValue(data, "monthReceivedRoomAmount");
        row.createCell(12).setCellValue(monthReceivedRoomAmount);
        row.createCell(13).setCellValue(getDoubleValue(data, "curMonthOweFee"));
        double curReceivableFee = getDoubleValue(data, "curReceivableFee");
        if (curReceivableFee <= 0) {
            row.createCell(14).setCellValue("0%");
        } else {
            BigDecimal collectionRate = new BigDecimal(monthReceivedRoomAmount)
                    .divide(new BigDecimal(curReceivableFee), 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(new BigDecimal(100)).setScale(2, BigDecimal.ROUND_HALF_UP);
            row.createCell(14).setCellValue(collectionRate.doubleValue() + "%");
        }
    }

    private String getValue(Map data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return "";
        }
        return value.toString();
    }

    private double getDoubleValue(Map data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return 0.0;
        }
        return Double.parseDouble(value.toString());
    }
}
