/**
 * 单元修改命令类
 * 
 * 该命令类用于处理外系统修改单元信息的请求，包括参数验证、业务逻辑处理和事务管理
 * 通过注解定义了API文档信息，支持小区单元信息的完整修改流程
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2024
 */
package com.java110.community.cmd.unit;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.floor.FloorDto;
import com.java110.dto.unit.UnitDto;
import com.java110.intf.community.IFloorInnerServiceSMO;
import com.java110.intf.community.IUnitInnerServiceSMO;
import com.java110.intf.community.IUnitV1InnerServiceSMO;
import com.java110.po.unit.UnitPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;


@Java110CmdDoc(title = "修改单元",
        description = "用于外系统修改单元信息功能",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/unit.updateUnit",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "unit.updateUnit",
        seq = 10
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "floorId", length = 30, remark = "楼栋ID"),
        @Java110ParamDoc(name = "unitId", length = 30, remark = "单元ID"),
        @Java110ParamDoc(name = "unitNum", length = 30, remark = "单元"),
        @Java110ParamDoc(name = "layerCount", length = 30, remark = "楼层"),
        @Java110ParamDoc(name = "lift", length = 30, remark = "1010 有电梯 2020 无电梯"),
        @Java110ParamDoc(name = "unitArea", length = 30, remark = "面积"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        }
)

@Java110ExampleDoc(
        reqBody="{\"floorId\":\"732022081690440002\",\"unitId\":\"123123\",\"unitNum\":\"2\",\"layerCount\":\"2\",\"lift\":\"1010\",\"remark\":\"2\",\"communityId\":\"2022081539020475\",\"unitArea\":\"2\"}",
        resBody="{'code':0,'msg':'成功'}"
)

@Java110Cmd(serviceCode = "unit.updateUnit")
public class UpdateUnitCmd extends Cmd {
    
    /** 楼栋内部服务接口 */
    @Autowired
    private IFloorInnerServiceSMO floorInnerServiceSMOImpl;

    /** 单元内部服务接口 */
    @Autowired
    private IUnitInnerServiceSMO unitInnerServiceSMOImpl;

    /** 单元V1版本内部服务接口 */
    @Autowired
    private IUnitV1InnerServiceSMO unitV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证修改单元请求的必填参数、数据格式和业务规则
     * 包括参数完整性检查、数据类型验证、业务关联性校验等
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws IllegalArgumentException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填参数是否存在
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求报文中未包含communityId节点");
        Assert.jsonObjectHaveKey(reqJson, "floorId", "请求报文中未包含floorId节点");
        Assert.jsonObjectHaveKey(reqJson, "unitId", "请求报文中未包含unitId节点");
        Assert.jsonObjectHaveKey(reqJson, "unitNum", "请求报文中未包含unitNum节点");
        Assert.jsonObjectHaveKey(reqJson, "layerCount", "请求报文中未包含layerCount节点");
        Assert.jsonObjectHaveKey(reqJson, "lift", "请求报文中未包含lift节点");

        // 验证楼层数是否为有效数字
        Assert.isInteger(reqJson.getString("layerCount"), "单元总层数据不是有效数字");

        // 验证电梯参数值是否合法（1010表示有电梯，2020表示无电梯）
        if (!"1010".equals(reqJson.getString("lift")) && !"2020".equals(reqJson.getString("lift"))) {
            throw new IllegalArgumentException("是否有电梯 传入数据错误");
        }

        // 创建楼栋查询对象
        FloorDto floorDto = new FloorDto();
        floorDto.setCommunityId(reqJson.getString("communityId"));
        floorDto.setFloorId(reqJson.getString("floorId"));
        // 校验小区楼ID和小区是否有对应关系
        int total = floorInnerServiceSMOImpl.queryFloorsCount(floorDto);

        // 如果查询结果小于1，说明传入的小区楼ID不属于该小区
        if (total < 1) {
            throw new IllegalArgumentException("传入小区楼ID不是该小区的楼");
        }

        // 创建单元查询对象
        UnitDto unitDto = new UnitDto();
        unitDto.setFloorId(reqJson.getString("floorId"));
        unitDto.setUnitId(reqJson.getString("unitId"));
        // 校验小区楼ID和单元ID是否有对应关系
        total = unitInnerServiceSMOImpl.queryUnitsCount(unitDto);
        // 如果查询结果小于1，说明传入的单元不属于该小区的楼
        if (total < 1) {
            throw new IllegalArgumentException("传入单元不是该小区的楼的单元");
        }
    }

    /**
     * 执行单元修改命令
     * 
     * 在事务中执行单元信息的更新操作，将请求参数转换为业务对象并调用服务层进行数据更新
     * 如果更新失败则抛出异常，成功则设置响应结果
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当单元修改失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 创建业务数据对象，组装需要更新的单元信息
        JSONObject businessUnit = new JSONObject();
        businessUnit.put("floorId", reqJson.getString("floorId"));
        businessUnit.put("layerCount", reqJson.getString("layerCount"));
        businessUnit.put("unitId", reqJson.getString("unitId"));
        businessUnit.put("unitNum", reqJson.getString("unitNum"));
        businessUnit.put("lift", reqJson.getString("lift"));
        businessUnit.put("remark", reqJson.getString("remark"));
        businessUnit.put("unitArea", reqJson.getString("unitArea"));
        businessUnit.put("userId", reqJson.getString("userId"));
        
        // 将JSON对象转换为单元持久化对象
        UnitPo unitPo = BeanConvertUtil.covertBean(businessUnit, UnitPo.class);
        
        // 调用V1版本服务接口更新单元信息
        int flag = unitV1InnerServiceSMOImpl.updateUnit(unitPo);

        // 如果更新影响的行数小于1，说明更新失败
        if (flag < 1) {
            throw new CmdException("修改单元失败");
        }

        // 设置成功的响应结果
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}