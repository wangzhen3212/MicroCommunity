/**
 * 积分使用命令类
 * 
 * 负责处理积分核销业务逻辑，包括参数验证、积分计算、账户扣款和积分使用记录保存等功能
 * 通过注解@Java110Cmd声明为服务命令，服务码为"integral.useIntegral"
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.acct.cmd.integral;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.account.AccountDto;
import com.java110.dto.integral.IntegralSettingDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.acct.IAccountInnerServiceSMO;
import com.java110.intf.acct.IIntegralSettingV1InnerServiceSMO;
import com.java110.intf.acct.IIntegralUserDetailV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.account.AccountDetailPo;
import com.java110.po.integral.IntegralUserDetailPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.List;

@Java110Cmd(serviceCode = "integral.useIntegral")
public class UseIntegralCmd extends Cmd {

    /** 积分设置服务接口 */
    @Autowired
    private IIntegralSettingV1InnerServiceSMO integralSettingV1InnerServiceSMOImpl;

    /** 积分用户详情服务接口 */
    @Autowired
    private IIntegralUserDetailV1InnerServiceSMO integralUserDetailV1InnerServiceSMOImpl;

    /** 账户服务接口 */
    @Autowired
    private IAccountInnerServiceSMO accountInnerServiceSMOImpl;

    /** 用户服务接口 */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整，确保必要的参数都存在
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     * @throws ParseException 当数据解析异常时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证账户ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "acctId", "核销账号不存在");
        // 验证使用金额参数是否存在
        Assert.hasKeyAndValue(reqJson, "useMoney", "核销账号不存在");
        // 验证小区ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
    }

    /**
     * 执行积分使用命令
     * 
     * 处理积分核销的核心业务逻辑，包括：
     * 1. 用户信息验证
     * 2. 积分设置查询
     * 3. 账户信息验证
     * 4. 积分计算和扣款
     * 5. 积分使用记录保存
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当业务逻辑执行失败时抛出命令异常
     * @throws ParseException 当数据解析异常时抛出解析异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从请求头中获取用户ID
        String userId = context.getReqHeaders().get("user-id");

        // 查询用户信息
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);
        // 验证用户存在且唯一
        Assert.listOnlyOne(userDtos, "用户不存在");
        
        // 查询积分设置信息
        IntegralSettingDto integralSettingDto = new IntegralSettingDto();
        integralSettingDto.setCommunityId(reqJson.getString("communityId"));
        List<IntegralSettingDto> integralSettingDtos = integralSettingV1InnerServiceSMOImpl.queryIntegralSettings(integralSettingDto);
        // 验证积分设置存在且唯一
        Assert.listOnlyOne(integralSettingDtos, "请设置积分设置");

        // 查询账户信息
        AccountDto accountDto = new AccountDto();
        accountDto.setAcctId(reqJson.getString("acctId"));
        List<AccountDto> accountDtos = accountInnerServiceSMOImpl.queryAccounts(accountDto);
        // 验证账户存在且唯一
        Assert.listOnlyOne(accountDtos, "账户不存在");

        // 获取积分兑换比例，如果设置为0则默认为1
        double settingMoney = Double.parseDouble(integralSettingDtos.get(0).getMoney());
        if(settingMoney == 0){
            settingMoney = 1;
        }
        
        // 计算需要使用的积分数量
        BigDecimal useMoneyDec = new BigDecimal(Double.parseDouble(reqJson.getString("useMoney")));
        // 根据积分兑换比例计算所需积分，保留2位小数，四舍五入
        useMoneyDec = useMoneyDec.divide(new BigDecimal(settingMoney),2, BigDecimal.ROUND_HALF_UP);

        // 将所需积分向上取整转换为整数
        long quantity = new Double(Math.ceil(useMoneyDec.doubleValue())).longValue();
        // 获取账户当前积分余额
        long oldQuantity = new Double(Double.parseDouble(accountDtos.get(0).getAmount())).longValue();

        // 验证积分是否足够
        if (quantity > oldQuantity) {
            throw new CmdException("当前积分不够(" + oldQuantity + ")");
        }

        // 创建账户扣款明细对象
        AccountDetailPo accountDetailPo = new AccountDetailPo();
        accountDetailPo.setObjType(accountDtos.get(0).getObjType());
        accountDetailPo.setObjId(accountDtos.get(0).getObjId());
        accountDetailPo.setAcctId(accountDtos.get(0).getAcctId());
        accountDetailPo.setRemark(reqJson.getString("remark"));
        accountDetailPo.setAmount(quantity + "");
        
        // 执行账户扣款操作
        int flag = accountInnerServiceSMOImpl.withholdAccount(accountDetailPo);
        if (flag < 1) {
            throw new CmdException("扣款失败");
        }

        // 创建积分使用记录对象
        IntegralUserDetailPo integralUserDetailPo = new IntegralUserDetailPo();
        integralUserDetailPo.setAcctDetailId("-1"); // 默认值
        integralUserDetailPo.setAcctId(reqJson.getString("acctId"));
        integralUserDetailPo.setAcctName(accountDtos.get(0).getAcctName());
        integralUserDetailPo.setDetailType("1001"); // 积分使用类型
        integralUserDetailPo.setBusinessKey("-1"); // 默认业务键
        integralUserDetailPo.setUseQuantity(quantity + ""); // 使用积分数量
        integralUserDetailPo.setMoney(reqJson.getString("useMoney")); // 使用金额
        integralUserDetailPo.setRemark(reqJson.getString("remark")); // 备注信息
        integralUserDetailPo.setCreateUserId(userDtos.get(0).getUserId()); // 创建用户ID
        integralUserDetailPo.setUserName(userDtos.get(0).getName()); // 用户姓名
        integralUserDetailPo.setTel(userDtos.get(0).getTel()); // 用户电话
        integralUserDetailPo.setCommunityId(reqJson.getString("communityId")); // 小区ID
        integralUserDetailPo.setUoId(GenerateCodeFactory.getGeneratorId("11")); // 生成唯一ID
        
        // 保存积分使用记录
        integralUserDetailV1InnerServiceSMOImpl.saveIntegralUserDetail(integralUserDetailPo);
    }
}