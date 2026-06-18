/**
 * 巡检计划员工管理命令类
 * 
 * 该类用于处理巡检计划员工列表查询的后台管理请求，主要功能包括：
 * 1. 验证分页参数和管理员权限
 * 2. 查询巡检计划员工数据
 * 3. 封装返回结果给前端
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
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

@Java110Cmd(serviceCode = "inspection.listAdminInspectionPlanStaffs")
public class ListAdminInspectionPlanStaffsCmd extends Cmd {

    /**
     * 巡检计划员工内部服务接口
     */
    @Autowired
    private IInspectionPlanStaffInnerServiceSMO inspectionPlanStaffInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 该方法用于验证请求参数的合法性，包括：
     * 1. 分页参数验证
     * 2. 管理员权限验证
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证分页参数（页码、每页条数等）
        super.validatePageInfo(reqJson);
        // 验证管理员权限
        super.validateAdmin(context);
    }

    /**
     * 执行命令方法
     * 
     * 该方法处理巡检计划员工列表查询的核心业务逻辑：
     * 1. 转换请求参数为DTO对象
     * 2. 查询总记录数
     * 3. 根据分页查询数据列表
     * 4. 封装返回结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当业务处理过程中出现错误时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为巡检计划员工DTO对象
        InspectionPlanStaffDto inspectionPlanStaffDto = BeanConvertUtil.covertBean(reqJson, InspectionPlanStaffDto.class);

        // 查询符合条件的巡检计划员工总记录数
        int count = inspectionPlanStaffInnerServiceSMOImpl.queryInspectionPlanStaffsCount(inspectionPlanStaffDto);

        // 巡检计划员工数据列表
        List<ApiInspectionPlanStaffDataVo> inspectionPlanStaffs = null;

        // 根据查询结果数量决定是否查询详细数据
        if (count > 0) {
            // 查询巡检计划员工详细数据并转换为VO对象列表
            inspectionPlanStaffs = BeanConvertUtil.covertBeanList(
                inspectionPlanStaffInnerServiceSMOImpl.queryInspectionPlanStaffs(inspectionPlanStaffDto), 
                ApiInspectionPlanStaffDataVo.class
            );
        } else {
            // 如果没有数据，返回空列表
            inspectionPlanStaffs = new ArrayList<>();
        }

        // 创建返回给前端的VO对象
        ApiInspectionPlanStaffVo apiInspectionPlanStaffVo = new ApiInspectionPlanStaffVo();

        // 设置总记录数
        apiInspectionPlanStaffVo.setTotal(count);
        // 计算总页数：总记录数 / 每页条数，向上取整
        apiInspectionPlanStaffVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        // 设置巡检计划员工数据列表
        apiInspectionPlanStaffVo.setInspectionPlanStaffs(inspectionPlanStaffs);

        // 创建HTTP响应实体，将VO对象转换为JSON字符串返回
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(
            JSONObject.toJSONString(apiInspectionPlanStaffVo), 
            HttpStatus.OK
        );

        // 设置响应实体到上下文
        context.setResponseEntity(responseEntity);
    }
}