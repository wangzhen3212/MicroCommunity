/**
 * 账户明细撤销命令类
 * 
 * 该命令用于撤销已入账的账户明细记录，主要功能包括：
 * 1. 验证撤销请求参数的完整性
 * 2. 查询并验证待撤销的入账明细记录
 * 3. 执行账户余额扣减操作
 * 4. 更新明细状态为已撤销状态
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.acct.cmd.account;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.account.AccountDetailDto;
import com.java110.intf.acct.IAccountDetailInnerServiceSMO;
import com.java110.intf.acct.IAccountInnerServiceSMO;
import com.java110.po.account.AccountDetailPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

@Java110Cmd(serviceCode = "account.cancelAccountDetail")
public class CancelAccountDetailCmd extends Cmd {

    /**
     * 账户明细内部服务接口
     */
    @Autowired
    private IAccountDetailInnerServiceSMO accountDetailInnerServiceSMOImpl;

    /**
     * 账户内部服务接口
     */
    @Autowired
    private IAccountInnerServiceSMO accountInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含撤销操作必需的字段：
     * - 明细ID（detailId）
     * - 小区ID（communityId） 
     * - 撤销原因（remark）
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证明细ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "detailId", "未包含明细");
        // 验证小区ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
        // 验证撤销原因参数是否存在
        Assert.hasKeyAndValue(reqJson, "remark", "未包含撤销原因");
    }

    /**
     * 执行撤销账户明细命令
     * 
     * 该方法在事务中执行以下操作：
     * 1. 根据明细ID查询入账明细记录
     * 2. 验证查询结果的唯一性
     * 3. 执行账户余额扣减操作（撤销入账）
     * 4. 更新明细状态为已撤销状态
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当撤销操作失败时抛出异常
     * @throws ParseException 当数据解析异常时抛出
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 创建查询条件对象，查询待撤销的入账明细
        AccountDetailDto accountDetailDto = new AccountDetailDto();
        accountDetailDto.setDetailId(reqJson.getString("detailId"));
        accountDetailDto.setDetailType(AccountDetailDto.DETAIL_TYPE_IN); // 设置明细类型为入账类型
        List<AccountDetailDto> accountDetailDtos = accountDetailInnerServiceSMOImpl.queryAccountDetails(accountDetailDto);

        // 验证查询结果：必须存在且唯一的一条记录
        Assert.listOnlyOne(accountDetailDtos, "入账明细不存在");

        // 创建账户扣减对象，执行余额扣减操作（撤销入账）
        AccountDetailPo accountDetailPo = new AccountDetailPo();
        accountDetailPo.setAcctId(accountDetailDtos.get(0).getAcctId()); // 设置账户ID
        accountDetailPo.setObjId(accountDetailDtos.get(0).getObjId());   // 设置对象ID
        accountDetailPo.setAmount(accountDetailDtos.get(0).getAmount()); // 设置扣减金额（原入账金额）
        // 设置撤销备注，包含明细ID和撤销原因
        accountDetailPo.setRemark("明细：" + reqJson.getString("detailId") + "撤销，原因：" + reqJson.getString("remark"));
        
        // 执行账户扣减操作
        int flag = accountInnerServiceSMOImpl.withholdAccount(accountDetailPo);
        if (flag < 1) {
            // 扣减操作失败，抛出异常
            throw new CmdException("撤销失败");
        }

        // 创建明细更新对象，将明细状态更新为已撤销
        AccountDetailPo accountDetailPo1 = new AccountDetailPo();
        accountDetailPo1.setDetailId(accountDetailDtos.get(0).getDetailId()); // 设置明细ID
        accountDetailPo1.setDetailType(AccountDetailDto.DETAIL_TYPE_IN_CANCEL); // 设置明细类型为入账撤销状态
        
        // 更新明细状态
        flag = accountDetailInnerServiceSMOImpl.updateAccountDetails(accountDetailPo1);
        if (flag < 1) {
            // 状态更新失败，抛出异常
            throw new CmdException("撤销失败");
        }
    }
}