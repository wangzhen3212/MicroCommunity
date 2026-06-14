/**
 * 社区费用汇总查询命令类
 * 
 * 功能：处理管理员获取社区费用汇总数据的请求，包括历史欠费、当月欠费、应收费用、实收费用等统计信息
 * 服务码：admin.getCommunityFeeSummary
 * 
 * 该类继承自Cmd基类，通过注入费用统计和基础数据统计服务，实现对社区各项费用数据的汇总查询
 * 主要统计指标包括：历史欠费、当月欠费、应收费用、实收费用、房屋数量等
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.report.cmd.admin;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.report.QueryStatisticsDto;
import com.java110.dto.store.StoreDto;
import com.java110.intf.store.IStoreInnerServiceSMO;
import com.java110.report.statistics.IBaseDataStatistics;
import com.java110.report.statistics.IFeeStatistics;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;

/**
 * 社区费用汇总查询命令类
 * 服务码：admin.getCommunityFeeSummary
 */
@Java110Cmd(serviceCode = "admin.getCommunityFeeSummary")
public class GetCommunityFeeSummaryCmd extends Cmd {

    /** 店铺内部服务接口，用于获取店铺相关信息 */
    @Autowired
    private IStoreInnerServiceSMO storeInnerServiceSMOImpl;

    /** 费用统计服务接口，用于统计各类费用数据 */
    @Autowired
    private IFeeStatistics feeStatisticsImpl;

    /** 基础数据统计服务接口，用于统计房屋等基础数据 */
    @Autowired
    private IBaseDataStatistics baseDataStatisticsImpl;

    /**
     * 请求参数验证方法
     * 
     * 功能：验证请求参数的有效性，确保包含必要的参数并进行管理员权限验证
     * 主要验证内容：
     * 1. 验证请求中必须包含开始日期和结束日期参数
     * 2. 验证当前用户是否具有管理员权限
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置上下文信息
     * @param reqJson 请求参数的JSON对象，包含查询条件参数
     * @throws CmdException 当命令执行出现异常时抛出，如参数缺失或权限不足
     * @throws ParseException 当参数解析出现异常时抛出，如日期格式不正确
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证必须包含开始日期参数，确保查询有时间范围
        Assert.hasKeyAndValue(reqJson, "startDate", "未包含开始日期");
        // 验证必须包含结束日期参数，确保查询有时间范围
        Assert.hasKeyAndValue(reqJson, "endDate", "未包含结束日期");

        // 验证管理员权限，确保只有管理员可以访问此功能
        super.validateAdmin(context);
    }

    /**
     * 命令执行方法
     * 
     * 功能：执行社区费用汇总查询，统计各类费用数据和房屋数据，并返回统计结果
     * 主要执行步骤：
     * 1. 构建查询参数对象
     * 2. 调用各统计服务获取费用和房屋数据
     * 3. 组装返回数据格式
     * 4. 设置响应实体
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置上下文信息
     * @param reqJson 请求参数的JSON对象，包含查询条件参数
     * @throws CmdException 当命令执行出现异常时抛出，如服务调用失败
     * @throws ParseException 当参数解析出现异常时抛出，如日期格式处理异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 创建查询统计数据传输对象，用于封装查询条件
        QueryStatisticsDto queryStatisticsDto = new QueryStatisticsDto();
        
        // 设置查询参数：社区ID、开始日期、结束日期
        queryStatisticsDto.setCommunityId(reqJson.getString("communityId"));
        queryStatisticsDto.setStartDate(reqJson.getString("startDate"));
        queryStatisticsDto.setEndDate(reqJson.getString("endDate"));
        
        // 处理结束日期格式，如果没有时间部分则添加23:59:59，确保包含当天的完整时间
        if(reqJson.containsKey("endDate") && !reqJson.getString("endDate").contains(":")) {
            queryStatisticsDto.setEndDate(reqJson.getString("endDate") + " 23:59:59");
        }
        
        // 设置其他可选查询参数，用于更精确的数据筛选
        queryStatisticsDto.setConfigId(reqJson.getString("configId"));    // 配置ID
        queryStatisticsDto.setFloorId(reqJson.getString("floorId"));      // 楼层ID
        queryStatisticsDto.setObjName(reqJson.getString("objName"));      // 对象名称
        queryStatisticsDto.setFeeTypeCd(reqJson.getString("feeTypeCd"));  // 费用类型编码
        queryStatisticsDto.setOwnerName(reqJson.getString("ownerName"));  // 业主姓名
        queryStatisticsDto.setLink(reqJson.getString("link"));            // 关联信息

        // 处理配置ID数组参数，将逗号分隔的字符串转换为数组
        if(reqJson.containsKey("configIds")){
            queryStatisticsDto.setConfigIds(reqJson.getString("configIds").split(","));
        }

        // 查询历史欠费金额：统计截止到查询开始日期之前的欠费总额
        double hisOweFee = feeStatisticsImpl.getHisMonthOweFee(queryStatisticsDto);

        // 查询当月欠费金额：统计查询时间范围内的当月欠费总额
        double curOweFee = feeStatisticsImpl.getCurMonthOweFee(queryStatisticsDto);

        // 查询当月应收金额：统计查询时间范围内应该收取的费用总额
        double curReceivableFee = feeStatisticsImpl.getCurReceivableFee(queryStatisticsDto);

        // 查询历史欠费追回金额：统计查询时间范围内收回的历史欠费金额
        double hisReceivedFee = feeStatisticsImpl.getHisReceivedFee(queryStatisticsDto);

        // 查询预交费用金额：统计查询时间范围内的预交费用总额
        double preReceivedFee = feeStatisticsImpl.getPreReceivedFee(queryStatisticsDto);

        // 查询实收金额：统计查询时间范围内实际收到的费用总额
        double receivedFee = feeStatisticsImpl.getReceivedFee(queryStatisticsDto);

        // 查询房屋总数：统计社区内的房屋总数量
        long roomCount = baseDataStatisticsImpl.getRoomCount(queryStatisticsDto);

        // 查询收费房屋数量：统计需要收费的房屋数量
        long feeRoomCount = feeStatisticsImpl.getFeeRoomCount(queryStatisticsDto);

        // 查询欠费户数：统计存在欠费的房屋户数
        int oweRoomCount = feeStatisticsImpl.getOweRoomCount(queryStatisticsDto);

        // 构建返回数据对象，将所有统计结果封装到JSON对象中
        JSONObject data = new JSONObject();
        data.put("hisOweFee", hisOweFee);           // 历史欠费金额
        data.put("curOweFee", curOweFee);           // 当月欠费金额
        data.put("hisReceivedFee", hisReceivedFee); // 历史欠费追回金额
        data.put("preReceivedFee", preReceivedFee); // 预交费用金额
        data.put("receivedFee", receivedFee);       // 实收费用金额
        data.put("roomCount", roomCount);           // 房屋总数
        data.put("feeRoomCount", feeRoomCount);     // 收费房屋数量
        data.put("oweRoomCount", oweRoomCount);     // 欠费户数
        data.put("curReceivableFee", curReceivableFee); // 当月应收金额

        // 将数据包装成数组格式返回，符合前端数据格式要求
        JSONArray datas = new JSONArray();
        datas.add(data);
        
        // 设置响应实体，将统计结果返回给前端
        context.setResponseEntity(ResultVo.createResponseEntity(datas));
    }
}