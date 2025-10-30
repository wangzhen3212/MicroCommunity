package com.java110.report.cmd.dataReport;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.floor.FloorDto;
import com.java110.dto.report.QueryStatisticsDto;
import com.java110.intf.community.IFloorV1InnerServiceSMO;
import com.java110.report.statistics.IBaseDataStatistics;
import com.java110.report.statistics.IFeeStatistics;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.Calendar;
import java.util.List;

/**
 * 数据报表收费统计查询命令类
 * 功能：根据楼栋查询收费情况分析数据，包括欠费统计、收费统计、历史欠费清缴等
 * 服务代码：dataReport.queryDataReportFeeStatistics
 */
@Java110Cmd(serviceCode = "dataReport.queryDataReportFeeStatistics")
public class QueryDataReportFeeStatisticsCmd extends Cmd {

    @Autowired
    private IFeeStatistics feeStatisticsImpl;

    @Autowired
    private IFloorV1InnerServiceSMO floorV1InnerServiceSMOImpl;

    @Autowired
    private IBaseDataStatistics baseDataStatisticsImpl;

    /**
     * 验证请求参数
     * @param event 命令事件
     * @param context 数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证必填参数
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
        Assert.hasKeyAndValue(reqJson, "floorIds", "未包含楼栋信息");

        // 处理日期格式，如果没有时间部分则添加默认时间
        String startDate = reqJson.getString("startDate");
        String endDate = reqJson.getString("endDate");
        if (!StringUtil.isEmpty(startDate) && !startDate.contains(":")) {
            startDate += " 00:00:00";
            reqJson.put("startDate", startDate);
        }
        if (!StringUtil.isEmpty(endDate) && !endDate.contains(":")) {
            endDate += " 23:59:59";
            reqJson.put("endDate", endDate);
        }
    }

    /**
     * 执行收费统计查询命令
     * @param event 命令事件
     * @param context 数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 解析楼栋ID数组
        String[] floorIds = reqJson.getString("floorIds").split(",");
        // 如果没有包含楼栋信息，直接返回空
        if (floorIds == null || floorIds.length < 1) {
            return;
        }
        
        JSONArray data = new JSONArray();
        // 根据楼栋ID循环查询每个楼栋的收费统计数据
        for (String floorId : floorIds) {
            doGetData(floorId, data, reqJson);
        }
        
        // 设置响应数据
        context.setResponseEntity(ResultVo.createResponseEntity(data));
    }

    /**
     * 获取单个楼栋的收费统计数据
     * @param floorId 楼栋ID
     * @param datas 数据集合，用于存储查询结果
     * @param reqJson 请求参数JSON对象
     */
    private void doGetData(String floorId, JSONArray datas, JSONObject reqJson) {
        JSONObject data = new JSONObject();
        
        // 构建查询条件对象
        QueryStatisticsDto queryStatisticsDto = new QueryStatisticsDto();
        queryStatisticsDto.setCommunityId(reqJson.getString("communityId"));
        queryStatisticsDto.setFloorId(floorId);
        queryStatisticsDto.setStartDate(reqJson.getString("startDate"));
        queryStatisticsDto.setEndDate(reqJson.getString("endDate"));
        queryStatisticsDto.setFeeTypeCd(reqJson.getString("feeTypeCd"));

        // 计算月份相关日期
        // 获取查询开始日期所在月份的第一天
        String monthFastDate = DateUtil.getFormatTimeStringB(DateUtil.getFirstDate(reqJson.getString("startDate")));
        // 获取查询开始日期所在月份的下个月第一天
        String monthLastDate = DateUtil.getFormatTimeStringB(DateUtil.getNextMonthFirstDate(reqJson.getString("startDate")));
        
        String startDate = reqJson.getString("startDate");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(DateUtil.getDateFromStringB(startDate));
        calendar.add(Calendar.DAY_OF_MONTH, 1); // 日期加1天，用于本日查询
        String endDate = DateUtil.getFormatTimeStringB(calendar.getTime());

        // 查询楼栋信息
        FloorDto floorDto = new FloorDto();
        floorDto.setFloorId(floorId);
        floorDto.setCommunityId(reqJson.getString("communityId"));
        List<FloorDto> floorDtos = floorV1InnerServiceSMOImpl.queryFloors(floorDto);
        Assert.listOnlyOne(floorDtos, "楼栋不存在");
        data.put("floorNum", floorDtos.get(0).getFloorNum()); // 楼栋编号

        // 查询户数
        long roomCount = baseDataStatisticsImpl.getRoomCount(queryStatisticsDto);
        data.put("roomCount", roomCount);

        // 查询历史欠费（设置查询月1日，避免历史和总欠费相同）
        queryStatisticsDto.setStartDate(monthFastDate);
        double hisMonthOweFee = feeStatisticsImpl.getHisMonthOweFee(queryStatisticsDto);
        data.put("hisMonthOweFee", hisMonthOweFee);

        // 查询总欠费
        queryStatisticsDto.setEndDate(monthLastDate);
        double oweFee = feeStatisticsImpl.getOweFee(queryStatisticsDto);
        data.put("oweFee", oweFee);

        // 查询本日已交户数
        queryStatisticsDto.setStartDate(startDate);
        queryStatisticsDto.setEndDate(endDate);
        double todayReceivedRoomCount = feeStatisticsImpl.getReceivedRoomCount(queryStatisticsDto);
        data.put("todayReceivedRoomCount", todayReceivedRoomCount);

        // 查询本日已交金额
        double todayReceivedRoomAmount = feeStatisticsImpl.getReceivedRoomAmount(queryStatisticsDto);
        data.put("todayReceivedRoomAmount", todayReceivedRoomAmount);

        // 查询历史欠费清缴户数
        queryStatisticsDto.setStartDate(startDate);
        queryStatisticsDto.setEndDate(endDate);
        queryStatisticsDto.setHisDate(monthFastDate);
        double hisOweReceivedRoomCount = feeStatisticsImpl.getHisOweReceivedRoomCount(queryStatisticsDto);
        data.put("hisOweReceivedRoomCount", hisOweReceivedRoomCount);
        
        // 查询历史欠费清缴金额
        double hisOweReceivedRoomAmount = feeStatisticsImpl.getHisOweReceivedRoomAmount(queryStatisticsDto);
        data.put("hisOweReceivedRoomAmount", hisOweReceivedRoomAmount);

        // 查询本月已收户数
        queryStatisticsDto.setStartDate(monthFastDate);
        queryStatisticsDto.setEndDate(monthLastDate);
        double monthReceivedRoomCount = feeStatisticsImpl.getMonthReceivedDetailCount(queryStatisticsDto);
        data.put("monthReceivedRoomCount", monthReceivedRoomCount);

        // 查询收费户数
        long feeRoomCount = feeStatisticsImpl.getFeeRoomCount(queryStatisticsDto);
        data.put("feeRoomCount", feeRoomCount);

        // 计算欠费户数
        int oweRoomCount = feeStatisticsImpl.getOweRoomCount(queryStatisticsDto);
        data.put("oweRoomCount", oweRoomCount);

        // 查询本月已收金额
        double monthReceivedRoomAmount = feeStatisticsImpl.getMonthReceivedDetailAmount(queryStatisticsDto);
        data.put("monthReceivedRoomAmount", monthReceivedRoomAmount);
        
        // 查询剩余未收金额
        double curMonthOweFee = feeStatisticsImpl.getCurMonthOweFee(queryStatisticsDto);
        data.put("curMonthOweFee", curMonthOweFee);

        // 查询当月应收金额
        queryStatisticsDto.setStartDate(monthFastDate);
        queryStatisticsDto.setEndDate(monthLastDate);
        double curReceivableFee = feeStatisticsImpl.getCurReceivableFee(queryStatisticsDto);
        data.put("curReceivableFee", curReceivableFee);

        // 注释掉的代码：欠费追回和预交费用查询
//        // 查询欠费追回
//        queryStatisticsDto.setStartDate(monthFastDate);
//        queryStatisticsDto.setEndDate(monthLastDate);
//        double hisReceivedFee = feeStatisticsImpl.getHisReceivedFee(queryStatisticsDto);
//        data.put("hisReceivedFee", hisReceivedFee);
//
//        // 查询预交费用
//        queryStatisticsDto.setStartDate(monthFastDate);
//        queryStatisticsDto.setEndDate(monthLastDate);
//        double preReceivedFee = feeStatisticsImpl.getPreReceivedFee(queryStatisticsDto);
//        data.put("preReceivedFee", preReceivedFee);

        // 将当前楼栋数据添加到结果集
        datas.add(data);
    }
}