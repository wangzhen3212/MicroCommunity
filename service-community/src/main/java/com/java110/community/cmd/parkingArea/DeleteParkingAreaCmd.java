/**
 * 删除停车场命令类
 * 
 * 该类负责处理删除停车场的业务逻辑，包括参数验证和删除操作。
 * 在删除停车场前会检查该停车场下是否存在停车位，如果存在则不允许删除。
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.community.cmd.parkingArea;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.intf.community.IParkingAreaAttrV1InnerServiceSMO;
import com.java110.intf.community.IParkingAreaV1InnerServiceSMO;
import com.java110.intf.community.IParkingSpaceInnerServiceSMO;
import com.java110.po.parking.ParkingAreaPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Java110Cmd(serviceCode = "parkingArea.deleteParkingArea")
public class DeleteParkingAreaCmd extends Cmd {

    /**
     * 停车场服务接口
     */
    @Autowired
    private IParkingAreaV1InnerServiceSMO parkingAreaV1InnerServiceSMOImpl;

    /**
     * 停车场属性服务接口
     */
    @Autowired
    private IParkingAreaAttrV1InnerServiceSMO parkingAreaAttrV1InnerServiceSMOImpl;

    /**
     * 停车位服务接口
     */
    @Autowired
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段信息
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置数据流信息
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含小区ID和停车场ID
        Assert.hasKeyAndValue(reqJson, "communityId", "必填，请填写小区信息");
        Assert.hasKeyAndValue(reqJson, "paId", "停车场ID不能为空");
    }

    /**
     * 执行删除停车场命令
     * 
     * 该方法执行删除停车场的核心业务逻辑，包括：
     * 1. 检查停车场下是否存在停车位
     * 2. 执行停车场删除操作
     * 3. 处理删除结果
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为停车场PO对象
        ParkingAreaPo parkingAreaPo = BeanConvertUtil.covertBean(reqJson, ParkingAreaPo.class);
        
        // 创建停车位查询条件对象
        ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
        // 设置查询条件：按停车场ID查询
        parkingSpaceDto.setPaId(parkingAreaPo.getPaId());
        
        // 查询该停车场下的所有停车位
        List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);
        
        // 检查停车场下是否存在停车位，如果存在则不允许删除
        if (parkingSpaceDtos != null && parkingSpaceDtos.size() > 0) {
            throw new IllegalArgumentException("请先删除该停车场下的停车位！");
        }
        
        // 执行停车场删除操作
        int flag = parkingAreaV1InnerServiceSMOImpl.deleteParkingArea(parkingAreaPo);
        
        // 检查删除操作是否成功，如果影响行数小于1则认为删除失败
        if (flag < 1) {
            throw new CmdException("保存停车场失败");
        }
    }
}