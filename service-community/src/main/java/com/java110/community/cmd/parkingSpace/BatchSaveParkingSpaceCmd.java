/**
 * 批量保存停车位命令类
 * 
 * 该命令类用于处理批量创建停车位的业务逻辑，根据起始编号和结束编号批量生成停车位信息，
 * 并将生成的停车位数据保存到数据库中。主要用于停车场管理系统中快速初始化停车位。
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.community.cmd.parkingSpace;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.community.IParkingSpaceV1InnerServiceSMO;
import com.java110.po.parking.ParkingSpacePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Java110Cmd(serviceCode = "parkingSpace.batchSaveParkingSpace")
public class BatchSaveParkingSpaceCmd extends Cmd {

    /**
     * 停车位服务接口，用于操作停车位数据
     */
    @Autowired
    private IParkingSpaceV1InnerServiceSMO parkingSpaceV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否包含必要的字段，确保后续业务逻辑能够正常执行
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置上下文数据
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含小区ID
        Assert.jsonObjectHaveKey(reqJson, "communityId", "未包含小区ID");
        // 验证请求参数中必须包含起始编号
        Assert.jsonObjectHaveKey(reqJson, "startNum", "请求报文中未包含开始编号");
        // 验证请求参数中必须包含结束编号
        Assert.jsonObjectHaveKey(reqJson, "endNum", "请求报文中未包含结束编号");
        // 验证请求参数中必须包含停车场ID
        Assert.jsonObjectHaveKey(reqJson, "paId", "请求报文中未包含停车场信息");
        // 验证请求参数中必须包含停车位类型
        Assert.jsonObjectHaveKey(reqJson, "parkingType", "请求报文中未包含车位类型");
    }

    /**
     * 执行批量保存停车位命令
     * 
     * 根据起始编号和结束编号批量生成停车位信息，设置停车位的基本属性，
     * 并将生成的停车位列表保存到数据库中
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当保存数据失败或没有生成停车位时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 初始化停车位列表，用于存储批量生成的停车位对象
        List<ParkingSpacePo> parkingSpacePos = new ArrayList<>();

        ParkingSpacePo parkingSpacePo = null;
        // 获取当前时间作为创建时间的基准
        Calendar createTime = Calendar.getInstance();
        
        // 循环生成从起始编号到结束编号的所有停车位
        for (int i = reqJson.getIntValue("startNum"); i <= reqJson.getIntValue("endNum"); i++) {
            // 每次循环增加1秒，确保每个停车位的创建时间不同
            createTime.add(Calendar.SECOND,1);
            // 将请求JSON转换为停车位PO对象
            parkingSpacePo = BeanConvertUtil.covertBean(reqJson, ParkingSpacePo.class);
            // 设置停车位编号，格式为：前缀 + 当前编号
            parkingSpacePo.setNum(reqJson.getString("preNum") + i);
            // 设置停车位状态为空闲状态 "F"
            parkingSpacePo.setState("F");
            // 生成停车位唯一ID
            parkingSpacePo.setPsId(GenerateCodeFactory.getPsId(GenerateCodeFactory.CODE_PREFIX_psId));
            // 设置业务ID为默认值
            parkingSpacePo.setbId("-1");
            // 设置创建时间，格式化为标准时间字符串
            parkingSpacePo.setCreateTime(DateUtil.getFormatTimeString(createTime.getTime(),DateUtil.DATE_FORMATE_STRING_A));
            // 将生成的停车位对象添加到列表中
            parkingSpacePos.add(parkingSpacePo);
        }

        // 检查是否成功生成了停车位
        if (parkingSpacePos.size() < 1) {
            throw new CmdException("未包含添加车位");
        }

        // 调用服务层方法批量保存停车位数据
        int flag = parkingSpaceV1InnerServiceSMOImpl.saveParkingSpaces(parkingSpacePos);

        // 检查保存操作是否成功
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }
    }
}