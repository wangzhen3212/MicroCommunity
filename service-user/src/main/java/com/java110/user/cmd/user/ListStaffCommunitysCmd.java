/**
 * 员工社区列表查询命令类
 * 
 * 该命令类用于处理员工可访问的社区列表查询请求，通过员工ID获取其有权限访问的社区信息
 * 实现了Cmd接口，遵循Java110框架的命令模式规范
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.user.cmd.user;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.privilege.RoleCommunityDto;
import com.java110.intf.user.IOrgV1InnerServiceSMO;
import com.java110.intf.user.IRoleCommunityV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import com.java110.vo.api.community.ApiCommunityDataVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "user.listStaffCommunitys")
public class ListStaffCommunitysCmd extends Cmd {

    /**
     * 角色社区服务接口，用于查询员工与社区的关联关系
     */
    @Autowired
    private IRoleCommunityV1InnerServiceSMO roleCommunityV1InnerServiceSMOImpl;

    /**
     * 组织架构服务接口
     */
    @Autowired
    private IOrgV1InnerServiceSMO orgV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求中是否包含必要的员工ID信息
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含staffId字段
        Assert.hasKeyAndValue(reqJson, "staffId", "未包含 员工信息");
    }

    /**
     * 命令执行方法
     * 
     * 根据员工ID查询其有权限访问的社区列表，并返回格式化后的结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        // 从请求头中获取商户ID
        String storeId = context.getReqHeaders().get("store-id");
        // 验证商户ID不能为空
        Assert.hasLength(storeId, "未包含商户信息");

        // 将请求参数转换为角色社区数据传输对象
        RoleCommunityDto orgCommunityDto = BeanConvertUtil.covertBean(reqJson, RoleCommunityDto.class);
        // 设置员工ID到查询条件中
        orgCommunityDto.setStaffId(reqJson.getString("staffId"));
        
        // 查询员工有权限的社区数量
        int count = roleCommunityV1InnerServiceSMOImpl.queryRoleCommunitysCount(orgCommunityDto);
        
        List<ApiCommunityDataVo> communitys = null;
        
        // 如果查询到有权限的社区
        if (count > 0) {
            // 获取员工有权限的社区详细信息列表
            List<RoleCommunityDto> roleCommunityDtos = roleCommunityV1InnerServiceSMOImpl.queryRoleCommunitys(orgCommunityDto);
            // 将角色社区DTO列表转换为API社区数据VO列表
            communitys = BeanConvertUtil.covertBeanList(roleCommunityDtos, ApiCommunityDataVo.class);
            
            // 双重循环匹配社区ID，设置社区名称到返回结果中
            for (RoleCommunityDto tmpOrgCommunityDto : roleCommunityDtos) {
                for (ApiCommunityDataVo tmpApiCommunityDataVo : communitys) {
                    // 当社区ID匹配时，设置社区名称
                    if (tmpOrgCommunityDto.getCommunityId().equals(tmpApiCommunityDataVo.getCommunityId())) {
                        tmpApiCommunityDataVo.setName(tmpOrgCommunityDto.getCommunityName());
                    }
                }
            }
        } else {
            // 如果没有查询到有权限的社区，返回空列表
            communitys = new ArrayList<>();
        }
        
        // 构造返回结果对象，包含总记录数和数据列表
        ResultVo resultVo = new ResultVo(1, communitys.size(), communitys);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应设置到上下文中
        context.setResponseEntity(responseEntity);

    }
}