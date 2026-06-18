/**
 * 资源调拨记录查询命令类
 * 
 * 该类负责处理资源调拨记录的列表查询请求，包括权限验证和数据过滤
 * 通过注解@Java110Cmd声明服务代码为"resourceStore.listAllocationStorehouses"
 * 主要功能包括查询调拨记录列表、权限验证、数据分页等
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.store.cmd.resourceStore;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.purchase.AllocationStorehouseDto;
import com.java110.dto.privilege.BasePrivilegeDto;
import com.java110.intf.community.IMenuInnerServiceSMO;
import com.java110.intf.store.IAllocationStorehouseInnerServiceSMO;
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

@Java110Cmd(serviceCode = "resourceStore.listAllocationStorehouses")
public class ListAllocationStorehousesCmd extends Cmd {

    /**
     * 调拨记录内部服务接口
     */
    @Autowired
    private IAllocationStorehouseInnerServiceSMO allocationStorehouseInnerServiceSMOImpl;

    /**
     * 菜单权限内部服务接口
     */
    @Autowired
    private IMenuInnerServiceSMO menuInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 主要验证分页参数的正确性，调用父类的分页信息验证方法
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置上下文信息
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 调用父类方法验证分页参数（如页码、每页条数等）
        super.validatePageInfo(reqJson);
    }

    /**
     * 执行调拨记录查询命令
     * 
     * 该方法处理调拨记录的查询逻辑，包括权限验证、数据过滤、分页查询等
     * 根据用户权限决定是否过滤数据，普通用户只能查看自己发起的调拨记录
     * 具有特定权限的用户可以查看所有调拨记录
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当数据解析失败时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从上下文中获取当前店铺ID
        String storeId = CmdContextUtils.getStoreId(context);
        
        // 将请求JSON转换为调拨记录DTO对象
        AllocationStorehouseDto allocationStorehouseDto = BeanConvertUtil.covertBean(reqJson, AllocationStorehouseDto.class);
        allocationStorehouseDto.setStoreId(storeId);
        
        // 获取请求中的用户ID
        String userId = reqJson.getString("userId");
        
        // 权限验证：检查用户是否具有查看所有调拨记录的权限
        BasePrivilegeDto basePrivilegeDto = new BasePrivilegeDto();
        basePrivilegeDto.setResource("/viewlistAllocationStorehouses"); // 设置资源路径
        basePrivilegeDto.setUserId(userId); // 设置用户ID
        List<Map> privileges = menuInnerServiceSMOImpl.checkUserHasResource(basePrivilegeDto);
        
        // 如果用户没有查看所有记录的权限，则只查询该用户发起的调拨记录
        if (privileges.size() == 0) {
            allocationStorehouseDto.setStartUserId(userId);
        }
        
        // 特殊处理：如果是通过调拨单ID查询详情，则不需要过滤用户ID
        // 这种情况用于调拨记录详情查看、调拨待办查看、调拨已办查看等场景
        if (!StringUtil.isEmpty(reqJson.getString("applyOrderId"))) {
            allocationStorehouseDto.setAsId(reqJson.getString("applyOrderId"));
            allocationStorehouseDto.setStartUserId(""); // 清空用户ID过滤条件
        }

        // 查询调拨记录总数，用于分页计算
        int count = allocationStorehouseInnerServiceSMOImpl.queryAllocationStorehousesCount(allocationStorehouseDto);

        List<AllocationStorehouseDto> allocationStorehouseDtos = null;

        // 根据查询结果数量决定返回数据
        if (count > 0) {
            // 查询调拨记录列表数据
            allocationStorehouseDtos = allocationStorehouseInnerServiceSMOImpl.queryAllocationStorehouses(allocationStorehouseDto);
        } else {
            // 如果没有数据，返回空列表
            allocationStorehouseDtos = new ArrayList<>();
        }

        // 构建返回结果对象，包含分页信息和数据列表
        // 计算总页数：总记录数 / 每页条数，向上取整
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, allocationStorehouseDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应设置到上下文中
        context.setResponseEntity(responseEntity);
    }
}