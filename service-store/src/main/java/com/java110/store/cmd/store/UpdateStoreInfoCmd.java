/**
 * 商户信息更新命令类
 * 
 * 该类负责处理商户信息的更新操作，通过接收前端请求参数，验证数据有效性，
 * 并调用相应的服务层方法完成商户信息的修改。
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.store.cmd.store;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.store.StoreDto;
import com.java110.intf.store.IStoreInnerServiceSMO;
import com.java110.intf.store.IStoreV1InnerServiceSMO;
import com.java110.po.store.StorePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Java110Cmd(serviceCode = "update.store.info")
public class UpdateStoreInfoCmd extends Cmd {

    /**
     * 商户内部服务接口，用于查询商户信息
     */
    @Autowired
    private IStoreInnerServiceSMO storeInnerServiceSMOImpl;

    /**
     * 商户V1版本内部服务接口，用于更新商户信息
     */
    @Autowired
    private IStoreV1InnerServiceSMO storeV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求报文中是否包含必要的商户ID参数，确保后续操作的数据完整性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求报文中必须包含storeId参数
        Assert.hasKeyAndValue(reqJson, "storeId", "请求报文中未包含storeId");
    }

    /**
     * 执行商户信息更新命令
     * 
     * 根据请求参数更新商户信息，包括查询原商户信息、合并更新数据、
     * 调用服务层完成数据更新等操作
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象，包含需要更新的商户信息
     * @throws CmdException 当商户不存在、存在多条数据或更新失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 创建商户查询对象，设置查询条件
        StoreDto storeDto = new StoreDto();
        storeDto.setStoreId(reqJson.getString("storeId"));
        
        // 根据商户ID查询商户信息
        List<StoreDto> storeDtos = storeInnerServiceSMOImpl.getStores(storeDto);
        
        // 验证查询结果：必须存在且仅有一条商户记录
        Assert.listOnlyOne(storeDtos, "未找到需要修改的商户或多条数据");

        // 创建业务数据对象，用于存储合并后的商户信息
        JSONObject businessStore = new JSONObject();
        // 将查询到的原商户信息转换为Map并放入业务对象
        businessStore.putAll(BeanConvertUtil.beanCovertMap(storeDtos.get(0)));
        // 将请求中的更新数据合并到业务对象中（请求数据会覆盖原数据）
        businessStore.putAll(reqJson);
        
        // 将合并后的业务数据转换为持久化对象
        StorePo storePo = BeanConvertUtil.covertBean(businessStore, StorePo.class);
        
        // 调用服务层方法更新商户信息，返回影响的行数
        int flag = storeV1InnerServiceSMOImpl.updateStore(storePo);
        
        // 检查更新结果，如果更新失败则抛出异常
        if (flag < 1) {
            throw new CmdException("修改商户失败");
        }
    }
}