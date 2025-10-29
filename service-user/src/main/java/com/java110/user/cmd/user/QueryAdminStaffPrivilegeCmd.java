package com.java110.user.cmd.user;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.store.StoreDto;
import com.java110.intf.store.IStoreV1InnerServiceSMO;
import com.java110.service.context.DataQuery;
import com.java110.service.smo.IQueryServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 查询管理员员工权限命令类
 * 
 * 该命令用于查询指定员工在物业领域的权限信息，并对权限数据进行去重处理
 * 服务码：user.queryAdminStaffPrivilege
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "user.queryAdminStaffPrivilege")
public class QueryAdminStaffPrivilegeCmd extends Cmd {

    @Autowired
    private IStoreV1InnerServiceSMO storeV1InnerServiceSMOImpl;

    @Autowired
    private IQueryServiceSMO queryServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证当前用户是否为管理员权限
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        super.validateAdmin(context);
    }

    /**
     * 执行命令方法
     * 
     * 查询指定员工在物业领域的权限信息，并对权限数据进行去重处理
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象，包含staffId等参数
     * @throws CmdException 当命令执行过程中出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 创建数据查询对象
        DataQuery dataQuery = new DataQuery();
        dataQuery.setServiceCode("query.user.privilege");
        
        // 构建查询参数
        JSONObject param = new JSONObject();
        param.put("userId", reqJson.getString("staffId")); // 设置员工ID
        param.put("domain", StoreDto.STORE_TYPE_PROPERTY); // 设置查询领域为物业
        dataQuery.setRequestParams(param);
        
        // 执行权限查询服务
        queryServiceSMOImpl.commonQueryService(dataQuery);
        ResponseEntity<String> privilegeGroup = dataQuery.getResponseEntity();
        
        // 检查查询结果状态
        if (privilegeGroup.getStatusCode() != HttpStatus.OK) {
            context.setResponseEntity(privilegeGroup);
            return;
        }
        
        // 解析查询结果
        JSONObject resultObj = JSONObject.parseObject(privilegeGroup.getBody().toString());
        JSONArray privileges = resultObj.getJSONArray("privileges");
        
        // 创建临时权限数组用于去重处理
        JSONArray tmpPrivilegeArrays = new JSONArray();
        
        // 遍历权限列表进行去重处理
        JSONObject privilegeObj = null;
        for (int privilegeIndex = 0; privilegeIndex < privileges.size(); privilegeIndex++) {
            privilegeObj = privileges.getJSONObject(privilegeIndex);
            // 调用去重方法处理当前权限对象
            hasSameData(privilegeObj, tmpPrivilegeArrays);
        }
        
        // 构建返回结果
        JSONObject resObj = new JSONObject();
        resObj.put("datas", privileges); // 将处理后的权限数据放入返回结果
        context.setResponseEntity(new ResponseEntity<String>(resObj.toJSONString(), HttpStatus.OK));
    }

    /**
     * 权限数据去重方法
     * 
     * 检查权限对象是否已存在于目标数组中，如果存在则根据pgId进行更新，不存在则添加
     * 
     * @param privilegeObj 当前处理的权限对象
     * @param tmpPrivilegeArrays 目标权限数组，用于存储去重后的权限数据
     */
    private void hasSameData(JSONObject privilegeObj, JSONArray tmpPrivilegeArrays) {
        JSONObject tmpPrivilegeObj = null;
        // 遍历目标数组查找相同pId的权限
        for (int tmpPrivilegeIndex = 0; tmpPrivilegeIndex < tmpPrivilegeArrays.size(); tmpPrivilegeIndex++) {
            tmpPrivilegeObj = tmpPrivilegeArrays.getJSONObject(tmpPrivilegeIndex);
            // 检查权限ID是否相同
            if (privilegeObj.getString("pId").equals(tmpPrivilegeObj.getString("pId"))) {
                // 如果当前权限有pgId，则替换目标数组中的对应权限
                if (!StringUtil.isEmpty(privilegeObj.getString("pgId"))) {
                    tmpPrivilegeArrays.remove(tmpPrivilegeIndex);
                    tmpPrivilegeArrays.add(privilegeObj);
                }
                return; // 找到相同权限，直接返回
            }
        }
        // 没有找到相同权限，将当前权限添加到目标数组
        tmpPrivilegeArrays.add(privilegeObj);
    }
}