package com.java110.acct.cmd.account;

import com.alibaba.fastjson.JSONObject;
import com.java110.acct.bmo.account.IGetAccountBMO;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.account.AccountDto;
import com.java110.dto.contract.ContractDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.owner.OwnerAppUserDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.owner.OwnerRoomRelDto;
import com.java110.intf.fee.IFeeInnerServiceSMO;
import com.java110.intf.user.IOwnerAppUserV1InnerServiceSMO;
import com.java110.intf.user.IOwnerV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.List;

/**
 * 查询业主账户命令类
 * 该命令用于处理业主账户查询请求，验证用户身份和权限，并返回业主账户信息
 * 服务代码：account.queryOwnerAccount
 */
@Java110Cmd(serviceCode = "account.queryOwnerAccount")
public class QueryOwnerAccountCmd extends Cmd {

    @Autowired
    private IOwnerAppUserV1InnerServiceSMO ownerAppUserV1InnerServiceSMOImpl;

    @Autowired
    private IOwnerV1InnerServiceSMO ownerV1InnerServiceSMOImpl;

    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    @Autowired
    private IGetAccountBMO getAccountBMOImpl;

    /**
     * 请求参数验证方法
     * 验证请求中是否包含必要的参数，并进行分页信息验证
     *
     * @param event   CmdEvent对象，包含命令事件信息
     * @param context ICmdDataFlowContext对象，数据流上下文
     * @param reqJson JSONObject对象，请求参数JSON
     * @throws CmdException    当命令执行异常时抛出
     * @throws ParseException  当参数解析异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证请求中必须包含小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
        // 调用父类方法验证分页信息
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 处理业主账户查询业务逻辑，包括用户认证状态检查、业主信息查询和账户信息获取
     *
     * @param event   CmdEvent对象，包含命令事件信息
     * @param context ICmdDataFlowContext对象，数据流上下文
     * @param reqJson JSONObject对象，请求参数JSON
     * @throws CmdException    当命令执行异常时抛出
     * @throws ParseException  当参数解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从上下文中获取当前用户ID
        String userId = CmdContextUtils.getUserId(context);
        
        // 构建业主应用用户查询条件
        OwnerAppUserDto ownerAppUserDto = new OwnerAppUserDto();
        ownerAppUserDto.setUserId(userId);
        ownerAppUserDto.setCommunityId(reqJson.getString("communityId"));
        
        // 查询业主应用用户信息
        List<OwnerAppUserDto> ownerAppUserDtos = ownerAppUserV1InnerServiceSMOImpl.queryOwnerAppUsers(ownerAppUserDto);
        
        // 检查用户认证状态：未认证或认证失败
        if (ListUtil.isNull(ownerAppUserDtos)
                || OwnerAppUserDto.STATE_AUDIT_ERROR.equals(ownerAppUserDtos.get(0).getState())) {
            // 未认证属于业务提示，不抛异常中断流程，返回业务校验结果
            context.setResponseEntity(ResultVo.createResponseEntity(ResultVo.CODE_BUSINESS_VERIFICATION, "用户未认证,请先认证", null));
            return;
        }

        // 检查用户认证状态：认证审核中
        if (OwnerAppUserDto.STATE_AUDITING.equals(ownerAppUserDtos.get(0).getState())) {
            ResultVo resultVo = new ResultVo(-101, "认证审核中");
            context.setResponseEntity(ResultVo.createResponseEntity(resultVo));
            return;
        }

        // 构建业主查询条件
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setMemberId(ownerAppUserDtos.get(0).getMemberId());
        ownerDto.setCommunityId(ownerAppUserDtos.get(0).getCommunityId());
        
        // 查询业主信息
        List<OwnerDto> ownerDtos = ownerV1InnerServiceSMOImpl.queryOwners(ownerDto);
        
        // 检查业主是否存在
        if (ListUtil.isNull(ownerDtos)) {
            throw new CmdException("业主不存在");
        }

        // 获取第一个业主信息
        ownerDto = ownerDtos.get(0);

        // 将业主的联系方式和ID添加到请求参数中
        reqJson.put("link", ownerDto.getLink());
        reqJson.put("ownerId", ownerDto.getOwnerId());

        // 将请求参数转换为账户DTO对象
        AccountDto accountDto = BeanConvertUtil.covertBean(reqJson, AccountDto.class);

        // 调用BMO层方法查询业主账户信息
        ResponseEntity<String> responseEntity = getAccountBMOImpl.queryOwnerAccount(accountDto, ownerDto);

        // 设置响应结果
        context.setResponseEntity(responseEntity);
    }
}