/**
 * 报修工单暂停处理命令类
 * 
 * 功能描述：处理报修工单的暂停操作，将工单状态变更为暂停状态，并记录暂停操作的相关信息
 * 服务编码：ownerRepair.repairStop
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.community.cmd.ownerRepair;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.repair.RepairDto;
import com.java110.dto.repair.RepairUserDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.community.IRepairInnerServiceSMO;
import com.java110.intf.community.IRepairPoolV1InnerServiceSMO;
import com.java110.intf.community.IRepairUserInnerServiceSMO;
import com.java110.intf.community.IRepairUserV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.owner.RepairPoolPo;
import com.java110.po.owner.RepairUserPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Java110Cmd(serviceCode = "ownerRepair.repairStop")
public class RepairStopCmd extends Cmd {

    /** 报修信息服务接口 */
    @Autowired
    private IRepairInnerServiceSMO repairInnerServiceSMOImpl;

    /** 报修派单信息服务接口 */
    @Autowired
    private IRepairUserInnerServiceSMO repairUserInnerServiceSMOImpl;

    /** 报修工单池V1服务接口 */
    @Autowired
    private IRepairPoolV1InnerServiceSMO repairPoolV1InnerServiceSMOImpl;

    /** 报修派单V1服务接口 */
    @Autowired
    private IRepairUserV1InnerServiceSMO repairUserV1InnerServiceSMOImpl;

    /** 用户V1服务接口 */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 参数校验方法
     * 
     * 功能描述：验证请求参数是否完整，确保必要的参数都存在
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求上下文信息
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证报修单ID是否存在
        Assert.hasKeyAndValue(reqJson, "repairId", "未包含报修单信息");
        // 验证暂停备注内容是否存在
        Assert.hasKeyAndValue(reqJson, "remark", "未包含派单内容");
        // 验证小区ID是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区信息");
    }

    /**
     * 命令执行方法
     * 
     * 功能描述：执行报修工单暂停操作，包括：
     * 1. 验证用户登录状态
     * 2. 查询报修单信息
     * 3. 更新报修单状态为暂停
     * 4. 记录暂停操作日志
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当日期解析错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        // 从上下文中获取当前用户ID
        String userId = CmdContextUtils.getUserId(context);
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        // 查询用户信息，验证用户登录状态
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);

        // 确保查询到的用户信息唯一，即用户已正确登录
        Assert.listOnlyOne(userDtos, "用户未登录");

        // 构建报修查询条件
        RepairDto repairDto = new RepairDto();
        repairDto.setRepairId(reqJson.getString("repairId"));
        // 查询报修单详细信息
        List<RepairDto> repairDtos = repairInnerServiceSMOImpl.queryRepairs(repairDto);
        // 确保查询到的报修单信息唯一
        Assert.listOnlyOne(repairDtos, "查询报修单错误！");
        
        // 获取当前报修单状态
        String state = repairDtos.get(0).getState();
        int flag = 0;
        
        // 如果报修单已经是暂停状态，则直接返回，无需重复操作
        if (RepairDto.STATE_STOP.equals(state)) {
            return;
        }
        
        // 报修单不是暂停状态，执行暂停操作
        
        // 将请求参数转换为报修工单PO对象
        RepairPoolPo repairPoolPo = BeanConvertUtil.covertBean(reqJson, RepairPoolPo.class);
        // 将报修状态设置为暂停状态
        repairPoolPo.setState(RepairDto.STATE_STOP);
        // 更新报修工单状态
        flag = repairPoolV1InnerServiceSMOImpl.updateRepairPoolNew(repairPoolPo);
        if (flag < 1) {
            throw new CmdException("修改工单失败");
        }
        
        // 创建报修派单记录，记录暂停操作
        RepairUserPo repairUserPo = new RepairUserPo();
        // 生成派单记录ID
        repairUserPo.setRuId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_ruId));
        // 设置报修单ID
        repairUserPo.setRepairId(reqJson.getString("repairId"));
        // 设置小区ID
        repairUserPo.setCommunityId(reqJson.getString("communityId"));
        // 创建日期格式化对象
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // 设置创建时间为当前时间
        repairUserPo.setCreateTime(simpleDateFormat.format(new Date()));
        // 设置派单状态为暂停状态
        repairUserPo.setState(RepairUserDto.STATE_STOP);
        // 设置暂停备注内容
        repairUserPo.setContext(reqJson.getString("remark"));
        // 设置当前处理员工ID
        repairUserPo.setStaffId(userDtos.get(0).getUserId());
        // 设置当前处理员工名称
        repairUserPo.setStaffName(userDtos.get(0).getName());
        
        // 查询当前正在处理的派单记录
        RepairUserDto repairUserDto = new RepairUserDto();
        repairUserDto.setRepairId(reqJson.getString("repairId"));
        // 查询状态为处理中的派单记录
        repairUserDto.setState(RepairUserDto.STATE_DOING);
        List<RepairUserDto> repairUserDtos = repairUserInnerServiceSMOImpl.queryRepairUsers(repairUserDto);
        // 确保查询到的派单记录唯一
        Assert.listOnlyOne(repairUserDtos, "查询报修派单错误！");
        
        // 设置上一节点处理员工信息
        repairUserPo.setPreStaffId(repairUserDtos.get(0).getStaffId());
        repairUserPo.setPreStaffName(repairUserDtos.get(0).getStaffName());
        // 设置开始时间和结束时间为当前时间（暂停操作瞬时完成）
        repairUserPo.setStartTime(simpleDateFormat.format(new Date()));
        repairUserPo.setEndTime(simpleDateFormat.format(new Date()));
        // 设置报修事件类型为用户审核
        repairUserPo.setRepairEvent(RepairUserDto.REPAIR_EVENT_AUDIT_USER);
        // 设置上一节点处理记录ID
        repairUserPo.setPreRuId(repairUserDtos.get(0).getRuId());
        
        // 保存暂停操作的派单记录
        flag = repairUserV1InnerServiceSMOImpl.saveRepairUserNew(repairUserPo);
        if (flag < 1) {
            throw new CmdException("添加工单失败");
        }

    }
}