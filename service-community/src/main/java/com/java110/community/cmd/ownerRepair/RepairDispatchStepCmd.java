package com.java110.community.cmd.ownerRepair;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.repair.RepairDto;
import com.java110.intf.community.IRepairInnerServiceSMO;
import com.java110.intf.community.IRepairPoolV1InnerServiceSMO;
import com.java110.intf.community.IRepairUserV1InnerServiceSMO;
import com.java110.po.owner.RepairPoolPo;
import com.java110.po.owner.RepairUserPo;
import com.java110.utils.constant.StateConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

/**
 * 报修工单派单步骤处理命令类
 * 
 * 该类负责处理报修工单的派单流程，包括验证派单参数、添加工单处理人员信息
 * 以及更新报修工单状态等核心业务逻辑
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode ownerRepair.repairDispatchStep
 */
@Java110Cmd(serviceCode = "ownerRepair.repairDispatchStep")
public class RepairDispatchStepCmd extends Cmd {

    /**
     * 报修工单内部服务接口
     */
    @Autowired
    private IRepairInnerServiceSMO repairInnerServiceSMOImpl;

    /**
     * 报修工单处理人员服务接口
     */
    @Autowired
    private IRepairUserV1InnerServiceSMO repairUserV1InnerServiceSMOImpl;

    /**
     * 报修工单池服务接口
     */
    @Autowired
    private IRepairPoolV1InnerServiceSMO repairPoolV1InnerServiceSMOImpl;

    /**
     * 验证派单请求参数
     * 
     * 检查请求参数中是否包含必要的派单信息，包括员工ID、报修单ID和小区ID
     * 如果缺少必要参数，将抛出CmdException异常
     * 
     * @param event 命令事件对象，包含请求上下文信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含员工信息
        Assert.hasKeyAndValue(reqJson, "userId", "未包含员工信息");
        // 验证请求参数中必须包含报修单信息
        Assert.hasKeyAndValue(reqJson, "repairId", "未包含报修单信息");
        // 验证请求参数中必须包含小区信息
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区信息");
    }

    /**
     * 执行派单命令
     * 
     * 处理报修工单派单的核心业务逻辑，包括添加处理人员信息和更新工单状态
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当业务处理失败时抛出异常
     * @throws ParseException 当日期解析失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 添加报修工单处理人员信息
        addBusinessRepairUser(reqJson);
        // 修改报修工单派单状态为"接单中"
        modifyBusinessRepairDispatch(reqJson, RepairDto.STATE_TAKING);
    }

    /**
     * 添加报修工单处理人员信息
     * 
     * 创建报修工单处理人员记录，设置初始状态和开始时间
     * 
     * @param paramInJson 输入参数JSON对象，包含userId、repairId、communityId等信息
     * @throws CmdException 当保存处理人员信息失败时抛出异常
     */
    public void addBusinessRepairUser(JSONObject paramInJson) {
        // 创建业务对象并复制输入参数
        JSONObject businessObj = new JSONObject();
        businessObj.putAll(paramInJson);
        // 设置处理人员状态为"员工未完成订单"
        businessObj.put("state", StateConstant.STAFF_NO_FINISH_ORDER);
        // 生成处理人员记录的唯一ID
        businessObj.put("ruId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_ruId));
        // 将JSON对象转换为实体对象
        RepairUserPo repairUserPo = BeanConvertUtil.covertBean(businessObj, RepairUserPo.class);
        // 设置处理开始时间为当前时间
        repairUserPo.setStartTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
        // 保存处理人员信息到数据库
        int flag = repairUserV1InnerServiceSMOImpl.saveRepairUserNew(repairUserPo);

        // 检查保存结果，如果失败则抛出异常
        if (flag < 1) {
            throw new CmdException("保存失败");
        }
    }

    /**
     * 修改报修工单派单状态
     * 
     * 根据报修单ID查询工单信息，并更新工单状态
     * 
     * @param paramInJson 输入参数JSON对象，包含repairId等信息
     * @param state 要更新的工单状态
     * @throws CmdException 当查询到多条数据或更新失败时抛出异常
     */
    public void modifyBusinessRepairDispatch(JSONObject paramInJson, String state) {
        // 查询报修单信息
        RepairDto repairDto = new RepairDto();
        repairDto.setRepairId(paramInJson.getString("repairId"));
        List<RepairDto> repairDtos = repairInnerServiceSMOImpl.queryRepairs(repairDto);
        
        // 验证查询结果，确保只查询到一条记录
        Assert.isOne(repairDtos, "查询到多条数据，repairId=" + repairDto.getRepairId());
        
        // 创建业务对象并复制查询到的报修单信息
        JSONObject businessOwnerRepair = new JSONObject();
        businessOwnerRepair.putAll(BeanConvertUtil.beanCovertMap(repairDtos.get(0)));
        // 更新工单状态
        businessOwnerRepair.put("state", state);
        
        // 将业务对象转换为报修工单池实体对象
        RepairPoolPo repairPoolPo = BeanConvertUtil.covertBean(businessOwnerRepair, RepairPoolPo.class);
        // 更新报修工单池中的工单状态
        int flag = repairPoolV1InnerServiceSMOImpl.updateRepairPoolNew(repairPoolPo);
        
        // 检查更新结果，如果失败则抛出异常
        if (flag < 1) {
            throw new CmdException("修改失败");
        }
    }
}