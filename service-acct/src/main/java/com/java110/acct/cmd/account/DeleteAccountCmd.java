package com.java110.acct.cmd.account;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.acct.IAccountInnerServiceSMO;
import com.java110.intf.acct.IAccountDetailInnerServiceSMO;
import com.java110.po.account.AccountPo;
import com.java110.utils.constant.StatusConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 删除账户命令类
 * 
 * 该类负责处理账户删除相关的业务逻辑，通过将账户状态标记为无效来实现逻辑删除
 * 采用命令模式设计，遵循Java110框架规范
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode account.deleteAccount
 */
@Java110Cmd(serviceCode = "account.deleteAccount")
public class DeleteAccountCmd extends Cmd {

    /**
     * 账户内部服务接口，用于操作账户数据
     */
    @Autowired
    private IAccountInnerServiceSMO accountInnerServiceSMOImpl;

    /**
     * 账户明细内部服务接口（当前类中未使用，但保留以备后续扩展）
     */
    @Autowired
    private IAccountDetailInnerServiceSMO accountDetailInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的acctId字段
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 验证请求JSON中必须包含acctId字段
        Assert.hasKeyAndValue(reqJson, "acctId", "acctId不能为空");
    }

    /**
     * 执行删除账户命令
     * 
     * 通过将账户状态更新为无效状态来实现逻辑删除
     * 如果更新失败则抛出异常
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当账户更新失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 创建账户PO对象并设置参数
        AccountPo accountPo = new AccountPo();
        // 设置要删除的账户ID
        accountPo.setAcctId(reqJson.getString("acctId"));
        // 将账户状态设置为无效状态，实现逻辑删除
        accountPo.setStatusCd(StatusConstant.STATUS_CD_INVALID);
        
        // 调用服务更新账户状态
        int flag = accountInnerServiceSMOImpl.updateAccount(accountPo);

        // 检查更新结果，如果影响行数小于1表示更新失败
        if (flag < 1) {
            throw new CmdException("更新失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}