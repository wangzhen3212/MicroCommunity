/**
 * 业主报修抢单命令处理类
 * 
 * 该类负责处理维修师傅抢单业务逻辑，包括：
 * 1. 验证抢单权限和条件
 * 2. 检查维修师傅当前未处理订单数量
 * 3. 验证维修师傅对报修类型的处理权限
 * 4. 更新报修单状态和维修记录
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.community.cmd.ownerRepair;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.repair.RepairDto;
import com.java110.dto.repair.RepairTypeUserDto;
import com.java110.dto.repair.RepairUserDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.community.*;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.owner.RepairPoolPo;
import com.java110.po.owner.RepairUserPo;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.lock.DistributedLock;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.List;

@Java110Cmd(serviceCode = "ownerRepair.grabbingRepair")
public class GrabbingRepairCmd extends Cmd {

    @Autowired
    private IRepairUserInnerServiceSMO repairUserInnerServiceSMOImpl;

    @Autowired
    private IRepairInnerServiceSMO repairInnerServiceSMOImpl;

    @Autowired
    private IRepairTypeUserInnerServiceSMO repairTypeUserInnerServiceSMOImpl;

    @Autowired
    private IRepairTypeUserInnerServiceSMO repairTypeUserInnerServiceSMO;

    @Autowired
    private IRepairPoolV1InnerServiceSMO repairPoolV1InnerServiceSMOImpl;

    @Autowired
    private IRepairUserV1InnerServiceSMO repairUserV1InnerServiceSMOImpl;

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    //域
    public static final String DOMAIN_COMMON = "DOMAIN.COMMON";

    //键(维修师傅未处理最大单数)
    public static final String REPAIR_NUMBER = "REPAIR_NUMBER";

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的报修单信息和小区信息
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON数据
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含报修单ID
        Assert.hasKeyAndValue(reqJson, "repairId", "未包含报修单信息");
        // 验证请求参数中必须包含小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区信息");
//        Assert.hasKeyAndValue(reqJson, "userId", "未包含用户ID");
//        Assert.hasKeyAndValue(reqJson, "userName", "未包含用户名称");
    }

    /**
     * 执行抢单命令
     * 
     * 处理维修师傅抢单的核心业务逻辑，包括：
     * 1. 获取并验证用户信息
     * 2. 检查维修师傅当前未处理订单数量
     * 3. 验证维修师傅对报修类型的处理权限
     * 4. 根据报修单状态执行不同的抢单逻辑
     * 5. 更新报修单状态和维修记录
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON数据
     * @throws CmdException 当业务处理失败时抛出异常
     * @throws ParseException 当日期解析失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从请求头中获取用户ID
        String userId = context.getReqHeaders().get("user-id");

        // 如果用户ID以"-"开头或为空，则从请求参数中获取
        if (userId.startsWith("-") || StringUtil.isEmpty(userId)) {
            userId = reqJson.getString("userId");
        }

        // 验证用户ID不能为空
        Assert.hasLength(userId, "员工不存在");

        // 查询用户信息
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);

        // 验证只能查询到一个用户
        Assert.listOnlyOne(userDtos, "未查询到用户信息");

        // 将用户信息设置到请求参数中
        reqJson.put("userId", userId);
        reqJson.put("userName", userDtos.get(0).getName());

        int flag = 0;
        // 生成分布式锁的请求ID
        String requestId = DistributedLock.getLockUUID();
        // 生成锁的key，使用类名+报修单ID确保唯一性
        String key = this.getClass().getSimpleName() + reqJson.getString("repairId");
        try {
            // 获取分布式锁，防止并发抢单
            DistributedLock.waitGetDistributedLock(key, requestId);
            //获取当前处理员工id
            String staffId = reqJson.getString("userId");
            /*RepairUserDto repairUser = new RepairUserDto();
            repairUser.setStaffId(staffId);
            repairUser.setState("10001"); //处理中
            int i = repairUserInnerServiceSMOImpl.queryRepairUsersCount(repairUser);*/
            
            // 查询维修师傅当前未处理的报修单数量
            RepairDto repair = new RepairDto();
            repair.setStaffId(staffId);
            repair.setCommunityId(reqJson.getString("communityId"));
            int i = repairInnerServiceSMOImpl.queryStaffRepairsCount(repair);
            
            // 从配置缓存中获取维修师傅最大未处理订单数
            String repairNumber = MappingCache.getValue(MappingConstant.REPAIR_DOMAIN, REPAIR_NUMBER);
            
            // 检查维修师傅当前未处理订单是否超过限制
            if (i >= Integer.parseInt(repairNumber)) {
                ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_BUSINESS_VERIFICATION, "您有超过" + Integer.parseInt(repairNumber) + "条未处理的订单急需处理，请处理完成后再进行抢单！");
                context.setResponseEntity(responseEntity);
                return;
            }
            
            // 查询报修单信息
            RepairDto repairDtoData = new RepairDto();
            repairDtoData.setRepairId(reqJson.getString("repairId"));
            repairDtoData.setCommunityId(reqJson.getString("communityId"));
            List<RepairDto> repairDtoList = repairInnerServiceSMOImpl.queryRepairs(repairDtoData);
            
            // 验证只能查询到一个报修单
            if (repairDtoList != null && repairDtoList.size() != 1) {
                ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_BUSINESS_VERIFICATION, "未找到工单信息或找到多条！");
                context.setResponseEntity(responseEntity);
                return;
            }
            
            // 获取报修类型
            String repairType = repairDtoList.get(0).getRepairType();
            
            // 查询维修师傅是否有处理该报修类型的权限
            RepairTypeUserDto repairTypeUser = new RepairTypeUserDto();
            repairTypeUser.setStaffId(staffId);
            repairTypeUser.setRepairType(repairType);
            // 查询工单设置表
            List<RepairTypeUserDto> repairTypeUserDtos = repairTypeUserInnerServiceSMO.queryRepairTypeUsers(repairTypeUser);
            
            // 验证维修师傅有且只有一条该报修类型的配置记录
            if (repairTypeUserDtos != null && repairTypeUserDtos.size() != 1) { //报修类型设置未添加该操作的员工！
                ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_BUSINESS_VERIFICATION, "对不起，您还没权限进行此操作，请联系管理员处理！");
                context.setResponseEntity(responseEntity);
                return;
            }
            
            // 获取维修师傅状态
            String staffState = repairTypeUserDtos.get(0).getState();
            if (!StringUtil.isEmpty(staffState) && staffState.equals("8888")) { //离线状态
                ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_BUSINESS_VERIFICATION, "员工处于离线状态，无法进行操作！");
                context.setResponseEntity(responseEntity);
                return;
            }
            
            // 再次验证维修师傅对该报修类型的处理权限
            RepairTypeUserDto repairTypeUserDto = new RepairTypeUserDto();
            repairTypeUserDto.setCommunityId(reqJson.getString("communityId"));
            repairTypeUserDto.setRepairType(repairType);
            repairTypeUserDto.setStaffId(reqJson.getString("userId"));
            int count = repairTypeUserInnerServiceSMOImpl.queryRepairTypeUsersCount(repairTypeUserDto);
            if (count < 1) {
                ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_BUSINESS_VERIFICATION, "您没有权限抢该类型报修单！");
                context.setResponseEntity(responseEntity);
                return;
            }
            
            // 获取报修id
            String repairId = reqJson.getString("repairId");
            RepairDto repairDto = new RepairDto();
            repairDto.setRepairId(repairId);
            List<RepairDto> repairDtos = repairInnerServiceSMOImpl.queryRepairs(repairDto);
            
            if (repairDtos == null || repairDtos.size() < 1) {
                ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_ERROR, "数据错误！");
                context.setResponseEntity(responseEntity);
            } else {
                // 获取报修单当前状态
                String state = repairDtos.get(0).getState();
                
                // 根据报修单状态执行不同的处理逻辑
                if (state.equals("1000")) {   //1000表示未派单
                    // 查询报修单的开始节点记录
                    RepairUserDto repairUserDto = new RepairUserDto();
                    repairUserDto.setRepairId(repairId);
                    repairUserDto.setCommunityId(reqJson.getString("communityId"));
                    repairUserDto.setRepairEvent(RepairUserDto.REPAIR_EVENT_START_USER);
                    List<RepairUserDto> repairUserDtos = repairUserInnerServiceSMOImpl.queryRepairUsers(repairUserDto);
                    
                    // 验证只能查询到一条开始节点记录
                    Assert.listOnlyOne(repairUserDtos, "未找到开始节点或找到多条");
                    
                    userId = reqJson.getString("userId");
                    String userName = reqJson.getString("userName");
                    
                    // 创建新的维修用户记录
                    RepairUserPo repairUserPo = new RepairUserPo();
                    repairUserPo.setRuId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_ruId));
                    repairUserPo.setStartTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
                    repairUserPo.setState(RepairUserDto.STATE_DOING);
                    repairUserPo.setRepairId(reqJson.getString("repairId"));
                    repairUserPo.setStaffId(userId);
                    repairUserPo.setStaffName(userName);
                    repairUserPo.setPreStaffId(repairUserDtos.get(0).getStaffId());
                    repairUserPo.setPreStaffName(repairUserDtos.get(0).getStaffName());
                    repairUserPo.setPreRuId(repairUserDtos.get(0).getRuId());
                    repairUserPo.setRepairEvent(RepairUserDto.REPAIR_EVENT_AUDIT_USER);
                    repairUserPo.setContext("");
                    repairUserPo.setCommunityId(reqJson.getString("communityId"));
                    
                    // 保存维修用户记录
                    flag = repairUserV1InnerServiceSMOImpl.saveRepairUserNew(repairUserPo);
                    if (flag < 1) {
                        throw new CmdException("修改用户失败");
                    }
                    
                    // 更新报修单状态为接单中
                    modifyBusinessRepairDispatch(reqJson, RepairDto.STATE_TAKING);
                    ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_OK, ResultVo.MSG_OK);
                    context.setResponseEntity(responseEntity);
                } else if (state.equals("1100")) {   //1100表示接单
                    ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_ERROR, "该订单处于接单状态，无法进行抢单！");
                    context.setResponseEntity(responseEntity);
                } else {
                    ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_ERROR, "状态异常！");
                    context.setResponseEntity(responseEntity);
                }
            }
        } finally {
            // 释放分布式锁
            DistributedLock.releaseDistributedLock(requestId, key);
        }
    }

    /**
     * 修改报修单派单业务
     * 
     * 更新报修单的状态信息
     * 
     * @param paramInJson 请求参数JSON对象
     * @param state 要更新的状态值
     */
    public void modifyBusinessRepairDispatch(JSONObject paramInJson, String state) {
        // 查询报修单信息
        RepairDto repairDto = new RepairDto();
        repairDto.setRepairId(paramInJson.getString("repairId"));
        List<RepairDto> repairDtos = repairInnerServiceSMOImpl.queryRepairs(repairDto);
        
        // 验证只能查询到一条报修单记录
        Assert.isOne(repairDtos, "查询到多条数据，repairId=" + repairDto.getRepairId());
        
        // 创建业务对象并设置新状态
        JSONObject businessOwnerRepair = new JSONObject();
        businessOwnerRepair.putAll(BeanConvertUtil.beanCovertMap(repairDtos.get(0)));
        businessOwnerRepair.put("state", state);
        
        // 计算应收金额并更新报修单
        RepairPoolPo repairPoolPo = BeanConvertUtil.covertBean(businessOwnerRepair, RepairPoolPo.class);
        int flag = repairPoolV1InnerServiceSMOImpl.updateRepairPoolNew(repairPoolPo);
        if (flag < 1) {
            throw new CmdException("修改工单失败");
        }
    }
}