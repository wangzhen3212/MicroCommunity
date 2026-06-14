package com.java110.report.cmd.dataReport;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.dict.DictDto;
import com.java110.dto.room.RoomDto;
import com.java110.dto.report.QueryStatisticsDto;
import com.java110.intf.dev.IDictV1InnerServiceSMO;
import com.java110.report.statistics.IBaseDataStatistics;
import com.java110.report.statistics.IFeeStatistics;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.MoneyUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 查询实收明细统计命令类
 * 该命令类用于处理实收明细统计查询请求，主要功能包括：
 * 1. 验证查询参数的有效性
 * 2. 查询房屋信息和实收费用数据
 * 3. 计算房屋的实收费用统计
 * 4. 返回分页格式的统计结果
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "dataReport.queryReceivedDetailStatistics")
public class QueryReceivedDetailStatisticsCmd extends Cmd {

    @Autowired
    private IFeeStatistics feeStatisticsImpl;

    @Autowired
    private IBaseDataStatistics baseDataStatisticsImpl;

    @Autowired
    private IDictV1InnerServiceSMO dictV1InnerServiceSMOImpl;

    /**
     * 验证请求参数的有效性
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证必须包含小区ID
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
     * 执行实收明细统计查询命令
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
        queryStatisticsDto.setFloorId(reqJson.getString("floorId"));
        queryStatisticsDto.setObjName(reqJson.getString("objName"));
        queryStatisticsDto.setFeeTypeCd(reqJson.getString("feeTypeCd"));
        queryStatisticsDto.setOwnerName(reqJson.getString("ownerName"));
        queryStatisticsDto.setLink(reqJson.getString("link"));
        queryStatisticsDto.setPage(reqJson.getInteger("page"));
        queryStatisticsDto.setRow(reqJson.getInteger("row"));
        
        // 查询符合条件的房屋数量
        long count = baseDataStatisticsImpl.getReceivedRoomCount(queryStatisticsDto);
        List<RoomDto> rooms = null;
        if (count > 0) {
            // 如果有数据则查询房屋详细信息
            rooms = baseDataStatisticsImpl.getReceivedRoomInfo(queryStatisticsDto);
        } else {
            // 没有数据则返回空列表
            rooms = new ArrayList<>();
        }

        // 计算房屋欠费实收数据
        JSONArray datas = computeRoomOweReceivedFee(rooms, queryStatisticsDto);

        // 构建分页结果对象
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) queryStatisticsDto.getRow()), count, datas);

        // 设置响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 计算房屋欠费实收费用
     * 该方法处理房屋的实收费用统计，按费用类型分类汇总
     * 
     * @param rooms 房屋信息列表
     * @param queryStatisticsDto 查询参数对象
     * @return JSONArray 包含房屋实收费用统计数据的JSON数组
     */
    private JSONArray computeRoomOweReceivedFee(List<RoomDto> rooms, QueryStatisticsDto queryStatisticsDto) {
        // 如果房屋列表为空，返回空数组
        if (ListUtil.isNull(rooms)) {
            return new JSONArray();
        }

        JSONArray datas = new JSONArray();
        JSONObject data = null;

        // 提取所有房屋ID
        List<String> objIds = new ArrayList<>();
        for (RoomDto roomDto : rooms) {
            objIds.add(roomDto.getRoomId());
            // 构建基础房屋信息
            data = new JSONObject();
            data.put("roomId", roomDto.getRoomId());
            data.put("roomName", roomDto.getFloorNum() + "-" + roomDto.getUnitNum() + "-" + roomDto.getRoomNum());
            data.put("ownerName", roomDto.getOwnerName());
            data.put("ownerId", roomDto.getOwnerId());
            data.put("link", roomDto.getLink());
            datas.add(data);
        }

        // 设置房屋ID数组到查询参数中
        queryStatisticsDto.setObjIds(objIds.toArray(new String[objIds.size()]));
        // 查询房屋实收费用信息
        List<Map> infos = feeStatisticsImpl.getObjReceivedFee(queryStatisticsDto);

        // 如果没有实收费用数据，直接返回房屋基础信息
        if (ListUtil.isNull(infos)) {
            return datas;
        }

        // 清洗数据，将数据按房屋ID和费用类型重新组织
        infos = washInfos(infos);

        BigDecimal receivedFee = null;
        List<Map> itemFees = null;
        String feeTypeCd = "";
        data.put("receivedFee", "0");

        // 查询费用类型字典配置
        DictDto dictDto = new DictDto();
        dictDto.setTableName("pay_fee_config");
        dictDto.setTableColumns("fee_type_cd_show");
        List<DictDto> dictDtos = dictV1InnerServiceSMOImpl.queryDicts(dictDto);

        // 根据房屋ID合并数据，将费用大类横向放入data中，并计算每个房屋费用大类的实收和房屋总实收
        for (int dataIndex = 0; dataIndex < datas.size(); dataIndex++) {
            receivedFee = new BigDecimal(0.00);
            data = datas.getJSONObject(dataIndex);
            // 循环费用大类统计信息
            for (Map info : infos) {
                // 只处理当前房屋的数据
                if (!data.getString("roomId").equals(info.get("payerObjId"))) {
                    continue;
                }
                // 遍历所有费用类型
                for (DictDto tDict : dictDtos) {
                    feeTypeCd = tDict.getStatusCd();
                    // 如果当前费用类型在统计信息中不存在，跳过
                    if (!info.containsKey(feeTypeCd)) {
                        continue;
                    }
                    // 累加实收费用
                    receivedFee = receivedFee.add(new BigDecimal(info.get(feeTypeCd + "receivedFee").toString()));
                    data.put("receivedFee" + feeTypeCd, info.get(feeTypeCd));
                }
            }
            // 设置总实收费用，并格式化金额
            data.put("receivedFee", MoneyUtil.computePriceScale(receivedFee.doubleValue()));
        }

        return datas;
    }

    /**
     * 清洗数据，将数据重新组织为按房屋ID和费用类型分类的结构
     * 原始数据格式：payerObjId, feeTypeCd, feeName, endTime, deadlineTime, amountOwed
     * 目标数据格式：按payerObjId纵向转换，每个费用类型对应一个费用列表
     * 
     * @param infos 原始费用信息列表
     * @return List<Map> 重新组织后的费用信息列表
     */
    private List<Map> washInfos(List<Map> infos) {
        List<Map> newInfos = new ArrayList<>();
        // 生成新的信息结构，按房屋ID分组
        for (Map info : infos) {
            generatorNewInfo(newInfos, info);
        }

        List<Map> tmpInfos = null;
        Map dInfo = null;
        // 为每个房屋ID组织费用数据
        for (Map nInfo : newInfos) {
            for (Map info : infos) {
                // 只处理相同房屋ID的数据
                if (!nInfo.get("payerObjId").equals(info.get("payerObjId"))) {
                    continue;
                }
                // 获取对应费用类型的费用列表
                tmpInfos = getTmpInfos(nInfo, info);
                // 深拷贝费用信息
                dInfo = new HashMap();
                dInfo.putAll(info);
                tmpInfos.add(dInfo);
                // 计算单项实收金额
                computeReceivedAmount(tmpInfos, info.get("feeTypeCd").toString(), nInfo);
                nInfo.put(info.get("feeTypeCd").toString(), tmpInfos);
            }
        }

        return newInfos;
    }

    /**
     * 计算每个费用大类的实收金额
     * 
     * @param tmpInfos 费用信息列表
     * @param feeTypeCd 费用类型代码
     * @param nInfo 目标信息Map，用于存储计算结果
     */
    private void computeReceivedAmount(List<Map> tmpInfos, String feeTypeCd, Map nInfo) {
        // 如果费用列表为空，设置实收金额为0
        if (tmpInfos == null || tmpInfos.size() < 1) {
            nInfo.put(feeTypeCd + "receivedFee", 0.0);
            return;
        }
        // 计算总实收金额
        BigDecimal receivedAmount = new BigDecimal(0.0);
        for (Map tInfo : tmpInfos) {
            receivedAmount = receivedAmount.add(new BigDecimal(tInfo.get("receivedAmount").toString()));
        }
        // 金额格式化，保留2位小数，四舍五入
        receivedAmount = receivedAmount.setScale(2, BigDecimal.ROUND_HALF_UP);
        nInfo.put(feeTypeCd + "receivedFee", receivedAmount.doubleValue());
    }

    /**
     * 获取指定费用类型的费用列表
     * 如果不存在则创建新的列表
     * 
     * @param nInfo 房屋信息Map
     * @param info 费用信息Map
     * @return List<Map> 费用类型对应的费用列表
     */
    private List<Map> getTmpInfos(Map nInfo, Map info) {
        String feeTypeCd = info.get("feeTypeCd").toString();
        // 如果已存在该费用类型的列表，直接返回
        if (nInfo.containsKey(feeTypeCd)) {
            return (List<Map>) nInfo.get(feeTypeCd);
        }

        // 不存在则创建新的列表
        return new ArrayList<>();
    }

    /**
     * 生成新的信息结构，按房屋ID去重
     * 
     * @param newInfos 新的信息列表
     * @param info 要添加的费用信息
     */
    private void generatorNewInfo(List<Map> newInfos, Map info) {
        // 如果列表为空，直接添加
        if (newInfos.size() < 1) {
            newInfos.add(info);
            return;
        }

        // 检查是否已存在相同房屋ID的信息
        for (Map newInfo : newInfos) {
            if (newInfo.get("payerObjId").equals(info.get("payerObjId"))) {
                return;
            }
        }

        // 不存在则添加新的房屋信息
        newInfos.add(info);
    }
}