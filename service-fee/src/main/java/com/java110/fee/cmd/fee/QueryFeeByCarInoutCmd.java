/**
 * 费用查询命令类 - 根据车辆进出记录查询费用
 * 
 * 该命令类用于处理通过车辆进出记录ID查询相关费用的业务逻辑，
 * 主要应用于停车场临时停车费用查询等场景
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
import com.java110.dto.fee.FeeDto;
import com.java110.intf.fee.IFeeInnerServiceSMO;
import com.java110.utils.constant.FeeTypeConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.api.ApiMainFeeVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Java110Cmd(serviceCode = "fee.queryFeeByCarInout")
public class QueryFeeByCarInoutCmd extends Cmd {

    /**
     * 费用内部服务接口，用于查询费用相关信息
     */
    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求JSON中是否包含必要的参数，确保后续业务逻辑能够正常执行
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求中必须包含小区ID
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求中未包含communityId信息");
        // 验证请求中必须包含进出记录ID
        Assert.jsonObjectHaveKey(reqJson, "inoutId", "请求中未包含inoutId信息");
        // 验证请求中必须包含费用状态
        Assert.jsonObjectHaveKey(reqJson, "state", "请求中未包含state信息");
    }

    /**
     * 命令执行方法
     * 
     * 根据车辆进出记录查询相关费用信息，主要处理临时停车费用查询业务
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为费用数据传输对象
        FeeDto feeDtoParamIn = BeanConvertUtil.covertBean(reqJson, FeeDto.class);
        
        // 设置缴费对象ID为进出记录ID
        feeDtoParamIn.setPayerObjId(reqJson.getString("inoutId"));
        // 设置费用状态
        feeDtoParamIn.setState(reqJson.getString("state"));
        // 设置费用类型为临时停车费用
        feeDtoParamIn.setFeeTypeCd(FeeTypeConstant.FEE_TYPE_TEMP_DOWN_PARKING_SPACE);
        // 设置费用标志位
        feeDtoParamIn.setFeeFlag("2006012");

        // 调用费用服务查询费用列表
        List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDtoParamIn);
        
        ResponseEntity<String> responseEntity = null;
        
        // 检查查询结果，如果无费用记录则返回空JSON
        if (feeDtos == null || feeDtos.size() == 0) {
            responseEntity = new ResponseEntity<String>("{}", HttpStatus.OK);
            context.setResponseEntity(responseEntity);
            return;
        }

        // 获取第一条费用记录（通常进出记录对应的费用只有一条）
        FeeDto feeDto = feeDtos.get(0);

        // 将费用数据传输对象转换为API响应视图对象
        ApiMainFeeVo apiFeeVo = BeanConvertUtil.covertBean(feeDto, ApiMainFeeVo.class);

        // 构建成功响应，将API响应对象转换为JSON字符串
        responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiFeeVo), HttpStatus.OK);

        // 设置响应实体到上下文
        context.setResponseEntity(responseEntity);
    }
}