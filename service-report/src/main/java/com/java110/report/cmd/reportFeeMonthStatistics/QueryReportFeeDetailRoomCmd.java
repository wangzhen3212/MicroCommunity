package com.java110.report.cmd.reportFeeMonthStatistics;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.room.RoomDto;
import com.java110.dto.report.QueryStatisticsDto;
import com.java110.report.statistics.IBaseDataStatistics;
import com.java110.report.statistics.IFeeStatistics;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.ListUtil;
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
 * 查询房屋费用明细表命令类
 * 以房屋为维度统计房屋的各个费用项的欠费和实收情况
 * 主要功能包括：参数验证、房屋信息查询、费用统计计算和结果返回
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode reportFeeMonthStatistics.queryReportFeeDetailRoom
 */
@Java110Cmd(serviceCode = "reportFeeMonthStatistics.queryReportFeeDetailRoom")
public class QueryReportFeeDetailRoomCmd extends Cmd {

    @Autowired
    private IFeeStatistics feeStatisticsImpl;

    @Autowired
    private IBaseDataStatistics baseDataStatisticsImpl;

    /**
     * 验证请求参数
     * 检查必要的参数是否存在，包括分页信息、日期范围和小区信息
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 命令执行异常
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
     * 执行查询房屋费用明细命令
     * 获取房屋信息并计算各房屋的费用欠费和实收情况
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 命令执行异常
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
        queryStatisticsDto.setFloorId(reqJson.getString("floorId"));
        queryStatisticsDto.setObjName(reqJson.getString("objName"));
        queryStatisticsDto.setFeeTypeCd(reqJson.getString("feeTypeCd"));
        queryStatisticsDto.setOwnerName(reqJson.getString("ownerName"));
        queryStatisticsDto.setLink(reqJson.getString("link"));
        queryStatisticsDto.setPage(reqJson.getInteger("page"));
        queryStatisticsDto.setRow(reqJson.getInteger("row"));

        // 查询房屋总数
        long count = baseDataStatisticsImpl.getRoomCount(queryStatisticsDto);
        List<RoomDto> rooms = null;
        
        // 根据查询结果获取房屋信息
        if (count > 0) {
            rooms = baseDataStatisticsImpl.getRoomInfo(queryStatisticsDto);
        } else {
            rooms = new ArrayList<>();
        }

        // 计算房屋欠费和实收数据
        JSONArray datas = computeRoomOweReceivedFee(rooms, queryStatisticsDto);

        // 构建返回结果
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) queryStatisticsDto.getRow()), count, datas);

        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 计算房屋欠费和实收费用
     * 遍历房屋列表，为每个房屋计算各费用类型的欠费和实收金额
     *
     * @param rooms 房屋信息列表
     * @param queryStatisticsDto 查询统计条件对象
     * @return JSONArray 包含房屋费用统计数据的JSON数组
     */
    private JSONArray computeRoomOweReceivedFee(List<RoomDto> rooms, QueryStatisticsDto queryStatisticsDto) {
        // 如果房屋列表为空，返回空数组
        if (ListUtil.isNull(rooms)) {
            return new JSONArray();
        }

        JSONArray datas = new JSONArray();
        JSONObject data = null;
        List<String> objIds = new ArrayList<>();

        // 遍历房屋列表，构建基础房屋信息
        for (RoomDto roomDto : rooms) {
            objIds.add(roomDto.getRoomId());
            data = new JSONObject();
            data.put("roomId", roomDto.getRoomId());
            // 构建房屋完整名称：楼层-单元-房间号
            data.put("roomName", roomDto.getFloorNum() + "-" + roomDto.getUnitNum() + "-" + roomDto.getRoomNum());
            data.put("ownerName", roomDto.getOwnerName());
            data.put("ownerId", roomDto.getOwnerId());
            data.put("link", roomDto.getLink());
            datas.add(data);
        }

        // 设置对象ID列表，用于批量查询费用信息
        queryStatisticsDto.setObjIds(objIds.toArray(new String[objIds.size()]));
        // 获取费用汇总信息
        List<Map> infos = feeStatisticsImpl.getObjFeeSummary(queryStatisticsDto);

        // 如果没有费用信息，直接返回基础房屋数据
        if (infos == null || infos.size() < 1) {
            return datas;
        }

        BigDecimal oweFee = null;
        BigDecimal receivedFee = null;
        double oweFeeD = 0;
        double receivedFeeD = 0;
        
        // 遍历每个房屋数据，计算费用统计
        for (int dataIndex = 0; dataIndex < datas.size(); dataIndex++) {
            // 初始化欠费和实收金额
            oweFee = new BigDecimal(0.00);
            receivedFee = new BigDecimal(0.00);
            data = datas.getJSONObject(dataIndex);
            
            // 遍历费用信息，匹配当前房屋的费用数据
            for (Map info : infos) {
                // 只处理当前房屋的费用信息
                if (!data.get("roomId").toString().equals(info.get("objId"))) {
                    continue;
                }

                // 获取欠费和实收金额
                oweFeeD = Double.parseDouble(info.get("oweFee").toString());
                receivedFeeD = Double.parseDouble(info.get("receivedFee").toString());

                // 累加总欠费和实收金额
                oweFee = oweFee.add(new BigDecimal(oweFeeD + ""));
                receivedFee = receivedFee.add(new BigDecimal(receivedFeeD + ""));
                
                // 按费用类型分别存储欠费和实收金额
                data.put("oweFee" + info.get("feeTypeCd").toString(), MoneyUtil.computePriceScale(oweFeeD));
                data.put("receivedFee" + info.get("feeTypeCd").toString(), MoneyUtil.computePriceScale(receivedFeeD));
            }
            
            // 设置房屋总欠费和实收金额
            data.put("oweFee", MoneyUtil.computePriceScale(oweFee.doubleValue()));
            data.put("receivedFee", MoneyUtil.computePriceScale(receivedFee.doubleValue()));
        }

        return datas;
    }
}