package com.java110.acct.cmd.account;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.doc.annotation.*;
import com.java110.dto.account.AccountDto;
import com.java110.dto.integral.IntegralConfigDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.acct.IAccountInnerServiceSMO;
import com.java110.intf.acct.IIntegralConfigV1InnerServiceSMO;
import com.java110.intf.acct.IIntegralGiftDetailV1InnerServiceSMO;
import com.java110.intf.user.IOwnerV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.account.AccountPo;
import com.java110.po.account.AccountDetailPo;
import com.java110.po.integral.IntegralGiftDetailPo;
import com.java110.service.smo.ISaveSystemErrorSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.lock.DistributedLock;
import com.java110.utils.util.Assert;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 积分赠送规则，提供第三方平台使用
 */
@Java110CmdDoc(title = "积分赠送",
        description = "三方平台赠送积分给用户",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/account.customSendAcct",
        resource = "acctDoc",
        author = "吴学文",
        serviceCode = "account.customSendAcct"
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "link", length = 30, remark = "手机号"),
        @Java110ParamDoc(name = "quantity", length = 30, remark = "赠送积分数量"),
        @Java110ParamDoc(name = "acctType", length = 30, remark = "账户类型 2003 现金账户 2004 积分账户 2006 金币账户"),
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        }
)

@Java110ExampleDoc(
        reqBody = "{'link':'18909711443','quantity':'10','acctType':'2003','communityId':'12323123'}",
        resBody = "{'code':0,'msg':'成功'}"
)
@Java110Cmd(serviceCode = "account.customSendAcct")
/**
 * 自定义账户赠送命令类
 * 处理第三方平台向用户账户赠送积分、现金等操作
 * 支持多种账户类型：现金账户、积分账户、金币账户
 * 
 * @author 吴学文
 * @version 1.0
 */
public class CustomSendAcctCmd extends Cmd {

    @Autowired
    private IIntegralGiftDetailV1InnerServiceSMO integralGiftDetailV1InnerServiceSMOImpl;

    @Autowired
    private IAccountInnerServiceSMO accountInnerServiceSMOImpl;

    @Autowired
    private ISaveSystemErrorSMO saveSystemErrorSMOImpl;

    @Autowired
    private IOwnerV1InnerServiceSMO ownerInnerServiceSMOImpl;

    @Autowired
    private IIntegralConfigV1InnerServiceSMO integralConfigV1InnerServiceSMOImpl;

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效
     *
     * @param event 命令事件对象，包含命令执行上下文信息
     * @param context 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数JSON对象，包含所有请求参数
     * @throws CmdException 当参数验证失败时抛出命令异常
     * @throws ParseException 当参数解析失败时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证必填参数是否存在
        Assert.hasKeyAndValue(reqJson, "link", "未包含手机号");
        Assert.hasKeyAndValue(reqJson, "acctType", "未包含账户类型");
        Assert.hasKeyAndValue(reqJson, "quantity", "未包含赠送数量");
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
    }

    /**
     * 执行账户赠送命令
     * 主要逻辑：查询账户、创建账户（如不存在）、预存金额、处理积分赠送
     *
     * @param event 命令事件对象，包含命令执行上下文信息
     * @param context 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数JSON对象，包含所有请求参数
     * @throws CmdException 当业务逻辑执行失败时抛出命令异常
     * @throws ParseException 当数据解析失败时抛出解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 获取操作用户ID，从请求头中获取用户标识
        String createUserId = context.getReqHeaders().get("user-id");

        // 构建账户查询条件，根据手机号、账户类型和小区查询账户信息
        AccountDto accountDto = new AccountDto();
        accountDto.setLink(reqJson.getString("link"));
        accountDto.setAcctType(reqJson.getString("acctType"));
        accountDto.setPartId(reqJson.getString("communityId"));
        
        // 查询账户信息，检查账户是否存在
        List<AccountDto> accountDtos = accountInnerServiceSMOImpl.queryAccounts(accountDto);

        // 如果账户不存在，则创建新账户
        if (accountDtos == null || accountDtos.size() < 1) {
            accountDtos = addAccountDto(accountDto, reqJson);
        }

        // 创建账户明细对象并设置参数，用于记录账户变动
        AccountDetailPo accountDetailPo = new AccountDetailPo();
        accountDetailPo.setAcctId(accountDtos.get(0).getAcctId()); // 设置账户ID
        accountDetailPo.setObjId(accountDtos.get(0).getObjId()); // 设置对象ID
        accountDetailPo.setObjType(accountDtos.get(0).getObjType()); // 设置对象类型
        accountDetailPo.setAmount(reqJson.getString("quantity")); // 设置赠送金额/积分数量
        accountDetailPo.setRemark("三方平台通过手工赠送"); // 设置操作备注
        
        // 执行预存操作，将金额/积分存入账户
        int flag = accountInnerServiceSMOImpl.prestoreAccount(accountDetailPo);

        // 检查预存操作是否成功，flag小于1表示操作失败
        if (flag < 1) {
            throw new CmdException("扣款失败");
        }

        // 如果是积分账户类型（2004），则额外处理积分赠送逻辑
        if("2004".equals(reqJson.getString("acctType"))) {
            doGiftIntegral(accountDtos.get(0), reqJson, createUserId);
        }
    }

    /**
     * 添加账户DTO对象
     * 当用户账户不存在时，创建新的账户记录
     *
     * @param accountDto 账户数据传输对象，包含账户基本信息
     * @param reqJson 请求参数JSON对象，包含创建账户所需参数
     * @return 账户DTO列表，包含新创建或查询到的账户信息
     */
    private List<AccountDto> addAccountDto(AccountDto accountDto, JSONObject reqJson) {
        // 获取账户名称：优先使用业主名称，其次使用用户名称
        String acctName = getAccountName(reqJson);

        // 如果账户名称为空，返回空列表，表示无法创建账户
        if (StringUtil.isEmpty(acctName)) {
            return new ArrayList<>();
        }
        
        // 使用分布式锁确保账户创建的原子性，防止并发创建重复账户
        String requestId = DistributedLock.getLockUUID(); // 生成唯一锁标识
        String key = this.getClass().getSimpleName() + "AddCountDto" + reqJson.getString("link"); // 构建锁的key
        try {
            // 获取分布式锁，确保同一时间只有一个线程可以创建账户
            DistributedLock.waitGetDistributedLock(key, requestId);

            // 创建账户PO对象并设置参数
            AccountPo accountPo = new AccountPo();
            accountPo.setAmount("0"); // 初始账户余额设为0
            accountPo.setAcctId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_acctId)); // 生成账户ID
            accountPo.setObjId(reqJson.getString("objId")); // 设置对象ID
            accountPo.setObjType(AccountDto.OBJ_TYPE_PERSON); // 设置对象类型为个人
            accountPo.setAcctType(reqJson.getString("acctType")); // 设置账户类型
            accountPo.setAcctName(acctName); // 设置账户名称
            accountPo.setPartId(reqJson.getString("communityId")); // 设置小区ID
            accountPo.setLink(reqJson.getString("link")); // 设置关联手机号
            
            // 保存账户信息到数据库
            accountInnerServiceSMOImpl.saveAccount(accountPo);
            
            // 重新查询账户信息并返回，确保获取到最新的账户数据
            List<AccountDto> accountDtos = accountInnerServiceSMOImpl.queryAccounts(accountDto);
            return accountDtos;
        } finally {
            // 释放分布式锁，确保锁一定会被释放
            DistributedLock.releaseDistributedLock(requestId, key);
        }
    }

    /**
     * 获取账户名称
     * 根据手机号查询业主或用户信息，获取账户显示名称
     *
     * @param reqJson 请求参数JSON对象，包含手机号和小区ID
     * @return 账户名称，优先返回业主名称，其次返回用户名称
     * @throws CmdException 当既不是业主也不是用户时抛出异常
     */
    private String getAccountName(JSONObject reqJson) {
        // 优先查询业主信息，根据手机号和小区ID查询
        OwnerDto tmpOwnerDto = new OwnerDto();
        tmpOwnerDto.setLink(reqJson.getString("link")); // 设置手机号
        tmpOwnerDto.setCommunityId(reqJson.getString("communityId")); // 设置小区ID
        List<OwnerDto> ownerDtos = ownerInnerServiceSMOImpl.queryOwners(tmpOwnerDto);
        
        // 如果找到业主，使用业主信息
        if (!ListUtil.isNull(ownerDtos)) {
            reqJson.put("objId", ownerDtos.get(0).getMemberId()); // 将业主ID设置到请求参数中
            return ownerDtos.get(0).getName(); // 返回业主名称
        }

        // 如果业主不存在，查询用户信息（游客）
        UserDto userDto = new UserDto();
        userDto.setTel(reqJson.getString("link")); // 设置手机号查询用户
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);
        
        // 如果找到用户，使用用户信息
        if (userDtos != null && userDtos.size() > 0) {
            reqJson.put("objId", "-1"); // 设置对象ID为-1，表示游客
            return userDtos.get(0).getName(); // 返回用户名称
        }
        
        // 既不是业主也不是用户，抛出异常
        throw new CmdException("业主不存在");
    }

    /**
     * 处理积分赠送逻辑
     * 为积分账户创建积分赠送明细记录
     *
     * @param accountDto 账户数据传输对象，包含账户基本信息
     * @param reqJson 请求参数JSON对象，包含赠送相关信息
     * @param createUserId 创建用户ID，记录操作人
     * @throws CmdException 当积分规则不存在或业主不存在时抛出异常
     */
    private void doGiftIntegral(AccountDto accountDto, JSONObject reqJson,String createUserId) {
        // 查询积分配置信息，根据小区ID查询积分规则
        IntegralConfigDto integralConfigDto = new IntegralConfigDto();
        integralConfigDto.setCommunityId(reqJson.getString("communityId")); // 设置小区ID
        List<IntegralConfigDto> integralConfigDtos = integralConfigV1InnerServiceSMOImpl.queryIntegralConfigs(integralConfigDto);
        
        // 验证积分规则是否存在，不存在则抛出异常
        if(integralConfigDtos == null || integralConfigDtos.size()< 1){
            throw new CmdException("积分规则不存在，请添加");
        }

        // 查询业主信息，确保业主存在
        OwnerDto tmpOwnerDto = new OwnerDto();
        tmpOwnerDto.setLink(reqJson.getString("link")); // 设置手机号
        tmpOwnerDto.setCommunityId(reqJson.getString("communityId")); // 设置小区ID
        List<OwnerDto> ownerDtos = ownerInnerServiceSMOImpl.queryOwners(tmpOwnerDto);
        
        // 如果业主不存在，直接返回，不记录积分赠送明细
        if (ownerDtos != null && ownerDtos.size() > 0) {
            return;
        }
        
        // 创建积分赠送明细记录，记录积分赠送的详细信息
        IntegralGiftDetailPo integralGiftDetailPo = new IntegralGiftDetailPo();
        integralGiftDetailPo.setCommunityId(reqJson.getString("communityId")); // 设置小区ID
        integralGiftDetailPo.setDetailId(GenerateCodeFactory.getGeneratorId("11")); // 生成明细ID
        integralGiftDetailPo.setConfigId(integralConfigDtos.get(0).getConfigId()); // 设置配置ID
        integralGiftDetailPo.setConfigName(integralConfigDtos.get(0).getConfigName()); // 设置配置名称
        integralGiftDetailPo.setRuleId("-1"); // 设置规则ID为-1，表示手工赠送
        integralGiftDetailPo.setRuleName(reqJson.getString("ruleName")); // 设置规则名称
        integralGiftDetailPo.setQuantity(reqJson.getString("quantity")); // 设置赠送数量
        integralGiftDetailPo.setCreateUserId(createUserId); // 设置创建用户ID
        integralGiftDetailPo.setUserName(ownerDtos.get(0).getName()); // 设置用户名称
        integralGiftDetailPo.setTel(ownerDtos.get(0).getLink()); // 设置用户手机号
        
        // 保存积分赠送明细到数据库
        integralGiftDetailV1InnerServiceSMOImpl.saveIntegralGiftDetail(integralGiftDetailPo);
    }
}