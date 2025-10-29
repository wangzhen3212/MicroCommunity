/**
 * 删除权限组与权限关联关系的命令类
 * 
 * 该命令负责处理删除权限组中权限关联的请求，包括参数验证和权限关联删除操作
 * 主要功能包括验证商户信息、检查权限关联关系、批量删除权限关联等
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.user.cmd.privilege;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.privilege.PrivilegeRelDto;
import com.java110.dto.store.StoreDto;
import com.java110.intf.order.IPrivilegeInnerServiceSMO;
import com.java110.intf.store.IStoreV1InnerServiceSMO;
import com.java110.intf.user.IPrivilegeRelV1InnerServiceSMO;
import com.java110.po.privilege.PrivilegeRelPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

@Java110Cmd(serviceCode = "delete.privilege.PrivilegeGroup")
public class DeletePrivilegePrivilegeGroupCmd extends Cmd {

    /**
     * 商户信息服务接口
     */
    @Autowired
    private IStoreV1InnerServiceSMO storeV1InnerServiceSMOImpl;

    /**
     * 权限信息服务接口
     */
    @Autowired
    private IPrivilegeInnerServiceSMO privilegeInnerServiceSMOImpl;

    /**
     * 权限关联关系服务接口
     */
    @Autowired
    private IPrivilegeRelV1InnerServiceSMO privilegeRelV1InnerServiceSMOImpl;

    /**
     * 验证请求参数的有效性
     * 
     * 该方法用于验证删除权限组权限关联请求的必需参数，包括：
     * 1. 权限组ID(pgId)必须存在
     * 2. 权限ID列表(pIds)必须存在且不为空
     * 3. 验证商户信息是否存在
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求头和设置响应
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求报文中必须包含权限组ID
        Assert.hasKeyAndValue(reqJson, "pgId", "请求报文中未包含角色");

        // 验证请求报文中必须包含权限ID列表
        if (!reqJson.containsKey("pIds")) {
            throw new CmdException("未包含权限");
        }

        // 获取权限ID列表并验证不能为空
        JSONArray pIds = reqJson.getJSONArray("pIds");
        if (pIds.size() < 1) {
            throw new CmdException("未包含权限");
        }

        // 从请求头中获取商户ID
        String storeId = context.getReqHeaders().get("store-id");

        // 查询商户信息
        StoreDto storeDto = new StoreDto();
        storeDto.setStoreId(storeId);
        storeDto.setPage(1);
        storeDto.setRow(1);
        List<StoreDto> storeDtos = storeV1InnerServiceSMOImpl.queryStores(storeDto);

        // 验证商户必须存在且唯一
        Assert.listOnlyOne(storeDtos, "商户不存在");

        // 将商户信息添加到请求参数中，供后续处理使用
        reqJson.put("storeId", storeDtos.get(0).getStoreId());
        reqJson.put("storeTypeCd", storeDtos.get(0).getStoreTypeCd());
    }

    /**
     * 执行删除权限组权限关联的核心业务逻辑
     * 
     * 该方法遍历权限ID列表，逐个删除权限组与权限的关联关系，
     * 并统计成功和失败的数量，最后返回操作结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文，用于设置响应结果
     * @param reqJson 请求参数的JSON对象，包含权限组ID和权限ID列表
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当数据解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 获取权限ID列表
        JSONArray pIds = reqJson.getJSONArray("pIds");
        int errorCount = 0; // 记录删除失败的数量
        JSONObject tmpPId = null; // 临时存储当前处理的权限ID对象
        PrivilegeRelPo tmpPrivilegePo = null; // 临时权限关联PO对象
        PrivilegeRelDto privilegeRelDto = null; // 权限关联查询条件对象
        int flag = 0; // 删除操作返回值
        List<PrivilegeRelDto> privilegeRelDtos = null; // 查询结果列表

        // 遍历所有权限ID，逐个删除关联关系
        for (int pIdIndex = 0; pIdIndex < pIds.size(); pIdIndex++) {
            // 获取当前处理的权限ID
            tmpPId = pIds.getJSONObject(pIdIndex);
            reqJson.put("pId", tmpPId.getString("pId"));

            // 构建权限关联查询条件
            privilegeRelDto = BeanConvertUtil.covertBean(reqJson, PrivilegeRelDto.class);
            // 查询权限关联关系
            privilegeRelDtos = privilegeRelV1InnerServiceSMOImpl.queryPrivilegeRels(privilegeRelDto);

            // 如果查询不到关联关系，跳过当前权限的处理
            if(privilegeRelDtos == null || privilegeRelDtos.size()<1){
                continue;
            }
            
            // 构建删除对象并执行删除操作
            tmpPrivilegePo = new PrivilegeRelPo();
            tmpPrivilegePo.setRelId(privilegeRelDtos.get(0).getRelId());
            flag = privilegeRelV1InnerServiceSMOImpl.deletePrivilegeRel(tmpPrivilegePo);
            
            // 统计删除失败的数量
            if (flag < 1) {
                errorCount++;
            }
        }

        // 构建响应结果
        JSONObject paramOut = new JSONObject();
        paramOut.put("success", pIds.size() - errorCount); // 成功数量
        paramOut.put("error", errorCount); // 失败数量

        // 设置响应实体
        context.setResponseEntity(ResultVo.createResponseEntity(paramOut.toJSONString()));
    }
}