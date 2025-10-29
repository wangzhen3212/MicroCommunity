/**
 * 员工已完成报修工单查询命令类
 * 
 * 该命令类用于处理员工已完成的报修工单列表查询请求，支持权限控制和状态筛选功能。
 * 根据系统配置和用户权限，决定是否显示所有工单或仅显示当前员工处理的工单。
 * 
 * @author Java110
 * @version 1.0
 * @since 2021-09-09
 */
package com.java110.community.cmd.ownerRepair;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.CommunitySettingFactory;
import com.java110.dto.privilege.BasePrivilegeDto;
import com.java110.dto.repair.RepairDto;
import com.java110.intf.community.IMenuInnerServiceSMO;
import com.java110.intf.community.IRepairInnerServiceSMO;
import com.java110.intf.community.IRepairUserInnerServiceSMO;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Java110Cmd(serviceCode = "ownerRepair.listStaffFinishRepairs")
public class ListStaffFinishRepairsCmd extends Cmd {

    @Autowired
    private IRepairInnerServiceSMO repairInnerServiceSMOImpl;

    @Autowired
    private IRepairUserInnerServiceSMO repairUserInnerServiceSMOImpl;

    @Autowired
    private IMenuInnerServiceSMO menuInnerServiceSMOImpl;

    // 公共域常量
    public static final String DOMAIN_COMMON = "DOMAIN.COMMON";

    // 员工查看报修列表配置项键名
    public static final String VIEW_LIST_STAFF_REPAIRS = "VIEW_LIST_STAFF_REPAIRS";

    /**
     * 请求参数验证方法
     * 
     * 验证请求中必须包含小区ID和员工ID信息，同时验证分页参数
     * 
     * @param event   命令事件对象
     * @param context 数据流上下文对象，包含请求和响应数据
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 调用父类方法验证分页信息
        super.validatePageInfo(reqJson);
        // 验证请求中必须包含小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "请求中未包含小区ID");
        // 验证请求中必须包含员工ID
        Assert.hasKeyAndValue(reqJson, "userId", "请求中未包含员工信息");
    }

    /**
     * 执行员工已完成报修工单查询命令
     * 
     * 根据系统配置和用户权限查询员工已完成的报修工单列表，支持状态筛选和分页功能
     * 
     * @param event   命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求参数JSON对象，包含查询条件和分页信息
     * @throws CmdException    命令执行异常
     * @throws ParseException  数据解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求参数转换为RepairDto对象
        RepairDto ownerRepairDto = BeanConvertUtil.covertBean(reqJson, RepairDto.class);
        String userId = reqJson.getString("userId");
        
        // 获取小区级别的配置，是否允许管理员查看所有工单
        String viewListStaffRepairs = CommunitySettingFactory.getValue(reqJson.getString("communityId"), VIEW_LIST_STAFF_REPAIRS);
        if (StringUtil.isEmpty(viewListStaffRepairs)) {
            // 如果小区配置为空，则从系统默认配置中获取
            viewListStaffRepairs = MappingCache.getValue(MappingConstant.REPAIR_DOMAIN, VIEW_LIST_STAFF_REPAIRS);
        }
        
        List<Map> privileges = null;
        // 这里加开关 其实让管理员看到所有单子这么做，不太优雅，建议 单独开发页面功能
        // 不要影响已办功能，add by 吴学文 2021-09-09
        // 检查是否开启管理员查看所有工单功能
        if ("ON".equals(viewListStaffRepairs)) {
            // 检查用户是否具有查看所有工单的权限
            BasePrivilegeDto basePrivilegeDto = new BasePrivilegeDto();
            basePrivilegeDto.setResource("/listStaffFinishRepairs");
            basePrivilegeDto.setUserId(userId);
            privileges = menuInnerServiceSMOImpl.checkUserHasResource(basePrivilegeDto);
        }
        
        // 如果用户没有权限或权限检查未开启，则只查询当前员工处理的工单
        if (privileges == null || privileges.size() == 0) {
            ownerRepairDto.setStaffId(reqJson.getString("userId"));
        }
        
        // 处理报修状态筛选条件
        if (reqJson.containsKey("repairStates")) {
            // 如果请求中包含状态参数，使用请求的状态列表
            String[] states = reqJson.getString("repairStates").split(",");
            ownerRepairDto.setStates(Arrays.asList(states));
        } else {
            // 默认查询PC WEB维修已办的状态列表
            // 包括：退回、转单、支付中、支付异常、待评价、待回访、已完成等状态
            String[] states = {RepairDto.STATE_BACK, RepairDto.STATE_TRANSFER, RepairDto.STATE_PAY, 
                             RepairDto.STATE_PAY_ERROR, RepairDto.STATE_APPRAISE, 
                             RepairDto.STATE_RETURN_VISIT, RepairDto.STATE_COMPLATE};
            ownerRepairDto.setStates(Arrays.asList(states));
        }
        
        // 查询符合条件的工单总数
        int count = repairInnerServiceSMOImpl.queryStaffFinishRepairsCount(ownerRepairDto);
        List<RepairDto> ownerRepairs = null;
        
        if (count > 0) {
            // 如果有数据，查询工单列表
            ownerRepairs = repairInnerServiceSMOImpl.queryStaffFinishRepairs(ownerRepairDto);
            // 刷新员工姓名信息（当前注释掉的方法）
            // refreshStaffName(ownerRepairs);
        } else {
            // 如果没有数据，返回空列表
            ownerRepairs = new ArrayList<>();
        }
        
        // 构建响应结果，包含分页信息和数据列表
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(
            (int) Math.ceil((double) count / (double) reqJson.getInteger("row")), // 计算总页数
            count, // 总记录数
            ownerRepairs // 数据列表
        );
        context.setResponseEntity(responseEntity);
    }
}