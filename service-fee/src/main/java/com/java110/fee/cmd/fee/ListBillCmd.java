/**
 * 账单查询命令类
 * 
 * 该类用于处理账单列表查询请求，负责验证查询参数并返回账单数据
 * 通过注解@Java110Cmd(serviceCode = "fee.listBill")声明服务代码
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.fee.BillDto;
import com.java110.intf.fee.IFeeConfigInnerServiceSMO;
import com.java110.intf.fee.IFeeInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "fee.listBill")
public class ListBillCmd extends Cmd {

    /**
     * 费用配置内部服务接口
     */
    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;

    /**
     * 费用内部服务接口
     */
    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，包括分页信息和必要参数
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于处理数据流
     * @param reqJson 请求JSON对象，包含前端传递的参数
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 调用父类方法验证分页信息
        super.validatePageInfo(reqJson);
        // 验证请求中必须包含小区ID参数
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区ID");
    }

    /**
     * 执行命令方法
     * 
     * 处理账单查询业务逻辑，包括查询账单数量和账单列表，并构建响应结果
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当命令执行过程中出现错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为BillDto对象
        BillDto billDto = BeanConvertUtil.covertBean(reqJson, BillDto.class);

        // 查询账单总数量
        int count = feeInnerServiceSMOImpl.queryBillCount(billDto);
        List<BillDto> billDtos = null;
        
        // 根据查询结果数量决定返回数据
        if (count > 0) {
            // 如果存在数据，查询账单列表
            billDtos = feeInnerServiceSMOImpl.queryBills(billDto);
        } else {
            // 如果没有数据，返回空列表
            billDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页显示行数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 构建返回结果对象
        ResultVo resultVo = new ResultVo(totalPage, count, billDtos);
        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应实体设置到数据流上下文中
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}