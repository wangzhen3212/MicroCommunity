/**
 * 角色管理命令类 - 查询待分配员工社区列表
 * 
 * 该命令类用于查询指定员工尚未关联的社区列表，主要用于角色权限分配时的社区选择功能
 * 通过对比员工已关联社区和系统中所有可用社区，返回待分配的社区列表
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
import com.java110.dto.PageDto;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.staffCommunity.StaffCommunityDto;
import com.java110.intf.community.ICommunityInnerServiceSMO;
import com.java110.intf.user.IStaffCommunityV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "role.listWaitAStaffCommunity")
public class ListWaitAStaffCommunityCmd extends Cmd {

    /**
     * 员工社区关联服务接口
     */
    @Autowired
    private IStaffCommunityV1InnerServiceSMO staffCommunityV1InnerServiceSMOImpl;

    /**
     * 社区信息服务接口
     */
    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数的有效性，包括分页信息验证、管理员权限验证和必要参数检查
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证管理员权限
        super.validateAdmin(context);
        // 验证必填参数staffId是否存在
        Assert.hasKeyAndValue(reqJson, "staffId", "必填，请填写角色");
    }

    /**
     * 执行命令主方法
     * 
     * 查询指定员工尚未关联的社区列表，返回分页结果
     * 逻辑流程：
     * 1. 查询员工已关联的社区列表
     * 2. 排除已关联社区，查询剩余的可用社区
     * 3. 构建分页响应结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 从上下文中获取店铺ID
        String storeId = CmdContextUtils.getStoreId(context);
        
        // 第一步：查询员工已关联的社区列表
        StaffCommunityDto staffCommunityDto = new StaffCommunityDto();
        staffCommunityDto.setStaffId(reqJson.getString("staffId")); // 设置员工ID
        staffCommunityDto.setStoreId(storeId); // 设置店铺ID
        staffCommunityDto.setPage(PageDto.DEFAULT_PAGE); // 设置默认分页
        
        // 调用服务查询员工社区关联关系
        List<StaffCommunityDto> staffCommunityDtos = staffCommunityV1InnerServiceSMOImpl.queryStaffCommunitys(staffCommunityDto);
        
        // 提取已关联社区的ID列表
        List<String> communityIds = new ArrayList<>();
        for (StaffCommunityDto tmpStaffCommunityDto : staffCommunityDtos) {
            communityIds.add(tmpStaffCommunityDto.getCommunityId());
        }
        
        // 第二步：查询排除已关联社区的可用社区列表
        CommunityDto communityDto = new CommunityDto();
        communityDto.setStoreId(storeId); // 设置店铺ID
        
        // 如果员工已有关联社区，则设置排除条件
        if (!ListUtil.isNull(communityIds)) {
            communityDto.setNotInCommunityId(communityIds.toArray(new String[communityIds.size()]));
        }
        
        // 设置查询条件：审核状态为通过(1100)
        communityDto.setAuditStatusCd("1100");
        // 设置分页参数
        communityDto.setPage(reqJson.getIntValue("page"));
        communityDto.setRow(reqJson.getIntValue("row"));
        
        // 查询符合条件的社区总数
        int count = communityInnerServiceSMOImpl.queryCommunitysCount(communityDto);

        List<CommunityDto> communitys = null;

        // 根据查询结果数量决定返回数据
        if (count > 0) {
            // 查询社区详情列表
            communitys = communityInnerServiceSMOImpl.queryCommunitys(communityDto);
        } else {
            // 无数据时返回空列表
            communitys = new ArrayList<>();
        }

        // 第三步：构建分页响应结果
        // 计算总页数：总记录数/每页记录数，向上取整
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, communitys);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应结果到上下文
        context.setResponseEntity(responseEntity);
    }
}