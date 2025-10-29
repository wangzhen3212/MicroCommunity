/**
 * 角色管理命令类 - 查询员工社区列表
 * 
 * 该命令类用于处理查询员工社区信息的请求，支持分页查询和权限验证。
 * 通过调用员工社区服务接口获取数据，并返回标准格式的响应结果。
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.user.cmd.role;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.staffCommunity.StaffCommunityDto;
import com.java110.intf.user.IStaffCommunityV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "role.listAStaffCommunity")
public class ListAStaffCommunityCmd extends Cmd {
    
    /**
     * 员工社区服务接口，用于查询员工社区相关信息
     */
    @Autowired
    private IStaffCommunityV1InnerServiceSMO staffCommunityV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 该方法用于验证请求参数的合法性，包括分页参数验证和管理员权限验证
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置上下文信息
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页参数，确保分页信息完整有效
        super.validatePageInfo(reqJson);
        // 验证管理员权限，确保当前用户具有管理员权限
        super.validateAdmin(cmdDataFlowContext);
    }

    /**
     * 执行命令方法 - 查询员工社区列表
     * 
     * 该方法负责处理查询员工社区列表的核心业务逻辑，包括：
     * 1. 获取商户ID
     * 2. 构建查询条件
     * 3. 查询数据总数和列表
     * 4. 构建响应结果
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 从上下文中获取当前商户ID
        String storeId = CmdContextUtils.getStoreId(cmdDataFlowContext);
        
        // 将请求JSON转换为员工社区数据传输对象
        StaffCommunityDto staffCommunityDto = BeanConvertUtil.covertBean(reqJson, StaffCommunityDto.class);
        // 设置商户ID到查询条件中
        staffCommunityDto.setStoreId(storeId);
        
        // 查询符合条件的员工社区总数
        int count = staffCommunityV1InnerServiceSMOImpl.queryStaffCommunitysCount(staffCommunityDto);

        List<StaffCommunityDto> staffCommunityDtos = null;

        // 根据查询结果数量决定如何处理数据
        if (count > 0) {
            // 如果有数据，则查询具体的员工社区列表
            staffCommunityDtos = staffCommunityV1InnerServiceSMOImpl.queryStaffCommunitys(staffCommunityDto);
        } else {
            // 如果没有数据，返回空列表
            staffCommunityDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数 / 每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 构建返回结果对象，包含总页数、总记录数和数据列表
        ResultVo resultVo = new ResultVo(totalPage, count, staffCommunityDtos);

        // 创建HTTP响应实体，设置状态码为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应实体设置到数据流上下文中
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}