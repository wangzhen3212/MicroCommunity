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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 查询押金命令类
 * 负责处理查询押金相关的业务逻辑，包括参数验证和押金数据查询
 * 通过服务码"fee.queryFeeDeposit"标识，用于系统中押金查询功能的实现
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "fee.queryFeeDeposit")
public class QueryFeeDepositCmd extends Cmd {

    /**
     * 费用明细服务接口，用于查询押金相关数据
     */
    @Autowired
    private IPayFeeDetailV1InnerServiceSMO feeDetailV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性，包括分页信息和必要字段的完整性
     *
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行出现异常时抛出
     * @throws ParseException 当参数解析出现异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证分页信息，确保分页参数正确
        super.validatePageInfo(reqJson);
        // 验证属性信息，确保必要属性存在
        super.validateProperty(context);
        // 验证小区ID是否存在，小区ID为必填参数
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
    }

    /**
     * 命令执行方法
     * 执行押金查询业务逻辑，包括数据查询和结果返回
     *
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行出现异常时抛出
     * @throws ParseException 当参数解析出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求JSON对象转换为FeeDetailDto对象，便于后续处理
        FeeDetailDto feeDetailDto = BeanConvertUtil.covertBean(reqJson, FeeDetailDto.class);

        // 查询押金总记录数，用于分页计算
        int total = feeDetailV1InnerServiceSMOImpl.queryFeeDepositCount(feeDetailDto);
        
        List<FeeDetailDto> feeDetailDtos = null;
        
        // 根据查询结果数量决定是否查询详细数据
        if (total > 0) {
            // 当存在数据时，查询押金详细信息列表
            feeDetailDtos = feeDetailV1InnerServiceSMOImpl.queryFeeDeposit(BeanConvertUtil.covertBean(reqJson, FeeDetailDto.class));
        } else {
            // 当无数据时，返回空列表
            feeDetailDtos = new ArrayList<>();
        }

        // 获取每页显示行数
        int row = reqJson.getInteger("row");
        // 计算总页数：总记录数除以每页行数，向上取整
        int totalPage = (int) Math.ceil((double) total / (double) row);
        // 创建响应实体，包含分页信息和数据列表
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(totalPage, total, feeDetailDtos);
        // 设置响应实体到上下文
        context.setResponseEntity(responseEntity);
    }
}