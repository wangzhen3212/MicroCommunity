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
 * 查询月欠费明细命令类
 * 该命令类用于处理查询小区月欠费明细的业务逻辑，包括参数验证、数据查询、欠费计算和数据格式化等功能
 * 通过调用费用统计服务获取欠费数据，并对数据进行清洗和转换处理
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode dataReport.queryMonthOweDetail
 */
@Java110Cmd(serviceCode = "dataReport.queryMonthOweDetail")
public class QueryMonthOweDetailCmd extends Cmd {

    @Autowired
    private IFeeStatistics feeStatisticsImpl;

    @Autowired
    private IBaseDataStatistics baseDataStatisticsImpl;

    @Autowired
    private IDictV1InnerServiceSMO dictV1InnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 检查必要的参数是否存在，并对日期参数进行格式化处理
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
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
     * 执行查询月欠费明细命令
     * 根据查询条件获取欠费明细数据，计算总欠费金额，并返回格式化结果
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
        queryStatisticsDto.setStartDate(reqJson.getString("feeStartDate"));
        queryStatisticsDto.setEndDate(reqJson.getString("feeEndDate"));
        queryStatisticsDto.setFloorId(reqJson.getString("floorId"));
        queryStatisticsDto.setObjName(reqJson.getString("objName"));
        queryStatisticsDto.setFeeTypeCd(reqJson.getString("feeTypeCd"));
        queryStatisticsDto.setOwnerName(reqJson.getString("ownerName"));
        queryStatisticsDto.setLink(reqJson.getString("link"));
        queryStatisticsDto.setPage(reqJson.getInteger("page"));
        queryStatisticsDto.setRow(reqJson.getInteger("row"));
        
        // 查询欠费明细记录总数
        long count = feeStatisticsImpl.getMonthOweDetailCount(queryStatisticsDto);
        List<Map> infos = null;
        if (count > 0) {
            // 如果有数据则查询明细信息
            infos = feeStatisticsImpl.getMonthOweDetailInfo(queryStatisticsDto);
        } else {
            // 没有数据返回空列表
            infos = new ArrayList<>();
        }
        
        // 计算总欠费金额
        double oweAmount = feeStatisticsImpl.getMonthOweDetailAmount(queryStatisticsDto);

        // 构建返回结果对象
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) queryStatisticsDto.getRow()), count, infos);
        resultVo.setSumTotal(oweAmount);

        // 设置响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 计算房屋欠费和实收费用
     * 对房屋列表进行欠费计算，按费用大类进行横向数据转换和汇总
     * 
     * @param rooms 房屋数据列表
     * @param queryStatisticsDto 查询参数对象
     * @return JSONArray 包含房屋欠费信息的JSON数组
     */
    private JSONArray computeRoomOweReceivedFee(List<RoomDto> rooms, QueryStatisticsDto queryStatisticsDto) {
        // 参数校验，如果房屋列表为空则返回空数组
        if (rooms == null || rooms.size() < 1) {
            return new JSONArray();
        }

        JSONArray datas = new JSONArray();
        JSONObject data = null;

        // 提取所有房屋ID
        List<String> objIds = new ArrayList<>();
        for (RoomDto roomDto : rooms) {
            objIds.add(roomDto.getRoomId());
            // 构建基础房屋信息对象
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

        // 如果没有费用信息则返回基础房屋数据
        if (infos == null || infos.size() < 1) {
            return datas;
        }

        // 清洗数据，将纵向数据转换为横向结构
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

        // 按房屋ID和费用大类合并数据，计算每个房屋的各类欠费和总欠费
        for (int dataIndex = 0; dataIndex < datas.size(); dataIndex++) {
            receivedFee = new BigDecimal(0.00);
            data = datas.getJSONObject(dataIndex);
            // 遍历费用信息，匹配当前房屋
            for (Map info : infos) {
                if (!data.getString("roomId").equals(info.get("payerObjId"))) {
                    continue;
                }
                // 遍历费用类型字典，计算各类费用
                for (DictDto tDict : dictDtos) {
                    feeTypeCd = tDict.getStatusCd();
                    if (!info.containsKey(feeTypeCd)) {
                        continue;
                    }
                    // 累加实收费用
                    receivedFee = receivedFee.add(new BigDecimal(info.get(feeTypeCd + "receivedFee").toString()));
                    data.put("receivedFee" + feeTypeCd, info.get(feeTypeCd));
                }
            }
            // 设置房屋总实收费用
            data.put("receivedFee", receivedFee.doubleValue());
        }

        return datas;
    }

    /**
     * 清洗费用信息数据
     * 将纵向的费用明细数据转换为按房屋ID和费用类型横向聚合的数据结构
     * 
     * @param infos 原始费用信息列表
     * @return List<Map> 清洗后的费用信息列表
     */
    private List<Map> washInfos(List<Map> infos) {
        List<Map> newInfos = new ArrayList<>();
        // 生成新的信息结构，按房屋ID分组
        for (Map info : infos) {
            generatorNewInfo(newInfos, info);
        }

        List<Map> tmpInfos = null;
        Map dInfo = null;
        // 对每个房屋的费用信息进行聚合处理
        for (Map nInfo : newInfos) {
            for (Map info : infos) {
                if (!nInfo.get("payerObjId").equals(info.get("payerObjId"))) {
                    continue;
                }
                // 获取对应费用类型的临时信息列表
                tmpInfos = getTmpInfos(nInfo, info);
                // 深拷贝费用信息
                dInfo = new HashMap();
                dInfo.putAll(info);
                tmpInfos.add(dInfo);
                // 计算单项欠费金额
                computeReceivedAmount(tmpInfos, info.get("feeTypeCd").toString(), nInfo);
                nInfo.put(info.get("feeTypeCd").toString(), tmpInfos);
            }
        }

        return newInfos;
    }

    /**
     * 计算每个费用大类的实收金额
     * 对指定费用类型的所有费用记录进行金额汇总
     * 
     * @param tmpInfos 费用信息列表
     * @param feeTypeCd 费用类型代码
     * @param nInfo 目标信息Map，用于存储计算结果
     */
    private void computeReceivedAmount(List<Map> tmpInfos, String feeTypeCd, Map nInfo) {
        // 如果费用信息为空，设置实收金额为0
        if (tmpInfos == null || tmpInfos.size() < 1) {
            nInfo.put(feeTypeCd + "receivedFee", 0.0);
            return;
        }
        // 计算总实收金额
        BigDecimal receivedAmount = new BigDecimal(0.0);
        for (Map tInfo : tmpInfos) {
            receivedAmount = receivedAmount.add(new BigDecimal(tInfo.get("receivedAmount").toString()));
        }
        // 金额四舍五入保留两位小数
        receivedAmount = receivedAmount.setScale(2, BigDecimal.ROUND_HALF_UP);
        nInfo.put(feeTypeCd + "receivedFee", receivedAmount.doubleValue());
    }

    /**
     * 获取指定费用类型的临时信息列表
     * 如果不存在则创建新的列表
     * 
     * @param nInfo 房屋信息Map
     * @param info 费用信息Map
     * @return List<Map> 费用类型的临时信息列表
     */
    private List<Map> getTmpInfos(Map nInfo, Map info) {
        String feeTypeCd = info.get("feeTypeCd").toString();
        // 如果已存在该费用类型的信息列表，直接返回
        if (nInfo.containsKey(feeTypeCd)) {
            return (List<Map>) nInfo.get(feeTypeCd);
        }

        // 不存在则创建新的列表
        return new ArrayList<>();
    }

    /**
     * 生成新的房屋信息记录
     * 确保每个房屋ID在列表中只存在一条记录
     * 
     * @param newInfos 新的信息列表
     * @param info 原始费用信息
     */
    private void generatorNewInfo(List<Map> newInfos, Map info) {
        // 如果列表为空，直接添加
        if (newInfos.size() < 1) {
            newInfos.add(info);
            return;
        }

        // 检查是否已存在相同房屋ID的记录
        for (Map newInfo : newInfos) {
            if (newInfo.get("payerObjId").equals(info.get("payerObjId"))) {
                return;
            }
        }

        // 不存在则添加新记录
        newInfos.add(info);
    }
}