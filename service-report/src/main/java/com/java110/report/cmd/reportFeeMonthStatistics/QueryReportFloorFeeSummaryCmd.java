package com.java110.report.cmd.reportFeeMonthStatistics;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.Java110ThreadPoolFactory;
import com.java110.dto.floor.FloorDto;
import com.java110.dto.report.QueryStatisticsDto;
import com.java110.dto.report.ReportFloorFeeStatisticsDto;
import com.java110.intf.community.IFloorInnerServiceSMO;
import com.java110.report.statistics.IFeeStatistics;
import com.java110.report.statistics.IFloorFeeStatistics;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 楼栋费用统计命令类
 * 用于查询楼栋费用月度统计汇总信息，包括收费率、欠费情况等统计数据的计算和返回
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode reportFeeMonthStatistics.queryReportFloorFeeSummary
 */
@Java110Cmd(serviceCode = "reportFeeMonthStatistics.queryReportFloorFeeSummary")
public class QueryReportFloorFeeSummaryCmd extends Cmd {

    @Autowired
    private IFloorFeeStatistics floorFeeStatisticsImpl;

    @Autowired
    private IFeeStatistics feeStatisticsImpl;

    @Autowired
    private IFloorInnerServiceSMO floorInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数中是否包含必要的字段
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证开始日期参数
        Assert.hasKeyAndValue(reqJson, "startDate", "未包含开始日期");
        // 验证结束日期参数
        Assert.hasKeyAndValue(reqJson, "endDate", "未包含结束日期");
        // 验证小区ID参数
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区信息");
    }

    /**
     * 命令执行方法
     * 处理楼栋费用统计查询请求，计算各项费用统计指标并返回结果
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
        
        // 处理结束日期格式，如果没有时间部分则添加23:59:59
        if (reqJson.containsKey("endDate") && !reqJson.getString("endDate").contains(":")) {
            queryStatisticsDto.setEndDate(reqJson.getString("endDate") + " 23:59:59");
        }
        queryStatisticsDto.setFeeTypeCd(reqJson.getString("feeTypeCd"));
        
        // 处理配置ID参数，支持多个ID以逗号分隔
        if (reqJson.containsKey("configIds")) {
            queryStatisticsDto.setConfigIds(reqJson.getString("configIds").split(","));
        }

        // 查询楼栋信息
        FloorDto floorDto = new FloorDto();
        floorDto.setCommunityId(reqJson.getString("communityId"));
        // 设置分页参数
        if(reqJson.containsKey("page")) {
            floorDto.setPage(reqJson.getIntValue("page"));
            floorDto.setRow(reqJson.getIntValue("row"));
        }
        List<FloorDto> floorDtos = floorInnerServiceSMOImpl.queryFloors(floorDto);

        // 如果楼栋列表为空，直接返回空结果
        if (ListUtil.isNull(floorDtos)) {
            context.setResponseEntity(ResultVo.createResponseEntity(new ArrayList<>()));
            return;
        }

        List<Map> datas = new ArrayList<>();
        Java110ThreadPoolFactory java110ThreadPoolFactory = null;
        try {
            // 创建线程池处理并发查询
            java110ThreadPoolFactory = Java110ThreadPoolFactory.getInstance().createThreadPool(5);
            for (FloorDto floorDto1 : floorDtos) {
                queryStatisticsDto.setFloorId(floorDto1.getFloorId());
                // 复制查询参数对象，避免线程间数据竞争
                QueryStatisticsDto tmpQueryStatisticsDto = BeanConvertUtil.covertBean(queryStatisticsDto,QueryStatisticsDto.class);
                // 提交线程任务处理每个楼栋的费用统计
                java110ThreadPoolFactory.submit(() -> {
                    // 查询楼栋费用汇总数据
                    List<Map> floorDatas = feeStatisticsImpl.getFloorFeeSummary(tmpQueryStatisticsDto);
                    if (!ListUtil.isNull(floorDatas)) {
                        datas.add(floorDatas.get(0));
                    }
                    return datas;
                });
            }
            // 等待所有线程任务完成
            java110ThreadPoolFactory.get();
        } finally {
            // 确保线程池被正确关闭
            if (java110ThreadPoolFactory != null) {
                java110ThreadPoolFactory.stop();
            }
        }

        // 如果数据为空，直接返回空结果
        if (ListUtil.isNull(datas)) {
            context.setResponseEntity(ResultVo.createResponseEntity(datas));
            return;
        }
        
        // 定义计算变量
        BigDecimal feeRoomCountDec = null;
        BigDecimal oweRoomCountDec = null;
        BigDecimal feeRoomRate = null;
        BigDecimal curReceivedFee = null;
        BigDecimal curReceivableFee = null;
        
        // 遍历每个楼栋的数据，计算统计指标
        for (Map data : datas) {
            // 计算户收费率： (收费房屋数 - 欠费房屋数) / 收费房屋数 * 100
            if (Double.parseDouble(data.get("feeRoomCount").toString()) > 0) {
                feeRoomCountDec = new BigDecimal(Double.parseDouble(data.get("feeRoomCount").toString()));
                oweRoomCountDec = new BigDecimal(Double.parseDouble(data.get("oweRoomCount").toString()));
                // 计算收费率并格式化为百分比，保留2位小数
                feeRoomRate = feeRoomCountDec.subtract(oweRoomCountDec).divide(feeRoomCountDec, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(100)).setScale(2, BigDecimal.ROUND_HALF_UP);
                data.put("feeRoomRate", feeRoomRate.doubleValue());
            } else {
                // 如果没有收费房屋，收费率为0
                data.put("feeRoomRate", 0.0);
            }

            // 计算费用收费率：当期实收金额 / 当期应收金额 * 100
            curReceivedFee = new BigDecimal(Double.parseDouble(data.get("curReceivedFee").toString()));
            curReceivableFee = new BigDecimal(Double.parseDouble(data.get("curReceivableFee").toString()));

            if (curReceivableFee.doubleValue() > 0) {
                // 计算费用收费率并格式化为百分比，保留2位小数
                feeRoomRate = curReceivedFee.divide(curReceivableFee, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(100)).setScale(2, BigDecimal.ROUND_HALF_UP);
                data.put("feeRate", feeRoomRate.doubleValue());
            } else {
                // 如果没有应收金额，收费率为0
                data.put("feeRate", 0.0);
            }
        }

        // 设置响应结果
        context.setResponseEntity(ResultVo.createResponseEntity(datas));
    }

    /**
     * 数据计算方法
     * 将各项费用统计数据合并到结果数据集中
     *
     * @param datas 结果数据集
     * @param oweRoomCounts 欠费房屋数列表
     * @param feeRoomCounts 收费房屋数列表
     * @param receivedFees 实收金额列表
     * @param preReceivedFees 预收金额列表
     * @param hisOweFees 历史欠费金额列表
     * @param curReceivableFees 当期应收金额列表
     * @param curReceivedFees 当期实收金额列表
     * @param hisReceivedFees 欠费追回金额列表
     */
    private void computeData(List<Map> datas, List<ReportFloorFeeStatisticsDto> oweRoomCounts,
                             List<ReportFloorFeeStatisticsDto> feeRoomCounts,
                             List<ReportFloorFeeStatisticsDto> receivedFees,
                             List<ReportFloorFeeStatisticsDto> preReceivedFees,
                             List<ReportFloorFeeStatisticsDto> hisOweFees,
                             List<ReportFloorFeeStatisticsDto> curReceivableFees,
                             List<ReportFloorFeeStatisticsDto> curReceivedFees,
                             List<ReportFloorFeeStatisticsDto> hisReceivedFees) {
        // 处理欠费房屋数数据
        for (Map data : datas) {
            data.put("oweRoomCount", "0");
            for (ReportFloorFeeStatisticsDto reportFloorFeeStatisticsDto : oweRoomCounts) {
                // 按楼栋ID匹配数据
                if (reportFloorFeeStatisticsDto.getFloorId().equals(data.get("floorId"))) {
                    data.put("oweRoomCount", reportFloorFeeStatisticsDto.getOweRoomCount());
                }
            }
        }

        // 处理收费房屋数数据
        for (Map data : datas) {
            data.put("feeRoomCount", "0");
            for (ReportFloorFeeStatisticsDto reportFloorFeeStatisticsDto : feeRoomCounts) {
                // 按楼栋ID匹配数据
                if (reportFloorFeeStatisticsDto.getFloorId().equals(data.get("floorId"))) {
                    data.put("feeRoomCount", reportFloorFeeStatisticsDto.getFeeRoomCount());
                }
            }
        }

        // 处理实收金额数据
        for (Map data : datas) {
            data.put("receivedFee", "0");
            for (ReportFloorFeeStatisticsDto reportFloorFeeStatisticsDto : receivedFees) {
                // 按楼栋ID匹配数据
                if (reportFloorFeeStatisticsDto.getFloorId().equals(data.get("floorId"))) {
                    data.put("receivedFee", reportFloorFeeStatisticsDto.getReceivedFee());
                }
            }
        }

        // 处理预收金额数据
        for (Map data : datas) {
            data.put("preReceivedFee", "0");
            for (ReportFloorFeeStatisticsDto reportFloorFeeStatisticsDto : preReceivedFees) {
                // 按楼栋ID匹配数据
                if (reportFloorFeeStatisticsDto.getFloorId().equals(data.get("floorId"))) {
                    data.put("preReceivedFee", reportFloorFeeStatisticsDto.getPreReceivedFee());
                }
            }
        }

        // 处理历史欠费金额数据
        for (Map data : datas) {
            data.put("hisOweFee", "0");
            for (ReportFloorFeeStatisticsDto reportFloorFeeStatisticsDto : hisOweFees) {
                // 按楼栋ID匹配数据
                if (reportFloorFeeStatisticsDto.getFloorId().equals(data.get("floorId"))) {
                    data.put("hisOweFee", reportFloorFeeStatisticsDto.getHisOweFee());
                }
            }
        }

        // 处理当期应收金额数据
        for (Map data : datas) {
            data.put("curReceivableFee", "0");
            for (ReportFloorFeeStatisticsDto reportFloorFeeStatisticsDto : curReceivableFees) {
                // 按楼栋ID匹配数据
                if (reportFloorFeeStatisticsDto.getFloorId().equals(data.get("floorId"))) {
                    data.put("curReceivableFee", reportFloorFeeStatisticsDto.getCurReceivableFee());
                }
            }
        }

        // 处理当期实收金额数据
        for (Map data : datas) {
            data.put("curReceivedFee", "0");
            for (ReportFloorFeeStatisticsDto reportFloorFeeStatisticsDto : curReceivedFees) {
                // 按楼栋ID匹配数据
                if (reportFloorFeeStatisticsDto.getFloorId().equals(data.get("floorId"))) {
                    data.put("curReceivedFee", reportFloorFeeStatisticsDto.getCurReceivedFee());
                }
            }
        }
        
        // 处理欠费追回金额数据
        for (Map data : datas) {
            data.put("hisReceivedFee", "0");
            for (ReportFloorFeeStatisticsDto reportFloorFeeStatisticsDto : hisReceivedFees) {
                // 按楼栋ID匹配数据
                if (reportFloorFeeStatisticsDto.getFloorId().equals(data.get("floorId"))) {
                    data.put("hisReceivedFee", reportFloorFeeStatisticsDto.getHisReceivedFee());
                }
            }
        }
    }
}