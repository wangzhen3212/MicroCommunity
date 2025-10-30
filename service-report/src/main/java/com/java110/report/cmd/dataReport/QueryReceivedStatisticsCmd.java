package com.java110.report.cmd.dataReport;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.report.QueryStatisticsDto;
import com.java110.report.statistics.IFeeStatistics;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.MoneyUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 查询实收统计命令类
 * 功能：处理数据报表中实收统计查询请求，按楼栋统计费用实收情况
 * 服务码：dataReport.queryReceivedStatistics
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "dataReport.queryReceivedStatistics")
public class QueryReceivedStatisticsCmd extends Cmd {

    @Autowired
    private IFeeStatistics feeStatisticsImpl;

    /**
     * 验证请求参数
     * 检查必要参数是否存在，并对日期参数进行格式化处理
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证小区ID必填
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
        
        // 处理开始日期格式，如果没有时间部分则添加默认时间
        String startDate = reqJson.getString("startDate");
        String endDate = reqJson.getString("endDate");
        if (!StringUtil.isEmpty(startDate) && !startDate.contains(":")) {
            startDate += " 00:00:00";
            reqJson.put("startDate", startDate);
        }
        // 处理结束日期格式，如果没有时间部分则添加默认时间
        if (!StringUtil.isEmpty(endDate) && !endDate.contains(":")) {
            endDate += " 23:59:59";
            reqJson.put("endDate", endDate);
        }
    }

    /**
     * 执行实收统计查询命令
     * 根据查询条件获取楼栋实收费用数据，并进行数据聚合计算
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 构建查询参数对象
        QueryStatisticsDto queryStatisticsDto = new QueryStatisticsDto();
        queryStatisticsDto.setCommunityId(reqJson.getString("communityId"));
        queryStatisticsDto.setStartDate(reqJson.getString("startDate"));
        queryStatisticsDto.setEndDate(reqJson.getString("endDate"));
        queryStatisticsDto.setConfigId(reqJson.getString("configId"));
        queryStatisticsDto.setObjName(reqJson.getString("objName"));
        queryStatisticsDto.setFeeTypeCd(reqJson.getString("feeTypeCd"));
        queryStatisticsDto.setOwnerName(reqJson.getString("ownerName"));

        List<Map> datas = null;
        // 按楼栋查询实收费用数据
        datas = feeStatisticsImpl.getReceivedFeeByFloor(queryStatisticsDto);

        // 对楼栋实收费用进行聚合计算
        datas = computeFloorReceivedFee(datas);

        // 设置响应结果
        context.setResponseEntity(ResultVo.createResponseEntity(datas));
    }

    /**
     * 计算楼栋实收费用
     * 对相同楼栋的费用数据进行聚合，计算总实收费用和各费用类型的实收费用
     *
     * @param datas 原始费用数据列表，包含楼栋信息和费用明细
     * @return 聚合后的楼栋费用数据列表，每个楼栋一条记录
     */
    private List<Map> computeFloorReceivedFee(List<Map> datas) {
        // 如果数据为空，返回空列表
        if (ListUtil.isNull(datas)) {
            return new ArrayList<>();
        }

        // 去重处理，获取唯一的楼栋列表
        List<Map> tmpDatas = new ArrayList<>();
        for (Map data : datas) {
            if (!hasInTmp(tmpDatas, data)) {
                tmpDatas.add(data);
            }
        }

        // 如果去重后数据为空，返回空列表
        if (ListUtil.isNull(tmpDatas)) {
            return new ArrayList<>();
        }

        // 对每个楼栋进行费用聚合计算
        BigDecimal receivedFee = null;
        double receivedFeeD = 0;
        for (Map tmpData : tmpDatas) {
            receivedFee = new BigDecimal(0.00);
            // 遍历原始数据，计算当前楼栋的总费用和各类型费用
            for (Map data : datas) {
                // 只处理相同楼栋的数据
                if (!data.get("floorId").toString().equals(tmpData.get("floorId"))) {
                    continue;
                }
                // 累加实收费用
                receivedFeeD = Double.parseDouble(data.get("receivedFee").toString());
                receivedFee = receivedFee.add(new BigDecimal(receivedFeeD + ""));
                // 按费用类型分别记录实收费用
                tmpData.put("receivedFee" + data.get("feeTypeCd").toString(), MoneyUtil.computePriceScale(receivedFeeD));
            }
            // 设置楼栋总实收费用
            tmpData.put("receivedFee", MoneyUtil.computePriceScale(receivedFee.doubleValue()));
        }

        return tmpDatas;
    }

    /**
     * 检查楼栋数据是否已存在于临时列表中
     * 通过楼栋ID判断是否重复
     *
     * @param tmpDatas 临时数据列表
     * @param data 待检查的数据
     * @return true-已存在，false-不存在
     */
    private boolean hasInTmp(List<Map> tmpDatas, Map data) {
        for (Map tmpData : tmpDatas) {
            // 通过楼栋ID判断是否重复
            if (tmpData.get("floorId").equals(data.get("floorId"))) {
                return true;
            }
        }
        return false;
    }

}