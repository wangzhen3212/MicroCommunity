package com.java110.report.cmd.admin;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.store.StoreDto;
import com.java110.intf.report.IBaseDataStatisticsInnerServiceSMO;
import com.java110.intf.store.IStoreInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.*;

/**
 * 查询小区运营数据分析命令类
 * 该命令类用于获取小区在指定时间段内的各项运营数据统计信息，
 * 包括缴费订单、报修单、巡检数据、保养数据、采购订单、领用订单、
 * 车辆进场、人员进场和合同起草等维度的数据分析
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "admin.getCommunityOperationalAnalysis")
public class GetCommunityOperationalAnalysisCmd extends Cmd {

    @Autowired
    private IStoreInnerServiceSMO storeInnerServiceSMOImpl;

    @Autowired
    private IBaseDataStatisticsInnerServiceSMO baseDataStatisticsInnerServiceSMOImpl;

    /**
     * 验证请求参数的有效性
     * 检查必须的参数是否存在，并验证当前用户是否有权限执行该操作
     *
     * @param event   命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException    当参数验证失败或权限不足时抛出
     * @throws ParseException  当日期解析异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证请求中必须包含小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区信息");

        // 从上下文中获取当前店铺ID
        String storeId = CmdContextUtils.getStoreId(context);

        // 构建店铺查询条件，验证当前用户是否为系统管理员
        StoreDto storeDto = new StoreDto();
        storeDto.setStoreId(storeId);
        storeDto.setStoreTypeCd(StoreDto.STORE_TYPE_ADMIN);
        int count = storeInnerServiceSMOImpl.getStoreCount(storeDto);
        
        // 如果用户不是系统管理员，抛出权限异常
        if (count < 1) {
            throw new CmdException("非法操作，请用系统管理员账户操作");
        }
    }

    /**
     * 执行小区运营数据分析命令
     * 查询指定时间段内小区的各项运营数据，并对数据进行日期填充处理
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据对象，包含查询参数
     * @throws CmdException    当命令执行过程中发生错误时抛出
     * @throws ParseException  当日期解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 获取请求中的开始时间和结束时间参数
        String startTime = reqJson.getString("startTime");
        String endTime = reqJson.getString("endTime");
        String storeId = CmdContextUtils.getStoreId(context);
        
        // 如果未提供开始时间，默认设置为7天前
        if (StringUtil.isEmpty(startTime)) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, -7);
            startTime = DateUtil.getFormatTimeString(calendar.getTime(), DateUtil.DATE_FORMATE_STRING_B);
        }
        
        // 如果未提供结束时间，默认设置为明天
        if (StringUtil.isEmpty(endTime)) {
            endTime = DateUtil.getAddDayStringB(new Date(), 1);
        }

        // 将处理后的时间参数重新设置到请求JSON中
        reqJson.put("startTime", startTime);
        reqJson.put("endTime", endTime);

        // 创建返回数据对象
        JSONObject data = new JSONObject();

        // 查询小区七天内缴费订单数
        List<Map> feeDetailData = baseDataStatisticsInnerServiceSMOImpl.getCommunityFeeDetailCountAnalysis(reqJson);
        // 对数据进行日期填充，确保时间序列完整
        feeDetailData = fillDate(feeDetailData, startTime, endTime);
        data.put("feeDetailData", feeDetailData);

        // 查询小区报修单订单数
        List<Map> repairData = baseDataStatisticsInnerServiceSMOImpl.getCommunityRepairCountAnalysis(reqJson);
        repairData = fillDate(repairData, startTime, endTime);
        data.put("repairData", repairData);

        // 查询巡检数据数
        List<Map> inspectionData = baseDataStatisticsInnerServiceSMOImpl.getCommunityInspectionAnalysis(reqJson);
        inspectionData = fillDate(inspectionData, startTime, endTime);
        data.put("inspectionData", inspectionData);

        // 查询保养数据数
        List<Map> maintainanceData = baseDataStatisticsInnerServiceSMOImpl.getCommunityMaintainanceAnalysis(reqJson);
        maintainanceData = fillDate(maintainanceData, startTime, endTime);
        data.put("maintainanceData", maintainanceData);

        // 查询采购订单数
        List<Map> itemInData = baseDataStatisticsInnerServiceSMOImpl.getCommunityItemInAnalysis(reqJson);
        itemInData = fillDate(itemInData, startTime, endTime);
        data.put("itemInData", itemInData);

        // 查询领用订单数
        List<Map> itemOutData = baseDataStatisticsInnerServiceSMOImpl.getCommunityItemOutAnalysis(reqJson);
        itemOutData = fillDate(itemOutData, startTime, endTime);
        data.put("itemOutData", itemOutData);

        // 查询车辆进场数
        List<Map> carInData = baseDataStatisticsInnerServiceSMOImpl.getCommunityCarInAnalysis(reqJson);
        carInData = fillDate(carInData, startTime, endTime);
        data.put("carInData", carInData);

        // 查询人员进场数
        List<Map> personInData = baseDataStatisticsInnerServiceSMOImpl.getCommunityPersonInAnalysis(reqJson);
        personInData = fillDate(personInData, startTime, endTime);
        data.put("personInData", personInData);

        // 查询起草合同数
        reqJson.put("storeId", storeId);
        List<Map> contractData = baseDataStatisticsInnerServiceSMOImpl.getCommunityContractAnalysis(reqJson);
        contractData = fillDate(contractData, startTime, endTime);
        data.put("contractData", contractData);
        
        // 设置响应数据
        context.setResponseEntity(ResultVo.createResponseEntity(data));
    }

    /**
     * 填充日期数据，确保指定时间范围内的每一天都有对应的数据记录
     * 如果某天没有数据，则创建一条countValue为0的记录
     *
     * @param datas     原始数据列表，包含日期和对应的统计值
     * @param startTime 开始时间，格式为yyyy-MM-dd
     * @param endTime   结束时间，格式为yyyy-MM-dd
     * @return List<Map> 填充完整后的数据列表，包含指定时间范围内每一天的数据
     */
    private List<Map> fillDate(List<Map> datas, String startTime, String endTime) {
        // 将字符串时间转换为Date对象
        Date sTime = DateUtil.getDateFromStringB(startTime);
        Date eTime = DateUtil.getDateFromStringB(endTime);
        List<Map> tempDatas = new ArrayList<>();
        
        // 遍历指定时间范围内的每一天
        while (sTime.getTime() <= eTime.getTime()) {
            // 检查当前日期是否在原始数据中存在
            Map data = hasInDatas(sTime, datas);
            if (data == null) {
                // 如果不存在，创建一条新的数据记录，countValue设为0
                data = new HashMap();
                data.put("createTime", DateUtil.getFormatTimeStringB(sTime));
                data.put("countValue", "0");
            }

            // 将数据添加到结果列表中
            tempDatas.add(data);

            // 日期加1天，继续处理下一天
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(sTime);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            sTime = calendar.getTime();
        }

        return tempDatas;
    }

    /**
     * 检查指定日期是否在数据列表中存在
     * 根据日期字符串匹配查找对应的数据记录
     *
     * @param sTime 要检查的日期
     * @param datas 数据列表，包含日期和统计值
     * @return Map 如果找到对应日期的数据记录则返回该记录，否则返回null
     */
    private Map hasInDatas(Date sTime, List<Map> datas) {
        // 将日期格式化为字符串用于比较
        String curTime = DateUtil.getFormatTimeStringB(sTime);
        
        // 遍历数据列表，查找匹配日期的记录
        for (Map data : datas) {
            if (curTime.equals(data.get("createTime"))) {
                return data;
            }
        }

        return null;
    }
}