package com.java110.report.cmd.reportFeeMonthStatistics;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.contract.ContractDto;
import com.java110.dto.report.QueryStatisticsDto;
import com.java110.report.statistics.IBaseDataStatistics;
import com.java110.report.statistics.IFeeStatistics;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.MoneyUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 查询合同费用明细表命令类
 * 以合同为维度统计各个费用项的欠费和实收情况，生成合同费用明细报表
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "reportFeeMonthStatistics.queryReportFeeDetailContract")
public class QueryReportFeeDetailContractCmd extends Cmd {

    /**
     * 费用统计服务接口
     */
    @Autowired
    private IFeeStatistics feeStatisticsImpl;

    /**
     * 基础数据统计服务接口
     */
    @Autowired
    private IBaseDataStatistics baseDataStatisticsImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException  命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证分页信息
        super.validatePageInfo(reqJson);
        // 验证必填参数
        Assert.hasKeyAndValue(reqJson, "startDate", "未包含开始日期");
        Assert.hasKeyAndValue(reqJson, "endDate", "未包含结束日期");
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区信息");
    }

    /**
     * 执行命令方法
     * 查询合同费用明细数据并返回统计结果
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException  命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从请求头中获取店铺ID
        String storeId = context.getReqHeaders().get("store-id");

        // 构建查询统计参数对象
        QueryStatisticsDto queryStatisticsDto = new QueryStatisticsDto();
        queryStatisticsDto.setCommunityId(reqJson.getString("communityId"));
        queryStatisticsDto.setStartDate(reqJson.getString("startDate"));
        queryStatisticsDto.setEndDate(reqJson.getString("endDate"));
        queryStatisticsDto.setConfigId(reqJson.getString("configId"));
        queryStatisticsDto.setFloorId(reqJson.getString("floorId"));
        queryStatisticsDto.setObjName(reqJson.getString("objName"));
        queryStatisticsDto.setFeeTypeCd(reqJson.getString("feeTypeCd"));
        queryStatisticsDto.setOwnerName(reqJson.getString("ownerName"));
        queryStatisticsDto.setLink(reqJson.getString("link"));
        queryStatisticsDto.setPage(reqJson.getInteger("page"));
        queryStatisticsDto.setRow(reqJson.getInteger("row"));
        queryStatisticsDto.setStoreId(storeId);

        // 查询合同总数
        long count = baseDataStatisticsImpl.getContractCount(queryStatisticsDto);
        List<ContractDto> contractDtos = null;
        
        // 根据查询结果获取合同列表
        if (count > 0) {
            contractDtos = baseDataStatisticsImpl.getContract(queryStatisticsDto);
        } else {
            contractDtos = new ArrayList<>();
        }

        // 计算合同欠费和实收费用数据
        JSONArray datas = computeContractOweReceivedFee(contractDtos, queryStatisticsDto);

        // 构建返回结果对象
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) queryStatisticsDto.getRow()), count, datas);

        // 设置响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 计算合同欠费和实收费用
     * 根据合同列表和查询条件，统计每个合同的各项费用欠费和实收情况
     *
     * @param contractDtos      合同数据列表
     * @param queryStatisticsDto 查询统计参数
     * @return 包含合同费用统计数据的JSON数组
     */
    private JSONArray computeContractOweReceivedFee(List<ContractDto> contractDtos, QueryStatisticsDto queryStatisticsDto) {
        // 如果合同列表为空，返回空数组
        if (contractDtos == null || contractDtos.size() < 1) {
            return new JSONArray();
        }

        JSONArray datas = new JSONArray();
        JSONObject data = null;

        // 收集所有合同ID
        List<String> objIds = new ArrayList<>();
        for (ContractDto contractDto : contractDtos) {
            objIds.add(contractDto.getContractId());
            // 构建基础合同信息对象
            data = new JSONObject();
            data.put("contractId", contractDto.getContractId());
            data.put("contractName", contractDto.getContractName() + "(" + contractDto.getContractCode() + ")");
            data.put("ownerName", contractDto.getPartyB());
            data.put("ownerId", contractDto.getObjId());
            data.put("link", contractDto.getbLink());
            datas.add(data);
        }

        // 设置查询参数中的对象ID列表
        queryStatisticsDto.setObjIds(objIds.toArray(new String[objIds.size()]));
        // 查询费用汇总信息
        List<Map> infos = feeStatisticsImpl.getObjFeeSummary(queryStatisticsDto);

        // 如果没有费用信息，直接返回基础合同数据
        if (infos == null || infos.size() < 1) {
            return datas;
        }

        // 计算每个合同的欠费和实收总额
        BigDecimal oweFee = null;
        BigDecimal receivedFee = null;
        double oweFeeD = 0;
        double receivedFeeD = 0;
        
        // 遍历所有合同数据
        for (int dataIndex = 0; dataIndex < datas.size(); dataIndex++) {
            data = datas.getJSONObject(dataIndex);
            // 初始化欠费和实收总额
            oweFee = new BigDecimal(0.00);
            receivedFee = new BigDecimal(0.00);
            
            // 遍历费用汇总信息，匹配当前合同
            for (Map info : infos) {
                // 如果费用信息不属于当前合同，跳过
                if (!data.get("contractId").toString().equals(info.get("objId"))) {
                    continue;
                }

                // 获取单个费用类型的欠费和实收金额
                oweFeeD = Double.parseDouble(info.get("oweFee").toString());
                receivedFeeD = Double.parseDouble(info.get("receivedFee").toString());

                // 累加总额
                oweFee = oweFee.add(new BigDecimal(oweFeeD + ""));
                receivedFee = receivedFee.add(new BigDecimal(receivedFeeD + ""));
                
                // 按费用类型分别存储欠费和实收金额
                data.put("oweFee" + info.get("feeTypeCd").toString(), MoneyUtil.computePriceScale(oweFeeD));
                data.put("receivedFee" + info.get("feeTypeCd").toString(), MoneyUtil.computePriceScale(receivedFeeD));
            }
            
            // 设置合同的总欠费和总实收金额
            data.put("oweFee", MoneyUtil.computePriceScale(oweFee.doubleValue()));
            data.put("receivedFee", MoneyUtil.computePriceScale(receivedFee.doubleValue()));
        }

        return datas;
    }
}