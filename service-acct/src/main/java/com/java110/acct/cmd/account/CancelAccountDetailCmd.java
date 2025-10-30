/**
 * 账户明细撤销命令类
 * 
 * 该命令用于撤销已入账的账户明细记录，主要功能包括：
 * 1. 验证撤销请求参数的完整性
 * 2. 查询并验证待撤销的入账明细记录
 * 3. 执行账户余额扣减操作
 * 4. 更新明细状态为已撤销状态
 * 
 * 使用@Java110Cmd注解标识为命令类，serviceCode指定服务编码
 * 继承Cmd基类，实现命令模式的处理流程
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

/**
 * 撤销账户明细命令类
 * 
 * 负责处理账户明细的撤销操作，包括参数验证、数据查询、余额扣减和状态更新等业务流程
 * 通过命令模式实现，确保操作的原子性和数据一致性
 */
@Java110Cmd(serviceCode = "account.cancelAccountDetail")
public class CancelAccountDetailCmd extends Cmd {

    /**
     * 账户明细内部服务接口
     * 用于查询和更新账户明细信息
     */
    @Autowired
    private IAccountDetailInnerServiceSMO accountDetailInnerServiceSMOImpl;

    /**
     * 账户内部服务接口
     * 用于执行账户余额的扣减操作
     */
    @Autowired
    private IAccountInnerServiceSMO accountInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含撤销操作必需的字段：
     * - 明细ID（detailId）：标识要撤销的具体明细记录
     * - 小区ID（communityId）：业务上下文标识
     * - 撤销原因（remark）：记录撤销操作的原因
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象，包含所有请求参数
     * @throws CmdException 当参数验证失败时抛出异常，包含具体的错误信息
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证明细ID参数是否存在，确保指定要撤销的明细记录
        Assert.hasKeyAndValue(reqJson, "detailId", "未包含明细");
        // 验证小区ID参数是否存在，确保业务上下文完整
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
        // 验证撤销原因参数是否存在，确保操作有据可查
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
     * 使用@Java110Transactional注解确保操作的原子性，要么全部成功，要么全部回滚
     *
     * @param event 命令事件对象，包含命令执行上下文
     * @param context 命令数据流上下文，用于处理请求响应
     * @param reqJson 请求参数的JSON对象，包含撤销操作所需参数
     * @throws CmdException 当撤销操作失败时抛出异常，包含操作失败的具体原因
     * @throws ParseException 当数据解析异常时抛出，通常由日期或数字格式问题引起
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 创建查询条件对象，用于查询待撤销的入账明细记录
        AccountDetailDto accountDetailDto = new AccountDetailDto();
        // 设置要查询的明细ID，从请求参数中获取
        accountDetailDto.setDetailId(reqJson.getString("detailId"));
        // 设置明细类型为入账类型，确保只查询入账记录
        accountDetailDto.setDetailType(AccountDetailDto.DETAIL_TYPE_IN);
        // 执行查询操作，获取符合条件的明细记录列表
        List<AccountDetailDto> accountDetailDtos = accountDetailInnerServiceSMOImpl.queryAccountDetails(accountDetailDto);

        // 验证查询结果：必须存在且唯一的一条记录，防止数据不一致
        Assert.listOnlyOne(accountDetailDtos, "入账明细不存在");

        // 创建账户扣减对象，用于执行余额扣减操作（撤销入账）
        AccountDetailPo accountDetailPo = new AccountDetailPo();
        // 设置账户ID，从查询结果中获取原记录的账户ID
        accountDetailPo.setAcctId(accountDetailDtos.get(0).getAcctId());
        // 设置对象ID，从查询结果中获取原记录的对象ID
        accountDetailPo.setObjId(accountDetailDtos.get(0).getObjId());
        // 设置扣减金额，使用原入账金额进行反向操作
        accountDetailPo.setAmount(accountDetailDtos.get(0).getAmount());
        // 设置撤销备注，记录撤销操作的详细信息，便于后续审计
        accountDetailPo.setRemark("明细：" + reqJson.getString("detailId") + "撤销，原因：" + reqJson.getString("remark"));
        
        // 执行账户扣减操作，撤销原入账记录对余额的影响
        int flag = accountInnerServiceSMOImpl.withholdAccount(accountDetailPo);
        if (flag < 1) {
            // 扣减操作失败，抛出异常并回滚事务
            throw new CmdException("撤销失败");
        }

        // 创建明细更新对象，用于更新明细状态为已撤销
        AccountDetailPo accountDetailPo1 = new AccountDetailPo();
        // 设置要更新的明细ID，确保只更新目标记录
        accountDetailPo1.setDetailId(accountDetailDtos.get(0).getDetailId());
        // 设置明细类型为入账撤销状态，标识该记录已被撤销
        accountDetailPo1.setDetailType(AccountDetailDto.DETAIL_TYPE_IN_CANCEL);
        
        // 更新明细状态，将记录标记为已撤销
        flag = accountDetailInnerServiceSMOImpl.updateAccountDetails(accountDetailPo1);
        if (flag < 1) {
            // 状态更新失败，抛出异常并回滚事务
            throw new CmdException("撤销失败");
        }
    }
}