package com.java110.report.cmd.reportFeeMonthStatistics;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.report.QueryStatisticsDto;
import com.java110.report.statistics.IBaseDataStatistics;
import com.java110.report.statistics.IFeeStatistics;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.MoneyUtil;
import com.java110.utils.util.StringUtil;
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
 * 费用月统计报表 - 业主费用明细查询命令类
 * 该命令类用于处理业主费用明细查询请求，根据查询条件统计业主的欠费和实收费用信息
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "reportFeeMonthStatistics.queryReportFeeDetailOwner")
public class QueryReportFeeDetailOwnerCmd extends Cmd {

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
     * @throws CmdException   命令异常
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
     * 处理业主费用明细查询业务逻辑，返回费用统计结果
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException   命令异常
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
        queryStatisticsDto.setLink(reqJson.getString("link"));
        queryStatisticsDto.setPage(reqJson.getInteger("page"));
        queryStatisticsDto.setRow(reqJson.getInteger("row"));

        // 查询业主总数
        long count = baseDataStatisticsImpl.getOwnerCount(queryStatisticsDto);
        List<OwnerDto> owners = null;
        
        // 根据查询结果获取业主信息
        if (count > 0) {
            owners = baseDataStatisticsImpl.getOwnerInfo(queryStatisticsDto);
        } else {
            owners = new ArrayList<>();
        }

        // 计算业主欠费和实收费用数据
        JSONArray datas = computeOwnerOweReceivedFee(owners, queryStatisticsDto);

        // 构建返回结果
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) queryStatisticsDto.getRow()), count, datas);

        // 设置响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 计算业主欠费和实收费用
     * 根据业主列表和查询条件，统计每个业主的欠费和实收费用明细
     *
     * @param owners            业主信息列表
     * @param queryStatisticsDto 查询统计参数对象
     * @return JSONArray 包含业主费用统计数据的JSON数组
     */
    private JSONArray computeOwnerOweReceivedFee(List<OwnerDto> owners, QueryStatisticsDto queryStatisticsDto) {
        // 空值检查
        if (owners == null || owners.size() < 1) {
            return new JSONArray();
        }

        JSONArray datas = new JSONArray();
        JSONObject data = null;

        // 提取业主ID列表，用于后续查询
        List<String> ownerIds = new ArrayList<>();
        for (OwnerDto ownerDto : owners) {
            ownerIds.add(ownerDto.getOwnerId());
            // 构建基础业主信息对象
            data = new JSONObject();
            data.put("ownerName", ownerDto.getName());
            data.put("ownerId", ownerDto.getOwnerId());
            data.put("link", ownerDto.getLink());
            datas.add(data);
        }

        // 设置业主ID列表到查询参数中
        queryStatisticsDto.setOwnerIds(ownerIds.toArray(new String[ownerIds.size()]));
        // 查询业主费用汇总信息
        List<Map> infos = feeStatisticsImpl.getOwnerFeeSummary(queryStatisticsDto);

        // 如果没有费用数据，直接返回基础业主信息
        if (infos == null || infos.size() < 1) {
            return datas;
        }

        // 费用统计变量
        BigDecimal oweFee = null;
        BigDecimal receivedFee = null;
        double oweFeeD = 0;
        double receivedFeeD = 0;
        
        // 遍历每个业主数据，计算费用统计
        for (int dataIndex = 0; dataIndex < datas.size(); dataIndex++) {
            data = datas.getJSONObject(dataIndex);
            // 初始化费用统计
            oweFee = new BigDecimal(0.00);
            receivedFee = new BigDecimal(0.00);
            
            // 遍历费用信息，匹配当前业主
            for (Map info : infos) {
                // 检查业主ID是否匹配
                if (!data.get("ownerId").toString().equals(info.get("ownerId"))) {
                    continue;
                }

                // 获取欠费和实收费用
                oweFeeD = Double.parseDouble(info.get("oweFee").toString());
                receivedFeeD = Double.parseDouble(info.get("receivedFee").toString());

                // 累加总费用
                oweFee = oweFee.add(new BigDecimal(oweFeeD + ""));
                receivedFee = receivedFee.add(new BigDecimal(receivedFeeD + ""));
                
                // 按费用类型分别存储费用明细
                data.put("oweFee" + info.get("feeTypeCd").toString(), MoneyUtil.computePriceScale(oweFeeD));
                data.put("receivedFee" + info.get("feeTypeCd").toString(), MoneyUtil.computePriceScale(receivedFeeD));
                data.put("objName", info.get("objName"));
            }
            
            // 设置总费用（格式化后）
            data.put("oweFee", MoneyUtil.computePriceScale(oweFee.doubleValue()));
            data.put("receivedFee", MoneyUtil.computePriceScale(receivedFee.doubleValue()));
            
            // 处理收费对象重复问题
            delRepeatObjName(data);
        }

        return datas;
    }

    /**
     * 去除重复的收费对象名称
     * 对收费对象名称进行去重处理，确保显示不重复的对象名称列表
     *
     * @param data 包含收费对象名称的JSON对象
     */
    private void delRepeatObjName(JSONObject data) {
        // 获取原始收费对象名称
        String objName = data.getString("objName");
        if (StringUtil.isEmpty(objName)) {
            return;
        }

        // 分割对象名称字符串
        String[] objNames = objName.split(",");
        List<String> oNames = new ArrayList<>();
        
        // 去重处理
        for (String oName : objNames) {
            if (!oNames.contains(oName)) {
                oNames.add(oName);
            }
        }
        
        // 重新构建对象名称字符串
        objName = "";
        for (String oName : oNames) {
            objName += (oName + ",");
        }
        
        // 去除末尾逗号
        if (objName.endsWith(",")) {
            objName = objName.substring(0, objName.length() - 1);
        }

        // 更新处理后的对象名称
        data.put("objName", objName);
    }
}