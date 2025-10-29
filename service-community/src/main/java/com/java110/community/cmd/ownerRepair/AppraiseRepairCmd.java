/**
 * 业主报修评价处理命令类
 * 
 * 该类负责处理业主对报修单的评价操作，包括验证评价参数、更新维修人员状态和报修单状态等业务逻辑
 * 支持不同报修渠道（业主端、管理端）和不同维修类型（有偿、无偿）的评价处理
 * 
 * @author Java110
 * @version 1.0
 * @see Cmd
 */
package com.java110.community.cmd.ownerRepair;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.repair.RepairDto;
import com.java110.dto.repair.RepairUserDto;
import com.java110.intf.community.IRepairInnerServiceSMO;
import com.java110.intf.community.IRepairPoolV1InnerServiceSMO;
import com.java110.intf.community.IRepairUserInnerServiceSMO;
import com.java110.intf.community.IRepairUserV1InnerServiceSMO;
import com.java110.po.owner.RepairPoolPo;
import com.java110.po.owner.RepairUserPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Java110Cmd(serviceCode = "ownerRepair.appraiseRepair")
public class AppraiseRepairCmd extends Cmd {

    @Autowired
    private IRepairUserInnerServiceSMO repairUserInnerServiceSMOImpl;

    @Autowired
    private IRepairInnerServiceSMO repairInnerServiceSMOImpl;

    @Autowired
    private IRepairUserV1InnerServiceSMO repairUserV1InnerServiceSMOImpl;

    @Autowired
    private IRepairPoolV1InnerServiceSMO repairPoolV1InnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 
     * 检查请求JSON中是否包含必要的参数：报修单ID、评价内容和小区ID
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        Assert.hasKeyAndValue(reqJson, "repairId", "未包含报修单信息");
        Assert.hasKeyAndValue(reqJson, "context", "未包含派单内容");
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区信息");
    }

    /**
     * 执行报修评价命令
     * 
     * 处理报修评价的核心业务逻辑，包括：
     * 1. 根据报修渠道和维修类型查询对应的维修人员记录
     * 2. 创建新的维修人员评价记录
     * 3. 更新报修单状态为已完成
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当业务处理失败时抛出异常
     * @throws ParseException 当日期解析失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        //获取报修渠道
        String repairChannel = reqJson.getString("repairChannel");
        //是否是公共区域
        String publicArea = reqJson.getString("publicArea");
        //查询维修类型
        String maintenanceType = reqJson.getString("maintenanceType");
        
        // 构建维修人员查询条件
        RepairUserDto repairUserDto = new RepairUserDto();
        repairUserDto.setRepairId(reqJson.getString("repairId"));
        repairUserDto.setCommunityId(reqJson.getString("communityId"));
        
        // 根据报修渠道和维修类型设置查询状态
        if (repairChannel.equals("Z")) { //如果是业主端报修，就查询是否有已评价状态的
            repairUserDto.setState(RepairUserDto.STATE_FINISH);
        } else if (!StringUtil.isEmpty(maintenanceType) && maintenanceType.equals("1001")) { //如果不是业主端报修，且是有偿的，就查询已支付状态的
            repairUserDto.setState(RepairUserDto.STATE_FINISH_PAY_FEE);
        } else { //其他的查询结单状态的
            repairUserDto.setState(RepairUserDto.STATE_CLOSE);
        }
        
        // 查询维修人员记录
        List<RepairUserDto> repairUserDtos = repairUserInnerServiceSMOImpl.queryRepairUsers(repairUserDto);
        Assert.listOnlyOne(repairUserDtos, "当前用户没有需要处理的订单");
        
        // 1.0 关闭自己订单 - 创建新的维修人员评价记录
        RepairUserPo repairUserPo = new RepairUserPo();
        repairUserPo.setRuId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_ruId));
        repairUserPo.setEndTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
        repairUserPo.setState(RepairUserDto.STATE_FINISH_VISIT); // 设置为回访完成状态
        repairUserPo.setContext(reqJson.getString("context")); // 评价内容
        repairUserPo.setCommunityId(reqJson.getString("communityId"));
        repairUserPo.setCreateTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
        repairUserPo.setRepairId(reqJson.getString("repairId"));
        repairUserPo.setStaffId(reqJson.getString("userId")); // 当前操作用户ID
        repairUserPo.setStaffName(reqJson.getString("userName")); // 当前操作用户名称
        
        // 设置开始时间为上一条记录的结束时间
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date endTime = repairUserDtos.get(0).getEndTime();
        repairUserPo.setStartTime(format.format(endTime));
        
        // 设置前一个维修人员信息，用于记录流转关系
        repairUserPo.setPreStaffId(repairUserDtos.get(0).getStaffId());
        repairUserPo.setPreStaffName(repairUserDtos.get(0).getStaffName());
        repairUserPo.setPreRuId(repairUserDtos.get(0).getRuId());
        repairUserPo.setRepairEvent("auditUser"); // 设置维修事件类型为审核用户
        
        // 保存新的维修人员记录
        int flag = repairUserV1InnerServiceSMOImpl.saveRepairUserNew(repairUserPo);
        if (flag < 1) {
            throw new CmdException("保存失败");
        }
        
        // 更新报修单状态为已完成
        modifyBusinessRepairDispatch(reqJson, RepairDto.STATE_COMPLATE);
        
        // 返回成功响应
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_OK, ResultVo.MSG_OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 修改报修单派单业务状态
     * 
     * 根据传入的状态更新报修单的状态，主要用于将报修单状态更新为已完成
     * 
     * @param paramInJson 包含报修单ID的参数JSON对象
     * @param state 要更新的状态值
     * @throws CmdException 当更新失败时抛出异常
     */
    public void modifyBusinessRepairDispatch(JSONObject paramInJson, String state) {
        //查询报修单
        RepairDto repairDto = new RepairDto();
        repairDto.setRepairId(paramInJson.getString("repairId"));
        List<RepairDto> repairDtos = repairInnerServiceSMOImpl.queryRepairs(repairDto);
        Assert.isOne(repairDtos, "查询到多条数据，repairId=" + repairDto.getRepairId());
        
        // 构建报修单更新对象
        JSONObject businessOwnerRepair = new JSONObject();
        businessOwnerRepair.putAll(BeanConvertUtil.beanCovertMap(repairDtos.get(0)));
        businessOwnerRepair.put("state", state); // 更新状态
        
        //计算 应收金额
        RepairPoolPo repairPoolPo = BeanConvertUtil.covertBean(businessOwnerRepair, RepairPoolPo.class);
        int flag = repairPoolV1InnerServiceSMOImpl.updateRepairPoolNew(repairPoolPo);
        if (flag < 1) {
            throw new CmdException("保存失败");
        }
    }
}