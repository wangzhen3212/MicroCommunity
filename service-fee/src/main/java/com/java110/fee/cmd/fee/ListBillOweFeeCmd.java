/**
 * 账单欠费查询命令类
 * 
 * 该类用于处理查询账单欠费信息的请求，支持分页查询功能。
 * 通过调用费用服务接口获取欠费数据，并返回标准格式的响应结果。
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.fee.BillOweFeeDto;
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

@Java110Cmd(serviceCode = "fee.listBillOweFee")
public class ListBillOweFeeCmd extends Cmd {

    /**
     * 费用配置服务接口
     * 用于处理费用配置相关的业务逻辑
     */
    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;

    /**
     * 费用服务接口
     * 用于处理费用相关的核心业务逻辑，包括欠费查询
     */
    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数的有效性，确保包含必要的参数和分页信息
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于处理请求和响应
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证小区ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区ID");
    }

    /**
     * 执行命令方法
     * 
     * 处理账单欠费查询请求，查询欠费数据并返回分页结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为业务DTO对象
        BillOweFeeDto billOweFeeDto = BeanConvertUtil.covertBean(reqJson, BillOweFeeDto.class);

        // 查询欠费记录总数
        int count = feeInnerServiceSMOImpl.queryBillOweFeeCount(billOweFeeDto);
        
        List<BillOweFeeDto> billOweFeeDtos = null;
        
        // 根据查询结果数量决定是否查询详细数据
        if (count > 0) {
            // 查询欠费详细数据列表
            billOweFeeDtos = feeInnerServiceSMOImpl.queryBillOweFees(billOweFeeDto);
        } else {
            // 如果没有数据，返回空列表
            billOweFeeDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数 / 每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 构建返回结果对象
        ResultVo resultVo = new ResultVo(totalPage, count, billOweFeeDtos);
        
        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应结果到上下文
        context.setResponseEntity(responseEntity);
    }
}