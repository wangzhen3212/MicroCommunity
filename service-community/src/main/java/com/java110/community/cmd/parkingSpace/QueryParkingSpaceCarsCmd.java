/**
 * 停车位车辆查询命令类
 * 
 * 该命令类负责处理停车位车辆查询请求，主要功能包括：
 * 1. 验证查询请求参数的完整性
 * 2. 根据车牌号查询对应的停车位信息
 * 3. 返回分页查询结果
 * 4. 处理特殊状态参数的转换
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.community.cmd.parkingSpace;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.DataFlowContext;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.intf.community.IParkingSpaceInnerServiceSMO;
import com.java110.intf.user.IOwnerCarInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.api.owner.ApiOwnerCarDataVo;
import com.java110.vo.api.owner.ApiOwnerCarVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "parkingSpace.queryParkingSpaceCars")
public class QueryParkingSpaceCarsCmd extends Cmd {

    /**
     * 停车位内部服务接口
     */
    @Autowired
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;

    /**
     * 业主车辆内部服务接口
     */
    @Autowired
    private IOwnerCarInnerServiceSMO ownerCarInnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 
     * 验证请求JSON中必须包含的参数：
     * - page: 页码
     * - row: 每页行数
     * - communityId: 小区ID
     * 同时验证page和row是否为有效数字
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证必要参数是否存在
        Assert.jsonObjectHaveKey(reqJson, "page", "请求中未包含page信息");
        Assert.jsonObjectHaveKey(reqJson, "row", "请求中未包含row信息");
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求中未包含communityId信息");
        
        // 验证数字参数的有效性
        Assert.isInteger(reqJson.getString("page"), "不是有效数字");
        Assert.isInteger(reqJson.getString("row"), "不是有效数字");
    }

    /**
     * 执行停车位车辆查询命令
     * 
     * 主要执行流程：
     * 1. 预处理请求数据
     * 2. 查询停车位车辆信息
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 命令执行异常
     * @throws ParseException 数据解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 预处理请求数据，处理特殊状态参数
        refreshReqJson(reqJson);

        // 根据车牌号查询停车位信息
        queryParkingSpaceCar(reqJson, context);
    }

    /**
     * 根据车牌号查询停车位信息
     * 
     * 执行分页查询：
     * 1. 查询总记录数
     * 2. 根据分页参数查询具体数据
     * 3. 计算总页数
     * 4. 设置响应结果
     * 
     * @param reqJson 请求JSON对象，包含查询条件和分页参数
     * @param dataFlowContext 数据流上下文对象，用于设置响应结果
     */
    private void queryParkingSpaceCar(JSONObject reqJson, ICmdDataFlowContext dataFlowContext) {
        // 初始化返回对象
        ApiOwnerCarVo apiOwnerCarVo = new ApiOwnerCarVo();

        // 获取每页行数
        int row = reqJson.getInteger("row");
        
        // 将请求参数转换为查询DTO
        OwnerCarDto ownerCarDto = BeanConvertUtil.covertBean(reqJson, OwnerCarDto.class);
        
        // 查询总记录数
        int total = ownerCarInnerServiceSMOImpl.queryOwnerCarsCount(ownerCarDto);
        apiOwnerCarVo.setTotal(total);
        
        // 如果存在数据，则查询具体记录
        if (total > 0) {
            List<OwnerCarDto> ownerCarDtos = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);
            // 将DTO列表转换为VO列表
            apiOwnerCarVo.setOwnerCars(BeanConvertUtil.covertBeanList(ownerCarDtos, ApiOwnerCarDataVo.class));
        }

        // 计算总页数：总记录数 / 每页行数，向上取整
        apiOwnerCarVo.setRecords((int) Math.ceil((double) total / (double) row));

        // 设置响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiOwnerCarVo), HttpStatus.OK);
        dataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 从业主车辆DTO列表中提取停车位ID数组
     * 
     * @param ownerCarDtos 业主车辆数据传输对象列表
     * @return 停车位ID字符串数组
     */
    private String[] getPsIds(List<OwnerCarDto> ownerCarDtos) {
        List<String> psIds = new ArrayList<String>();
        // 遍历所有业主车辆，收集停车位ID
        for (OwnerCarDto ownerCarDto : ownerCarDtos) {
            psIds.add(ownerCarDto.getPsId());
        }

        // 将List转换为数组返回
        return psIds.toArray(new String[psIds.size()]);
    }

    /**
     * 预处理请求数据
     * 
     * 主要功能：
     * 处理特殊状态参数"SH"，将其转换为包含"S"和"H"的状态数组
     * 
     * @param reqJson 请求JSON对象
     */
    private void refreshReqJson(JSONObject reqJson) {
        // 检查是否存在state参数
        if (!reqJson.containsKey("state")) {
            return;
        }

        // 如果state值为"SH"，则转换为包含"S"和"H"的数组
        if ("SH".equals(reqJson.getString("state"))) {
            reqJson.put("states", new String[]{"S", "H"});
            // 移除原始的state参数
            reqJson.remove("state");
        }
    }
}