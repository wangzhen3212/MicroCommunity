/**
 * 角色待分配小区查询命令类
 * 
 * 功能：查询指定员工尚未分配的小区列表，用于角色权限管理中的小区分配功能
 * 通过对比已分配小区和所有可用小区，返回待分配的小区列表
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.user.cmd.role;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.PageDto;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.privilege.RoleCommunityDto;
import com.java110.dto.staffCommunity.StaffCommunityDto;
import com.java110.intf.community.ICommunityInnerServiceSMO;
import com.java110.intf.user.IRoleCommunityV1InnerServiceSMO;
import com.java110.intf.user.IStaffCommunityV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import com.java110.vo.api.community.ApiCommunityDataVo;
import com.java110.vo.api.community.ApiCommunityVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "role.listWaitStaffCommunity")
public class ListWaitStaffCommunityCmd extends Cmd {

    /**
     * 员工小区关系服务接口
     */
    @Autowired
    private IStaffCommunityV1InnerServiceSMO staffCommunityV1InnerServiceSMOImpl;

    /**
     * 小区信息服务接口
     */
    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，确保必要的参数存在且格式正确
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 调用父类的分页信息验证方法
        super.validatePageInfo(reqJson);

        // 验证staffId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "staffId", "必填，请填写角色");
    }

    /**
     * 命令执行方法
     * 
     * 主要逻辑：查询指定员工尚未分配的小区列表
     * 1. 获取员工已分配的小区列表
     * 2. 排除已分配的小区，查询剩余可用小区
     * 3. 返回分页的待分配小区列表
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 从上下文中获取商户ID
        String storeId = CmdContextUtils.getStoreId(context);
        
        // 构建员工小区查询条件
        StaffCommunityDto staffCommunityDto = new StaffCommunityDto();
        staffCommunityDto.setStaffId(reqJson.getString("staffId"));  // 设置员工ID
        staffCommunityDto.setStoreId(storeId);  // 设置商户ID
        staffCommunityDto.setPage(PageDto.DEFAULT_PAGE);  // 设置默认页码
        
        // 查询员工已分配的小区列表
        List<StaffCommunityDto> staffCommunityDtos = staffCommunityV1InnerServiceSMOImpl.queryStaffCommunitys(staffCommunityDto);
        
        // 收集已分配小区的ID列表
        List<String> communityIds = new ArrayList<>();
        for (StaffCommunityDto tmpStaffCommunityDto : staffCommunityDtos) {
            communityIds.add(tmpStaffCommunityDto.getCommunityId());
        }
        
        // 构建小区查询条件，排除已分配的小区
        CommunityDto communityDto = new CommunityDto();
        communityDto.setStoreId(storeId);  // 设置商户ID
        
        // 如果员工已有分配的小区，则排除这些小区
        if (!ListUtil.isNull(communityIds)) {
            communityDto.setNotInCommunityId(communityIds.toArray(new String[communityIds.size()]));
        }
        
        // 设置查询条件：审核状态为通过(1100)，成员ID为商户ID
        communityDto.setAuditStatusCd("1100");
        communityDto.setMemberId(storeId);
        
        // 设置分页参数
        communityDto.setPage(reqJson.getIntValue("page"));
        communityDto.setRow(reqJson.getIntValue("row"));
        
        // 查询符合条件的待分配小区总数
        int count = communityInnerServiceSMOImpl.queryCommunitysCount(communityDto);

        List<CommunityDto> communitys = null;

        // 根据查询结果数量决定返回的数据
        if (count > 0) {
            // 查询待分配小区列表
            communitys = communityInnerServiceSMOImpl.queryCommunitys(communityDto);
        } else {
            // 如果没有数据，返回空列表
            communitys = new ArrayList<>();
        }

        // 构建返回结果对象，包含分页信息和数据
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, communitys);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应到上下文
        context.setResponseEntity(responseEntity);
    }
}