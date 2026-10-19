package com.java110.job.export.adapt;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.dto.data.ExportDataDto;
import com.java110.dto.reportFee.ReportFeeMonthStatisticsDto;
import com.java110.intf.report.IQueryPayFeeDetailInnerServiceSMO;
import com.java110.job.export.IExportDataAdapt;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Date;

/**
 * 缴费明细导出
 */
@Service("reportPayFeeDetail")
public class ReportPayFeeDetailAdapt implements IExportDataAdapt {

    @Autowired
    private IQueryPayFeeDetailInnerServiceSMO queryPayFeeDetailInnerServiceSMOImpl;

    private static final int MAX_ROW = 60000;

    @Override
    public SXSSFWorkbook exportData(ExportDataDto exportDataDto) throws ParseException {
        SXSSFWorkbook workbook = null;  //工作簿
        String userId = "";
        //工作表
        workbook = new SXSSFWorkbook();
        workbook.setCompressTempFiles(false);
        Sheet sheet = workbook.createSheet("缴费明细表");
        Row row = sheet.createRow(0);
        row.createCell(0).setCellValue("订单号");
        row.createCell(1).setCellValue("房号/业主");
        row.createCell(2).setCellValue("费用类型>费用项");
        row.createCell(3).setCellValue("费用状态");
        row.createCell(4).setCellValue("支付方式");
        row.createCell(5).setCellValue("缴费时间段");
        row.createCell(6).setCellValue("缴费时间");
        row.createCell(7).setCellValue("收银员");
        row.createCell(8).setCellValue("应缴/应收金额(元)");
        row.createCell(9).setCellValue("实收金额(元)");
        row.createCell(10).setCellValue("账户抵扣(元)");
        row.createCell(11).setCellValue("优惠/减免金额(元)");
        row.createCell(12).setCellValue("赠送金额(元)");
        row.createCell(13).setCellValue("滞纳金(元)");
        row.createCell(14).setCellValue("面积(平方米)");
        row.createCell(15).setCellValue("车牌号");
        row.createCell(16).setCellValue("说明");
        JSONObject reqJson = exportDataDto.getReqJson();

        String endTime = reqJson.getString("endTime");

        if (!StringUtil.isEmpty(endTime) && !endTime.contains(":")) {
            endTime += " 23:59:59";
            reqJson.put("endTime", endTime);
        }
        ReportFeeMonthStatisticsDto reportFeeMonthStatisticsDto = BeanConvertUtil.covertBean(reqJson, ReportFeeMonthStatisticsDto.class);
        //查询数据
        getRepairPayFeeDetail(sheet, reportFeeMonthStatisticsDto);
        return workbook;
    }

    private void getRepairPayFeeDetail(Sheet sheet, ReportFeeMonthStatisticsDto reportFeeMonthStatisticsDto) throws ParseException {
        reportFeeMonthStatisticsDto.setPage(1);
        reportFeeMonthStatisticsDto.setRow(MAX_ROW);
        ResultVo resultVo = queryPayFeeDetailInnerServiceSMOImpl.query(reportFeeMonthStatisticsDto);
        appendData(resultVo, sheet, 0);
        if (resultVo.getRecords() < 2) {
            return;
        }
        for (int page = 2; page <= resultVo.getRecords(); page++) {
            reportFeeMonthStatisticsDto.setPage(page);
            reportFeeMonthStatisticsDto.setRow(MAX_ROW);
            resultVo = queryPayFeeDetailInnerServiceSMOImpl.query(reportFeeMonthStatisticsDto);
            appendData(resultVo, sheet, (page - 1) * MAX_ROW);
        }
    }

    private void appendData(ResultVo resultVo, Sheet sheet, int step) throws ParseException {
        Object data = resultVo.getData();
        JSONArray reportFeeMonthStatisticsDtos = JSONArray.parseArray(JSONObject.toJSONString(data));
//        List<ReportFeeMonthStatisticsDto> reportFeeMonthStatisticsDtos = (List<ReportFeeMonthStatisticsDto>) resultVo.getData();
        if (ListUtil.isNull(reportFeeMonthStatisticsDtos)) {
            return;
        }
        Row row = null;
        JSONObject dataObj = null;
        for (int roomIndex = 0; roomIndex < reportFeeMonthStatisticsDtos.size(); roomIndex++) {
            row = sheet.createRow(roomIndex + step + 1);
            dataObj = reportFeeMonthStatisticsDtos.getJSONObject(roomIndex);
            row.createCell(0).setCellValue(getValue(dataObj, "oId", "-"));
            row.createCell(1).setCellValue(getValue(dataObj, "payerObjName") + "/" + getValue(dataObj, "ownerName"));
            row.createCell(2).setCellValue(getValue(dataObj, "feeTypeCdName") + ">" + getValue(dataObj, "feeName"));
            row.createCell(3).setCellValue(getValue(dataObj, "stateName"));
            row.createCell(4).setCellValue(getValue(dataObj, "primeRate"));
            row.createCell(5).setCellValue(formatDate(dataObj.getString("startTime")) + " ~ " + formatDate(dataObj.getString("endTime")));
            row.createCell(6).setCellValue(formatDateTime(dataObj.getDate("createTime")));
            row.createCell(7).setCellValue(getValue(dataObj, "cashierName", "-"));
            row.createCell(8).setCellValue(getValue(dataObj, "payableAmount") + "/" + getValue(dataObj, "receivableAmount"));
            row.createCell(9).setCellValue(getValue(dataObj, "receivedAmount"));
            row.createCell(10).setCellValue(getValue(dataObj, "withholdAmount", "0"));
            row.createCell(11).setCellValue(getValue(dataObj, "preferentialAmount") + "/" + getValue(dataObj, "deductionAmount"));
            row.createCell(12).setCellValue(getValue(dataObj, "giftAmount"));
            row.createCell(13).setCellValue(getValue(dataObj, "lateFee"));
            row.createCell(14).setCellValue(getValue(dataObj, "builtUpArea"));
            row.createCell(15).setCellValue(getValue(dataObj, "carNum", "-"));
            row.createCell(16).setCellValue(getValue(dataObj, "remark"));
        }
    }

    private String getValue(JSONObject dataObj, String key) {
        return getValue(dataObj, key, "");
    }

    private String getValue(JSONObject dataObj, String key, String defaultValue) {
        String value = dataObj.getString(key);
        return StringUtil.isEmpty(value) ? defaultValue : value;
    }

    private String formatDate(String date) {
        if (StringUtil.isEmpty(date)) {
            return "";
        }
        if (date.length() > 10) {
            return date.substring(0, 10);
        }
        return date;
    }

    private String formatDateTime(Date date) {
        if (date == null) {
            return "";
        }
        return DateUtil.getFormatTimeStringA(date);
    }
}
