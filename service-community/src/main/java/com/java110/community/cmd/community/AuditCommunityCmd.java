/**
 * 小区审核命令类
 * 
 * 该类用于处理小区信息的审核操作，包括参数验证和小区状态更新
 * 通过注解@Java110Cmd声明服务代码为"community.auditCommunity"
 * 实现小区审核状态和审核原因的更新功能
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.community.cmd.community;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.community.CommunityDto;
import com.java110.intf.community.ICommunityInnerServiceSMO;
import com.java110.intf.community.ICommunityV1InnerServiceSMO;
import com.java110.po.community.CommunityPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

@Java110Cmd(serviceCode = "community.auditCommunity")
public class AuditCommunityCmd extends Cmd {

    /**
     * 小区内部服务接口
     * 用于查询小区信息
     */
    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;

    /**
     * 小区V1版本内部服务接口
     * 用于更新小区信息
     */
    @Autowired
    private ICommunityV1InnerServiceSMO communityV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的小区审核信息
     * 包括小区ID、审核状态和审核原因
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证小区ID参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "communityId", "小区ID不能为空");
        // 验证审核状态参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "state", "必填，请填写小区审核状态");
        // 验证审核原因参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "remark", "必填，请填写小区审核原因");
    }

    /**
     * 执行小区审核命令
     * 
     * 根据请求参数更新小区的审核状态和审核原因
     * 使用@Java110Transactional注解确保操作的事务性
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当小区信息查询失败或更新失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 创建小区数据传输对象
        CommunityDto communityDto = new CommunityDto();
        // 设置要查询的小区ID
        communityDto.setCommunityId(reqJson.getString("communityId"));
        // 根据小区ID查询小区信息
        List<CommunityDto> communityDtos = communityInnerServiceSMOImpl.queryCommunitys(communityDto);
        
        // 验证查询结果，确保只查询到一条小区记录
        Assert.listOnlyOne(communityDtos, "未查询到该小区信息【" + communityDto.getCommunityId() + "】");
        // 获取查询到的小区信息
        communityDto = communityDtos.get(0);

        // 将小区DTO对象转换为Map，便于后续操作
        Map oldCommunityInfo = BeanConvertUtil.beanCovertMap(communityDto);
        // 更新小区状态为请求参数中的审核状态
        oldCommunityInfo.put("state", reqJson.getString("state"));
        // 将更新后的Map转换为小区持久化对象
        CommunityPo communityPo = BeanConvertUtil.covertBean(oldCommunityInfo, CommunityPo.class);
        // 执行小区信息更新操作
        int flag = communityV1InnerServiceSMOImpl.updateCommunity(communityPo);

        // 检查更新操作是否成功，如果影响行数小于1则抛出异常
        if (flag < 1) {
            throw new CmdException("更新小区失败");
        }
    }
}