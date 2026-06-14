/**
 * 查询未添加到权限组的权限命令类
 * 
 * 该命令用于查询系统中尚未添加到指定权限组的权限列表，支持根据商户类型、权限名称等条件进行筛选，
 * 并对查询结果进行去重处理和国际化支持
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.user.cmd.user;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.language.Language;
import com.java110.dto.store.StoreDto;
import com.java110.intf.store.IStoreV1InnerServiceSMO;
import com.java110.service.context.DataQuery;
import com.java110.service.smo.IQueryServiceSMO;
import com.java110.utils.constant.CommonConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.factory.ApplicationContextFactory;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Java110Cmd(serviceCode = "query.privilegeGroup.noAddPrivilege")
public class QueryPrivilegeGroupNoAddPrivilegeCmd extends Cmd {

    @Autowired
    private IStoreV1InnerServiceSMO storeV1InnerServiceSMOImpl;

    @Autowired
    private IQueryServiceSMO queryServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 当前实现中该方法为空，可根据业务需求添加参数校验逻辑
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 参数验证逻辑预留，可根据业务需求添加具体的参数校验
    }

    /**
     * 命令执行方法
     * 
     * 主要功能：
     * 1. 获取用户和商户信息
     * 2. 确定商户类型
     * 3. 查询未添加到权限组的权限列表
     * 4. 对查询结果进行去重处理
     * 5. 支持国际化处理
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 从请求头中获取用户ID和商户ID
        String userId = context.getReqHeaders().get("user-id");
        String storeId = context.getReqHeaders().get("store-id");

        // 如果请求头中没有用户ID，则从请求参数中获取
        if(StringUtil.isEmpty(userId)){
            userId = reqJson.getString("userId");
        }
        
        String storeTypeCd = "";
        // 判断请求参数中是否包含商户类型编码，如果没有则查询商户信息获取
        if(!reqJson.containsKey("storeTypeCd") || StringUtil.isEmpty(reqJson.getString("storeTypeCd"))) {
            // 构建商户查询条件
            StoreDto storeDto = new StoreDto();
            storeDto.setStoreId(storeId);
            storeDto.setPage(1);
            storeDto.setRow(1);
            // 查询商户信息
            List<StoreDto> storeDtos = storeV1InnerServiceSMOImpl.queryStores(storeDto);

            // 验证商户存在且唯一
            Assert.listOnlyOne(storeDtos, "商户不存在");
            storeTypeCd = storeDtos.get(0).getStoreTypeCd();

            // 如果是管理员商户，移除社区ID参数
            if(StoreDto.STORE_TYPE_ADMIN.equals(storeTypeCd)){
                reqJson.remove("communityId");
            }
        }else{
            // 直接从请求参数中获取商户类型编码
            storeTypeCd = reqJson.getString("storeTypeCd");
        }

        // 构建数据查询对象
        DataQuery dataQuery = new DataQuery();
        dataQuery.setServiceCode("query.privilegeGroup.noAddPrivilege");
        JSONObject param = new JSONObject();
        // 设置查询参数：商户ID、商户类型编码、权限组ID、权限名称、社区ID
        param.put("storeId", storeId);
        param.put("storeTypeCd", storeTypeCd);
        param.put("pgId", reqJson.getString("pgId"));
        param.put("pName",  reqJson.getString("pName"));
        param.put("communityId",  reqJson.getString("communityId"));
        dataQuery.setRequestParams(param);
        
        // 执行通用查询服务
        queryServiceSMOImpl.commonQueryService(dataQuery);
        ResponseEntity<String> privileges = dataQuery.getResponseEntity();
        
        // 检查查询响应状态，如果不是200则直接返回响应
        if (privileges.getStatusCode() != HttpStatus.OK) {
            context.setResponseEntity(privileges);
            return ;
        }

        // 解析查询结果
        JSONObject resultObj = JSONObject.parseObject(privileges.getBody().toString());
        JSONArray privilegeArrays = resultObj.getJSONArray("privileges");
        JSONObject privilegeObj = null;

        // 创建临时权限数组用于存储去重后的权限数据
        JSONArray tmpPrivilegeArrays = new JSONArray();

        // 遍历权限数组，进行去重处理
        for (int privilegeIndex = 0; privilegeIndex < privilegeArrays.size(); privilegeIndex++) {
            privilegeObj = privilegeArrays.getJSONObject(privilegeIndex);
            hasSameData(privilegeObj, tmpPrivilegeArrays);
        }

        // 国际化处理：根据请求头中的语言标识进行权限菜单的国际化转换
        Object lang = context.getReqHeaders().get(CommonConstant.JAVA110_LANG);
        if (!StringUtil.isNullOrNone(lang) && !CommonConstant.LANG_ZH_CN.equals(lang)) {
            Language language = ApplicationContextFactory.getBean(lang.toString(), Language.class);
            if (language != null) {
                tmpPrivilegeArrays = language.getPrivilegeMenuDto(tmpPrivilegeArrays);
            }
        }

        // 设置最终响应结果
        context.setResponseEntity(new ResponseEntity<String>(tmpPrivilegeArrays.toJSONString(), HttpStatus.OK));
    }

    /**
     * 权限数据去重方法
     * 
     * 根据权限ID进行去重处理，如果发现重复的权限ID，则保留有权限组ID的权限记录
     * 
     * @param privilegeObj 当前处理的权限对象
     * @param tmpPrivilegeArrays 临时存储去重后权限数据的数组
     */
    private void hasSameData(JSONObject privilegeObj, JSONArray tmpPrivilegeArrays) {
        JSONObject tmpPrivilegeObj = null;
        // 遍历临时权限数组，检查是否存在相同权限ID的记录
        for (int tmpPrivilegeIndex = 0; tmpPrivilegeIndex < tmpPrivilegeArrays.size(); tmpPrivilegeIndex++) {
            tmpPrivilegeObj = tmpPrivilegeArrays.getJSONObject(tmpPrivilegeIndex);
            // 如果找到相同权限ID的记录
            if (privilegeObj.getString("pId").equals(tmpPrivilegeObj.getString("pId"))) {
                // 如果当前权限对象有权限组ID，则替换临时数组中的对应记录
                if (!StringUtil.isEmpty(privilegeObj.getString("pgId"))) {
                    tmpPrivilegeArrays.remove(tmpPrivilegeIndex);
                    tmpPrivilegeArrays.add(privilegeObj);
                }
                return;
            }
        }
        // 如果没有找到重复的权限ID，则将当前权限对象添加到临时数组中
        tmpPrivilegeArrays.add(privilegeObj);
    }
}