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
 * 费用配置管理命令类
 * 负责查询管理员配置的费用对象列表
 * 服务代码：feeConfig.listAdminConfigFeeObjs
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "feeConfig.listAdminConfigFeeObjs")
public class ListAdminConfigFeeObjsCmd extends Cmd {

    /**
     * 费用配置服务接口，用于查询费用对象相关信息
     */
    @Autowired
    private IPayFeeConfigV1InnerServiceSMO payFeeConfigV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整并检查管理员权限
     *
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     * @throws ParseException 当参数解析失败时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证请求参数中必须包含configId字段
        Assert.hasKeyAndValue(reqJson, "configId", "未包含费用项");
        // 验证当前用户是否具有管理员权限
        super.validateAdmin(context);
    }

    /**
     * 命令执行方法
     * 查询费用对象列表并返回分页结果
     *
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，用于设置响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当数据解析过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求JSON对象转换为FeeConfigDto对象
        FeeConfigDto feeConfigDto = BeanConvertUtil.covertBean(reqJson, FeeConfigDto.class);

        // 查询符合条件的费用对象总数
        int count = payFeeConfigV1InnerServiceSMOImpl.queryFeeObjsCount(feeConfigDto);

        List<FeeDto> feeDtos = null;

        // 根据查询结果数量决定如何处理数据
        if (count > 0) {
            // 如果存在数据，查询费用对象列表并转换为FeeDto对象列表
            feeDtos = BeanConvertUtil.covertBeanList(payFeeConfigV1InnerServiceSMOImpl.queryFeeObjs(feeConfigDto), FeeDto.class);
        } else {
            // 如果没有数据，返回空列表
            feeDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页显示数量，向上取整
        // 创建包含分页信息的响应实体
        ResponseEntity<String> responseEntity
                = ResultVo.createResponseEntity(
                (int) Math.ceil((double) count / (double) reqJson.getInteger("row")),
                count,
                feeDtos);

        // 将响应实体设置到上下文中
        context.setResponseEntity(responseEntity);
    }
}