package com.java110.report.cmd.dataReport;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.IotDataDto;
import com.java110.dto.report.QueryStatisticsDto;
import com.java110.intf.job.IIotInnerServiceSMO;
import com.java110.report.statistics.IInoutStatistics;
import com.java110.report.statistics.IOrderStatistics;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;

/**
 * 数据统计出入统计命令类
 * 
 * 该类负责处理数据报表查询请求，主要统计小区内的各种出入数据，
 * 包括车辆进出、人员进出、物品出入、房屋管理等相关统计数据
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode dataReport.queryInoutDataReport
 */
@Java110Cmd(serviceCode = "dataReport.queryInoutDataReport")
public class QueryInoutDataReportCmd extends Cmd {

    /**
     * 出入统计服务接口
     */
    @Autowired
    private IInoutStatistics inoutStatisticsImpl;

    /**
     * IoT设备内部服务接口
     */
    @Autowired
    private IIotInnerServiceSMO iotInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数是否完整，并对日期参数进行格式化处理
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证必填参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
        Assert.hasKeyAndValue(reqJson, "startDate", "未包含开始时间");
        Assert.hasKeyAndValue(reqJson, "endDate", "未包含结束时间");
        
        // 获取日期参数
        String startDate = reqJson.getString("startDate");
        String endDate = reqJson.getString("endDate");
        
        // 如果日期不包含时间部分，自动添加时间
        if (!startDate.contains(":")) {
            startDate += " 00:00:00";
            reqJson.put("startDate", startDate);
        }
        if (!endDate.contains(":")) {
            endDate += " 23:59:59";
            reqJson.put("endDate", endDate);
        }
    }

    /**
     * 执行命令方法
     * 
     * 处理数据报表查询业务逻辑，统计各类出入数据并返回结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
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
        
        // 初始化数据容器
        JSONArray datas = new JSONArray();
        JSONObject data = null;

        // 调用IoT服务获取车辆和人员进出数据
        IotDataDto iotDataDto = new IotDataDto();
        iotDataDto.setData(reqJson);
        iotDataDto.setIotApiCode("listMachineInoutBmoImpl");
        ResultVo resultVo = iotInnerServiceSMOImpl.postIotData(iotDataDto);
        
        // 初始化统计变量
        int carInCount = 0;
        int carOutCount = 0;
        int personInCount = 0;
        
        // 处理IoT服务返回结果
        if(resultVo.getCode() == ResultVo.CODE_OK){
            JSONObject paramOut = BeanConvertUtil.beanCovertJson(resultVo.getData());
            carInCount = paramOut.getIntValue("carInCount");
            carOutCount = paramOut.getIntValue("carOutCount");
            personInCount = paramOut.getIntValue("personInCount");
        }
        
        // 统计进场车辆数
        data = new JSONObject();
        data.put("name","进场车辆数");
        data.put("value", carInCount);
        datas.add(data);

        // 统计出场车辆数
        data = new JSONObject();
        data.put("name","出场车辆数");
        data.put("value", carOutCount);
        datas.add(data);

        // 统计进场人员数
        data = new JSONObject();
        data.put("name","进场人员数");
        data.put("value", personInCount);
        datas.add(data);

        // 统计人脸同步数
        double personFaceToMachine = inoutStatisticsImpl.getPersonFaceToMachineCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","人脸同步数");
        data.put("value", personFaceToMachine);
        datas.add(data);

        // 统计采购入库数
        double purchaseInCount = inoutStatisticsImpl.purchaseInCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","采购入库数");
        data.put("value", purchaseInCount);
        datas.add(data);

        // 统计领用出库数
        double purchaseOutCount = inoutStatisticsImpl.purchaseOutCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","领用出库数");
        data.put("value", purchaseOutCount);
        datas.add(data);

        // 统计采购入库金额
        double purchaseInAmount = inoutStatisticsImpl.purchaseInAmount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","采购入库金额");
        data.put("value", purchaseInAmount);
        datas.add(data);

        // 统计领用出库金额
        double purchaseOutAmount = inoutStatisticsImpl.purchaseOutAmount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","完成巡检");
        data.put("value", purchaseOutAmount);
        datas.add(data);

        // 统计调拨数量
        double allocationCount = inoutStatisticsImpl.allocationCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","调拨数量");
        data.put("value", allocationCount);
        datas.add(data);

        // 统计房屋装修数
        double roomRenovationCount = inoutStatisticsImpl.roomRenovationCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","房屋装修数");
        data.put("value", roomRenovationCount);
        datas.add(data);

        // 统计物品放行数量
        double itemReleaseCount = inoutStatisticsImpl.itemReleaseCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","物品放行");
        data.put("value", itemReleaseCount);
        datas.add(data);

        // 统计交房数量
        double roomInCount = inoutStatisticsImpl.roomInCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","交房数量");
        data.put("value", roomInCount);
        datas.add(data);

        // 统计退房数量
        double roomOutCount = inoutStatisticsImpl.roomOutCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","退房数量");
        data.put("value", roomOutCount);
        datas.add(data);

        // 统计业主绑定数量
        double ownerRegisterCount = inoutStatisticsImpl.ownerRegisterCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","业主绑定");
        data.put("value", ownerRegisterCount);
        datas.add(data);

        // 统计未考勤数
        double noAttendanceCount = inoutStatisticsImpl.noAttendanceCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","未考勤数");
        data.put("value", noAttendanceCount);
        datas.add(data);

        // 设置响应结果
        context.setResponseEntity(ResultVo.createResponseEntity(datas));
    }
}