package com.java110.dev.cmd.menu;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.privilege.BasePrivilegeDto;
import com.java110.dto.menu.MenuDto;
import com.java110.dto.menu.MenuGroupDto;
import com.java110.intf.community.IMenuInnerServiceSMO;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.ListenerExecuteException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

/**
 * 配置菜单命令类
 * 负责处理菜单配置相关的业务逻辑，包括菜单组、菜单和权限的保存
 * 
 * @author Java110
 * @serviceCode menu.configMenu
 */
@Java110Cmd(serviceCode = "menu.configMenu")
public class ConfigMenuCmd extends Cmd {
    
    @Autowired
    private IMenuInnerServiceSMO menuInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效
     * 
     * @param event 命令事件
     * @param context 数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 从请求中获取数据数组
        JSONArray infos = reqJson.getJSONArray("data");

        // 验证菜单视图相关参数
        Assert.hasKeyByFlowData(infos, "addMenuView", "name", "必填，请填写菜单名称");
        Assert.hasKeyByFlowData(infos, "addMenuView", "url", "必填，请菜单菜单地址");
        Assert.hasKeyByFlowData(infos, "addMenuView", "seq", "必填，请填写序列");
        Assert.hasKeyByFlowData(infos, "addMenuView", "isShow", "必填，请选择是否显示菜单");
        
        // 验证权限视图相关参数
        Assert.hasKeyByFlowData(infos, "addPrivilegeView", "name", "必填，请填写权限名称");
        Assert.hasKeyByFlowData(infos, "addPrivilegeView", "domain", "必填，请选择商户类型");
        Assert.hasKeyByFlowData(infos, "addPrivilegeView", "resource", "必填，请填写资源路径");
    }

    /**
     * 命令执行方法
     * 处理菜单配置的核心业务逻辑
     * 
     * @param event 命令事件
     * @param context 数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 从请求头中获取用户ID
        String userId = context.getReqHeaders().get("user-id");
        // 从请求中获取数据数组
        JSONArray infos = reqJson.getJSONArray("data");

        // 从数据数组中获取各个组件的数据
        JSONObject viewMenuGroupInfo = getObj(infos, "viewMenuGroupInfo");    // 菜单组信息
        JSONObject addMenuView = getObj(infos, "addMenuView");                // 菜单视图信息
        JSONObject addPrivilegeView = getObj(infos, "addPrivilegeView");      // 权限视图信息

        // 处理菜单ID：如果菜单视图没有mId，则使用权限视图的mId，否则使用菜单视图的mId
        if (!hasKey(addMenuView, "mId")) {
            addPrivilegeView.put("mId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.MENU));
        } else {
            addPrivilegeView.put("mId", addMenuView.getString("mId"));
        }

        // 保存菜单组（如果不存在）
        if (!hasKey(viewMenuGroupInfo, "gId")) {
            saveMenuGroup(viewMenuGroupInfo, userId);
        }

        // 保存菜单权限（如果不存在）
        if (!hasKey(addPrivilegeView, "pId")) {
            saveMenuPrivilege(addPrivilegeView, userId);
        }
        
        // 保存菜单（如果不存在）
        if (!hasKey(addMenuView, "mId")) {
            addMenuView.put("mId", addPrivilegeView.getString("mId"));
            addMenuView.put("gId", viewMenuGroupInfo.getString("gId"));
            addMenuView.put("pId", addPrivilegeView.getString("pId"));
            saveMenu(addMenuView, userId);
        }

        // 构建返回参数
        JSONObject outParam = new JSONObject();
        outParam.put("gId", viewMenuGroupInfo.getString("gId"));      // 菜单组ID
        outParam.put("pId", addPrivilegeView.getString("pId"));       // 权限ID
        outParam.put("mId", addMenuView.getString("mId"));            // 菜单ID

        // 设置响应实体
        context.setResponseEntity(ResultVo.createResponseEntity(outParam));
    }

    /**
     * 保存菜单组信息
     * 
     * @param info 菜单组信息Map
     * @param userId 用户ID
     * @throws ListenerExecuteException 保存失败时抛出异常
     */
    private void saveMenuGroup(Map info, String userId) {
        // 生成菜单组ID
        info.put("gId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.MENU_GROUP));
        info.put("userId", userId);
        
        // 转换为DTO对象
        MenuGroupDto menuGroupDto = BeanConvertUtil.covertBean(info, MenuGroupDto.class);
        
        // 调用服务保存菜单组
        if (menuInnerServiceSMOImpl.saveMenuGroup(menuGroupDto) < 1) {
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "参数异常，保存菜单组失败");
        }
    }

    /**
     * 保存菜单信息
     * 
     * @param info 菜单信息Map
     * @param userId 用户ID
     * @throws ListenerExecuteException 保存失败时抛出异常
     */
    private void saveMenu(Map info, String userId) {
        // 设置用户ID
        info.put("userId", userId);
        
        // 转换为DTO对象
        MenuDto menuDto = BeanConvertUtil.covertBean(info, MenuDto.class);
        
        // 调用服务保存菜单
        if (menuInnerServiceSMOImpl.saveMenu(menuDto) < 1) {
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "参数异常，保存菜单失败");
        }
    }

    /**
     * 保存菜单权限信息
     * 
     * @param info 权限信息Map
     * @param userId 用户ID
     * @throws ListenerExecuteException 保存失败时抛出异常
     */
    private void saveMenuPrivilege(Map info, String userId) {
        // 生成权限ID
        info.put("pId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.BASE_PRIVILEGE));
        info.put("userId", userId);
        
        // 转换为DTO对象
        BasePrivilegeDto basePrivilegeDto = BeanConvertUtil.covertBean(info, BasePrivilegeDto.class);
        
        // 调用服务保存权限
        if (menuInnerServiceSMOImpl.saveBasePrivilege(basePrivilegeDto) < 1) {
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "参数异常，保存菜单权限失败");
        }
    }

    /**
     * 检查JSON对象中是否包含指定键且值有效
     * 
     * @param info JSON对象
     * @param key 要检查的键
     * @return true-存在有效值，false-不存在或值无效
     */
    private boolean hasKey(JSONObject info, String key) {
        // 检查键是否存在、值是否为空、值是否以"-"开头（表示无效值）
        if (!info.containsKey(key)
                || StringUtil.isEmpty(info.getString(key))
                || info.getString(key).startsWith("-")) {
            return false;
        }
        return true;
    }

    /**
     * 从JSON数组中根据流程组件名称获取对应的JSON对象
     * 
     * @param infos JSON数组
     * @param flowComponent 流程组件名称
     * @return 对应的JSON对象
     * @throws IllegalArgumentException 未找到对应组件时抛出异常
     */
    private JSONObject getObj(JSONArray infos, String flowComponent) {
        JSONObject serviceInfo = null;

        // 遍历数组查找指定组件
        for (int infoIndex = 0; infoIndex < infos.size(); infoIndex++) {
            // 验证每个对象都包含flowComponent字段
            Assert.hasKeyAndValue(infos.getJSONObject(infoIndex), "flowComponent", "未包含服务流程组件名称");

            // 匹配指定组件名称
            if (flowComponent.equals(infos.getJSONObject(infoIndex).getString("flowComponent"))) {
                serviceInfo = infos.getJSONObject(infoIndex);
                Assert.notNull(serviceInfo, "未包含服务信息");
                return serviceInfo;
            }
        }

        // 未找到指定组件时抛出异常
        throw new IllegalArgumentException("未找到组件编码为【" + flowComponent + "】数据");
    }
}