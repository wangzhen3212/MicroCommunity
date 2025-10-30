package com.java110.acct.cmd.account;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.account.AccountDetailDto;
import com.java110.intf.acct.IAccountDetailInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 查询管理员账户明细命令类
 * 
 * 该类负责处理管理员查询账户明细的请求，包括参数验证、数据查询和结果返回等功能。
 * 通过注解@Java110Cmd标识服务代码为"account.queryAdminAccountDetail"
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "account.queryAdminAccountDetail")
public class QueryAdminAccountDetailCmd extends Cmd {

    /**
     * 账户明细内部服务接口，用于查询账户明细数据
     */
    @Autowired
    private IAccountDetailInnerServiceSMO accountDetailInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，包括管理员权限验证和分页信息验证
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行出现异常时抛出
     * @throws ParseException 当参数解析出现异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证管理员权限
        super.validateAdmin(context);
        // 验证分页参数（如页码、每页记录数等）
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 
     * 处理查询管理员账户明细的核心业务逻辑，包括数据查询、结果封装和响应设置
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行出现异常时抛出
     * @throws ParseException 当参数解析出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将JSON请求参数转换为AccountDetailDto对象
        AccountDetailDto accountDetailDto = BeanConvertUtil.covertBean(reqJson, AccountDetailDto.class);
        
        // 查询账户明细总记录数
        int count = accountDetailInnerServiceSMOImpl.queryAccountDetailsCount(accountDetailDto);

        List<AccountDetailDto> accountDetailDtos = null;
        // 根据查询结果判断是否有数据
        if (count > 0) {
            // 有数据时查询具体的账户明细列表
            accountDetailDtos = accountDetailInnerServiceSMOImpl.queryAccountDetails(accountDetailDto);
        } else {
            // 无数据时返回空列表
            accountDetailDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数 / 每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) accountDetailDto.getRow());
        // 封装返回结果，包含总页数、总记录数和数据列表
        ResultVo resultVo = new ResultVo(totalPage, count, accountDetailDtos);

        // 创建HTTP响应实体，设置响应状态为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        // 将响应实体设置到上下文中
        context.setResponseEntity(responseEntity);
    }
}