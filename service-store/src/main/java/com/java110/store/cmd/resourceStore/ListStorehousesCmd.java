/**
 * 仓库查询命令类
 * 
 * 该类负责处理仓库列表查询请求，支持分页查询和条件筛选功能。
 * 通过注解配置了API文档信息，包括请求参数、响应格式和示例。
 * 主要功能包括参数验证、数据查询、结果封装和响应返回。
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023
 */
package com.java110.store.cmd.resourceStore;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.store.StorehouseDto;
import com.java110.intf.store.IStorehouseInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Java110CmdDoc(title = "查询仓库",
        description = "外部系统通过此接口查询仓库",
        httpMethod = "get",
        url = "http://{ip}:{port}/app/resourceStore.listStorehouses",
        resource = "storeDoc",
        author = "吴学文",
        serviceCode = "resourceStore.listStorehouses"
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "page", length = 11, type = "int", remark = "页数"),
        @Java110ParamDoc(name = "row", length = 11, type = "int", remark = "行业数"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
                @Java110ParamDoc(name = "data", type = "Object", remark = "有效数据"),
                @Java110ParamDoc(parentNodeName = "data", name = "shName", type = "String", remark = "仓库"),
                @Java110ParamDoc(parentNodeName = "data", name = "shId", type = "String", remark = "仓库ID"),
        }
)

@Java110ExampleDoc(
        reqBody = "\n" +
                "http://localhost:3000/app/resourceStore.listStorehouses?shName=&shType=&isShow=&shId=&communityId=2023052267100146&page=1&row=10",
        resBody = "{\n" +
                "    \"code\": 0,\n" +
                "    \"data\": [\n" +
                "        {\n" +
                "            \"allocationFlowName\": \"调拨审核\",\n" +
                "            \"allocationRafId\": \"102023082479330020\",\n" +
                "            \"allocationSwitch\": \"ON\",\n" +
                "            \"createTime\": \"2023-08-24 21:56:01\",\n" +
                "            \"isShow\": \"true\",\n" +
                "            \"page\": -1,\n" +
                "            \"purchaseFlowName\": \"采购审核流程\",\n" +
                "            \"purchaseRafId\": \"102023082406130006\",\n" +
                "            \"purchaseSwitch\": \"ON\",\n" +
                "            \"records\": 0,\n" +
                "            \"row\": 0,\n" +
                "            \"shDesc\": \"\",\n" +
                "            \"shId\": \"102023082412640003\",\n" +
                "            \"shName\": \"A仓库\",\n" +
                "            \"statusCd\": \"0\",\n" +
                "            \"storeId\": \"10202305221014329108000168\",\n" +
                "            \"total\": 0,\n" +
                "            \"useFlowName\": \"领用审核\",\n" +
                "            \"useRafId\": \"102023082414750013\",\n" +
                "            \"useSwitch\": \"ON\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"msg\": \"成功\",\n" +
                "    \"page\": 0,\n" +
                "    \"records\": 1,\n" +
                "    \"rows\": 0,\n" +
                "    \"total\": 1\n" +
                "}"
)

@Java110Cmd(serviceCode = "resourceStore.listStorehouses")
public class ListStorehousesCmd extends Cmd {

    /**
     * 仓库内部服务接口，用于执行仓库相关的数据操作
     */
    @Autowired
    private IStorehouseInnerServiceSMO storehouseInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，主要进行分页信息的验证
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置上下文信息
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 调用父类的分页信息验证方法，验证page和row参数
        super.validatePageInfo(reqJson);
    }

    /**
     * 执行命令方法
     * 
     * 处理仓库查询请求，根据条件查询仓库列表并返回结果
     * 支持普通查询和特殊标记查询两种模式
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当数据解析失败时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从上下文中获取店铺ID
        String storeId = CmdContextUtils.getStoreId(context);
        
        // 将请求JSON转换为仓库数据传输对象
        StorehouseDto storehouseDto = BeanConvertUtil.covertBean(reqJson, StorehouseDto.class);
        
        // 检查是否存在特殊标记flag，且值为"1"
        if (reqJson.containsKey("flag") && !StringUtil.isEmpty(reqJson.getString("flag")) && reqJson.getString("flag").equals("1")) {
            // 特殊标记模式下，清空小区ID，用于查询所有仓库
            storehouseDto.setCommunityId("");
        }
        
        // 设置店铺ID到查询条件中
        storehouseDto.setStoreId(storeId);
        
        // 查询符合条件的仓库总数
        int count = storehouseInnerServiceSMOImpl.queryStorehousesCount(storehouseDto);
        
        // 初始化结果列表
        List<StorehouseDto> storehouseDtos = new ArrayList<>();
        
        // 如果存在符合条件的仓库记录
        if (count > 0) {
            // 查询仓库列表数据
            List<StorehouseDto> storehouseList = storehouseInnerServiceSMOImpl.queryStorehouses(storehouseDto);
            
            // 特殊标记处理逻辑：需要根据小区ID和仓库开放状态进行筛选
            if (reqJson.containsKey("flag") && !StringUtil.isEmpty(reqJson.getString("flag")) && reqJson.getString("flag").equals("1")) {
                // 遍历查询到的仓库列表
                for (StorehouseDto storehouse : storehouseList) {
                    // 获取仓库所属小区ID
                    String communityId = storehouse.getCommunityId();
                    // 获取仓库是否对外开放标识
                    String isShow = storehouse.getIsShow();
                    
                    // 判断是否为本小区仓库
                    if (communityId.equals(reqJson.getString("communityId"))) {
                        // 本小区仓库，直接添加到结果列表
                        storehouseDtos.add(storehouse);
                    } else if (isShow.equals("true")) {
                        // 对外开放的仓库，添加到结果列表
                        storehouseDtos.add(storehouse);
                    }
                    // 非本小区且不对外开放的仓库不添加到结果中
                }
            } else {
                // 普通查询模式，直接添加所有查询结果
                storehouseDtos.addAll(storehouseList);
            }
        } else {
            // 没有查询到数据，返回空列表
            storehouseDtos = new ArrayList<>();
        }
        
        // 计算总页数：总记录数除以每页显示数量，向上取整
        int totalPages = (int) Math.ceil((double) storehouseDtos.size() / (double) reqJson.getInteger("row"));
        
        // 创建结果对象，封装分页信息和数据
        ResultVo resultVo = new ResultVo(totalPages, storehouseDtos.size(), storehouseDtos);
        
        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        
        // 设置响应到上下文
        context.setResponseEntity(responseEntity);
    }
}