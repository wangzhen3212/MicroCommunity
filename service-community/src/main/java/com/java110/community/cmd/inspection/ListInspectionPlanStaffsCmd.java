package com.java110.community.cmd.inspection;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.inspection.InspectionPlanStaffDto;
import com.java110.intf.community.IInspectionPlanStaffInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.api.inspectionPlanStaff.ApiInspectionPlanStaffDataVo;
import com.java110.vo.api.inspectionPlanStaff.ApiInspectionPlanStaffVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 巡检计划员工列表查询命令类
 * 
 * 该类负责处理巡检计划员工列表查询请求，包括参数验证和数据查询返回
 * 通过服务码 inspection.listInspectionPlanStaffs 标识对应的业务处理
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "inspection.listInspectionPlanStaffs")
public class ListInspectionPlanStaffsCmd extends Cmd {

    /**
     * 巡检计划员工内部服务接口
     */
    @Autowired
    private IInspectionPlanStaffInnerServiceSMO inspectionPlanStaffInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数的有效性，包括分页信息和必要字段的检查
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证分页信息
        super.validatePageInfo(reqJson);
        // 验证属性信息
        super.validateProperty(context);
        // 验证小区ID不能为空
        Assert.hasKeyAndValue(reqJson, "communityId", "小区信息不能为空");
    }

    /**
     * 命令执行方法
     * 
     * 处理巡检计划员工列表查询业务逻辑，包括数据查询、结果封装和响应返回
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为巡检计划员工数据传输对象
        InspectionPlanStaffDto inspectionPlanStaffDto = BeanConvertUtil.covertBean(reqJson, InspectionPlanStaffDto.class);

        // 查询巡检计划员工总数
        int count = inspectionPlanStaffInnerServiceSMOImpl.queryInspectionPlanStaffsCount(inspectionPlanStaffDto);

        // 巡检计划员工数据列表
        List<ApiInspectionPlanStaffDataVo> inspectionPlanStaffs = null;

        // 根据查询结果数量决定如何处理数据
        if (count > 0) {
            // 查询到数据时，将查询结果转换为API展示用的数据对象列表
            inspectionPlanStaffs = BeanConvertUtil.covertBeanList(
                inspectionPlanStaffInnerServiceSMOImpl.queryInspectionPlanStaffs(inspectionPlanStaffDto), 
                ApiInspectionPlanStaffDataVo.class
            );
        } else {
            // 没有查询到数据时，返回空列表
            inspectionPlanStaffs = new ArrayList<>();
        }

        // 创建API响应对象
        ApiInspectionPlanStaffVo apiInspectionPlanStaffVo = new ApiInspectionPlanStaffVo();

        // 设置总记录数
        apiInspectionPlanStaffVo.setTotal(count);
        // 计算总页数：总记录数 / 每页记录数，向上取整
        apiInspectionPlanStaffVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        // 设置巡检计划员工数据列表
        apiInspectionPlanStaffVo.setInspectionPlanStaffs(inspectionPlanStaffs);

        // 创建HTTP响应实体，将响应对象转换为JSON字符串
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(
            JSONObject.toJSONString(apiInspectionPlanStaffVo), 
            HttpStatus.OK
        );

        // 设置响应实体到上下文
        context.setResponseEntity(responseEntity);
    }
}