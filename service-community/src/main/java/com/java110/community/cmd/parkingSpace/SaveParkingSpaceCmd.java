/**
 * 停车位保存命令类
 * 
 * 该类负责处理停车位信息的保存操作，包括普通车位和子母车位的特殊处理。
 * 通过注解@Java110Cmd声明服务代码为"parkingSpace.saveParkingSpace"，
 * 当接收到对应服务请求时，会执行该命令类的验证和业务处理逻辑。
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.community.cmd.parkingSpace;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.intf.community.IParkingSpaceV1InnerServiceSMO;
import com.java110.po.parking.ParkingSpacePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

@Java110Cmd(serviceCode = "parkingSpace.saveParkingSpace")
public class SaveParkingSpaceCmd extends Cmd {

    /**
     * 停车位服务接口，用于操作停车位数据
     */
    @Autowired
    private IParkingSpaceV1InnerServiceSMO parkingSpaceV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求报文中的必要字段是否存在，确保后续业务处理的数据完整性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置上下文数据
     * @param reqJson 请求报文JSON对象，包含前端传递的参数
     * @throws CmdException 当验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 验证请求报文中必须包含小区ID
        Assert.jsonObjectHaveKey(reqJson, "communityId", "未包含小区ID");
        // 验证请求报文中必须包含车位编号
        Assert.jsonObjectHaveKey(reqJson, "num", "请求报文中未包含age");
        // 验证请求报文中必须包含车位面积
        Assert.jsonObjectHaveKey(reqJson, "area", "请求报文中未包含name");
        // 验证请求报文中必须包含用户ID
        Assert.jsonObjectHaveKey(reqJson, "userId", "请求报文中未包含userId");
        // 验证请求报文中必须包含停车场ID
        Assert.jsonObjectHaveKey(reqJson, "paId", "请求报文中未包含停车场信息");
    }

    /**
     * 命令执行方法
     * 
     * 处理停车位保存的核心业务逻辑，包括普通车位保存和子母车位的特殊处理。
     * 对于子母车位，会自动创建对应的子车位。
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求报文JSON对象
     * @throws CmdException 当保存数据失败时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 创建业务停车位对象，复制请求参数
        JSONObject businessParkingSpace = new JSONObject();
        businessParkingSpace.putAll(reqJson);
        
        // 设置停车位默认状态为"F"（空闲状态）
        businessParkingSpace.put("state", "F");
        // 生成停车位唯一ID
        businessParkingSpace.put("psId", GenerateCodeFactory.getPsId(GenerateCodeFactory.CODE_PREFIX_psId));
        // 设置业务ID为默认值-1
        businessParkingSpace.put("bId", "-1");
        // 设置创建时间为当前时间
        businessParkingSpace.put("createTime", new Date());
        
        // 将JSON对象转换为停车位POJO对象
        ParkingSpacePo parkingSpacePo = BeanConvertUtil.covertBean(businessParkingSpace, ParkingSpacePo.class);

        // 调用服务保存停车位信息
        int flag = parkingSpaceV1InnerServiceSMOImpl.saveParkingSpace(parkingSpacePo);

        // 检查保存结果，失败则抛出异常
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 如果不是子母车位类型，直接返回
        if(!ParkingSpaceDto.TYPE_CD_SON_MOTHER.equals(parkingSpacePo.getParkingType())){
            return ;
        }

        // 如果是子母车位，需要额外创建子车位
        // 重新生成子车位的唯一ID
        parkingSpacePo.setPsId(GenerateCodeFactory.getPsId(GenerateCodeFactory.CODE_PREFIX_psId));
        // 设置子车位的编号（在母车位编号基础上添加后缀）
        parkingSpacePo.setNum(parkingSpacePo.getNum()+ParkingSpaceDto.NUM_MOTHER);
        
        // 保存子车位信息
        flag = parkingSpaceV1InnerServiceSMOImpl.saveParkingSpace(parkingSpacePo);
        
        // 检查子车位保存结果，失败则抛出异常
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }
    }
}