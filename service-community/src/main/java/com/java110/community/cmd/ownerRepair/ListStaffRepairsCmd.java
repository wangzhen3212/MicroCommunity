/**
 * 员工报修工单查询命令类
 * 
 * 该命令类用于处理员工查看报修工单列表的请求，根据用户权限和系统配置决定是否显示所有工单
 * 还是仅显示分配给当前员工的工单。支持分页查询和权限控制。
 * 
 * @author Java110
 * @version 1.0
 * @see Cmd
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
import java.util.List;
import java.util.Map;

@Java110Cmd(serviceCode = "ownerRepair.listStaffRepairs")
public class ListStaffRepairsCmd extends Cmd {

    /**
     * 报修工单服务接口
     */
    @Autowired
    private IRepairInnerServiceSMO repairInnerServiceSMOImpl;

    /**
     * 菜单权限服务接口
     */
    @Autowired
    private IMenuInnerServiceSMO menuInnerServiceSMOImpl;

    /**
     * 公共域常量
     */
    public static final String DOMAIN_COMMON = "DOMAIN.COMMON";

    /**
     * 查看员工报修工单列表的配置键名
     */
    public static final String VIEW_LIST_STAFF_REPAIRS = "VIEW_LIST_STAFF_REPAIRS";

    /**
     * 请求参数验证方法
     * 
     * 验证请求中必须包含小区ID和用户ID参数，同时验证分页信息
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 数据流上下文对象，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 调用父类方法验证分页信息
        super.validatePageInfo(reqJson);
        // 验证请求中必须包含小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "请求中未包含小区ID");
        // 验证请求中必须包含员工信息
        Assert.hasKeyAndValue(reqJson, "userId", "请求中未包含员工信息");
    }

    /**
     * 执行员工报修工单查询命令
     * 
     * 根据系统配置和用户权限查询报修工单列表，支持分页显示。
     * 如果配置为允许查看所有工单且用户有相应权限，则显示所有工单；
     * 否则只显示分配给当前员工的工单。
     * 
     * @param event 命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当数据解析错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求参数转换为RepairDto对象
        RepairDto ownerRepairDto = BeanConvertUtil.covertBean(reqJson, RepairDto.class);
        
        // 获取用户ID
        String userId = reqJson.getString("userId");
        
        // 从小区设置中获取查看员工报修工单的配置值
        String viewListStaffRepairs = CommunitySettingFactory.getValue(reqJson.getString("communityId"), VIEW_LIST_STAFF_REPAIRS);
        
        // 如果小区设置中没有配置，则从全局映射缓存中获取默认值
        if (StringUtil.isEmpty(viewListStaffRepairs)) {
            viewListStaffRepairs = MappingCache.getValue(MappingConstant.REPAIR_DOMAIN, VIEW_LIST_STAFF_REPAIRS);
        }
        
        List<Map> privileges = null;
        
        // 判断是否允许管理员查看所有工单
        if ("ON".equals(viewListStaffRepairs)) {
            // 配置为ON时，检查用户是否有查看所有工单的权限
            BasePrivilegeDto basePrivilegeDto = new BasePrivilegeDto();
            basePrivilegeDto.setResource("/viewListStaffRepairs"); // 设置权限资源路径
            basePrivilegeDto.setUserId(userId); // 设置用户ID
            // 检查用户是否拥有指定资源的权限
            privileges = menuInnerServiceSMOImpl.checkUserHasResource(basePrivilegeDto);
        } else {
            // 配置不为ON时，只查询当前员工的工单
            ownerRepairDto.setStaffId(userId);
        }
        
        // 如果用户没有查看所有工单的权限，则只查询当前员工的工单
        if (privileges == null || privileges.size() == 0) {
            ownerRepairDto.setStaffId(userId);
        }
        
        // 查询符合条件的工单总数
        int count = repairInnerServiceSMOImpl.queryStaffRepairsCount(ownerRepairDto);
        
        List<RepairDto> ownerRepairs = null;
        
        // 如果有符合条件的工单，则查询工单列表
        if (count > 0) {
            ownerRepairs = repairInnerServiceSMOImpl.queryStaffRepairs(ownerRepairDto);
        } else {
            // 没有符合条件的工单时返回空列表
            ownerRepairs = new ArrayList<>();
        }
        
        // 创建响应实体，包含分页信息和工单列表
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(
            (int) Math.ceil((double) count / (double) reqJson.getInteger("row")), // 计算总页数
            count, // 总记录数
            ownerRepairs // 工单列表数据
        );
        
        // 设置响应实体到上下文
        context.setResponseEntity(responseEntity);
    }
}