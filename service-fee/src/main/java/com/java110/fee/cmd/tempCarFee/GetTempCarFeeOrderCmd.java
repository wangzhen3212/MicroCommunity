package com.java110.fee.cmd.tempCarFee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.parking.ParkingBoxAreaDto;
import com.java110.dto.fee.TempCarPayOrderDto;
import com.java110.fee.bmo.tempCarFee.IGetTempCarFeeRules;
import com.java110.intf.acct.ICouponUserV1InnerServiceSMO;
import com.java110.intf.community.IParkingBoxAreaV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * 临时车费用订单查询命令类
 * 
 * 该类用于处理临时车费用订单查询请求，主要功能包括：
 * 1. 验证请求参数的有效性
 * 2. 根据车牌号和停车场信息查询临时车费用订单
 * 3. 支持通过岗亭ID自动获取对应的停车场ID
 * 
 * 服务编码：tempCarFee.getTempCarFeeOrder
 * 请求路径：/app/tempCarFee.GetTempCarFeeOrder
 * 
 * @author 吴学文 at 2021-09-16 22:26:04 mail: 928255095@qq.com
 * @open source address: https://gitee.com/wuxw7/MicroCommunity
 * @官网：http://www.homecommunity.cn
 * 
 * @温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的修改的原因以及联系邮箱
 */
@Java110Cmd(serviceCode = "tempCarFee.getTempCarFeeOrder")
public class GetTempCarFeeOrderCmd extends Cmd {
    
    /**
     * 优惠券用户服务接口
     */
    @Autowired
    private ICouponUserV1InnerServiceSMO couponUserV1InnerServiceSMOImpl;
    
    /**
     * 临时车费用规则查询业务接口
     */
    @Autowired
    private IGetTempCarFeeRules getTempCarFeeRulesImpl;

    /**
     * 停车场岗亭区域服务接口
     */
    @Autowired
    private IParkingBoxAreaV1InnerServiceSMO parkingBoxAreaV1InnerServiceSMOImpl;

    /**
     * 验证请求参数的有效性
     * 
     * 该方法主要验证以下内容：
     * 1. 车牌号必须存在
     * 2. 如果停车场ID不存在，则通过岗亭ID获取默认停车场ID
     * 3. 最终确保停车场ID存在
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {

        // 验证车牌号参数必须存在且不为空
        Assert.hasKeyAndValue(reqJson, "carNum", "carNum不能为空");
        
        // 如果停车场ID为空，则通过岗亭ID获取默认停车场ID
        if(StringUtil.isEmpty(reqJson.getString("paId"))){
            // 验证岗亭ID参数必须存在且不为空
            Assert.hasKeyAndValue(reqJson, "boxId", "boxId不能为空");

            // 构建停车场岗亭区域查询条件
            ParkingBoxAreaDto parkingBoxAreaDto = new ParkingBoxAreaDto();
            // 设置查询默认区域
            parkingBoxAreaDto.setDefaultArea(ParkingBoxAreaDto.DEFAULT_AREA_TRUE);
            // 设置岗亭ID
            parkingBoxAreaDto.setBoxId(reqJson.getString("boxId"));
            // 查询岗亭对应的停车场区域信息
            List<ParkingBoxAreaDto> parkingBoxAreaDtos = parkingBoxAreaV1InnerServiceSMOImpl.queryParkingBoxAreas(parkingBoxAreaDto);
            
            // 验证查询结果，如果未找到对应的停车场则抛出异常
            if(parkingBoxAreaDtos == null || parkingBoxAreaDtos.size()< 1){
                throw new CmdException("未找到停车场");
            }
            // 将查询到的第一个停车场ID设置到请求参数中
            reqJson.put("paId",parkingBoxAreaDtos.get(0).getPaId());
        }
        
        // 最终验证停车场ID参数必须存在且不为空
        Assert.hasKeyAndValue(reqJson, "paId", "paId不能为空");
    }

    /**
     * 执行临时车费用订单查询命令
     * 
     * 该方法主要功能：
     * 1. 构建临时车支付订单查询条件
     * 2. 设置车牌号和停车场ID等查询参数
     * 3. 处理优惠券ID列表参数
     * 4. 调用业务层接口查询费用订单信息
     * 5. 设置响应结果
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当业务处理过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 创建临时车支付订单查询对象
        TempCarPayOrderDto tempCarPayOrderDto = new TempCarPayOrderDto();
        // 设置停车场ID
        tempCarPayOrderDto.setPaId(reqJson.getString("paId"));
        // 设置车牌号
        tempCarPayOrderDto.setCarNum(reqJson.getString("carNum"));
        
        // 如果请求中包含优惠券ID列表参数，则进行设置
        if(reqJson.containsKey("pccIds") && !StringUtil.isEmpty(reqJson.getString("pccIds"))){
            // 将逗号分隔的优惠券ID字符串转换为数组
            tempCarPayOrderDto.setPccIds(reqJson.getString("pccIds").split(","));
        }
        
        // 调用业务层接口查询临时车费用订单信息
        ResponseEntity<String> responseEntity = getTempCarFeeRulesImpl.getTempCarFeeOrder(tempCarPayOrderDto);
        // 将查询结果设置到响应上下文中
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}