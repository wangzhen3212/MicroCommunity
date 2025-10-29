package com.java110.user.cmd.carInout;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.common.ICarInoutV1InnerServiceSMO;
import com.java110.po.car.CarInoutPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 车辆进出记录保存命令类
 * 负责处理车辆进出记录的保存操作，包括参数验证和业务逻辑处理
 * 
 * @author Java110
 * @version 1.0
 */
//@Java110Cmd(serviceCode = "carInout.saveCarInout")
public class SaveCarInoutCmd extends Cmd {

    /**
     * 车辆进出记录服务接口
     */
    @Autowired
    private ICarInoutV1InnerServiceSMO carInoutV1InnerServiceSMOImpl;
    
    /**
     * 参数验证方法
     * 验证请求参数中是否包含必要的字段
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置数据
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证车辆状态字段是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "state", "必填，请填写车辆状态");
        // 验证小区信息字段是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "communityId", "必填，请填写小区信息");
        // 验证车牌号字段是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "carNum", "必填，请填写车牌号");
        // 验证进场时间字段是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "inTime", "必填，请选择进场时间");
        // 验证出场时间字段是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "outTime", "必填，请选择出场时间");
    }

    /**
     * 执行命令方法
     * 处理车辆进出记录的保存业务逻辑
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置数据
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当保存操作失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        // 生成车辆进出记录的唯一ID
        reqJson.put("inoutId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_inoutId));
        
        // 将JSON对象转换为车辆进出记录POJO对象
        CarInoutPo carInoutPo = BeanConvertUtil.covertBean(reqJson, CarInoutPo.class);

        // 调用服务层保存车辆进出记录
        int flag = carInoutV1InnerServiceSMOImpl.saveCarInout(carInoutPo);

        // 检查保存操作是否成功，flag小于1表示保存失败
        if(flag < 1){
            throw new CmdException("车辆进出失败");
        }
    }
}