/**
 * 单元管理命令类 - 添加单元功能
 * 
 * 该命令类用于处理添加单元信息的业务逻辑，包括参数验证、数据校验和单元信息保存等功能。
 * 通过注解方式定义了API文档信息，支持事务处理，确保数据一致性。
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023
 */
package com.java110.community.cmd.unit;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
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

/**
 * API文档注解 - 定义添加单元接口的完整文档信息
 */
@Java110CmdDoc(title = "添加单元",
        description = "用于外系统添加单元信息功能",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/unit.saveUnit",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "unit.saveUnit",
        seq = 9
)

/**
 * 请求参数文档注解
 */
@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "floorId", length = 30, remark = "楼栋ID"),
        @Java110ParamDoc(name = "unitNum", length = 30, remark = "单元"),
        @Java110ParamDoc(name = "layerCount", length = 30, remark = "楼层"),
        @Java110ParamDoc(name = "lift", length = 30, remark = "1010 有电梯 2020 无电梯"),
        @Java110ParamDoc(name = "unitArea", length = 30, remark = "面积"),
})

/**
 * 响应参数文档注解
 */
@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        }
)

/**
 * 请求响应示例文档注解
 */
@Java110ExampleDoc(
        reqBody="{\"floorId\":\"732022081690440002\",\"unitNum\":\"2\",\"layerCount\":\"2\",\"lift\":\"1010\",\"remark\":\"2\",\"communityId\":\"2022081539020475\",\"unitArea\":\"2\"}",
        resBody="{'code':0,'msg':'成功'}"
)

/**
 * 命令注解 - 标识该命令对应的服务编码
 */
@Java110Cmd(serviceCode = "unit.saveUnit")
public class SaveUnitCmd extends Cmd {
    
    /** 楼栋信息服务接口 */
    @Autowired
    private IFloorInnerServiceSMO floorInnerServiceSMOImpl;

    /** 单元查询服务接口 */
    @Autowired
    private IUnitInnerServiceSMO unitInnerServiceSMOImpl;

    /** 单元保存服务接口 */
    @Autowired
    private IUnitV1InnerServiceSMO unitV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 对请求参数进行完整性校验和业务规则验证，确保数据的有效性和一致性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置上下文信息
     * @param reqJson 请求参数JSON对象
     * @throws IllegalArgumentException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 校验必需参数是否存在
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求报文中未包含communityId节点");
        Assert.jsonObjectHaveKey(reqJson, "floorId", "请求报文中未包含floorId节点");
        Assert.jsonObjectHaveKey(reqJson, "unitNum", "请求报文中未包含unitNum节点");
        Assert.jsonObjectHaveKey(reqJson, "layerCount", "请求报文中未包含layerCount节点");
        Assert.jsonObjectHaveKey(reqJson, "lift", "请求报文中未包含lift节点");

        // 验证楼层数是否为有效数字
        Assert.isInteger(reqJson.getString("layerCount"), "单元总层数据不是有效数字");

        // 验证电梯类型参数是否合法
        if (!"1010".equals(reqJson.getString("lift")) && !"2020".equals(reqJson.getString("lift"))) {
            throw new IllegalArgumentException("是否有电梯 传入数据错误");
        }

        // 校验楼栋信息是否属于指定小区
        FloorDto floorDto = new FloorDto();
        floorDto.setCommunityId(reqJson.getString("communityId"));
        floorDto.setFloorId(reqJson.getString("floorId"));
        // 查询楼栋数量，验证楼栋与小区的对应关系
        int total = floorInnerServiceSMOImpl.queryFloorsCount(floorDto);

        if (total < 1) {
            throw new IllegalArgumentException("传入小区楼ID不是该小区的楼");
        }

        // 校验单元编号是否已存在，避免重复添加
        UnitDto unitDto = new UnitDto();
        unitDto.setCommunityId(reqJson.getString("communityId"));
        unitDto.setFloorId(reqJson.getString("floorId"));
        unitDto.setUnitNum(reqJson.getString("unitNum"));
        int count = unitInnerServiceSMOImpl.queryUnitsCount(unitDto);

        if(count > 0){
            throw new IllegalArgumentException("单元编号已经存在，请勿重复添加");
        }
    }

    /**
     * 执行命令方法
     * 
     * 处理添加单元的核心业务逻辑，包括数据转换、单元信息保存和响应结果设置
     * 使用事务注解确保数据操作的原子性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当保存单元失败时抛出命令异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 构建业务数据对象
        JSONObject businessUnit = new JSONObject();
        businessUnit.put("floorId", reqJson.getString("floorId"));
        businessUnit.put("layerCount", reqJson.getString("layerCount"));
        // 生成单元ID：如果请求中未包含unitId，则自动生成；否则使用请求中的unitId
        businessUnit.put("unitId", !reqJson.containsKey("unitId") ? GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_unitId)
                : reqJson.getString("unitId"));
        businessUnit.put("unitNum", reqJson.getString("unitNum"));
        businessUnit.put("lift", reqJson.getString("lift"));
        businessUnit.put("remark", reqJson.getString("remark"));
        businessUnit.put("unitArea", reqJson.getString("unitArea"));
        businessUnit.put("userId", reqJson.getString("userId"));
        
        // 将JSON对象转换为单元持久化对象
        UnitPo unitPo = BeanConvertUtil.covertBean(businessUnit, UnitPo.class);
        
        // 调用服务保存单元信息
        int flag = unitV1InnerServiceSMOImpl.saveUnit(unitPo);

        // 检查保存结果
        if (flag < 1) {
            throw new CmdException("保存单元失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}