/**
 * 报修工单启动命令类
 * 
 * 功能描述：处理报修工单的启动操作，将暂停状态的报修工单重新启动，
 * 并记录工单处理人员的操作信息，更新工单状态为接单状态
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
import com.java110.utils.constant.BusinessTypeConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Java110Cmd(serviceCode = "ownerRepair.repairStart")
public class RepairStartCmd extends Cmd {

    /**
     * 报修信息服务接口
     */
    @Autowired
    private IRepairInnerServiceSMO repairInnerServiceSMOImpl;

    /**
     * 报修用户关系信息服务接口
     */
    @Autowired
    private IRepairUserInnerServiceSMO repairUserInnerServiceSMOImpl;

    /**
     * 报修工单池V1版本服务接口
     */
    @Autowired
    private IRepairPoolV1InnerServiceSMO repairPoolV1InnerServiceSMOImpl;

    /**
     * 报修用户关系V1版本服务接口
     */
    @Autowired
    private IRepairUserV1InnerServiceSMO repairUserV1InnerServiceSMOImpl;

    /**
     * 用户V1版本服务接口
     */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 功能描述：验证请求参数是否包含必要的报修单信息和小区的信息
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求上下文信息
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求中是否包含报修单ID
        Assert.hasKeyAndValue(reqJson, "repairId", "未包含报修单信息");
        // 验证请求中是否包含小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区信息");
    }

    /**
     * 执行命令方法
     * 
     * 功能描述：执行报修工单启动操作，包括验证用户身份、查询报修信息、
     * 更新工单状态、记录处理人员信息等业务流程
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当业务处理失败时抛出命令异常
     * @throws ParseException 当日期解析失败时抛出解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        // 从上下文中获取当前用户ID
        String userId = CmdContextUtils.getUserId(context);
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        // 查询用户信息
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);

        // 验证用户信息，确保用户已登录且唯一
        Assert.listOnlyOne(userDtos, "用户未登录");

        RepairDto repairDto = new RepairDto();
        repairDto.setRepairId(reqJson.getString("repairId"));
        // 根据报修单ID查询报修信息
        List<RepairDto> repairDtos = repairInnerServiceSMOImpl.queryRepairs(repairDto);
        // 验证报修信息是否存在且唯一
        Assert.listOnlyOne(repairDtos, "查询报修信息错误！");
        
        String state = repairDtos.get(0).getState();
        int flag = 0;
        // 检查报修单当前状态，如果不是暂停状态则直接返回
        if (!RepairDto.STATE_STOP.equals(state)) {
            return;
        }
        
        // 状态是暂停状态，开始启动流程
        RepairPoolPo repairPoolPo = new RepairPoolPo();
        repairPoolPo.setRepairId(reqJson.getString("repairId"));
        repairPoolPo.setState(RepairDto.STATE_TAKING); // 状态变为接单状态
        // 更新报修工单池状态
        flag = repairPoolV1InnerServiceSMOImpl.updateRepairPoolNew(repairPoolPo);
        if (flag < 1) {
            throw new CmdException("修改工单失败");
        }
        
        RepairUserDto repairUserDto = new RepairUserDto();
        repairUserDto.setRepairId(reqJson.getString("repairId"));
        repairUserDto.setState(RepairUserDto.STATE_STOP); // 查询暂停状态的派单记录
        // 查询报修派单信息
        List<RepairUserDto> repairUserDtos = repairUserInnerServiceSMOImpl.queryRepairUsers(repairUserDto);
        if (ListUtil.isNull(repairUserDtos)) {
            throw new IllegalArgumentException("启动报修单错误！");
        }
        
        // 创建日期格式化对象
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // 创建新的报修用户关系对象
        RepairUserPo repairUserPo = new RepairUserPo();
        repairUserPo.setRuId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_ruId));
        repairUserPo.setRepairId(reqJson.getString("repairId"));
        repairUserPo.setbId("-1"); // 设置业务ID为默认值
        repairUserPo.setCommunityId(reqJson.getString("communityId"));
        repairUserPo.setCreateTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
        repairUserPo.setState(RepairUserDto.STATE_START); // 设置为启动状态
        repairUserPo.setContext("启动报修流程"); // 设置操作内容描述
        repairUserPo.setStaffId(userDtos.get(0).getUserId()); // 设置当前处理员工id
        repairUserPo.setStaffName(userDtos.get(0).getName()); // 设置当前处理员工名称

        // 设置前一个处理人员信息
        repairUserPo.setPreStaffId(repairUserDtos.get(0).getStaffId());
        repairUserPo.setPreStaffName(repairUserDtos.get(0).getStaffName());
        repairUserPo.setStartTime(simpleDateFormat.format(new Date())); // 设置开始时间
        repairUserPo.setEndTime(simpleDateFormat.format(new Date())); // 设置结束时间
        repairUserPo.setPreRuId(repairUserDtos.get(0).getRuId()); // 设置前一个记录ID

        repairUserPo.setRepairEvent(RepairUserDto.REPAIR_EVENT_AUDIT_USER); // 设置报修事件类型为审核用户
        // 保存报修用户关系记录
        flag = repairUserV1InnerServiceSMOImpl.saveRepairUserNew(repairUserPo);
        if (flag < 1) {
            throw new CmdException("添加报修工单信息失败！");
        }

    }
}