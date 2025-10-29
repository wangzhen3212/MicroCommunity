/**
 * 权限组信息保存命令类
 * 
 * 该类负责处理权限组信息的保存请求，包括参数验证和业务逻辑处理
 * 通过注解@Java110Cmd声明服务代码为"save.privilegeGroup.info"
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.user.cmd.privilege;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.privilege.PrivilegeGroupDto;
import com.java110.dto.store.StoreDto;
import com.java110.intf.order.IPrivilegeInnerServiceSMO;
import com.java110.intf.store.IStoreV1InnerServiceSMO;
import com.java110.intf.user.IPrivilegeGroupV1InnerServiceSMO;
import com.java110.po.privilege.PrivilegeGroupPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

@Java110Cmd(serviceCode = "save.privilegeGroup.info")
public class SavePrivilegeGroupInfoCmd extends Cmd {

    /**
     * 商户服务接口，用于查询商户信息
     */
    @Autowired
    private IStoreV1InnerServiceSMO storeV1InnerServiceSMOImpl;

    /**
     * 权限服务接口，用于权限相关操作
     */
    @Autowired
    private IPrivilegeInnerServiceSMO privilegeInnerServiceSMOImpl;

    /**
     * 权限组服务接口，用于权限组的增删改查操作
     */
    @Autowired
    private IPrivilegeGroupV1InnerServiceSMO privilegeGroupV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 该方法负责验证请求参数的合法性，包括：
     * 1. 检查请求报文中是否包含必要的name节点
     * 2. 验证商户是否存在且唯一
     * 3. 将商户ID和商户类型编码添加到请求JSON中
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求头等信息
     * @param reqJson 请求JSON对象，包含客户端传递的参数
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求报文中必须包含name节点
        Assert.jsonObjectHaveKey(reqJson, "name", "请求报文中未包含name节点");
        
        // 从请求头中获取商户ID
        String storeId = context.getReqHeaders().get("store-id");
        
        // 构建商户查询条件
        StoreDto storeDto = new StoreDto();
        storeDto.setStoreId(storeId);
        storeDto.setPage(1);
        storeDto.setRow(1);
        
        // 查询商户信息
        List<StoreDto> storeDtos = storeV1InnerServiceSMOImpl.queryStores(storeDto);
        
        // 验证商户存在且唯一
        Assert.listOnlyOne(storeDtos, "商户不存在");
        
        // 将商户ID和商户类型编码添加到请求JSON中，供后续业务逻辑使用
        reqJson.put("storeId", storeDtos.get(0).getStoreId());
        reqJson.put("storeTypeCd", storeDtos.get(0).getStoreTypeCd());
    }

    /**
     * 执行权限组保存命令
     * 
     * 该方法负责执行权限组的保存操作，包括：
     * 1. 生成权限组ID
     * 2. 转换请求参数为权限组PO对象
     * 3. 设置权限组的域和角色类型
     * 4. 调用服务保存权限组信息
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON对象，包含经过验证的参数
     * @throws CmdException 当保存操作失败时抛出异常
     * @throws ParseException 当参数解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 生成权限组ID
        reqJson.put("pgId", GenerateCodeFactory.getPgId());
        
        // 将请求JSON转换为权限组PO对象
        PrivilegeGroupPo privilegeGroupPo = BeanConvertUtil.covertBean(reqJson, PrivilegeGroupPo.class);
        
        // 设置权限组的域为商户类型编码
        privilegeGroupPo.setDomain(reqJson.getString("storeTypeCd"));
        
        // 设置权限组的角色类型为用户角色
        privilegeGroupPo.setRoleType(PrivilegeGroupDto.ROLE_TYPE_USER);
        
        // 调用服务保存权限组信息，并获取操作结果
        int flag = privilegeGroupV1InnerServiceSMOImpl.savePrivilegeGroup(privilegeGroupPo);
        
        // 检查保存操作是否成功，失败则抛出异常
        if (flag < 1) {
            throw new CmdException("保存失败");
        }
    }
}