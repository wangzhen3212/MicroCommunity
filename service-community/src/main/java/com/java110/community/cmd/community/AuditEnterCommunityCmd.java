/**
 * 小区入驻审核命令类
 * 
 * 该类用于处理小区入驻申请的审核操作，包括参数验证和审核状态更新
 * 通过注解标识为服务代码 "community.auditEnterCommunity"
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.community.cmd.community;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.community.CommunityMemberDto;
import com.java110.intf.community.ICommunityInnerServiceSMO;
import com.java110.intf.community.ICommunityMemberV1InnerServiceSMO;
import com.java110.po.community.CommunityMemberPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

@Java110Cmd(serviceCode = "community.auditEnterCommunity")
public class AuditEnterCommunityCmd  extends Cmd {

    /**
     * 小区成员服务接口，用于更新小区成员信息
     */
    @Autowired
    private ICommunityMemberV1InnerServiceSMO communityMemberV1InnerServiceSMOImpl;

    /**
     * 小区信息服务接口，用于查询小区成员信息
     */
    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整，确保必要的参数都存在
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证小区成员ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityMemberId", "小区成员ID不能为空");
        // 验证审核状态参数是否存在
        Assert.hasKeyAndValue(reqJson, "state", "必填，请填写小区审核状态");
        // 验证审核原因参数是否存在
        Assert.hasKeyAndValue(reqJson, "remark", "必填，请填写小区审核原因");
    }

    /**
     * 执行小区入驻审核命令
     * 
     * 根据小区成员ID查询成员信息，更新审核状态，并保存到数据库
     * 使用事务注解确保操作的原子性
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当更新操作失败或查询结果不符合预期时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 创建小区成员查询对象
        CommunityMemberDto communityMemberDto = new CommunityMemberDto();
        // 设置要查询的小区成员ID
        communityMemberDto.setCommunityMemberId(reqJson.getString("communityMemberId"));
        
        // 根据小区成员ID查询成员信息
        List<CommunityMemberDto> communityMemberDtos = communityInnerServiceSMOImpl.getCommunityMembers(communityMemberDto);
        
        // 验证查询结果，确保只查询到一条记录
        Assert.listOnlyOne(communityMemberDtos, "未查询到该小区成员信息【" + communityMemberDto.getCommunityMemberId() + "】");
        
        // 获取查询到的小区成员信息
        communityMemberDto = communityMemberDtos.get(0);

        // 将小区成员DTO对象转换为Map，便于后续字段更新
        Map oldCommunityInfo = BeanConvertUtil.beanCovertMap(communityMemberDto);
        // 更新审核状态字段
        oldCommunityInfo.put("auditStatusCd", reqJson.getString("state"));
        
        // 将更新后的Map转换为PO对象，用于数据库更新操作
        CommunityMemberPo communityMemberPo = BeanConvertUtil.covertBean(oldCommunityInfo, CommunityMemberPo.class);
        
        // 执行数据库更新操作
        int flag = communityMemberV1InnerServiceSMOImpl.updateCommunityMember(communityMemberPo);

        // 检查更新操作是否成功，flag小于1表示更新失败
        if (flag < 1) {
            throw new CmdException("更新小区失败");
        }
    }
}