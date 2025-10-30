package com.java110.report.cmd.reportFeeMonthStatistics;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.Java110ThreadPoolFactory;
import com.java110.dto.report.QueryStatisticsDto;
import com.java110.report.statistics.IBaseDataStatistics;
import com.java110.report.statistics.IFeeStatistics;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.MoneyUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 查询费用汇总表命令类
 * 
 * <p>该类用于处理费用月统计报表的查询请求，通过多线程并发查询各项费用统计数据，
 * 包括历史欠费、当月欠费、应收费用、实收费用等关键财务指标</p>
 * 
 * <p>主要功能：</p>
 * <ul>
 *   <li>验证查询参数的有效性</li>
 *   <li>使用线程池并发查询各项费用统计指标</li>
 *   <li>汇总查询结果并返回给前端</li>
 * </ul>
 * 
 * @author wuxw
 * @version 1.0
 * @serviceCode reportFeeMonthStatistics.queryReportFeeSummary
 */
@Java110Cmd(serviceCode = "reportFeeMonthStatistics.queryReportFeeSummary")
public class QueryReportFeeSummaryCmd extends Cmd {

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
     * 校验查询参数的有效性
     * 
     * <p>验证请求参数中必须包含开始日期、结束日期和小区信息等必要字段</p>
     *
     * @param event   命令事件对象，包含请求上下文信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 调用父类方法验证分页信息
        super.validatePageInfo(reqJson);
        // 验证必须包含开始日期
        Assert.hasKeyAndValue(reqJson, "startDate", "未包含开始日期");
        // 验证必须包含结束日期
        Assert.hasKeyAndValue(reqJson, "endDate", "未包含结束日期");
        // 验证必须包含小区信息
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区信息");
    }

    /**
     * 执行费用汇总查询命令
     * 
     * <p>使用线程池并发查询各项费用统计指标，包括历史欠费、当月欠费、应收费用、
     * 实收费用、房屋数量等，并将结果汇总返回</p>
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 创建查询统计数据传输对象
        QueryStatisticsDto queryStatisticsDto = new QueryStatisticsDto();
        // 设置小区ID
        queryStatisticsDto.setCommunityId(reqJson.getString("communityId"));
        
        // 处理开始日期格式，如果日期不包含时间部分，则添加默认时间
        if (reqJson.containsKey("startDate") && !reqJson.getString("startDate").contains(":")) {
            queryStatisticsDto.setStartDate(reqJson.getString("startDate") + " 00:00:00");
        }
        // 处理结束日期格式，如果日期不包含时间部分，则添加默认时间
        if (reqJson.containsKey("endDate") && !reqJson.getString("endDate").contains(":")) {
            queryStatisticsDto.setEndDate(reqJson.getString("endDate") + " 23:59:59");
        }
        
        // 设置其他查询条件参数
        queryStatisticsDto.setConfigId(reqJson.getString("configId"));
        queryStatisticsDto.setFloorId(reqJson.getString("floorId"));
        queryStatisticsDto.setObjName(reqJson.getString("objName"));
        queryStatisticsDto.setFeeTypeCd(reqJson.getString("feeTypeCd"));
        queryStatisticsDto.setOwnerName(reqJson.getString("ownerName"));
        queryStatisticsDto.setLink(reqJson.getString("link"));
        
        // 处理费用配置ID数组
        if (reqJson.containsKey("configIds")) {
            queryStatisticsDto.setConfigIds(reqJson.getString("configIds").split(","));
        }

        // 创建结果数据对象
        JSONObject data = new JSONObject();

        // 创建线程池工厂实例
        Java110ThreadPoolFactory java110ThreadPoolFactory = null;
        try {
            // 创建包含5个线程的线程池
            java110ThreadPoolFactory = Java110ThreadPoolFactory.getInstance().createThreadPool(5);
            
            // 提交历史欠费查询任务
            java110ThreadPoolFactory.submit(() -> {
                // 查询历史月份欠费金额
                double hisOweFee = feeStatisticsImpl.getHisMonthOweFee(queryStatisticsDto);
                // 将金额格式化为两位小数并存入结果数据
                data.put("hisOweFee", MoneyUtil.computePriceScale(hisOweFee));
                return hisOweFee;
            });

            // 提交当月欠费查询任务
            java110ThreadPoolFactory.submit(() -> {
                // 查询当前月份欠费金额
                double curOweFee = feeStatisticsImpl.getCurMonthOweFee(queryStatisticsDto);
                // 将金额格式化为两位小数并存入结果数据
                data.put("curOweFee", MoneyUtil.computePriceScale(curOweFee));
                return curOweFee;
            });

            // 提交当月应收费用查询任务
            java110ThreadPoolFactory.submit(() -> {
                // 查询当前月份应收费用金额
                double curReceivableFee = feeStatisticsImpl.getCurReceivableFee(queryStatisticsDto);
                // 将金额格式化为两位小数并存入结果数据
                data.put("curReceivableFee", MoneyUtil.computePriceScale(curReceivableFee));
                return curReceivableFee;
            });

            // 提交欠费追回查询任务
            java110ThreadPoolFactory.submit(() -> {
                // 查询历史欠费追回金额
                double hisReceivedFee = feeStatisticsImpl.getHisReceivedFee(queryStatisticsDto);
                // 将金额格式化为两位小数并存入结果数据
                data.put("hisReceivedFee", MoneyUtil.computePriceScale(hisReceivedFee));
                return hisReceivedFee;
            });

            // 提交预交费用查询任务
            java110ThreadPoolFactory.submit(() -> {
                // 查询预交费用金额
                double preReceivedFee = feeStatisticsImpl.getPreReceivedFee(queryStatisticsDto);
                // 将金额格式化为两位小数并存入结果数据
                data.put("preReceivedFee", MoneyUtil.computePriceScale(preReceivedFee));
                return preReceivedFee;
            });

            // 提交实收费用查询任务
            java110ThreadPoolFactory.submit(() -> {
                // 查询实际收到费用金额
                double receivedFee = feeStatisticsImpl.getReceivedFee(queryStatisticsDto);
                // 将金额格式化为两位小数并存入结果数据
                data.put("receivedFee", MoneyUtil.computePriceScale(receivedFee));
                return receivedFee;
            });

            // 提交房屋总数查询任务
            java110ThreadPoolFactory.submit(() -> {
                // 查询符合条件的房屋总数
                long roomCount = baseDataStatisticsImpl.getRoomCount(queryStatisticsDto);
                // 将房屋数量存入结果数据
                data.put("roomCount", roomCount);
                return roomCount;
            });
            
            // 提交收费房屋数查询任务
            java110ThreadPoolFactory.submit(() -> {
                // 查询需要收费的房屋数量
                long feeRoomCount = feeStatisticsImpl.getFeeRoomCount(queryStatisticsDto);
                // 将收费房屋数量存入结果数据
                data.put("feeRoomCount", feeRoomCount);
                return feeRoomCount;
            });
            
            // 提交欠费户数查询任务
            java110ThreadPoolFactory.submit(() -> {
                // 查询欠费房屋数量
                int oweRoomCount = feeStatisticsImpl.getOweRoomCount(queryStatisticsDto);
                // 将欠费户数存入结果数据
                data.put("oweRoomCount", oweRoomCount);
                return oweRoomCount;
            });

            // 等待所有线程任务执行完成
            java110ThreadPoolFactory.get();
        } finally {
            // 确保线程池被正确关闭
            if (java110ThreadPoolFactory != null) {
                java110ThreadPoolFactory.stop();
            }
        }
        
        // 将结果数据封装为数组格式
        JSONArray datas = new JSONArray();
        datas.add(data);
        // 设置响应实体返回给前端
        context.setResponseEntity(ResultVo.createResponseEntity(datas));
    }
}