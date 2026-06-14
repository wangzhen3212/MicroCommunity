/**
 * 查询楼栋信息命令类
 * 
 * 该类用于处理外系统查询楼栋信息的请求，支持分页查询楼栋列表
 * 通过注解配置API文档信息，包括请求参数、响应格式和示例
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2024
 */
package com.java110.community.cmd.floor;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.floor.FloorDto;
import com.java110.intf.community.IFloorInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.api.ApiFloorDataVo;
import com.java110.vo.api.ApiFloorVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * API文档注解 - 查询楼栋接口
 */
@Java110CmdDoc(title = "查询楼栋",
        description = "用于外系统查询楼栋信息功能",
        httpMethod = "get",
        url = "http://{ip}:{port}/app/floor.queryFloors",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "floor.queryFloors",
        seq = 8
)

/**
 * API参数文档注解 - 定义请求参数
 */
@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "page", type = "int",length = 11, remark = "页数"),
        @Java110ParamDoc(name = "row", type = "int",length = 11, remark = "行数"),
})

/**
 * API响应文档注解 - 定义返回参数结构
 */
@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
                @Java110ParamDoc(name = "apiFloorDataVoList", type = "Array", remark = "有效数据"),
                @Java110ParamDoc(parentNodeName = "apiFloorDataVoList",name = "floorId", type = "String", remark = "楼栋ID"),
                @Java110ParamDoc(parentNodeName = "apiFloorDataVoList",name = "floorNum", type = "String", remark = "楼栋编号"),
        }
)

/**
 * API示例文档注解 - 提供请求和响应示例
 */
@Java110ExampleDoc(
        reqBody="http://{ip}:{port}/app/floor.queryFloors?page=1&row=10&communityId=123123",
        resBody="{'code':0,'msg':'成功','apiFloorDataVoList':[{'floorId':'123123','floorNum':'123213'}]}"
)

/**
 * 命令注解 - 注册服务代码
 */
@Java110Cmd(serviceCode = "floor.queryFloors")
public class QueryFloorsCmd extends Cmd {

    /**
     * 楼栋内部服务接口
     */
    @Autowired
    private IFloorInnerServiceSMO floorInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求JSON中是否包含必需的参数，并对参数格式进行检查
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必需参数是否存在
        Assert.jsonObjectHaveKey(reqJson, "page", "请求中未包含page信息");
        Assert.jsonObjectHaveKey(reqJson, "row", "请求中未包含row信息");
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求中未包含communityId信息");
        
        // 验证参数格式是否为有效数字
        Assert.isInteger(reqJson.getString("page"), "page不是有效数字");
        Assert.isInteger(reqJson.getString("row"), "row不是有效数字");
    }

    /**
     * 执行查询楼栋命令
     * 
     * 处理楼栋查询业务逻辑，包括分页查询楼栋列表和计算总页数
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当业务处理过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 从请求参数中获取行数（每页显示数量）
        // int page = reqJson.getInteger("page"); // 注释掉的页码参数
        int row = reqJson.getInteger("row");
        // String communityId = reqJson.getString("communityId"); // 注释掉的小区ID参数

        // 创建API响应对象
        ApiFloorVo apiFloorVo = new ApiFloorVo();

        // 查询总记录数 - 将请求参数转换为FloorDto对象进行查询
        int total = floorInnerServiceSMOImpl.queryFloorsCount(BeanConvertUtil.covertBean(reqJson, FloorDto.class));
        apiFloorVo.setTotal(total);
        
        // 如果存在数据，则查询楼栋列表
        if (total > 0) {
            // 查询楼栋数据列表
            List<FloorDto> floorDtoList = floorInnerServiceSMOImpl.queryFloors(BeanConvertUtil.covertBean(reqJson, FloorDto.class));
            // 将楼栋DTO列表转换为API数据VO列表
            apiFloorVo.setApiFloorDataVoList(BeanConvertUtil.covertBeanList(floorDtoList, ApiFloorDataVo.class));
        }

        // 计算总页数：总记录数 / 每页行数，向上取整
        apiFloorVo.setRecords((int) Math.ceil((double) total / (double) row));

        // 构建HTTP响应实体，将API响应对象转换为JSON字符串
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiFloorVo), HttpStatus.OK);
        // 设置响应实体到数据流上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}