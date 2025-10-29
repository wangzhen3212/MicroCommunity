package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.fee.FeeDetailDto;
import com.java110.intf.fee.IPayFeeDetailV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 查询押金命令类
 * 负责处理查询管理员押金相关费用的业务逻辑
 * 通过服务码"fee.queryAdminFeeDeposit"标识，用于系统命令调度
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "fee.queryAdminFeeDeposit")
public class QueryAdminFeeDepositCmd extends Cmd {

    /**
     * 费用详情服务接口，用于查询押金相关数据
     */
    @Autowired
    private IPayFeeDetailV1InnerServiceSMO feeDetailV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性和管理员权限
     *
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行出现异常时抛出
     * @throws ParseException 当参数解析出现异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证管理员权限
        super.validateAdmin(context);
    }

    /**
     * 命令执行方法
     * 处理查询管理员押金的业务逻辑，包括数据查询和响应构建
     *
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，用于设置响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行出现异常时抛出
     * @throws ParseException 当参数解析出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求JSON转换为FeeDetailDto对象
        FeeDetailDto feeDetailDto = BeanConvertUtil.covertBean(reqJson, FeeDetailDto.class);

        // 查询押金总记录数
        int total = feeDetailV1InnerServiceSMOImpl.queryFeeDepositCount(feeDetailDto);
        List<FeeDetailDto> feeDetailDtos = null;
        
        // 根据查询结果决定是否查询详细数据
        if (total > 0) {
            // 查询押金详细数据
            feeDetailDtos = feeDetailV1InnerServiceSMOImpl.queryFeeDeposit(BeanConvertUtil.covertBean(reqJson, FeeDetailDto.class));
        } else {
            // 如果没有数据，返回空列表
            feeDetailDtos = new ArrayList<>();
        }

        // 获取每页显示行数
        int row = reqJson.getInteger("row");
        // 计算总页数并构建响应实体
        // 使用Math.ceil确保向上取整，正确处理分页
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity((int) Math.ceil((double) total / (double) row), total, feeDetailDtos);
        // 设置响应到上下文
        context.setResponseEntity(responseEntity);
    }
}