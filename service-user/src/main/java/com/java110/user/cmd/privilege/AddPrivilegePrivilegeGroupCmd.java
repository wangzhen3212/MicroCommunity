/**
 * 权限组添加命令类
 * 
 * 该命令类用于处理添加权限组到权限的关联关系请求
 * 主要功能包括验证请求参数的有效性，以及执行权限关联的批量添加操作
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.user.cmd.privilege;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
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

/**
 * 添加权限组命令类
 * 
 * 该类继承自Cmd基类，用于处理权限组与权限的关联关系添加操作
 * 通过@Java110Cmd注解标识服务代码为"add.privilege.PrivilegeGroup"
 * 主要职责包括参数验证和批量添加权限关联关系
 */
@Java110Cmd(serviceCode = "add.privilege.PrivilegeGroup")
public class AddPrivilegePrivilegeGroupCmd extends Cmd {

    /**
     * 商户信息服务接口
     * 用于查询和验证商户信息
     */
    @Autowired
    private IStoreV1InnerServiceSMO storeV1InnerServiceSMOImpl;

    /**
     * 权限信息服务接口
     * 用于权限相关的业务操作
     */
    @Autowired
    private IPrivilegeInnerServiceSMO privilegeInnerServiceSMOImpl;

    /**
     * 权限关联关系服务接口
     * 用于保存权限与权限组的关联关系
     */
    @Autowired
    private IPrivilegeRelV1InnerServiceSMO privilegeRelV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证添加权限组请求的必需参数，包括角色ID和权限ID列表
     * 同时验证商户信息的有效性，并将商户信息添加到请求参数中供后续使用
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求头信息和设置响应
     * @param reqJson 请求参数的JSON对象，包含权限组ID和权限ID列表
     * @throws CmdException 当参数验证失败时抛出异常，包括缺少必需参数或商户不存在等情况
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求报文中必须包含角色ID（权限组ID）
        Assert.hasKeyAndValue(reqJson, "pgId", "请求报文中未包含角色");

        // 验证请求报文中必须包含权限ID列表
        if (!reqJson.containsKey("pIds")) {
            throw new CmdException("未包含权限");
        }

        // 获取权限ID数组并验证至少包含一个权限
        JSONArray pIds = reqJson.getJSONArray("pIds");
        if (pIds.size() < 1) {
            throw new CmdException("未包含权限");
        }

        // 从请求头中获取商户ID，用于后续商户信息验证
        String storeId = context.getReqHeaders().get("store-id");

        // 构建商户查询条件对象
        StoreDto storeDto = new StoreDto();
        storeDto.setStoreId(storeId);  // 设置商户ID
        storeDto.setPage(1);           // 设置查询页码
        storeDto.setRow(1);            // 设置每页记录数
        
        // 查询商户信息，验证商户是否存在
        List<StoreDto> storeDtos = storeV1InnerServiceSMOImpl.queryStores(storeDto);

        // 验证商户存在且唯一，如果不存在或存在多个则抛出异常
        Assert.listOnlyOne(storeDtos, "商户不存在");

        // 将商户信息添加到请求参数中，供后续处理使用
        reqJson.put("storeId", storeDtos.get(0).getStoreId());        // 设置商户ID
        reqJson.put("storeTypeCd", storeDtos.get(0).getStoreTypeCd()); // 设置商户类型编码
    }

    /**
     * 执行权限组添加命令
     * 
     * 批量添加权限与权限组的关联关系，并统计操作结果
     * 遍历权限ID列表，为每个权限创建与权限组的关联关系
     * 
     * @param event 命令事件对象，包含命令执行上下文信息
     * @param context 命令数据流上下文，用于设置响应结果
     * @param reqJson 请求参数的JSON对象，包含权限组ID和权限ID列表
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当数据解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 获取权限ID列表，用于批量创建关联关系
        JSONArray pIds = reqJson.getJSONArray("pIds");
        int errorCount = 0; // 记录失败的操作数量
        JSONObject tmpPId = null; // 临时存储单个权限ID对象
        PrivilegeRelPo tmpPrivilegePo = null; // 临时存储权限关联对象
        int flag = 0; // 单次操作结果标志，用于判断保存操作是否成功

        // 遍历所有权限ID，逐个创建权限与权限组的关联关系
        for (int pIdIndex = 0; pIdIndex < pIds.size(); pIdIndex++) {
            // 获取当前权限ID对象
            tmpPId = pIds.getJSONObject(pIdIndex);
            // 将当前权限ID设置到请求参数中，用于后续对象转换
            reqJson.put("pId", tmpPId.getString("pId"));
            // 将请求参数转换为权限关联对象，准备保存到数据库
            tmpPrivilegePo = BeanConvertUtil.covertBean(reqJson, PrivilegeRelPo.class);
            
            // 注释掉的代码：生成关联关系ID
            // 如果需要自动生成关联关系ID，可以取消注释并使用以下代码
            //tmpPrivilegePo.setRelId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_relId));
            
            // 保存权限关联关系到数据库，flag表示影响的行数
            flag = privilegeRelV1InnerServiceSMOImpl.savePrivilegeRel(tmpPrivilegePo);
            // 统计失败的操作数量，如果影响行数小于1表示保存失败
            if (flag < 1) {
                errorCount++;
            }
        }

        // 构建操作结果响应对象，包含成功和失败的数量统计
        JSONObject paramOut = new JSONObject();
        paramOut.put("success", pIds.size() - errorCount); // 计算成功数量：总数量减去失败数量
        paramOut.put("error", errorCount); // 记录失败数量

        // 设置响应实体，将操作结果返回给调用方
        context.setResponseEntity(ResultVo.createResponseEntity(paramOut.toJSONString()));
    }
}