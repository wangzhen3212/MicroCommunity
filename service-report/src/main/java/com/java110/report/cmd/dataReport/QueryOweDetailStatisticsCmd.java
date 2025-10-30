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
 * 查询欠费明细统计命令类
 * 
 * 该类用于处理欠费明细统计查询请求，主要功能包括：
 * 1. 验证查询参数的有效性
 * 2. 查询欠费房屋信息
 * 3. 计算房屋欠费和实收费用
 * 4. 返回统计结果数据
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "dataReport.queryOweDetailStatistics")
public class QueryOweDetailStatisticsCmd extends Cmd {

    @Autowired
    private IFeeStatistics feeStatisticsImpl;

    @Autowired
    private IBaseDataStatistics baseDataStatisticsImpl;

    @Autowired
    private IDictV1InnerServiceSMO dictV1InnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 
     * 主要验证内容：
     * 1. 检查必填参数communityId是否存在
     * 2. 格式化开始日期和结束日期，补充时间部分
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证小区ID必填
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
        
        // 格式化开始日期，如果没有时间部分则补充"00:00:00"
        String startDate = reqJson.getString("startDate");
        String endDate = reqJson.getString("endDate");
        if (!StringUtil.isEmpty(startDate) && !startDate.contains(":")) {
            startDate += " 00:00:00";
            reqJson.put("startDate", startDate);
        }
        // 格式化结束日期，如果没有时间部分则补充"23:59:59"
        if (!StringUtil.isEmpty(endDate) && !endDate.contains(":")) {
            endDate += " 23:59:59";
            reqJson.put("endDate", endDate);
        }
    }

    /**
     * 执行欠费明细统计查询命令
     * 
     * 主要处理流程：
     * 1. 构建查询条件对象
     * 2. 查询欠费房屋数量和详细信息
     * 3. 计算房屋欠费和实收费用
     * 4. 构建返回结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 构建查询统计DTO对象
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

        // 查询欠费房屋数量
        long count = baseDataStatisticsImpl.getOweRoomCount(queryStatisticsDto);
        List<RoomDto> rooms = null;
        if (count > 0) {
            // 如果有欠费房屋，查询房屋详细信息
            rooms = baseDataStatisticsImpl.getOweRoomInfo(queryStatisticsDto);
        } else {
            // 没有欠费房屋，返回空列表
            rooms = new ArrayList<>();
        }

        // 计算房屋欠费实收数据
        JSONArray datas = computeRoomOweReceivedFee(rooms, queryStatisticsDto);

        // 构建分页返回结果
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) queryStatisticsDto.getRow()), count, datas);

        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 计算房屋欠费和实收费用
     * 
     * 处理流程：
     * 1. 构建房屋基础信息数据
     * 2. 查询房屋欠费信息
     * 3. 数据清洗和转换
     * 4. 计算各类费用欠费金额
     * 5. 计算总欠费金额
     * 
     * @param rooms 房屋列表
     * @param queryStatisticsDto 查询统计条件
     * @return JSONArray 包含房屋欠费明细的数据数组
     */
    private JSONArray computeRoomOweReceivedFee(List<RoomDto> rooms, QueryStatisticsDto queryStatisticsDto) {
        // 空值检查，如果房屋列表为空则返回空数组
        if (rooms == null || rooms.size() < 1) {
            return new JSONArray();
        }

        JSONArray datas = new JSONArray();
        JSONObject data = null;

        // 构建房屋ID列表和基础房屋信息数据
        List<String> objIds = new ArrayList<>();
        for (RoomDto roomDto : rooms) {
            objIds.add(roomDto.getRoomId());
            data = new JSONObject();
            data.put("roomId", roomDto.getRoomId());
            // 构建房屋名称：楼层-单元-房间号
            data.put("roomName", roomDto.getFloorNum() + "-" + roomDto.getUnitNum() + "-" + roomDto.getRoomNum());
            data.put("ownerName", roomDto.getOwnerName());
            data.put("ownerId", roomDto.getOwnerId());
            data.put("link", roomDto.getLink());
            datas.add(data);
        }

        // 设置查询对象ID列表
        queryStatisticsDto.setObjIds(objIds.toArray(new String[objIds.size()]));
        // 查询房屋欠费信息
        List<Map> infos = feeStatisticsImpl.getObjOweFee(queryStatisticsDto);

        // 如果没有欠费信息，直接返回房屋基础信息
        if (ListUtil.isNull(infos)) {
            return datas;
        }

        // 数据清洗：将欠费信息按房屋ID和费用类型重新组织
        infos = washInfos(infos);

        // 查询费用类型字典配置
        DictDto dictDto = new DictDto();
        dictDto.setTableName("pay_fee_config");
        dictDto.setTableColumns("fee_type_cd_show");
        List<DictDto> dictDtos = dictV1InnerServiceSMOImpl.queryDicts(dictDto);

        // 计算每个房屋的各类费用欠费和总欠费
        for (int dataIndex = 0; dataIndex < datas.size(); dataIndex++) {
            BigDecimal oweFee = new BigDecimal(0.00);
            data = datas.getJSONObject(dataIndex);
            
            // 遍历欠费信息，匹配当前房屋
            for (Map info : infos) {
                if (!data.getString("roomId").equals(info.get("payerObjId"))) {
                    continue; // 跳过不匹配的房屋
                }
                
                // 遍历费用类型字典，计算各类费用欠费
                for (DictDto tDict : dictDtos) {
                    String feeTypeCd = tDict.getStatusCd();
                    if (!info.containsKey(feeTypeCd)) {
                        continue; // 跳过不存在的费用类型
                    }
                    // 累加各类费用欠费金额
                    oweFee = oweFee.add(new BigDecimal(info.get(feeTypeCd + "oweFee").toString()));
                    data.put("oweFee" + feeTypeCd, info.get(feeTypeCd));
                }
            }
            // 设置房屋总欠费金额
            data.put("oweFee", oweFee.doubleValue());
        }

        return datas;
    }

    /**
     * 清洗和转换欠费信息数据
     * 
     * 将原始欠费信息按房屋ID重新组织，将同一房屋的不同费用类型数据合并
     * 原始数据结构：按费用记录平铺
     * 转换后结构：按房屋ID分组，每个房屋包含各类费用的明细列表
     * 
     * @param infos 原始欠费信息列表
     * @return List<Map> 转换后的欠费信息列表
     */
    private List<Map> washInfos(List<Map> infos) {
        List<Map> newInfos = new ArrayList<>();
        // 第一步：生成按房屋ID分组的基础结构
        for (Map info : infos) {
            generatorNewInfo(newInfos, info);
        }

        List<Map> tmpInfos = null;
        Map dInfo = null;
        // 第二步：填充每个房屋的各类费用明细
        for (Map nInfo : newInfos) {
            for (Map info : infos) {
                if (!nInfo.get("payerObjId").equals(info.get("payerObjId"))) {
                    continue; // 跳过不匹配的房屋
                }
                // 获取当前费用类型的明细列表
                tmpInfos = getTmpInfos(nInfo, info);
                if (tmpInfos == null) {
                    continue; // 跳过无效的费用类型
                }
                // 深拷贝费用信息并添加到明细列表
                dInfo = new HashMap();
                dInfo.putAll(info);
                tmpInfos.add(dInfo);
                // 计算当前费用类型的总欠费金额
                computeOweAmount(tmpInfos, info.get("feeTypeCd").toString(), nInfo);
                nInfo.put(info.get("feeTypeCd").toString(), tmpInfos);
            }
        }

        return newInfos;
    }

    /**
     * 计算指定费用类型的总欠费金额
     * 
     * 遍历费用明细列表，累加所有记录的欠费金额
     * 
     * @param tmpInfos 费用明细列表
     * @param feeTypeCd 费用类型编码
     * @param nInfo 房屋欠费信息对象
     */
    private void computeOweAmount(List<Map> tmpInfos, String feeTypeCd, Map nInfo) {
        // 空值检查
        if (tmpInfos == null || tmpInfos.size() < 1) {
            nInfo.put(feeTypeCd + "oweFee", 0.0);
            return;
        }
        // 计算总欠费金额
        BigDecimal oweFee = new BigDecimal(0.0);
        for (Map tInfo : tmpInfos) {
            oweFee = oweFee.add(new BigDecimal(tInfo.get("amountOwed").toString()));
        }
        // 金额格式化，保留2位小数，四舍五入
        oweFee = oweFee.setScale(2, BigDecimal.ROUND_HALF_UP);
        nInfo.put(feeTypeCd + "oweFee", oweFee.doubleValue());
    }

    /**
     * 获取指定费用类型的明细列表
     * 
     * 如果房屋信息中已存在该费用类型的列表，则返回现有列表
     * 否则创建新的列表
     * 
     * @param nInfo 房屋欠费信息对象
     * @param info 费用信息对象
     * @return List<Map> 费用明细列表
     */
    private List<Map> getTmpInfos(Map nInfo, Map info) {
        // 检查费用类型编码是否存在
        if (!info.containsKey("feeTypeCd")) {
            return null;
        }
        String feeTypeCd = info.get("feeTypeCd").toString();
        // 如果已存在该费用类型的列表，直接返回
        if (nInfo.containsKey(feeTypeCd)) {
            return (List<Map>) nInfo.get(feeTypeCd);
        }

        // 创建新的费用明细列表
        return new ArrayList<>();
    }

    /**
     * 生成新的房屋欠费信息对象
     * 
     * 避免重复添加相同的房屋信息
     * 
     * @param newInfos 新的房屋信息列表
     * @param info 原始费用信息对象
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
                return; // 已存在，不重复添加
            }
        }

        // 添加新的房屋信息
        newInfos.add(info);
    }
}