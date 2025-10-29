/**
 * 查询店铺权限组命令类
 * 
 * 该命令类用于处理查询店铺权限组的请求，根据用户信息和店铺信息获取对应的权限组列表。
 * 支持根据用户角色和店铺类型进行权限组过滤，确保用户只能访问自己有权限的权限组。
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.user.cmd.user;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.store.StoreDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.store.IStoreV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.service.context.DataQuery;
import com.java110.service.smo.IQueryServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Java110Cmd(serviceCode = "query.store.privilegeGroup")
public class QueryStorePrivilegeGroupCmd extends Cmd {

    @Autowired
    private IStoreV1InnerServiceSMO storeV1InnerServiceSMOImpl;

    @Autowired
    private IQueryServiceSMO queryServiceSMOImpl;

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 当前实现中该方法为空，未进行具体的参数验证逻辑
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 当前未实现参数验证逻辑，可根据业务需求添加验证规则
    }

    /**
     * 执行命令方法
     * 
     * 主要业务流程：
     * 1. 获取用户信息和店铺信息
     * 2. 验证用户存在性
     * 3. 获取店铺类型编码
     * 4. 构建查询参数并调用查询服务
     * 5. 处理查询结果并返回权限组列表
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 从请求头中获取用户ID、店铺ID和小区ID
        String userId = context.getReqHeaders().get("user-id");
        String storeId = context.getReqHeaders().get("store-id");
        String communityId = context.getReqHeaders().get("communityId");
        
        // 如果请求头中用户ID为空，则从请求参数中获取
        if (StringUtil.isEmpty(userId)) {
            userId = reqJson.getString("userId");
        }
        
        // 查询用户信息，验证用户存在性
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        userDto.setPage(1);
        userDto.setRow(1);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);
        // 断言确保用户存在且唯一
        Assert.listOnlyOne(userDtos, "用户不存在");
        
        String storeTypeCd = "";
        // 判断请求参数中是否包含店铺类型编码，如果没有则查询店铺信息获取
        if (!reqJson.containsKey("storeTypeCd") || StringUtil.isEmpty(reqJson.getString("storeTypeCd"))) {
            StoreDto storeDto = new StoreDto();
            storeDto.setStoreId(storeId);
            storeDto.setPage(1);
            storeDto.setRow(1);
            List<StoreDto> storeDtos = storeV1InnerServiceSMOImpl.queryStores(storeDto);
            // 断言确保店铺存在且唯一
            Assert.listOnlyOne(storeDtos, "商户不存在");
            storeTypeCd = storeDtos.get(0).getStoreTypeCd();
        } else {
            // 直接从请求参数中获取店铺类型编码
            storeTypeCd = reqJson.getString("storeTypeCd");
        }
        
        // 构建数据查询对象
        DataQuery dataQuery = new DataQuery();
        dataQuery.setServiceCode("query.store.privilegeGroup");
        JSONObject param = new JSONObject();
        param.put("storeId", storeId);
        param.put("storeTypeCd", storeTypeCd);
        
        // 注释掉的代码：原设计考虑根据用户级别过滤小区ID
        // 如果用户不是管理员，则添加小区ID作为查询条件
        // 但此设计被认为不合理，因为会导致跨小区管理员无法看到所有权限组
//        if(!UserDto.LEVEL_CD_ADMIN.equals(userDtos.get(0).getLevelCd())){
//            param.put("communityId", communityId);
//        }
        
        dataQuery.setRequestParams(param);
        // 调用通用查询服务获取权限组信息
        queryServiceSMOImpl.commonQueryService(dataQuery);
        ResponseEntity<String> privilegeGroup = dataQuery.getResponseEntity();
        
        // 检查查询响应状态，如果不是200则直接返回错误响应
        if (privilegeGroup.getStatusCode() != HttpStatus.OK) {
            context.setResponseEntity(privilegeGroup);
            return;
        }
        
        // 解析查询结果
        JSONObject privilegeGroupObj = JSONObject.parseObject(privilegeGroup.getBody().toString());
        // 断言确保返回结果包含privilegeGroups节点
        Assert.jsonObjectHaveKey(privilegeGroupObj, "privilegeGroups", "查询菜单未返回privilegeGroups节点");
        JSONArray privilegeGroups = privilegeGroupObj.getJSONArray("privilegeGroups");
        
        // 返回权限组列表
        context.setResponseEntity(new ResponseEntity<String>(privilegeGroups.toJSONString(), HttpStatus.OK));
    }
}