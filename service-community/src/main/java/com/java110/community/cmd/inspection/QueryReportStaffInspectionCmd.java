package com.java110.community.cmd.inspection;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.inspection.InspectionStaffDto;
import com.java110.intf.community.IInspectionPlanStaffV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

/**
 * 员工巡检情况统计命令类
 * 
 * 该类用于处理物业手机端员工巡检情况统计查询请求，方便管理人员查看员工的巡检完成情况
 * 通过查询指定小区和日期的巡检数据，统计每个员工的已巡检和未巡检数量
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023
 */
@Java110CmdDoc(title = "员工巡检情况统计",
        description = "物业手机端员工巡检情况统计，方便老板查看",
        httpMethod = "get",
        url = "http://{ip}:{port}/app/inspection.queryReportStaffInspection",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "inspection.queryReportStaffInspection"
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区信息"),
        @Java110ParamDoc(name = "queryTime", length = 30, remark = "查询日期 YYYY-MM-DD"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
                @Java110ParamDoc(name = "data", type = "Object", remark = "有效数据"),
                @Java110ParamDoc(parentNodeName = "data",name = "staffName", type = "String", remark = "员工名称"),
                @Java110ParamDoc(parentNodeName = "data",name = "finishCount", type = "String", remark = "已巡检"),
                @Java110ParamDoc(parentNodeName = "data",name = "waitCount", type = "String", remark = "未巡检"),
        }
)

@Java110ExampleDoc(
        reqBody="http://{ip}:{port}/app/inspection.queryReportStaffInspection?communityId=12323&queryTime=2022-11-11",
        resBody="{'code':0,'msg':'成功','data':{'staffName':'123123','finishCount':'123213','waitCount':123123}}"
)

@Java110Cmd(serviceCode = "inspection.queryReportStaffInspection")
public class QueryReportStaffInspectionCmd extends Cmd{

    /**
     * 巡检计划员工服务接口，用于查询员工巡检统计数据
     */
    @Autowired
    private IInspectionPlanStaffV1InnerServiceSMO inspectionPlanStaffV1InnerServiceSMOImpl;
    
    /**
     * 请求参数验证方法
     * 
     * 验证请求中是否包含必要的参数：小区ID和查询日期
     * 如果缺少必要参数，会抛出CmdException异常
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求中必须包含小区ID参数
        Assert.hasKeyAndValue(reqJson,"communityId","未包含小区信息");
        // 验证请求中必须包含查询日期参数
        Assert.hasKeyAndValue(reqJson,"queryTime","未包含查询日期");
    }

    /**
     * 执行员工巡检统计查询命令
     * 
     * 将请求参数转换为数据传输对象，调用服务层查询员工巡检统计数据，
     * 并将查询结果设置到响应中返回给前端
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当日期解析错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求JSON参数转换为InspectionStaffDto对象
        // 使用BeanConvertUtil工具类进行对象属性映射
        InspectionStaffDto inspectionStaffDto = BeanConvertUtil.covertBean(reqJson, InspectionStaffDto.class);

        // 调用服务层接口，查询员工巡检统计报告
        // 根据小区ID和查询日期获取员工的巡检完成情况
        List<InspectionStaffDto> inspectionStaffDtos = inspectionPlanStaffV1InnerServiceSMOImpl.queryStaffInspectionReport(inspectionStaffDto);

        // 将查询结果封装成标准响应格式并设置到上下文中
        // ResultVo.createResponseEntity方法会将数据包装成{code:0, msg:"成功", data:[]}格式
        context.setResponseEntity(ResultVo.createResponseEntity(inspectionStaffDtos));
    }
}