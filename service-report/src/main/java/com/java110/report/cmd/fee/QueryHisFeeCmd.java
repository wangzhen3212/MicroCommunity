/**
 * 费用历史查询命令类
 * 
 * 该命令类用于处理费用变更记录查询请求，通过接收前端传入的参数，
 * 调用相应的服务接口获取费用历史数据，并返回分页格式的查询结果。
 * 主要功能包括参数验证和费用历史记录查询。
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.report.cmd.fee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.intf.report.IReportCommunityInnerServiceSMO;
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
 * 查询费用变更记录
 */
@Java110Cmd(serviceCode = "fee.queryHisFee")
public class QueryHisFeeCmd extends Cmd {

    /**
     * 报表社区内部服务接口，用于查询费用相关数据
     */
    @Autowired
    private IReportCommunityInnerServiceSMO reportCommunityInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 该方法用于验证前端传入的请求参数是否合法，主要检查必填参数是否存在。
     * 如果参数验证失败，会抛出相应的异常。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当参数解析失败时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证请求参数中必须包含小区ID
        Assert.hasKeyAndValue(reqJson,"communityId","未包含小区");
        // 调用父类的属性验证方法，进行额外的参数验证
        super.validateProperty(context);
    }

    /**
     * 命令执行方法
     * 
     * 该方法负责处理费用历史记录查询的核心业务逻辑，包括：
     * 1. 参数转换和分页处理
     * 2. 查询总记录数
     * 3. 查询费用历史记录列表
     * 4. 构建分页响应结果
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当参数解析失败时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 获取分页参数：每页显示行数
        int row = reqJson.getInteger("row");
        // 将请求JSON对象转换为FeeDto对象，便于后续查询
        FeeDto feeDto = BeanConvertUtil.covertBean(reqJson, FeeDto.class);

        // 查询费用历史记录总条数
        int total = reportCommunityInnerServiceSMOImpl.queryHisFeeCount(feeDto);
        
        // 初始化费用历史记录列表
        List<FeeDto> feeDtos = null;
        
        // 根据总记录数判断是否需要查询详细数据
        if (total > 0) {
            // 总记录数大于0时，查询费用历史记录列表
            feeDtos = reportCommunityInnerServiceSMOImpl.queryHisFees(feeDto);
        } else {
            // 总记录数为0时，返回空列表
            feeDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页行数，向上取整
        // 创建分页响应实体，包含总页数、总记录数和数据列表
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity((int) Math.ceil((double) total / (double) row), total, feeDtos);
        // 将响应实体设置到上下文中，返回给前端
        context.setResponseEntity(responseEntity);
    }
}