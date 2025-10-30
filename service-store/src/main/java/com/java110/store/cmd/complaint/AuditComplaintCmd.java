/**
 * 投诉审核命令类
 * 
 * 该类用于处理投诉单的审核操作，包括验证审核参数、更新投诉状态和记录审核事件。
 * 通过注解@Java110Cmd声明服务代码为"complaint.auditComplaint"。
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.store.cmd.complaint;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.complaint.ComplaintDto;
import com.java110.dto.complaint.ComplaintEventDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.store.IComplaintEventV1InnerServiceSMO;
import com.java110.intf.store.IComplaintV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.complaint.ComplaintPo;
import com.java110.po.complaint.ComplaintEventPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Java110Cmd(serviceCode = "complaint.auditComplaint")
public class AuditComplaintCmd extends Cmd {

    /**
     * 用户服务接口，用于查询用户信息
     */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 投诉事件服务接口，用于保存投诉事件记录
     */
    @Autowired
    private IComplaintEventV1InnerServiceSMO complaintEventV1InnerServiceSMOImpl;

    /**
     * 投诉服务接口，用于查询和更新投诉信息
     */
    @Autowired
    private IComplaintV1InnerServiceSMO complaintV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否包含必要的字段，确保投诉审核操作的基本数据完整性。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 数据流上下文，用于获取和设置上下文数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证投诉ID不能为空
        Assert.hasKeyAndValue(reqJson, "complaintId", "投诉ID不能为空");
        // 验证小区ID不能为空
        Assert.hasKeyAndValue(reqJson, "communityId", "必填，请填写小区信息");
        // 验证审核内容不能为空
        Assert.hasKeyAndValue(reqJson, "context", "必填，请填写批注");
    }

    /**
     * 执行投诉审核命令
     * 
     * 处理投诉审核的核心业务逻辑，包括：
     * 1. 验证用户登录状态
     * 2. 查询投诉信息
     * 3. 更新投诉状态为已完成
     * 4. 记录审核事件
     * 5. 返回操作结果
     * 
     * @param event 命令事件对象
     * @param context 数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当业务逻辑执行失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        // 从上下文中获取当前用户ID
        String userId = CmdContextUtils.getUserId(context);

        // 构建用户查询条件
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        // 查询用户信息
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);

        // 验证用户存在且唯一
        Assert.listOnlyOne(userDtos, "用户未登录");

        // 构建投诉查询条件
        ComplaintDto complaintDto = new ComplaintDto();
        complaintDto.setComplaintId(reqJson.getString("complaintId"));
        complaintDto.setCommunityId(reqJson.getString("communityId"));

        // 查询投诉信息
        List<ComplaintDto> complaintDtos = complaintV1InnerServiceSMOImpl.queryComplaints(complaintDto);
        // 验证投诉存在且唯一
        Assert.listOnlyOne(complaintDtos, "未存在或存在多条投诉单");

        // 更新投诉状态为已完成
        ComplaintPo complaintPo = new ComplaintPo();
        complaintPo.setComplaintId(complaintDtos.get(0).getComplaintId());
        complaintPo.setState(ComplaintDto.STATE_FINISH);
        complaintV1InnerServiceSMOImpl.updateComplaint(complaintPo);

        // 创建投诉事件记录
        ComplaintEventPo complaintEventPo = new ComplaintEventPo();
        // 生成事件ID
        complaintEventPo.setEventId(GenerateCodeFactory.getGeneratorId("11"));
        // 设置创建用户信息
        complaintEventPo.setCreateUserId(userDtos.get(0).getUserId());
        complaintEventPo.setCreateUserName(userDtos.get(0).getName());
        // 设置关联的投诉ID
        complaintEventPo.setComplaintId(complaintDtos.get(0).getComplaintId());
        // 设置审核备注内容
        complaintEventPo.setRemark(reqJson.getString("context"));
        // 设置事件类型为处理类型
        complaintEventPo.setEventType(ComplaintEventDto.EVENT_TYPE_DO);
        // 设置小区ID
        complaintEventPo.setCommunityId(complaintDtos.get(0).getCommunityId());

        // 保存投诉事件记录
        complaintEventV1InnerServiceSMOImpl.saveComplaintEvent(complaintEventPo);

        // 设置响应结果为成功
        context.setResponseEntity(ResultVo.success());
    }
}