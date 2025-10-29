/**
 * 业主报修工单结束命令类
 * 
 * 该类负责处理业主报修工单的结束操作，包括验证请求参数、更新工单状态、
 * 记录工单处理人员信息以及保存工单结束原因等业务逻辑
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
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
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.owner.RepairPoolPo;
import com.java110.po.owner.RepairUserPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.List;

@Java110Cmd(serviceCode = "ownerRepair.repairEnd")
public class RepairEndCmd extends Cmd {

    // 报修工单池服务接口
    @Autowired
    private IRepairPoolV1InnerServiceSMO repairPoolV1InnerServiceSMOImpl;
    
    // 报修工单服务接口
    @Autowired
    private IRepairInnerServiceSMO repairInnerServiceSMOImpl;

    // 报修工单处理人员服务接口
    @Autowired
    private IRepairUserInnerServiceSMO repairUserInnerServiceSMOImpl;

    // 用户服务接口
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 验证请求参数方法
     * 
     * 验证请求JSON中是否包含必要的参数：小区ID、工单ID和结束原因
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求上下文信息
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     * @throws ParseException 当参数解析失败时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证小区ID参数
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
        // 验证工单ID参数
        Assert.hasKeyAndValue(reqJson, "repairId", "未包含工单");
        // 验证结束原因参数
        Assert.hasKeyAndValue(reqJson, "context", "未包含工单");
    }

    /**
     * 执行工单结束命令方法
     * 
     * 主要功能：
     * 1. 获取当前登录用户信息
     * 2. 验证工单是否存在
     * 3. 关闭正在处理的工单人员记录
     * 4. 更新工单状态为已完成
     * 5. 创建工单结束记录
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当业务逻辑执行失败时抛出命令异常
     * @throws ParseException 当日期解析失败时抛出解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        // 从上下文中获取当前登录用户ID
        String staffId = CmdContextUtils.getUserId(context);
        
        // 构建用户查询条件
        UserDto userDto = new UserDto();
        userDto.setUserId(staffId);
        
        // 查询当前登录用户信息
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);
        
        // 验证用户是否存在
        if (ListUtil.isNull(userDtos)) {
            throw new CmdException("登陆用户不存在");
        }

        // 构建工单查询条件
        RepairDto repairDto = new RepairDto();
        repairDto.setRepairId(reqJson.getString("repairId"));
        repairDto.setCommunityId(reqJson.getString("communityId"));
        
        // 查询工单信息
        List<RepairDto> repairDtos = repairInnerServiceSMOImpl.queryRepairs(repairDto);
        
        // 验证工单是否存在
        if (ListUtil.isNull(repairDtos)) {
            throw new CmdException("工单不存在");
        }
        
        // 构建工单处理人员查询条件，查找正在处理中的工单人员记录
        RepairUserDto repairUserDto = new RepairUserDto();
        repairUserDto.setRepairId(reqJson.getString("repairId"));
        repairUserDto.setCommunityId(reqJson.getString("communityId"));
        repairUserDto.setState(RepairUserDto.STATE_DOING); // 查询状态为处理中的记录
        
        // 查询正在处理中的工单人员记录
        List<RepairUserDto> repairUserDtos = repairUserInnerServiceSMOImpl.queryRepairUsers(repairUserDto);
        
        RepairUserPo repairUserPo = null;
        
        // 如果存在正在处理的工单人员记录，则关闭这些记录
        if (!ListUtil.isNull(repairUserDtos)) {
            for (RepairUserDto tRepairUserDto : repairUserDtos) {
                repairUserPo = new RepairUserPo();
                repairUserPo.setRuId(tRepairUserDto.getRuId()); // 设置工单人员记录ID
                repairUserPo.setState(RepairUserDto.STATE_CLOSE); // 更新状态为关闭
                repairUserPo.setEndTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A)); // 设置结束时间
                repairUserPo.setCommunityId(reqJson.getString("communityId")); // 设置小区ID
                repairUserPo.setContext(userDtos.get(0).getName() + "结束了工单"); // 设置结束说明
                
                // 更新工单人员记录状态
                repairUserInnerServiceSMOImpl.updateRepairUser(repairUserPo);
            }
        }

        // 更新工单池中的工单状态
        RepairPoolPo repairPoolPo = new RepairPoolPo();
        repairPoolPo.setRepairId(reqJson.getString("repairId")); // 设置工单ID
        repairPoolPo.setState(RepairDto.STATE_COMPLATE); // 更新状态为已完成
        repairPoolPo.setFinishTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A)); // 设置完成时间
        
        // 执行工单状态更新操作
        int flag = repairPoolV1InnerServiceSMOImpl.updateRepairPoolNew(repairPoolPo);

        // 创建新的工单结束记录
        repairUserPo = new RepairUserPo();
        repairUserPo.setRuId(GenerateCodeFactory.getGeneratorId("11")); // 生成工单人员记录ID
        repairUserPo.setRepairId(reqJson.getString("repairId")); // 设置工单ID
        repairUserPo.setUserId(staffId); // 设置用户ID
        repairUserPo.setCommunityId(reqJson.getString("communityId")); // 设置小区ID
        repairUserPo.setState(RepairUserDto.STATE_CLOSE); // 设置状态为关闭
        repairUserPo.setContext("结束原因："+reqJson.getString("context")); // 设置结束原因
        repairUserPo.setStaffId(staffId); // 设置处理人员ID
        repairUserPo.setStaffName(userDtos.get(0).getName()); // 设置处理人员姓名
        repairUserPo.setPreStaffId(staffId); // 设置前一个处理人员ID
        repairUserPo.setPreStaffName(userDtos.get(0).getName()); // 设置前一个处理人员姓名
        repairUserPo.setStartTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A)); // 设置开始时间
        repairUserPo.setEndTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A)); // 设置结束时间
        repairUserPo.setRepairEvent(RepairUserDto.REPAIR_EVENT_AUDIT_USER); // 设置工单事件类型
        repairUserPo.setPreRuId("-1"); // 设置前一个工单人员记录ID
        repairUserPo.setCreateTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A)); // 设置创建时间
        
        // 保存工单结束记录
        repairUserInnerServiceSMOImpl.saveRepairUser(repairUserPo);
    }
}