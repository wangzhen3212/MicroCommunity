package com.java110.store.cmd.resourceStore;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.resource.ResourceStoreTimesDto;
import com.java110.intf.community.IMenuInnerServiceSMO;
import com.java110.intf.store.IResourceStoreTimesV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 查询价格物品命令类
 * 该命令用于查询资源库中的物品及其相关信息，支持分页查询
 * 主要功能包括参数验证、数据查询和结果封装
 * 
 * @author wuxw
 * @version 1.0
 * @since 2023-01-19
 */
@Java110Cmd(serviceCode = "resourceStore.listResourceStoresAndTimes")
public class ListResourceStoresAndTimesCmd extends Cmd {

    /**
     * 资源库时间服务接口，用于查询资源库物品信息
     */
    @Autowired
    private IResourceStoreTimesV1InnerServiceSMO resourceStoreTimesV1InnerServiceSMOImpl;

    /**
     * 菜单服务接口，用于获取菜单相关信息
     */
    @Autowired
    private IMenuInnerServiceSMO menuInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 调用父类的分页信息验证方法
        super.validatePageInfo(reqJson);
        // 验证请求报文中必须包含小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含小区ID");
        // 验证请求报文中必须包含商户ID
        Assert.hasKeyAndValue(reqJson, "storeId", "请求报文中未包含商户ID");
    }

    /**
     * 命令执行方法
     * 执行资源库物品查询业务逻辑，包括数据查询和结果封装
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     * @throws ParseException 当数据解析错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求JSON对象转换为资源库时间数据传输对象
        ResourceStoreTimesDto resourceStoreTimesDto = BeanConvertUtil.covertBean(reqJson, ResourceStoreTimesDto.class);

        // 查询符合条件的资源库物品总数
        int count = resourceStoreTimesV1InnerServiceSMOImpl.queryResourceStoreTimessCount(resourceStoreTimesDto);
        
        List<ResourceStoreTimesDto> resourceStores = null;
        // 根据查询结果数量判断是否进行详细查询
        if (count > 0) {
            // 查询资源库物品详细信息列表
            resourceStores = resourceStoreTimesV1InnerServiceSMOImpl.queryResourceStoreTimess(resourceStoreTimesDto);
        } else {
            // 如果没有查询到数据，返回空列表
            resourceStores = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页显示数量，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 封装返回结果对象，包含总页数、总记录数和数据列表
        ResultVo resultVo = new ResultVo(totalPage, count, resourceStores);

        // 创建HTTP响应实体，设置响应状态为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应实体设置到上下文中
        context.setResponseEntity(responseEntity);
    }
}