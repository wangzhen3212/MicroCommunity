package com.java110.job.export.adapt;

import com.alibaba.fastjson.JSONObject;
import com.java110.dto.data.ExportDataDto;
import com.java110.dto.payFeeOrder.PayFeeOrderDetailDto;
import com.java110.dto.payFeeOrder.PayFeeOrderDto;
import com.java110.intf.fee.IPayFeeOrderV1InnerServiceSMO;
import com.java110.job.export.IExportDataAdapt;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 缴费订单导出
 */
@Service("reportPayFeeOrder")
public class ReportPayFeeOrderAdapt implements IExportDataAdapt {

    @Autowired
    private IPayFeeOrderV1InnerServiceSMO payFeeOrderV1InnerServiceSMOImpl;

    private static final int MAX_ROW = 60000;

    @Override
    public SXSSFWorkbook exportData(ExportDataDto exportDataDto) {
        SXSSFWorkbook workbook = new SXSSFWorkbook();
        workbook.setCompressTempFiles(false);
        Sheet sheet = workbook.createSheet("缴费订单表");
        Row row = sheet.createRow(0);
        row.createCell(0).setCellValue("订单号");
        row.createCell(1).setCellValue("订单类型");
        row.createCell(2).setCellValue("房号/业主");
        row.createCell(3).setCellValue("费用项");
        row.createCell(4).setCellValue("缴费时间段");
        row.createCell(5).setCellValue("明细应收/实收");
        row.createCell(6).setCellValue("订单应收/实收");
        row.createCell(7).setCellValue("费用状态");
        row.createCell(8).setCellValue("支付方式");
        row.createCell(9).setCellValue("缴费时间");
        row.createCell(10).setCellValue("收银员");
        row.createCell(11).setCellValue("账户抵扣(元)");
        row.createCell(12).setCellValue("优惠");
        row.createCell(13).setCellValue("赠送金额(元)");
        row.createCell(14).setCellValue("滞纳金(元)");
        row.createCell(15).setCellValue("面积(平方米)");
        row.createCell(16).setCellValue("车牌号");
        row.createCell(17).setCellValue("说明");

        JSONObject reqJson = exportDataDto.getReqJson();
        String endTime = reqJson.getString("endTime");
        if (!StringUtil.isEmpty(endTime) && !endTime.contains(":")) {
            endTime += " 23:59:59";
            reqJson.put("endTime", endTime);
        }
        PayFeeOrderDto payFeeOrderDto = BeanConvertUtil.covertBean(reqJson, PayFeeOrderDto.class);
        JSONObject countInfo = payFeeOrderV1InnerServiceSMOImpl.queryPayFeeOrdersCount(payFeeOrderDto);
        int count = Integer.parseInt(countInfo.get("count").toString());
        if (count < 1) {
            return workbook;
        }

        CellStyle wrapStyle = workbook.createCellStyle();
        wrapStyle.setWrapText(true);

        int pages = (int) Math.ceil((double) count / (double) MAX_ROW);
        for (int page = 1; page <= pages; page++) {
            payFeeOrderDto.setPage(page);
            payFeeOrderDto.setRow(MAX_ROW);
            List<PayFeeOrderDto> payFeeOrderDtos = payFeeOrderV1InnerServiceSMOImpl.queryReportPayFeeOrders(payFeeOrderDto);
            appendData(payFeeOrderDtos, sheet, (page - 1) * MAX_ROW, wrapStyle);
        }
        return workbook;
    }

    private void appendData(List<PayFeeOrderDto> payFeeOrderDtos, Sheet sheet, int step, CellStyle wrapStyle) {
        if (ListUtil.isNull(payFeeOrderDtos)) {
            return;
        }
        Row row = null;
        for (int index = 0; index < payFeeOrderDtos.size(); index++) {
            PayFeeOrderDto payFeeOrderDto = payFeeOrderDtos.get(index);
            row = sheet.createRow(index + step + 1);
            row.createCell(0).setCellValue(getValue(payFeeOrderDto.getPayOrderId(), "-"));
            row.createCell(1).setCellValue(PayFeeOrderDto.ORDER_TYPE_PAY_FEE.equals(payFeeOrderDto.getOrderType()) ? "缴费单" : "预付单");
            row.createCell(2).setCellValue(getValue(payFeeOrderDto.getPayerObjName()) + "/" + getValue(payFeeOrderDto.getOwnerName()));
            row.createCell(6).setCellValue(getValue(payFeeOrderDto.getReceivableAmount()) + "/" + getValue(payFeeOrderDto.getReceivedAmount()));
            row.createCell(7).setCellValue(getValue(payFeeOrderDto.getStateName()));
            row.createCell(8).setCellValue(getValue(payFeeOrderDto.getPrimeRateName()));
            row.createCell(9).setCellValue(payFeeOrderDto.getCreateTime() == null ? "" : DateUtil.getFormatTimeStringA(payFeeOrderDto.getCreateTime()));
            row.createCell(10).setCellValue(getValue(payFeeOrderDto.getCashierName(), "-"));
            row.createCell(11).setCellValue(getWithholdAmount(payFeeOrderDto));
            row.createCell(12).setCellValue(getPreferentialAmount(payFeeOrderDto));
            row.createCell(13).setCellValue(getValue(payFeeOrderDto.getGiftAmount()));
            row.createCell(14).setCellValue(getLateFee(payFeeOrderDto));
            row.createCell(15).setCellValue(getValue(payFeeOrderDto.getBuiltUpArea()));
            row.createCell(16).setCellValue(getValue(payFeeOrderDto.getCarNum(), "-"));
            row.createCell(17).setCellValue(getValue(payFeeOrderDto.getRemark()));

            StringBuilder feeNames = new StringBuilder();
            StringBuilder feePeriods = new StringBuilder();
            StringBuilder feeAmounts = new StringBuilder();
            List<PayFeeOrderDetailDto> payFeeOrderDetailDtos = payFeeOrderDto.getPayFeeOrderDetailDtos();
            if (!ListUtil.isNull(payFeeOrderDetailDtos)) {
                for (PayFeeOrderDetailDto payFeeOrderDetailDto : payFeeOrderDetailDtos) {
                    if (feeNames.length() > 0) {
                        feeNames.append("\n");
                        feePeriods.append("\n");
                        feeAmounts.append("\n");
                    }
                    feeNames.append(getValue(payFeeOrderDetailDto.getFeeName()));
                    feePeriods.append(formatDate(payFeeOrderDetailDto.getStartTime()))
                            .append(" ~ ")
                            .append(formatDate(payFeeOrderDetailDto.getEndTime()));
                    feeAmounts.append(getValue(payFeeOrderDetailDto.getReceivableAmount()))
                            .append("/")
                            .append(getValue(payFeeOrderDetailDto.getReceivedAmount()));
                }
            }
            createWrappedCell(row, 3, feeNames.toString(), wrapStyle);
            createWrappedCell(row, 4, feePeriods.toString(), wrapStyle);
            createWrappedCell(row, 5, feeAmounts.toString(), wrapStyle);
        }
    }

    private String getWithholdAmount(PayFeeOrderDto payFeeOrderDto) {
        if (!StringUtil.isEmpty(payFeeOrderDto.getWithholdAmount())) {
            return payFeeOrderDto.getWithholdAmount();
        }
        return getValue(payFeeOrderDto.getAcctAmount(), "0");
    }

    private String getPreferentialAmount(PayFeeOrderDto payFeeOrderDto) {
        if (!StringUtil.isEmpty(payFeeOrderDto.getPreferentialAmount())) {
            return payFeeOrderDto.getPreferentialAmount();
        }
        return getValue(payFeeOrderDto.getDiscountAmount());
    }

    private String getLateFee(PayFeeOrderDto payFeeOrderDto) {
        if (!StringUtil.isEmpty(payFeeOrderDto.getLateFee())) {
            return payFeeOrderDto.getLateFee();
        }
        return getValue(payFeeOrderDto.getLateAmount());
    }

    private void createWrappedCell(Row row, int column, String value, CellStyle wrapStyle) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(wrapStyle);
    }

    private String getValue(String value) {
        return getValue(value, "");
    }

    private String getValue(String value, String defaultValue) {
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
}
