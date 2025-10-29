package com.java110.community.cmd.repair;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.repair.RepairUserDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.community.IRepairUserV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.owner.RepairUserPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

/**
 * 报修评论回复命令类
 * 处理报修评价的回复功能，允许用户对已有的报修评论进行回复操作
 * 通过注解@Java110Cmd声明服务代码为"repair.replyRepairAppraise"
 */
@Java110Cmd(serviceCode = "repair.replyRepairAppraise")
public class ReplyRepairAppraiseCmd extends Cmd {

    /**
     * 报修用户服务接口，用于处理报修相关的用户操作
     */
    @Autowired
    private IRepairUserV1InnerServiceSMO repairUserV1InnerServiceSMOImpl;

    /**
     * 用户服务接口，用于处理用户信息查询等操作
     */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 验证请求参数方法
     * 对传入的请求参数进行校验，确保包含必要的字段
     *
     * @param event   命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException   当命令执行出现异常时抛出
     * @throws ParseException 当数据解析出现异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证请求中必须包含小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区ID");
        // 验证请求中必须包含回复内容
        Assert.hasKeyAndValue(reqJson, "replyContext", "未包含回复内容");
        // 验证请求中必须包含评论ID
        Assert.hasKeyAndValue(reqJson, "ruId", "未包含评论ID");
    }

    /**
     * 执行回复报修评价命令
     * 处理报修评价的回复逻辑，包括用户验证、评论验证和回复记录创建
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException   当命令执行出现异常时抛出
     * @throws ParseException 当数据解析出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        // 从上下文中获取当前用户ID
        String userId = CmdContextUtils.getUserId(context);

        // 构建用户查询条件
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        // 查询用户信息
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);

        // 验证用户存在且唯一
        Assert.listOnlyOne(userDtos, "用户未登录");

        // 构建报修评论查询条件
        RepairUserDto repairUserDto = new RepairUserDto();
        repairUserDto.setRuId(reqJson.getString("ruId"));
        // 查询要回复的评论信息
        List<RepairUserDto> repairUserDtos = repairUserV1InnerServiceSMOImpl.queryRepairUserNews(repairUserDto);

        // 验证评论存在且唯一
        Assert.listOnlyOne(repairUserDtos, "评论不存在");

        // 创建新的回复记录对象
        RepairUserPo repairUserPo = new RepairUserPo();
        // 生成回复记录的唯一ID
        repairUserPo.setRuId(GenerateCodeFactory.getGeneratorId("11"));
        // 设置关联的报修ID
        repairUserPo.setRepairId(repairUserDtos.get(0).getRepairId());
        // 设置回复用户ID
        repairUserPo.setUserId(userDtos.get(0).getUserId());
        // 设置小区ID
        repairUserPo.setCommunityId(repairUserDtos.get(0).getCommunityId());
        // 设置状态为回复评价状态
        repairUserPo.setState(RepairUserDto.STATE_REPLY_APPRAISE);
        // 构建回复内容，格式为"回复原评论人:回复内容"
        repairUserPo.setContext("回复" + repairUserDtos.get(0).getStaffName() + ":" + reqJson.getString("replyContext"));
        // 设置回复人员ID
        repairUserPo.setStaffId(userId);
        // 设置回复人员姓名
        repairUserPo.setStaffName(userDtos.get(0).getName());
        // 设置原评论人员ID
        repairUserPo.setPreStaffId(repairUserDtos.get(0).getStaffId());
        // 设置原评论人员姓名
        repairUserPo.setPreStaffName(repairUserDtos.get(0).getStaffName());
        // 设置回复开始时间为原评论的结束时间
        repairUserPo.setStartTime(DateUtil.getFormatTimeStringA(repairUserDtos.get(0).getEndTime()));
        // 设置回复结束时间为当前时间
        repairUserPo.setEndTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
        // 设置报修事件类型
        repairUserPo.setRepairEvent(repairUserDtos.get(0).getRepairEvent());
        // 设置原评论ID
        repairUserPo.setPreRuId(reqJson.getString("ruId"));
        
        // 保存回复记录到数据库
        repairUserV1InnerServiceSMOImpl.saveRepairUserNew(repairUserPo);
    }
}