/**
 * 楼栋信息保存命令类
 * 
 * 该命令类用于处理添加楼栋信息的业务逻辑，包括参数验证、数据保存等功能。
 * 通过注解配置了API文档信息，支持事务处理，确保数据一致性。
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023
 */
package com.java110.community.cmd.floor;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.doc.annotation.*;
import com.java110.dto.floor.FloorDto;
import com.java110.intf.community.IFloorInnerServiceSMO;
import com.java110.intf.community.IFloorV1InnerServiceSMO;
import com.java110.po.floor.FloorPo;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.ListenerExecuteException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;


@Java110CmdDoc(title = "添加楼栋",
        description = "用于外系统添加楼栋信息功能",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/floor.saveFloor",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "floor.saveFloor",
        seq = 5
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "name", length = 64, remark = "名称"),
        @Java110ParamDoc(name = "floorArea", length = 64, remark = "面积"),
        @Java110ParamDoc(name = "floorNum", length = 64, remark = "编号"),
        @Java110ParamDoc(name = "seq", type = "int", length = 11, remark = "排序"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        }
)

@Java110ExampleDoc(
        reqBody = "{\"floorId\":\"\",\"name\":\"2号楼\",\"floorName\":\"\",\"floorArea\":\"22\",\"floorNum\":\"2\",\"remark\":\"2\",\"errorInfo\":\"\",\"seq\":\"22\",\"communityId\":\"2022081539020475\"}",
        resBody = "{'code':0,'msg':'成功'}"
)


@Java110Cmd(serviceCode = "floor.saveFloor")
public class SaveFloorCmd extends Cmd {
    
    /**
     * 楼栋信息服务接口
     */
    @Autowired
    private IFloorInnerServiceSMO floorInnerServiceSMOImpl;

    /**
     * 楼栋V1版本服务接口
     */
    @Autowired
    private IFloorV1InnerServiceSMO floorV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，包括必填字段检查和业务规则验证
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws ListenerExecuteException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填字段是否存在
        Assert.jsonObjectHaveKey(reqJson, "name", "请求报文中未包含name");
        Assert.jsonObjectHaveKey(reqJson, "userId", "请求报文中未包含userId");
        Assert.jsonObjectHaveKey(reqJson, "floorNum", "请求报文中未包含floorNum");
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求报文中未包含communityId");
        
        // 创建楼栋数据传输对象
        FloorDto floorDto = new FloorDto();
        floorDto.setFloorNum(reqJson.getString("floorNum"));
        floorDto.setCommunityId(reqJson.getString("communityId"));

        // 检查楼栋编号是否已存在
        int floorCount = floorInnerServiceSMOImpl.queryFloorsCount(floorDto);

        if (floorCount > 0) {
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "楼栋编号已经存在");
        }

        // 重置楼栋数据传输对象，检查楼栋名称是否已存在
        floorDto = new FloorDto();
        floorDto.setFloorName(reqJson.getString("name"));
        floorDto.setCommunityId(reqJson.getString("communityId"));

        floorCount = floorInnerServiceSMOImpl.queryFloorsCount(floorDto);

        if (floorCount > 0) {
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "楼栋名称已经存在");
        }
    }

    /**
     * 执行保存楼栋命令
     * 
     * 处理楼栋信息的保存逻辑，包括生成楼栋ID、组装业务数据、调用服务保存数据
     * 该方法使用事务注解确保数据操作的原子性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当保存操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 生成楼栋ID
        String floorId = GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_floorId);
        
        // 组装业务数据
        JSONObject businessFloor = new JSONObject();
        businessFloor.put("floorId", floorId);
        businessFloor.put("name", reqJson.getString("name"));
        businessFloor.put("remark", reqJson.getString("remark"));
        businessFloor.put("userId", reqJson.getString("userId"));
        businessFloor.put("floorNum", reqJson.getString("floorNum"));
        businessFloor.put("communityId", reqJson.getString("communityId"));
        businessFloor.put("floorArea", reqJson.getString("floorArea"));
        businessFloor.put("seq", reqJson.getString("seq"));
        
        // 将JSON对象转换为持久化对象
        FloorPo floorPo = BeanConvertUtil.covertBean(businessFloor, FloorPo.class);
        
        // 调用服务保存楼栋信息
        int flag = floorV1InnerServiceSMOImpl.saveFloor(floorPo);

        // 检查保存结果
        if (flag < 1) {
            throw new CmdException("保存楼栋失败");
        }

        // 设置响应结果
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}