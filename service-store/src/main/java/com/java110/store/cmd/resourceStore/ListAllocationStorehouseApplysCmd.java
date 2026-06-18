package com.java110.store.cmd.resourceStore;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.purchase.AllocationStorehouseApplyDto;
import com.java110.dto.privilege.BasePrivilegeDto;
import com.java110.intf.community.IMenuInnerServiceSMO;
import com.java110.intf.store.IAllocationStorehouseApplyInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 调拨记录查询命令类
 * 
 * 该类负责处理资源库调拨记录的查询请求，包括分页查询、权限控制等功能
 * 通过注解@Java110Cmd声明服务代码，用于路由到具体的命令处理
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "resourceStore.listAllocationStorehouseApplys")
public class ListAllocationStorehouseApplysCmd extends Cmd {

    /**
     * 调拨记录内部服务接口，用于查询调拨记录数据
     */
    @Autowired
    private IAllocationStorehouseApplyInnerServiceSMO allocationStorehouseApplyInnerServiceSMOImpl;

    /**
     * 菜单内部服务接口，用于权限验证
     */
    @Autowired
    private IMenuInnerServiceSMO menuInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，主要调用父类的分页信息验证方法
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置上下文数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 调用父类方法验证分页参数（如页码、每页条数等）
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 
     * 处理调拨记录的查询请求，包括权限控制、数据查询和结果返回
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当数据解析失败时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求JSON转换为调拨记录DTO对象
        AllocationStorehouseApplyDto allocationStorehouseApplyDto = BeanConvertUtil.covertBean(reqJson, AllocationStorehouseApplyDto.class);
        
        // 执行权限控制逻辑，根据用户权限过滤数据
        privilegeControl(context, allocationStorehouseApplyDto);
        
        // 查询符合条件的调拨记录总数
        int count = allocationStorehouseApplyInnerServiceSMOImpl.queryAllocationStorehouseApplysCount(allocationStorehouseApplyDto);

        List<AllocationStorehouseApplyDto> allocationStorehouseApplyDtos = null;

        // 根据查询结果数量决定如何处理
        if (count > 0) {
            // 有数据时查询具体的调拨记录列表
            allocationStorehouseApplyDtos = allocationStorehouseApplyInnerServiceSMOImpl.queryAllocationStorehouseApplys(allocationStorehouseApplyDto);
        } else {
            // 无数据时返回空列表
            allocationStorehouseApplyDtos = new ArrayList<>();
        }

        // 构建返回结果对象，包含分页信息和数据列表
        // 计算总页数：总记录数 / 每页条数，向上取整
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, allocationStorehouseApplyDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应设置到上下文中
        context.setResponseEntity(responseEntity);
    }

    /**
     * 权限控制方法
     * 
     * 根据用户权限控制数据访问范围：
     * - 有查看所有调拨记录权限的用户可以看到所有数据
     * - 无权限的用户只能看到自己创建的调拨记录
     * 
     * @param context 命令数据流上下文，用于获取用户信息
     * @param allocationStorehouseApplyDto 调拨记录查询条件DTO对象
     */
    private void privilegeControl(ICmdDataFlowContext context, AllocationStorehouseApplyDto allocationStorehouseApplyDto) {
        // 如果查询条件中包含申请ID，说明是查询单个记录，不做权限控制
        if (!StringUtil.isEmpty(allocationStorehouseApplyDto.getApplyId())) {
            return;
        }
        
        // 从上下文中获取当前用户ID
        String userId = CmdContextUtils.getUserId(context);
        
        // 构建权限验证对象
        BasePrivilegeDto basePrivilegeDto = new BasePrivilegeDto();
        // 设置需要验证的资源路径
        basePrivilegeDto.setResource("/viewlistAllocationStorehouses");
        // 设置用户ID
        basePrivilegeDto.setUserId(userId);
        
        // 检查用户是否具有查看所有调拨记录的权限
        List<Map> privileges = menuInnerServiceSMOImpl.checkUserHasResource(basePrivilegeDto);
        
        // 如果用户没有权限，则设置只能查看自己创建的记录
        if (privileges.size() == 0) {
            allocationStorehouseApplyDto.setStartUserId(userId);
        }
    }
}