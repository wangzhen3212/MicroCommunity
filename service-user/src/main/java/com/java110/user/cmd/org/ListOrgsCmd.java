/**
 * 组织架构查询命令类
 * 
 * 该类用于处理组织架构列表查询请求，支持按条件查询组织信息，
 * 包括权限验证、分页查询和数据转换等功能
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.user.cmd.org;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.privilege.BasePrivilegeDto;
import com.java110.dto.org.OrgDto;
import com.java110.dto.org.OrgStaffRelDto;
import com.java110.intf.community.IMenuInnerServiceSMO;
import com.java110.intf.user.IOrgInnerServiceSMO;
import com.java110.intf.user.IOrgStaffRelInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.api.org.ApiOrgDataVo;
import com.java110.vo.api.org.ApiOrgVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Java110Cmd(serviceCode = "org.listOrgs")
public class ListOrgsCmd extends Cmd {

    /** 组织架构内部服务接口 */
    @Autowired
    private IOrgInnerServiceSMO orgInnerServiceSMOImpl;

    /** 菜单权限内部服务接口 */
    @Autowired
    private IMenuInnerServiceSMO menuInnerServiceSMOImpl;

    /** 组织员工关系内部服务接口 */
    @Autowired
    private IOrgStaffRelInnerServiceSMO iOrgStaffRelInnerServiceSMO;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数的有效性，包括分页参数和必填字段校验
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求头信息
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 调用父类分页信息验证
        super.validatePageInfo(reqJson);
        
        // 如果请求参数中没有storeId，从请求头中获取并设置
        if (!reqJson.containsKey("storeId")) {
            String storeId = context.getReqHeaders().get("store-id");
            reqJson.put("storeId", storeId);
        }
        
        // 验证storeId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "storeId", "必填，请填写商户ID");
    }

    /**
     * 执行组织架构查询命令
     * 
     * 根据请求参数查询组织架构信息，支持权限控制和数据过滤，
     * 返回分页格式的组织架构数据
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为OrgDto对象
        OrgDto orgDto = BeanConvertUtil.covertBean(reqJson, OrgDto.class);
        
        // 处理二级组织架构的特殊逻辑
        if (reqJson.containsKey("orgLevel") && reqJson.getString("orgLevel").equals("2")) {
            // 检查用户是否有查看所有组织架构的权限
            BasePrivilegeDto basePrivilegeDto = new BasePrivilegeDto();
            basePrivilegeDto.setResource("/viewAllOrganization"); // 设置权限资源路径
            basePrivilegeDto.setUserId(reqJson.getString("userId")); // 设置用户ID
            List<Map> privileges = menuInnerServiceSMOImpl.checkUserHasResource(basePrivilegeDto);
            
            // 如果用户没有查看所有组织架构的权限
            if (privileges.size() == 0) {
                // 查询员工所属的二级组织架构信息
                OrgStaffRelDto orgStaffRelDto = new OrgStaffRelDto();
                orgStaffRelDto.setStaffId(reqJson.getString("userId")); // 设置员工ID
                List<OrgStaffRelDto> orgStaffRelDtos = iOrgStaffRelInnerServiceSMO.queryOrgInfoByStaffIds(orgStaffRelDto);
                
                // 如果查询到员工所属的组织信息
                if (orgStaffRelDtos.size() > 0) {
                    // 设置查询条件为员工所属的二级组织ID，实现数据过滤
                    orgDto.setOrgId(orgStaffRelDtos.get(0).getCompanyId());
                }
            }
        }

        // 查询组织架构总数
        int count = orgInnerServiceSMOImpl.queryOrgsCount(orgDto);

        List<ApiOrgDataVo> orgs = null;

        // 根据查询结果数量处理数据
        if (count > 0) {
            // 查询组织架构列表并转换为API响应对象
            orgs = BeanConvertUtil.covertBeanList(orgInnerServiceSMOImpl.queryOrgs(orgDto), ApiOrgDataVo.class);
        } else {
            // 如果没有数据，返回空列表
            orgs = new ArrayList<>();
        }

        // 构建API响应对象
        ApiOrgVo apiOrgVo = new ApiOrgVo();
        apiOrgVo.setTotal(count); // 设置总记录数
        // 计算总页数：总记录数 / 每页记录数，向上取整
        apiOrgVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        apiOrgVo.setOrgs(orgs); // 设置组织架构数据列表

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiOrgVo), HttpStatus.OK);

        // 设置响应结果到上下文
        context.setResponseEntity(responseEntity);
    }
}