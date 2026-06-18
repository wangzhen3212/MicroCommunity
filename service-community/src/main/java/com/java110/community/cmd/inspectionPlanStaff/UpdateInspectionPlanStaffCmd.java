/**
 * 巡检计划工作人员更新命令类
 * 
 * 该类负责处理更新巡检计划工作人员信息的业务逻辑，包括参数验证和数据更新操作。
 * 通过注解@Java110Cmd声明服务代码，与前端请求进行映射。
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.community.cmd.inspectionPlanStaff;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.inspection.InspectionPlanStaffDto;
import com.java110.intf.community.IInspectionPlanStaffInnerServiceSMO;
import com.java110.intf.community.IInspectionPlanStaffV1InnerServiceSMO;
import com.java110.po.inspection.InspectionPlanStaffPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Java110Cmd(serviceCode = "inspectionPlanStaff.updateInspectionPlanStaff")
public class UpdateInspectionPlanStaffCmd extends Cmd {

    /**
     * 巡检计划工作人员V1版本内部服务接口
     */
    @Autowired
    private IInspectionPlanStaffV1InnerServiceSMO inspectionPlanStaffV1InnerServiceSMOImpl;

    /**
     * 巡检计划工作人员内部服务接口
     */
    @Autowired
    private IInspectionPlanStaffInnerServiceSMO inspectionPlanStaffInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证更新巡检计划工作人员请求中的必需参数是否完整，
     * 确保后续业务逻辑能够正常执行。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证必需参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "ipStaffId", "ipStaffId不能为空");
        Assert.hasKeyAndValue(reqJson, "staffId", "请求报文中未包含staffId");
        Assert.hasKeyAndValue(reqJson, "staffName", "请求报文中未包含staffName");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        Assert.hasKeyAndValue(reqJson, "startTime", "请求报文中未包含startTime");
        Assert.hasKeyAndValue(reqJson, "endTime", "请求报文中未包含endTime");
        Assert.hasKeyAndValue(reqJson, "inspectionPlanId", "请求报文中未包含inspectionPlanId");
    }

    /**
     * 执行更新巡检计划工作人员命令
     * 
     * 根据请求参数更新指定的巡检计划工作人员信息，
     * 包括查询现有数据、合并更新数据、执行更新操作等步骤。
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 包含更新数据的请求JSON对象
     * @throws CmdException 当更新操作失败或数据不存在时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 创建查询条件对象，用于查询现有的巡检计划工作人员信息
        InspectionPlanStaffDto inspectionPlanStaffDto = new InspectionPlanStaffDto();
        inspectionPlanStaffDto.setIpStaffId(reqJson.getString("ipStaffId"));
        inspectionPlanStaffDto.setCommunityId(reqJson.getString("communityId"));
        
        // 根据条件查询巡检计划工作人员列表
        List<InspectionPlanStaffDto> inspectionPlanStaffDtos = inspectionPlanStaffInnerServiceSMOImpl.queryInspectionPlanStaffs(inspectionPlanStaffDto);

        // 验证查询结果，确保只存在一条匹配记录
        Assert.listOnlyOne(inspectionPlanStaffDtos, "未找到需要修改的活动 或多条数据");

        // 创建业务数据对象，合并现有数据和更新数据
        JSONObject businessInspectionPlanStaff = new JSONObject();
        // 将查询到的现有数据转换为Map并放入业务对象
        businessInspectionPlanStaff.putAll(BeanConvertUtil.beanCovertMap(inspectionPlanStaffDtos.get(0)));
        // 将请求中的更新数据合并到业务对象中
        businessInspectionPlanStaff.putAll(reqJson);
        
        // 将合并后的业务数据转换为持久化对象
        InspectionPlanStaffPo inspectionPlanStaffPo = BeanConvertUtil.covertBean(businessInspectionPlanStaff, InspectionPlanStaffPo.class);

        // 执行更新操作，返回受影响的行数
        int flag = inspectionPlanStaffV1InnerServiceSMOImpl.updateInspectionPlanStaff(inspectionPlanStaffPo);
        
        // 检查更新结果，如果更新失败则抛出异常
        if (flag < 1) {
            throw new CmdException("修改巡检师傅失败");
        }
    }
}