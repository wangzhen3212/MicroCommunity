/**
 * 删除访客记录命令类
 * 
 * 该类负责处理删除访客记录的业务逻辑，包括：
 * 1. 删除访客记录
 * 2. 释放关联的车位资源
 * 3. 删除车辆白名单信息
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.community.cmd.visit;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.machine.CarBlackWhiteDto;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.dto.visit.VisitDto;
import com.java110.intf.community.IParkingSpaceInnerServiceSMO;
import com.java110.intf.community.IVisitV1InnerServiceSMO;
import com.java110.intf.user.ICarBlackWhiteV1InnerServiceSMO;
import com.java110.po.car.CarBlackWhitePo;
import com.java110.po.owner.VisitPo;
import com.java110.po.parking.ParkingSpacePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Java110Cmd(serviceCode = "visit.deleteVisit")
public class DeleteVisitCmd extends Cmd {

    @Autowired
    private IVisitV1InnerServiceSMO visitV1InnerServiceSMOImpl;

    @Autowired
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;

    @Autowired
    private ICarBlackWhiteV1InnerServiceSMO carBlackWhiteV1InnerServiceSMOImpl;

    /**
     * 参数校验方法
     * 
     * 验证请求参数中是否包含必填的访客记录ID
     * 
     * @param event 命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含访客记录ID
        Assert.hasKeyAndValue(reqJson, "vId", "访客记录ID不能为空");
    }

    /**
     * 执行删除访客记录命令
     * 
     * 主要业务逻辑：
     * 1. 根据访客ID查询访客信息
     * 2. 逻辑删除访客记录
     * 3. 如果访客有车辆且分配了车位，则释放车位并删除车辆白名单
     * 
     * @param event 命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 构建查询条件，根据访客ID查询访客信息
        VisitDto visitDto = new VisitDto();
        visitDto.setvId(reqJson.getString("vId"));
        List<VisitDto> visitDtos = visitV1InnerServiceSMOImpl.queryVisits(visitDto);
        
        // 验证查询结果必须且只能有一条记录
        Assert.listOnlyOne(visitDtos, "查询访客信息错误！");
        
        // 将请求参数转换为PO对象，用于删除操作
        VisitPo visitPo = BeanConvertUtil.covertBean(reqJson, VisitPo.class);
        // 设置状态为删除状态（1表示逻辑删除）
        visitPo.setStatusCd("1");
        
        // 执行删除操作
        int flag = visitV1InnerServiceSMOImpl.deleteVisit(visitPo);
        if (flag < 1) {
            throw new CmdException("删除异常");
        }
        
        // 检查访客是否有车辆且分配了车位（车位ID不为空且不等于-1）
        if (!StringUtil.isEmpty(visitDtos.get(0).getCarNum()) 
                && !StringUtil.isEmpty(visitDtos.get(0).getPsId()) 
                && !visitDtos.get(0).getPsId().equals("-1")) {
            
            // 有车辆且有车位的情况，需要释放车位资源
            
            // 查询车位信息
            ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
            parkingSpaceDto.setPsId(visitDtos.get(0).getPsId());
            List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);
            
            // 验证车位信息必须存在且唯一
            Assert.listOnlyOne(parkingSpaceDtos, "车位信息不存在或存在多条！");
            
            // 检查车位状态是否为出租状态（H）
            if (parkingSpaceDtos.get(0).getState().equals("H")) {
                // 车位状态：出售 S，出租 H，空闲 F
                // 将出租状态的车位释放为空闲状态
                ParkingSpacePo parkingSpacePo = new ParkingSpacePo();
                parkingSpacePo.setPsId(parkingSpaceDtos.get(0).getPsId());
                parkingSpacePo.setState("F"); // 设置为空闲状态
                parkingSpaceInnerServiceSMOImpl.updateParkingSpace(parkingSpacePo); // 释放车位
            }
            
            // 删除车辆白名单信息
            CarBlackWhiteDto carBlackWhiteDto = new CarBlackWhiteDto();
            carBlackWhiteDto.setCarNum(visitDtos.get(0).getCarNum()); // 设置车牌号
            carBlackWhiteDto.setPaId(parkingSpaceDtos.get(0).getPaId()); // 设置停车场ID
            List<CarBlackWhiteDto> carBlackWhiteDtos = carBlackWhiteV1InnerServiceSMOImpl.queryCarBlackWhites(carBlackWhiteDto);
            
            // 根据查询结果处理白名单
            if (carBlackWhiteDtos != null && carBlackWhiteDtos.size() == 1) {
                // 找到唯一白名单记录，执行删除
                CarBlackWhitePo carBlackWhitePo = new CarBlackWhitePo();
                carBlackWhitePo.setBwId(carBlackWhiteDtos.get(0).getBwId());
                carBlackWhiteV1InnerServiceSMOImpl.deleteCarBlackWhite(carBlackWhitePo); // 删除白名单
            } else if (carBlackWhiteDtos != null && carBlackWhiteDtos.size() > 1) {
                // 查询到多条白名单记录，抛出异常
                throw new IllegalArgumentException("查询出多条白名单信息！");
            } else {
                // 未查询到白名单记录，抛出异常
                throw new IllegalArgumentException("查询不到白名单信息！");
            }
        }
    }
}