package com.java110.report.cmd.reportFeeMonthStatistics;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.contract.ContractDto;
import com.java110.dto.owner.OwnerCarDto;
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
 * 查询车辆费用明细表命令类
 * 以车辆为维度统计车辆的各个费用项欠费和实收情况
 * 主要功能包括：验证查询参数、查询车辆信息、计算车辆费用明细并返回统计结果
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "reportFeeMonthStatistics.queryReportFeeDetailCar")
public class QueryReportFeeDetailCarCmd extends Cmd {

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
     * 验证请求参数
     * 检查必要的查询参数是否完整和有效
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException   命令执行异常
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
     * 执行查询车辆费用明细命令
     * 查询车辆信息并计算各车辆的费用欠费和实收情况
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException   命令执行异常
     * @throws ParseException 解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从请求头中获取店铺ID
        String storeId = context.getReqHeaders().get("store-id");

        // 构建查询统计DTO对象
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

        // 查询车辆总数
        long count = baseDataStatisticsImpl.getCarCount(queryStatisticsDto);
        List<OwnerCarDto> ownerCarDtos = null;
        
        // 根据查询结果获取车辆列表
        if (count > 0) {
            ownerCarDtos = baseDataStatisticsImpl.getCar(queryStatisticsDto);
        } else {
            ownerCarDtos = new ArrayList<>();
        }

        // 计算车辆欠费和实收费用数据
        JSONArray datas = computeCarOweReceivedFee(ownerCarDtos, queryStatisticsDto);

        // 构建返回结果
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) queryStatisticsDto.getRow()), count, datas);

        // 设置响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 计算车辆欠费和实收费用
     * 根据车辆列表和查询条件，统计每辆车的各项费用欠费和实收情况
     *
     * @param ownerCarDtos     车辆信息列表
     * @param queryStatisticsDto 查询统计条件
     * @return 包含费用统计结果的JSON数组
     */
    private JSONArray computeCarOweReceivedFee(List<OwnerCarDto> ownerCarDtos, QueryStatisticsDto queryStatisticsDto) {
        // 如果车辆列表为空，返回空数组
        if (ownerCarDtos == null || ownerCarDtos.size() < 1) {
            return new JSONArray();
        }

        JSONArray datas = new JSONArray();
        JSONObject data = null;

        // 收集所有车辆ID用于后续费用查询
        List<String> objIds = new ArrayList<>();
        for (OwnerCarDto ownerCarDto : ownerCarDtos) {
            objIds.add(ownerCarDto.getCarId());
            // 构建基础车辆信息对象
            data = new JSONObject();
            data.put("carId", ownerCarDto.getCarId());
            data.put("carNum", ownerCarDto.getCarNum());
            data.put("ownerName", ownerCarDto.getOwnerName());
            data.put("ownerId", ownerCarDto.getOwnerId());
            data.put("link", ownerCarDto.getLink());
            datas.add(data);
        }

        // 设置对象ID列表并查询费用汇总信息
        queryStatisticsDto.setObjIds(objIds.toArray(new String[objIds.size()]));
        List<Map> infos = feeStatisticsImpl.getObjFeeSummary(queryStatisticsDto);

        // 如果没有费用信息，直接返回基础车辆信息
        if (infos == null || infos.size() < 1) {
            return datas;
        }

        // 计算每辆车的费用统计
        BigDecimal oweFee = null;
        BigDecimal receivedFee = null;
        double oweFeeD = 0;
        double receivedFeeD = 0;
        for (int dataIndex = 0; dataIndex < datas.size(); dataIndex++) {
            // 初始化费用累计值
            oweFee = new BigDecimal(0.00);
            receivedFee = new BigDecimal(0.00);
            data = datas.getJSONObject(dataIndex);
            
            // 遍历费用信息，匹配对应的车辆
            for (Map info : infos) {
                if (!data.get("carId").toString().equals(info.get("objId"))) {
                    continue; // 跳过不匹配的费用记录
                }

                // 获取欠费和实收金额
                oweFeeD = Double.parseDouble(info.get("oweFee").toString());
                receivedFeeD = Double.parseDouble(info.get("receivedFee").toString());

                // 累加总费用
                oweFee = oweFee.add(new BigDecimal(oweFeeD + ""));
                receivedFee = receivedFee.add(new BigDecimal(receivedFeeD + ""));
                
                // 按费用类型分别记录欠费和实收
                data.put("oweFee" + info.get("feeTypeCd").toString(), MoneyUtil.computePriceScale(oweFeeD));
                data.put("receivedFee" + info.get("feeTypeCd").toString(), MoneyUtil.computePriceScale(receivedFeeD));
            }
            
            // 设置总欠费和总实收
            data.put("oweFee", MoneyUtil.computePriceScale(oweFee.doubleValue()));
            data.put("receivedFee", MoneyUtil.computePriceScale(receivedFee.doubleValue()));
        }

        return datas;
    }
}