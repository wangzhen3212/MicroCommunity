/**
 * 关闭报修派单命令类
 * 
 * 该命令类用于处理报修单的关闭派单操作，主要功能包括：
 * 1. 验证关闭派单请求参数的完整性
 * 2. 更新报修处理人员状态信息
 * 3. 更新报修单状态信息
 * 4. 根据处理结果设置报修单的最终状态
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.community.cmd.ownerRepair;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.DataFlowContext;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.repair.RepairDto;
import com.java110.dto.repair.RepairUserDto;
import com.java110.intf.community.IRepairInnerServiceSMO;
import com.java110.intf.community.IRepairPoolV1InnerServiceSMO;
import com.java110.intf.community.IRepairUserInnerServiceSMO;
import com.java110.intf.community.IRepairUserV1InnerServiceSMO;
import com.java110.po.owner.RepairPoolPo;
import com.java110.po.owner.RepairUserPo;
import com.java110.utils.constant.BusinessTypeConstant;
import com.java110.utils.constant.StateConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

@Java110Cmd(serviceCode = "ownerRepair.closeRepairDispatch")
public class CloseRepairDispatchCmd extends Cmd {

    /**
     * 报修内部服务接口
     */
    @Autowired
    private IRepairInnerServiceSMO repairInnerServiceSMOImpl;

    /**
     * 报修用户内部服务接口
     */
    @Autowired
    private IRepairUserInnerServiceSMO repairUserInnerServiceSMOImpl;

    /**
     * 报修用户V1版本内部服务接口
     */
    @Autowired
    private IRepairUserV1InnerServiceSMO repairUserV1InnerServiceSMOImpl;

    /**
     * 报修池V1版本内部服务接口
     */
    @Autowired
    private IRepairPoolV1InnerServiceSMO repairPoolV1InnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 
     * 该方法用于验证关闭报修派单请求的必需参数是否完整
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置上下文数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证必需参数是否存在
        Assert.hasKeyAndValue(reqJson, "state", "未包含处理信息");
        Assert.hasKeyAndValue(reqJson, "context", "未包含处理内容");
        Assert.hasKeyAndValue(reqJson, "repairId", "未包含报修单信息");
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区信息");
        Assert.hasKeyAndValue(reqJson, "staffId", "未包含员工ID");
    }

    /**
     * 执行关闭报修派单命令
     * 
     * 该方法负责执行关闭报修派单的核心业务逻辑，包括：
     * 1. 更新报修处理人员信息
     * 2. 更新报修单状态信息
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当业务处理失败时抛出异常
     * @throws ParseException 当数据解析异常时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 更新报修处理人员信息
        modifyBusinessRepairUser(reqJson);
        // 更新报修单状态信息
        modifyBusinessRepair(reqJson);
    }

    /**
     * 修改报修处理人员业务信息
     * 
     * 该方法用于更新指定报修单处理人员的状态和处理内容
     * 
     * @param paramInJson 包含更新参数的JSON对象，必须包含：
     *                   - repairId: 报修单ID
     *                   - staffId: 员工ID
     *                   - state: 处理状态
     *                   - context: 处理内容
     * @throws CmdException 当更新失败或查询到多条数据时抛出异常
     */
    public void modifyBusinessRepairUser(JSONObject paramInJson) {
        // 构建查询条件
        RepairUserDto repairUserDto = new RepairUserDto();
        repairUserDto.setRepairId(paramInJson.getString("repairId"));
        repairUserDto.setUserId(paramInJson.getString("staffId"));
        
        // 查询报修处理人员信息
        List<RepairUserDto> repairUserDtos = repairUserInnerServiceSMOImpl.queryRepairUsers(repairUserDto);
        
        // 验证查询结果，确保只查询到一条记录
        Assert.isOne(repairUserDtos, "查询到多条数据，repairId=" + repairUserDto.getRepairId() + " userId = " + repairUserDto.getUserId());
        
        // 构建更新数据
        JSONObject businessObj = new JSONObject();
        businessObj.putAll(BeanConvertUtil.beanCovertMap(repairUserDtos.get(0)));
        businessObj.put("state", paramInJson.getString("state"));
        businessObj.put("context", paramInJson.getString("context"));

        // 转换为PO对象并执行更新
        RepairUserPo repairUserPo = BeanConvertUtil.covertBean(businessObj, RepairUserPo.class);
        int flag = repairUserV1InnerServiceSMOImpl.updateRepairUserNew(repairUserPo);
        
        // 验证更新结果
        if (flag < 1) {
            throw new CmdException("修改用户失败");
        }
    }

    /**
     * 修改报修单业务信息
     * 
     * 该方法用于更新报修单的状态信息，根据处理结果设置不同的状态值
     * 
     * @param paramInJson 包含更新参数的JSON对象，必须包含：
     *                   - repairId: 报修单ID
     *                   - state: 处理状态
     * @throws CmdException 当更新失败或查询到多条数据时抛出异常
     */
    public void modifyBusinessRepair(JSONObject paramInJson) {
        // 查询报修单
        RepairDto repairDto = new RepairDto();
        repairDto.setRepairId(paramInJson.getString("repairId"));

        List<RepairDto> repairDtos = repairInnerServiceSMOImpl.queryRepairs(repairDto);

        // 验证查询结果，确保只查询到一条记录
        Assert.isOne(repairDtos, "查询到多条数据，repairId=" + repairDto.getRepairId());

        // 构建更新数据
        JSONObject businessOwnerRepair = new JSONObject();
        businessOwnerRepair.putAll(BeanConvertUtil.beanCovertMap(repairDtos.get(0)));
        // 根据处理状态设置报修单最终状态：10002表示派单完成，其他表示未派单
        businessOwnerRepair.put("state", "10002".equals(paramInJson.getString("state")) ? StateConstant.REPAIR_DISPATCH_FINISH : StateConstant.REPAIR_NO_DISPATCH);

        // 转换为PO对象并执行更新
        RepairPoolPo repairPoolPo = BeanConvertUtil.covertBean(businessOwnerRepair, RepairPoolPo.class);
        int flag = repairPoolV1InnerServiceSMOImpl.updateRepairPoolNew(repairPoolPo);
        
        // 验证更新结果
        if (flag < 1) {
            throw new CmdException("修改失败");
        }
    }
}