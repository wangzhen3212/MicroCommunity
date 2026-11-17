package com.java110.store.cmd.contract;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.contract.ContractDto;
import com.java110.intf.store.IContractInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 合同导出 Excel（台账 / 履约 / 到期提醒 三处复用），返回文件名与 base64 内容
 */
@Java110Cmd(serviceCode = "contract.exportContracts")
public class ExportContractsCmd extends Cmd {

    private static final String EXPORT_LEDGER = "ledger";
    private static final String EXPORT_PERFORMANCE = "performance";
    private static final String EXPORT_REMIND = "remind";
    private static final String DEFAULT_DAYS = "30";

    private static final String[] LEDGER_HEADERS = {"合同编号", "合同名称", "合同模板", "甲方主体", "签约对象",
            "合同金额", "开始日期", "结束日期", "状态", "经办人"};
    private static final String[] PERFORMANCE_HEADERS = {"合同编号", "合同名称", "付款方式", "合同金额", "已付金额",
            "待付金额", "付款节点", "下一节点日期", "履约状态"};
    private static final String[] REMIND_HEADERS = {"合同编号", "合同名称", "合同模板", "签约对象", "到期日期",
            "剩余天数", "合同金额", "经办人"};

    @Autowired
    private IContractInnerServiceSMO contractInnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String storeId = CmdContextUtils.getStoreId(context);
        String exportType = StringUtil.isEmpty(reqJson.getString("exportType"))
                ? EXPORT_LEDGER : reqJson.getString("exportType");

        String sheetName;
        String[] headers;
        List<String[]> rows = new ArrayList<>();
        if (EXPORT_PERFORMANCE.equals(exportType)) {
            sheetName = "合同履约";
            headers = PERFORMANCE_HEADERS;
            rows = buildPerformanceRows(reqJson, storeId);
        } else if (EXPORT_REMIND.equals(exportType)) {
            sheetName = "到期提醒";
            headers = REMIND_HEADERS;
            rows = buildRemindRows(reqJson, storeId);
        } else {
            sheetName = "合同台账";
            headers = LEDGER_HEADERS;
            rows = buildLedgerRows(reqJson, storeId);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("fileName", sheetName + "_" + DateUtil.getNow("yyyyMMddHHmmss") + ".xlsx");
        data.put("base64", buildExcel(sheetName, headers, rows));
        context.setResponseEntity(ResultVo.createResponseEntity(data));
    }

    /**
     * 台账导出（列表查询不 join 正文表）
     */
    private List<String[]> buildLedgerRows(JSONObject reqJson, String storeId) {
        ContractDto contractDto = BeanConvertUtil.covertBean(reqJson, ContractDto.class);
        contractDto.setStoreId(storeId);
        contractDto.setStatusCd("0");
        contractDto.setPage(-1);
        List<ContractDto> contractDtos = contractInnerServiceSMOImpl.queryContracts(contractDto);

        List<String[]> rows = new ArrayList<>();
        if (contractDtos == null) {
            return rows;
        }
        for (ContractDto contract : contractDtos) {
            rows.add(new String[]{contract.getContractCode(), contract.getContractName(),
                    contract.getTemplateName(), contract.getPartyA(), contract.getPartyB(),
                    contract.getAmount(), contract.getStartTime(), contract.getEndTime(),
                    stateNameOf(contract.getState()), contract.getOperator()});
        }
        return rows;
    }

    private List<String[]> buildPerformanceRows(JSONObject reqJson, String storeId) {
        Map<String, Object> info = new HashMap<>();
        info.put("storeId", storeId);
        info.put("page", -1);
        if (!StringUtil.isEmpty(reqJson.getString("category"))) {
            info.put("category", reqJson.getString("category"));
        } else {
            info.put("categories", new String[]{ContractDto.CATEGORY_PURCHASE, ContractDto.CATEGORY_MAINTAIN});
        }
        if (!StringUtil.isEmpty(reqJson.getString("payState"))) {
            info.put("payState", reqJson.getString("payState"));
        }
        if (!StringUtil.isEmpty(reqJson.getString("searchText"))) {
            info.put("searchText", reqJson.getString("searchText"));
        }
        List<Map> performances = contractInnerServiceSMOImpl.queryContractPerformances(info);

        List<String[]> rows = new ArrayList<>();
        if (performances == null) {
            return rows;
        }
        for (Map performance : performances) {
            rows.add(new String[]{valueOf(performance.get("contractCode")), valueOf(performance.get("contractName")),
                    valueOf(performance.get("payMode")), valueOf(performance.get("amount")),
                    valueOf(performance.get("paidAmount")), valueOf(performance.get("unpaidAmount")),
                    valueOf(performance.get("paidCount")) + "/" + valueOf(performance.get("planCount")),
                    valueOf(performance.get("nextPlanTime")), performanceStateOf(performance)});
        }
        return rows;
    }

    private List<String[]> buildRemindRows(JSONObject reqJson, String storeId) {
        Map<String, Object> info = new HashMap<>();
        info.put("storeId", storeId);
        info.put("page", -1);
        info.put("days", StringUtil.isEmpty(reqJson.getString("days")) ? DEFAULT_DAYS : reqJson.getString("days"));
        if (!StringUtil.isEmpty(reqJson.getString("category"))) {
            info.put("category", reqJson.getString("category"));
        }
        if (!StringUtil.isEmpty(reqJson.getString("searchText"))) {
            info.put("searchText", reqJson.getString("searchText"));
        }
        info.put("includeExpired", "1");
        List<Map> reminds = contractInnerServiceSMOImpl.queryContractReminds(info);

        List<String[]> rows = new ArrayList<>();
        if (reminds == null) {
            return rows;
        }
        for (Map remind : reminds) {
            rows.add(new String[]{valueOf(remind.get("contractCode")), valueOf(remind.get("contractName")),
                    valueOf(remind.get("templateName")), valueOf(remind.get("partyB")),
                    valueOf(remind.get("endTime")), valueOf(remind.get("daysLeft")),
                    valueOf(remind.get("amount")), valueOf(remind.get("operator"))});
        }
        return rows;
    }

    private String buildExcel(String sheetName, String[] headers, List<String[]> rows) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        try {
            Sheet sheet = workbook.createSheet(sheetName);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                String[] rowData = rows.get(rowIndex);
                for (int colIndex = 0; colIndex < rowData.length; colIndex++) {
                    row.createCell(colIndex).setCellValue(rowData[colIndex] == null ? "" : rowData[colIndex]);
                }
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception e) {
            throw new CmdException("导出合同数据失败：" + e.getMessage());
        } finally {
            try {
                workbook.close();
            } catch (Exception e) {
                // 忽略关闭异常
            }
        }
    }

    private String valueOf(Object value) {
        return value == null ? "" : value.toString();
    }

    private String performanceStateOf(Map performance) {
        String state = valueOf(performance.get("state"));
        if (!ContractDto.STATE_AUDIT_FINISH.equals(state)) {
            return stateNameOf(state);
        }
        if (parseInt(performance.get("overdueCount")) > 0) {
            return "存在逾期";
        }
        if (parseInt(performance.get("planCount")) > 0
                && parseInt(performance.get("paidCount")) >= parseInt(performance.get("planCount"))) {
            return "已付清";
        }
        return "履约中";
    }

    private String stateNameOf(String state) {
        if (ContractDto.STATE_DRAFT.equals(state)) {
            return "草稿";
        }
        if (ContractDto.STATE_AUDIT.equals(state)) {
            return "待审核";
        }
        if (ContractDto.STATE_AUDIT_FINISH.equals(state)) {
            return "生效中";
        }
        if (ContractDto.STATE_FAIL.equals(state)) {
            return "已驳回";
        }
        if (ContractDto.STATE_COMPLAINT.equals(state)) {
            return "已终止";
        }
        return "";
    }

    private int parseInt(Object value) {
        if (value == null || StringUtil.isEmpty(value.toString())) {
            return 0;
        }
        return Integer.parseInt(value.toString());
    }
}
