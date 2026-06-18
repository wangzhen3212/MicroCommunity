/**
 * 编辑楼栋命令类
 * 
 * 该命令类用于处理外系统编辑楼栋信息的请求，主要功能包括：
 * 1. 验证请求参数的完整性和业务规则
 * 2. 执行楼栋信息的更新操作
 * 3. 返回操作结果
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

import java.util.List;


/**
 * API文档注解 - 编辑楼栋接口
 */
@Java110CmdDoc(title = "编辑楼栋",
        description = "用于外系统编辑楼栋信息功能",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/floor.editFloor",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "floor.editFloor",
        seq = 6
)

/**
 * 请求参数文档注解
 */
@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "name", length = 64, remark = "名称"),
        @Java110ParamDoc(name = "floorArea", length = 64, remark = "面积"),
        @Java110ParamDoc(name = "floorNum", length = 64, remark = "编号"),
        @Java110ParamDoc(name = "seq",type = "int",length = 11, remark = "排序"),
        @Java110ParamDoc(name = "floorId",length = 30, remark = "楼栋ID"),
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
        reqBody="{\"floorId\":\"123123\",\"name\":\"2号楼\",\"floorName\":\"\",\"floorArea\":\"22\",\"floorNum\":\"2\",\"remark\":\"2\",\"errorInfo\":\"\",\"seq\":\"22\",\"communityId\":\"2022081539020475\"}",
        resBody="{'code':0,'msg':'成功'}"
)

/**
 * 命令注解，指定服务编码
 */
@Java110Cmd(serviceCode = "floor.editFloor")
public class EditFloorCmd extends Cmd {
    
    /**
     * 楼栋内部服务接口
     */
    @Autowired
    private IFloorInnerServiceSMO floorInnerServiceSMOImpl;

    /**
     * 楼栋V1版本内部服务接口
     */
    @Autowired
    private IFloorV1InnerServiceSMO floorV1InnerServiceSMOImpl;

    /**
     * 验证请求参数方法
     * 
     * 该方法用于验证编辑楼栋请求的合法性，包括：
     * 1. 检查必要参数是否存在
     * 2. 验证楼栋编号是否重复
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置上下文数据
     * @param reqJson 请求参数的JSON对象
     * @throws ListenerExecuteException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必要参数是否存在
        Assert.jsonObjectHaveKey(reqJson, "floorId", "请求报文中未包含floorId");
        Assert.jsonObjectHaveKey(reqJson, "name", "请求报文中未包含name");
        Assert.jsonObjectHaveKey(reqJson, "userId", "请求报文中未包含userId");
        Assert.jsonObjectHaveKey(reqJson, "floorNum", "请求报文中未包含floorNum");
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求报文中未包含communityId");

        // 创建楼栋DTO对象用于查询
        FloorDto floorDto = new FloorDto();
        floorDto.setFloorNum(reqJson.getString("floorNum"));        // 设置楼栋编号
        floorDto.setCommunityId(reqJson.getString("communityId"));  // 设置小区ID
        
        // 查询相同编号的楼栋信息
        List<FloorDto> floorDtos = floorInnerServiceSMOImpl.queryFloors(floorDto);

        // 验证楼栋编号是否重复
        // 如果存在相同编号的楼栋，且不是当前编辑的楼栋，则抛出异常
        if (floorDtos != null && floorDtos.size() > 0 && !reqJson.getString("floorId").equals(floorDtos.get(0).getFloorId())) {
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "楼栋编号已经存在");
        }

    }

    /**
     * 执行编辑楼栋命令方法
     * 
     * 该方法负责执行楼栋信息的更新操作，包括：
     * 1. 构建楼栋业务数据
     * 2. 转换数据对象
     * 3. 调用服务更新楼栋信息
     * 4. 处理更新结果并返回响应
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当楼栋更新失败时抛出异常
     */
    @Override
    @Java110Transactional  // 事务注解，确保操作在事务中执行
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 构建楼栋业务数据对象
        JSONObject businessFloor = new JSONObject();
        businessFloor.put("floorId", reqJson.getString("floorId"));        // 楼栋ID
        businessFloor.put("name", reqJson.getString("floorName"));         // 楼栋名称
        businessFloor.put("remark", reqJson.getString("remark"));          // 备注信息
        businessFloor.put("userId", reqJson.getString("userId"));          // 用户ID
        businessFloor.put("floorNum", reqJson.getString("floorNum"));      // 楼栋编号
        businessFloor.put("communityId", reqJson.getString("communityId"));// 小区ID
        businessFloor.put("floorArea", reqJson.getString("floorArea"));    // 楼栋面积
        businessFloor.put("seq", reqJson.getString("seq"));                // 排序序号
        
        // 将JSON对象转换为楼栋PO对象
        FloorPo floorPo = BeanConvertUtil.covertBean(businessFloor, FloorPo.class);
        
        // 调用服务更新楼栋信息
        int flag = floorV1InnerServiceSMOImpl.updateFloor(floorPo);

        // 检查更新结果，如果更新记录数小于1，则认为更新失败
        if (flag < 1) {
            throw new CmdException("修改楼栋失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}