package com.java110.user.cmd.carInout;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.common.ICarInoutV1InnerServiceSMO;
import com.java110.po.car.CarInoutPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 车辆进出场更新命令类
 * 负责处理车辆进出场信息的更新操作，包括参数验证和业务逻辑处理
 * 
 * @author Java110
 */
//@Java110Cmd(serviceCode = "carInout.updateCarInout")
public class UpdateCarInoutCmd extends Cmd{

    /**
     * 车辆进出场服务接口，用于调用车辆进出场相关的业务方法
     */
    @Autowired
    private ICarInoutV1InnerServiceSMO carInoutV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证进出场ID是否为空
        Assert.hasKeyAndValue(reqJson, "inoutId", "进出场ID不能为空");
        // 验证小区ID是否为空
        Assert.hasKeyAndValue(reqJson, "communityId", "必填，请填写小区信息");

        // 验证车辆状态是否为空
        Assert.hasKeyAndValue(reqJson, "state", "必填，请填写车辆状态");
        // 验证车牌号是否为空
        Assert.hasKeyAndValue(reqJson, "carNum", "必填，请填写车牌号");
        // 验证进场时间是否为空
        Assert.hasKeyAndValue(reqJson, "inTime", "必填，请选择进场时间");
        // 验证出场时间是否为空
        Assert.hasKeyAndValue(reqJson, "outTime", "必填，请选择出场时间");
    }

    /**
     * 执行命令方法
     * 处理车辆进出场更新的核心业务逻辑
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当业务处理失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将JSON对象转换为车辆进出场POJO对象
        CarInoutPo carInoutPo = BeanConvertUtil.covertBean(reqJson, CarInoutPo.class);

        // 调用服务层保存车辆进出场信息
        int flag = carInoutV1InnerServiceSMOImpl.saveCarInout(carInoutPo);

        // 检查保存操作是否成功，flag小于1表示保存失败
        if(flag < 1){
            throw new CmdException("车辆进出失败");
        }
    }
}