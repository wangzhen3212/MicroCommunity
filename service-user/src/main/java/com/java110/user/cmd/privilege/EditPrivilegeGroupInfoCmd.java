/**
 * 编辑权限组信息命令类
 * 
 * 该类负责处理编辑权限组信息的业务逻辑，包括参数验证和数据更新操作。
 * 通过注解@Java110Cmd声明服务代码为"edit.privilegeGroup.info"，
 * 当接收到对应服务代码的请求时，会执行该命令类的validate和doCmd方法。
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

@Java110Cmd(serviceCode = "edit.privilegeGroup.info")
public class EditPrivilegeGroupInfoCmd extends Cmd{

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
     * 参数验证方法
     * 
     * 该方法用于验证请求参数的合法性，包括：
     * 1. 验证请求报文中是否包含必要的字段
     * 2. 验证商户信息的有效性
     * 3. 将商户信息添加到请求参数中供后续使用
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求头等信息
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        // 验证请求报文中必须包含name节点
        Assert.jsonObjectHaveKey(reqJson, "name", "请求报文中未包含name节点");

        // 验证请求报文中必须包含pgId节点，用于标识要编辑的权限组
        Assert.hasKeyAndValue(reqJson,"pgId","角色不存在");

        // 从请求头中获取商户ID
        String storeId = context.getReqHeaders().get("store-id");

        // 构建商户查询条件
        StoreDto storeDto = new StoreDto();
        storeDto.setStoreId(storeId);
        storeDto.setPage(1);  // 设置查询页码为第一页
        storeDto.setRow(1);   // 设置每页显示一条记录
        
        // 执行商户查询
        List<StoreDto> storeDtos = storeV1InnerServiceSMOImpl.queryStores(storeDto);

        // 验证查询结果必须且只能有一条商户记录
        Assert.listOnlyOne(storeDtos,"商户不存在");

        // 将商户ID和商户类型编码添加到请求参数中，供后续业务逻辑使用
        reqJson.put("storeId",storeDtos.get(0).getStoreId());
        reqJson.put("storeTypeCd",storeDtos.get(0).getStoreTypeCd());

    }

    /**
     * 执行命令方法
     * 
     * 该方法负责执行实际的权限组信息更新操作，包括：
     * 1. 将请求参数转换为权限组PO对象
     * 2. 调用服务层更新权限组信息
     * 3. 处理更新结果并返回相应信息
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象，包含要更新的权限组信息
     * @throws CmdException 当更新操作失败时抛出异常
     * @throws ParseException 当参数解析异常时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        // 将请求JSON对象转换为权限组PO对象
        PrivilegeGroupPo privilegeGroupPo = BeanConvertUtil.covertBean(reqJson,PrivilegeGroupPo.class);

        // 调用权限组服务接口更新权限组信息
        int flag = privilegeGroupV1InnerServiceSMOImpl.updatePrivilegeGroup(privilegeGroupPo);

        // 检查更新操作是否成功，如果影响行数小于1则认为更新失败
        if(flag  < 1){
            throw new CmdException("保存失败");
        }

    }
}