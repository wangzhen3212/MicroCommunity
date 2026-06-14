/**
 * 费用配置查询命令类
 * 
 * 该命令类用于查询费用配置关联的费用对象列表，支持分页查询功能
 * 通过调用费用配置服务获取费用对象数据，并返回标准格式的响应结果
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.fee.cmd.feeConfig;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.dto.fee.FeeDto;
import com.java110.intf.fee.IPayFeeConfigV1InnerServiceSMO;
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
 * 查询费用配置关联的费用对象命令类
 * 服务代码：feeConfig.listConfigFeeObjs
 * 主要功能：根据费用配置ID查询关联的费用对象列表，支持分页显示
 */
@Java110Cmd(serviceCode = "feeConfig.listConfigFeeObjs")
public class ListConfigFeeObjsCmd extends Cmd {

    /**
     * 费用配置服务接口，用于查询费用配置相关信息
     */
    @Autowired
    private IPayFeeConfigV1InnerServiceSMO payFeeConfigV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段，确保后续业务逻辑能够正常执行
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于处理请求和响应
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行出现异常时抛出
     * @throws ParseException 当参数解析出现异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证请求参数中必须包含小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
        // 验证请求参数中必须包含费用配置ID
        Assert.hasKeyAndValue(reqJson, "configId", "未包含费用项");
        // 调用父类的属性验证方法
        super.validateProperty(context);
    }

    /**
     * 命令执行方法
     * 
     * 根据费用配置信息查询关联的费用对象列表，并返回分页格式的响应结果
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于处理请求和响应
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行出现异常时抛出
     * @throws ParseException 当参数解析出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求JSON对象转换为FeeConfigDto对象
        FeeConfigDto feeConfigDto = BeanConvertUtil.covertBean(reqJson, FeeConfigDto.class);

        // 查询费用对象的总记录数
        int count = payFeeConfigV1InnerServiceSMOImpl.queryFeeObjsCount(feeConfigDto);

        // 费用对象列表
        List<FeeDto> feeDtos = null;

        // 根据查询结果数量判断是否查询具体数据
        if (count > 0) {
            // 如果存在数据，查询费用对象列表并转换为FeeDto对象列表
            feeDtos = BeanConvertUtil.covertBeanList(payFeeConfigV1InnerServiceSMOImpl.queryFeeObjs(feeConfigDto), FeeDto.class);
        } else {
            // 如果没有数据，返回空列表
            feeDtos = new ArrayList<>();
        }

        // 创建分页响应实体，包含总页数、总记录数和数据列表
        ResponseEntity<String> responseEntity
                = ResultVo.createResponseEntity(
                (int) Math.ceil((double) count / (double) reqJson.getInteger("row")), // 计算总页数
                count, // 总记录数
                feeDtos); // 数据列表

        // 设置响应实体到上下文
        context.setResponseEntity(responseEntity);
    }
}