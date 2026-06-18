package com.java110.report.cmd.dataReport;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.dict.DictDto;
import com.java110.dto.report.QueryStatisticsDto;
import com.java110.dto.room.RoomDto;
import com.java110.intf.dev.IDictV1InnerServiceSMO;
import com.java110.report.statistics.IBaseDataStatistics;
import com.java110.report.statistics.IFeeStatistics;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
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
 * 查询月实收明细统计命令类
 * 
 * 该类负责处理查询月实收明细统计的业务逻辑，包括参数验证、数据查询、数据清洗和结果返回等功能
 * 通过调用费用统计服务获取月实收明细数据，并对数据进行处理和格式化
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode dataReport.queryMonthReceivedDetail
 */
@Java110Cmd(serviceCode = "dataReport.queryMonthReceivedDetail")
public class QueryMonthReceivedDetailCmd extends Cmd {

    @Autowired
    private IFeeStatistics feeStatisticsImpl;

    @Autowired
    private IBaseDataStatistics baseDataStatisticsImpl;

    @Autowired
    private IDictV1InnerServiceSMO dictV1InnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 
     * 检查必要的请求参数是否存在，并对日期参数进行格式化处理
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证必要参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
        Assert.hasKeyAndValue(reqJson, "feeStartDate", "未包含开始时间");
        Assert.hasKeyAndValue(reqJson, "feeEndDate", "未包含结束时间");

        // 格式化开始日期，如果没有时间部分则添加默认时间
        String startDate = reqJson.getString("feeStartDate");
        String endDate = reqJson.getString("feeEndDate");
        if (!StringUtil.isEmpty(startDate) && !startDate.contains(":")) {
            startDate += " 00:00:00";
            reqJson.put("feeStartDate", startDate);
        }
        // 格式化结束日期，如果没有时间部分则添加默认时间
        if (!StringUtil.isEmpty(endDate) && !endDate.contains(":")) {
            endDate += " 23:59:59";
            reqJson.put("feeEndDate", endDate);
        }
    }

    /**
     * 执行查询月实收明细命令
     * 
     * 根据查询条件获取月实收明细数据，包括数据总数、明细列表和实收总金额
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 构建查询参数对象
        QueryStatisticsDto queryStatisticsDto = new QueryStatisticsDto();
        queryStatisticsDto.setCommunityId(reqJson.getString("communityId"));
        queryStatisticsDto.setStartDate(reqJson.getString("feeStartDate"));
        queryStatisticsDto.setEndDate(reqJson.getString("feeEndDate"));
        queryStatisticsDto.setFloorId(reqJson.getString("floorId"));
        queryStatisticsDto.setObjName(reqJson.getString("objName"));
        queryStatisticsDto.setFeeTypeCd(reqJson.getString("feeTypeCd"));
        queryStatisticsDto.setOwnerName(reqJson.getString("ownerName"));
        queryStatisticsDto.setLink(reqJson.getString("link"));
        queryStatisticsDto.setPage(reqJson.getInteger("page"));
        queryStatisticsDto.setRow(reqJson.getInteger("row"));
        
        // 查询数据总数
        long count = feeStatisticsImpl.getMonthReceivedDetailCount(queryStatisticsDto);
        List<Map> infos = null;
        if (count > 0) {
            // 如果有数据则查询明细信息
            infos = feeStatisticsImpl.getMonthReceivedDetailInfo(queryStatisticsDto);
        } else {
            // 没有数据返回空列表
            infos = new ArrayList<>();
        }

        // 查询实收总金额
        double receivedAmount = feeStatisticsImpl.getMonthReceivedDetailAmount(queryStatisticsDto);

        // 构建返回结果
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) queryStatisticsDto.getRow()), count, infos);
        resultVo.setSumTotal(receivedAmount);
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 计算房屋欠费和实收费用
     * 
     * 根据房屋列表和查询条件，计算每个房屋的欠费和实收费用，并按费用大类进行横向展示
     * 
     * @param rooms 房屋列表
     * @param queryStatisticsDto 查询参数对象
     * @return JSONArray 包含房屋费用信息的JSON数组
     */
    private JSONArray computeRoomOweReceivedFee(List<RoomDto> rooms, QueryStatisticsDto queryStatisticsDto) {
        // 如果房屋列表为空，返回空数组
        if (rooms == null || rooms.size() < 1) {
            return new JSONArray();
        }

        JSONArray datas = new JSONArray();
        JSONObject data = null;

        // 构建房屋ID列表和基础房屋信息
        List<String> objIds = new ArrayList<>();
        for (RoomDto roomDto : rooms) {
            objIds.add(roomDto.getRoomId());
            data = new JSONObject();
            data.put("roomId", roomDto.getRoomId());
            data.put("roomName", roomDto.getFloorNum() + "-" + roomDto.getUnitNum() + "-" + roomDto.getRoomNum());
            data.put("ownerName", roomDto.getOwnerName());
            data.put("ownerId", roomDto.getOwnerId());
            data.put("link", roomDto.getLink());
            datas.add(data);
        }

        // 设置房屋ID列表到查询参数中
        queryStatisticsDto.setObjIds(objIds.toArray(new String[objIds.size()]));
        // 查询房屋实收费用信息
        List<Map> infos = feeStatisticsImpl.getObjReceivedFee(queryStatisticsDto);

        // 如果没有费用信息，直接返回房屋基础信息
        if (infos == null || infos.size() < 1) {
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

        // 根据房屋ID合并数据，将费用大类横向放入data中，并计算每个房屋费用大类的实收和总实收
        for (int dataIndex = 0; dataIndex < datas.size(); dataIndex++) {
            receivedFee = new BigDecimal(0.00);
            data = datas.getJSONObject(dataIndex);
            // 循环费用信息，匹配当前房屋
            for (Map info : infos) {
                if (!data.getString("roomId").equals(info.get("payerObjId"))) {
                    continue;
                }
                // 循环费用类型字典，计算各类费用实收金额
                for (DictDto tDict : dictDtos) {
                    feeTypeCd = tDict.getStatusCd();
                    if (!info.containsKey(feeTypeCd)) {
                        continue;
                    }
                    // 累加各类费用实收金额
                    receivedFee = receivedFee.add(new BigDecimal(info.get(feeTypeCd + "receivedFee").toString()));
                    data.put("receivedFee" + feeTypeCd, info.get(feeTypeCd));
                }
            }
            // 设置房屋总实收金额
            data.put("receivedFee", receivedFee.doubleValue());
        }

        return datas;
    }

    /**
     * 清洗费用数据
     * 
     * 将原始费用数据按照房屋ID和费用类型重新组织，转换为按房屋ID纵向的数据结构
     * 
     * @param infos 原始费用数据列表
     * @return List<Map> 清洗后的费用数据列表
     */
    private List<Map> washInfos(List<Map> infos) {
        List<Map> newInfos = new ArrayList<>();
        // 生成新的数据结构，按房屋ID分组
        for (Map info : infos) {
            generatorNewInfo(newInfos, info);
        }

        List<Map> tmpInfos = null;
        Map dInfo = null;
        // 重新组织数据，按房屋ID和费用类型分类
        for (Map nInfo : newInfos) {
            for (Map info : infos) {
                if (!nInfo.get("payerObjId").equals(info.get("payerObjId"))) {
                    continue;
                }
                // 获取对应费用类型的临时列表
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
     * 根据费用明细列表，计算指定费用类型的总实收金额
     * 
     * @param tmpInfos 费用明细列表
     * @param feeTypeCd 费用类型代码
     * @param nInfo 存储计算结果的Map对象
     */
    private void computeReceivedAmount(List<Map> tmpInfos, String feeTypeCd, Map nInfo) {
        // 如果费用明细为空，设置实收金额为0
        if (tmpInfos == null || tmpInfos.size() < 1) {
            nInfo.put(feeTypeCd + "receivedFee", 0.0);
            return;
        }
        // 计算总实收金额
        BigDecimal receivedAmount = new BigDecimal(0.0);
        for (Map tInfo : tmpInfos) {
            receivedAmount = receivedAmount.add(new BigDecimal(tInfo.get("receivedAmount").toString()));
        }
        // 金额四舍五入保留2位小数
        receivedAmount = receivedAmount.setScale(2, BigDecimal.ROUND_HALF_UP);
        nInfo.put(feeTypeCd + "receivedFee", receivedAmount.doubleValue());
    }

    /**
     * 获取费用类型的临时列表
     * 
     * 从房屋信息中获取指定费用类型的费用列表，如果不存在则创建新的列表
     * 
     * @param nInfo 房屋信息Map
     * @param info 费用信息Map
     * @return List<Map> 费用类型的费用列表
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
     * 生成新的房屋信息
     * 
     * 根据费用信息生成按房屋ID分组的新数据结构，避免重复
     * 
     * @param newInfos 新的房屋信息列表
     * @param info 费用信息
     */
    private void generatorNewInfo(List<Map> newInfos, Map info) {
        // 如果列表为空，直接添加
        if (newInfos.size() < 1) {
            newInfos.add(info);
            return;
        }

        // 检查是否已存在该房屋的信息
        for (Map newInfo : newInfos) {
            if (newInfo.get("payerObjId").equals(info.get("payerObjId"))) {
                return;
            }
        }

        // 不存在则添加新的房屋信息
        newInfos.add(info);
    }
}