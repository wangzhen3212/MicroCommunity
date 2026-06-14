package com.java110.report.cmd.dataReport;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.report.QueryStatisticsDto;
import com.java110.report.statistics.IInoutStatistics;
import com.java110.report.statistics.IOthersStatistics;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;

/**
 * 数据统计其他数据报表查询命令类
 * 
 * 该类负责处理其他各类业务数据的统计查询，包括场地预约、合同管理、车辆管理、
 * 优惠券和积分等业务模块的数据统计，并返回统一的统计结果格式
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode dataReport.queryOthersDataReport
 */
@Java110Cmd(serviceCode = "dataReport.queryOthersDataReport")
public class QueryOthersDataReportCmd extends Cmd {

    /**
     * 其他统计数据服务接口，用于获取各类业务统计数据
     */
    @Autowired
    private IOthersStatistics othersStatisticsImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数中必须包含小区ID、开始时间和结束时间，并对时间格式进行标准化处理
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     * @throws ParseException 当时间解析失败时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证必填参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
        Assert.hasKeyAndValue(reqJson, "startDate", "未包含开始时间");
        Assert.hasKeyAndValue(reqJson, "endDate", "未包含结束时间");
        
        // 获取开始时间和结束时间
        String startDate = reqJson.getString("startDate");
        String endDate = reqJson.getString("endDate");
        
        // 标准化开始时间格式，如果没有包含时间部分则添加默认时间
        if (!startDate.contains(":")) {
            startDate += " 00:00:00";
            reqJson.put("startDate", startDate);
        }
        
        // 标准化结束时间格式，如果没有包含时间部分则添加默认时间
        if (!endDate.contains(":")) {
            endDate += " 23:59:59";
            reqJson.put("endDate", endDate);
        }
    }

    /**
     * 执行其他数据报表查询命令
     * 
     * 查询各类业务统计数据，包括场地预约、合同管理、车辆管理、优惠券和积分等，
     * 并将统计结果封装成统一的JSON格式返回
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行失败时抛出命令异常
     * @throws ParseException 当时间解析失败时抛出解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从请求头中获取店铺ID
        String storeId = context.getReqHeaders().get("store-id");
        
        // 构建查询参数对象
        QueryStatisticsDto queryStatisticsDto = new QueryStatisticsDto();
        queryStatisticsDto.setStartDate(reqJson.getString("startDate"));
        queryStatisticsDto.setEndDate(reqJson.getString("endDate"));
        queryStatisticsDto.setCommunityId(reqJson.getString("communityId"));
        queryStatisticsDto.setStoreId(storeId);
        
        // 初始化数据数组，用于存储所有统计结果
        JSONArray datas = new JSONArray();
        JSONObject data = null;
        
        // 查询场地预约数统计
        long venueReservationCount = othersStatisticsImpl.venueReservationCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","场地预约数");
        data.put("value", venueReservationCount);
        datas.add(data);

        // 查询合同数统计
        long contractCount = othersStatisticsImpl.contractCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","合同数");
        data.put("value", contractCount);
        datas.add(data);

        // 查询合同资产变更统计
        long contractChangeCount = othersStatisticsImpl.contractChangeCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","合同资产变更");
        data.put("value", contractChangeCount);
        datas.add(data);

        // 查询租期变更统计
        long leaseChangeCount = othersStatisticsImpl.leaseChangeCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","租期变更");
        data.put("value", leaseChangeCount);
        datas.add(data);

        // 查询主体变更统计
        long mainChange = othersStatisticsImpl.mainChange(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","主体变更");
        data.put("value", mainChange);
        datas.add(data);

        // 查询到期合同统计
        long expirationContract = othersStatisticsImpl.expirationContract(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","到期合同");
        data.put("value", expirationContract);
        datas.add(data);

        // 查询车辆数统计
        long carCount = othersStatisticsImpl.carCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","车辆数");
        data.put("value", carCount);
        datas.add(data);

        // 查询车位申请统计
        long carApplyCount = othersStatisticsImpl.carApplyCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","车位申请");
        data.put("value", carApplyCount);
        datas.add(data);

        // 查询停车券购买统计（返回值为double类型，可能涉及金额）
        double buyParkingCouponCount = othersStatisticsImpl.buyParkingCouponCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","停车券购买");
        data.put("value", buyParkingCouponCount);
        datas.add(data);

        // 查询停车券核销统计
        long writeOffParkingCouponCount = othersStatisticsImpl.writeOffParkingCouponCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","停车券核销");
        data.put("value", writeOffParkingCouponCount);
        datas.add(data);

        // 查询赠送优惠券统计（返回值为double类型，可能涉及金额或数量）
        double sendCouponCount = othersStatisticsImpl.sendCouponCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","赠送优惠券");
        data.put("value", sendCouponCount);
        datas.add(data);

        // 查询使用优惠券统计
        long writeOffCouponCount = othersStatisticsImpl.writeOffCouponCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","使用优惠券");
        data.put("value", writeOffCouponCount);
        datas.add(data);

        // 查询赠送积分统计（返回值为double类型，可能涉及积分数量）
        double sendIntegralCount = othersStatisticsImpl.sendIntegralCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","退房数量");
        data.put("value", sendIntegralCount);
        datas.add(data);

        // 查询使用积分统计（返回值为double类型，可能涉及积分数量）
        double writeOffIntegralCount = othersStatisticsImpl.writeOffIntegralCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","使用积分");
        data.put("value", writeOffIntegralCount);
        datas.add(data);

        // 将统计结果封装成统一响应格式并设置到上下文中
        context.setResponseEntity(ResultVo.createResponseEntity(datas));
    }
}