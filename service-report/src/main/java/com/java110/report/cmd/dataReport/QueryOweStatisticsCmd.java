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
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 查询欠费统计命令类
 * 负责处理欠费统计数据的查询和计算，按楼栋维度统计各类费用的欠费情况
 * 服务代码：dataReport.queryOweStatistics
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "dataReport.queryOweStatistics")
public class QueryOweStatisticsCmd extends Cmd {

    @Autowired
    private IFeeStatistics feeStatisticsImpl;

    /**
     * 验证请求参数
     * 检查必要参数是否存在，并对日期参数进行格式化处理
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证小区ID必填
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
        
        // 处理开始日期格式
        String startDate = reqJson.getString("startDate");
        String endDate = reqJson.getString("endDate");
        
        // 如果开始日期不为空且不包含时间部分，添加默认时间
        if (!StringUtil.isEmpty(startDate) && !startDate.contains(":")) {
            startDate += " 00:00:00";
            reqJson.put("startDate", startDate);
        }
        
        // 如果结束日期不为空且不包含时间部分，添加默认时间
        if (!StringUtil.isEmpty(endDate) && !endDate.contains(":")) {
            endDate += " 23:59:59";
            reqJson.put("endDate", endDate);
        }
    }

    /**
     * 执行欠费统计查询命令
     * 查询欠费数据并按楼栋进行汇总计算
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 构建查询条件对象
        QueryStatisticsDto queryStatisticsDto = new QueryStatisticsDto();
        queryStatisticsDto.setCommunityId(reqJson.getString("communityId"));
        queryStatisticsDto.setStartDate(reqJson.getString("startDate"));
        queryStatisticsDto.setEndDate(reqJson.getString("endDate"));
        queryStatisticsDto.setConfigId(reqJson.getString("configId"));
        queryStatisticsDto.setObjName(reqJson.getString("objName"));
        queryStatisticsDto.setFeeTypeCd(reqJson.getString("feeTypeCd"));
        queryStatisticsDto.setOwnerName(reqJson.getString("ownerName"));

        List<Map> datas = null;
        // 按楼栋查询欠费数据
        datas = feeStatisticsImpl.getOweFeeByFloor(queryStatisticsDto);

        // 计算欠费实收费用
        datas = computeOweReceivedFee(datas);

        // 设置响应结果
        context.setResponseEntity(ResultVo.createResponseEntity(datas));
    }

    /**
     * 计算欠费实收费用
     * 按楼栋维度汇总各类费用的欠费金额，并计算总欠费
     *
     * @param datas 原始欠费数据列表
     * @return 处理后的欠费数据列表，按楼栋汇总
     */
    private List<Map> computeOweReceivedFee(List<Map> datas) {
        // 检查数据是否为空
        if (datas == null || datas.size() < 1) {
            return new ArrayList<>();
        }

        // 去重处理：获取不重复的楼栋数据
        List<Map> tmpDatas = new ArrayList<>();
        for (Map data : datas) {
            if (!hasInTmp(tmpDatas, data)) {
                tmpDatas.add(data);
            }
        }

        // 再次检查去重后的数据
        if (tmpDatas == null || tmpDatas.size() < 1) {
            return new ArrayList<>();
        }

        BigDecimal oweFee = null;
        // 遍历每个楼栋，计算总欠费和各类费用欠费
        for (Map tmpData : tmpDatas) {
            oweFee = new BigDecimal(0.00);
            // 遍历所有数据，匹配当前楼栋的数据
            for (Map data : datas) {
                // 只处理相同楼栋ID的数据
                if (!data.get("floorId").toString().equals(tmpData.get("floorId"))) {
                    continue;
                }

                // 累加欠费金额
                oweFee = oweFee.add(new BigDecimal(data.get("oweFee").toString()));
                // 按费用类型分别存储欠费金额
                tmpData.put("oweFee" + data.get("feeTypeCd").toString(), data.get("oweFee"));
            }
            // 设置楼栋总欠费金额
            tmpData.put("oweFee", oweFee.doubleValue());
        }

        return tmpDatas;
    }

    /**
     * 检查数据是否已存在于临时列表中
     * 根据楼栋ID判断数据是否重复
     *
     * @param tmpDatas 临时数据列表
     * @param data 待检查的数据
     * @return true-已存在，false-不存在
     */
    private boolean hasInTmp(List<Map> tmpDatas, Map data) {
        for (Map tmpData : tmpDatas) {
            // 比较楼栋ID判断是否重复
            if (tmpData.get("floorId").equals(data.get("floorId"))) {
                return true;
            }
        }
        return false;
    }

}