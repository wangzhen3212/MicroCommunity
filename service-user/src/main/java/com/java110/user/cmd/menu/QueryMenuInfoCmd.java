/**
 * 查询菜单信息命令类
 * 
 * 该类负责处理菜单查询请求，根据用户ID、店铺ID等参数查询用户可访问的菜单信息，
 * 并将查询结果转换为前端所需的菜单格式结构
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.user.cmd.menu;

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
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Java110Cmd(serviceCode = "query.menu.info")
public class QueryMenuInfoCmd extends Cmd {

    @Autowired
    private IStoreV1InnerServiceSMO storeV1InnerServiceSMOImpl;

    @Autowired
    private IQueryServiceSMO queryServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求中是否包含必要的用户ID参数，如果请求头中不存在则从请求体中获取
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 从请求头中获取用户ID
        String userId = context.getReqHeaders().get("user-id");
        //String storeId = context.getReqHeaders().get("store-id");

        // 如果请求头中用户ID为空，则从请求体中获取
        if(StringUtil.isEmpty(userId)){
            userId = reqJson.getString("userId");
        }

        // 验证用户ID不能为空
        Assert.hasLength(userId, "未包含用户");
    }

    /**
     * 执行菜单查询命令
     * 
     * 根据用户ID、店铺ID等参数查询用户菜单权限，并将查询结果转换为前端所需的格式
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        // 从请求头中获取用户ID和店铺ID
        String userId = context.getReqHeaders().get("user-id");
        String storeId = context.getReqHeaders().get("store-id");

        // 设置默认菜单组类型为P_WEB（网页端）
        String groupType = "P_WEB";
        // 如果请求中包含groupType参数且不为空，则使用请求中的值
        if(reqJson.containsKey("groupType") && !StringUtil.isEmpty(reqJson.getString("groupType"))){
            groupType = reqJson.getString("groupType");
        }

        // 如果请求头中用户ID为空，则从请求体中获取
        if(StringUtil.isEmpty(userId)){
            userId = reqJson.getString("userId");
        }
        
        String domain = "";
        // 如果请求中不包含domain参数或domain为空，则根据店铺ID查询店铺信息获取domain
        if(!reqJson.containsKey("domain") || StringUtil.isEmpty(reqJson.getString("domain"))) {

            StoreDto storeDto = new StoreDto();
            storeDto.setStoreId(storeId);
            storeDto.setPage(1);
            storeDto.setRow(1);
            // 查询店铺信息
            List<StoreDto> storeDtos = storeV1InnerServiceSMOImpl.queryStores(storeDto);

            // 验证店铺存在且唯一
            Assert.listOnlyOne(storeDtos, "商户不存在");
            // 从店铺信息中获取domain（店铺类型编码）
            domain = storeDtos.get(0).getStoreTypeCd();
        }else{
            // 如果请求中包含domain参数，则直接使用
            domain = reqJson.getString("domain");
        }

        // 构建数据查询对象
        DataQuery dataQuery = new DataQuery();
        dataQuery.setServiceCode("query.menu.info");
        JSONObject param = new JSONObject();
        param.put("userId", userId);
        param.put("domain", domain);
        param.put("groupType", groupType);
        dataQuery.setRequestParams(param);
        // 执行通用查询服务
        queryServiceSMOImpl.commonQueryService(dataQuery);
        ResponseEntity<String> privilegeGroup = dataQuery.getResponseEntity();
        // 如果查询返回状态不是200，则直接返回错误响应
        if (privilegeGroup.getStatusCode() != HttpStatus.OK) {
            context.setResponseEntity(privilegeGroup);
            return ;
        }
        // 解析查询结果
        JSONObject resultObj = JSONObject.parseObject(privilegeGroup.getBody().toString());

        // 如果结果中不包含menus字段，直接返回
        if (!resultObj.containsKey("menus")) {
            return;
        }

        // 刷新菜单信息并设置响应
        context.setResponseEntity(refreshMenusInfo(resultObj.getJSONArray("menus")));

    }

    /**
     * 刷新菜单信息
     * 
     * 将原始菜单数据转换为前端所需的树形结构格式
     * 原始数据格式：[{
     *     "gId": "800201904001",
     *     "menuDescription": "添加员工",
     *     "menuGroupSeq": 1,
     *     "menuSeq": 1,
     *     "icon": "fa-desktop",
     *     "mId": "700201904001",
     *     "menuName": "添加员工",
     *     "pId": "500201904001",
     *     "menuGroupName": "员工管理",
     *     "label": "",
     *     "menuGroupDescription": "员工管理",
     *     "url": "/"
     * }]
     * 
     * 目标格式：
     * "[{'id':1,'icon':'fa-desktop','name':'我的菜单','label':'HOT','childs':[" +
     * "{'name':'子菜单','href':'http://www.baidu.com'}]}," +
     * "{'id':2,'icon':'fa-flask','name':'我的菜单','childs':[],'href':'/doc'}," +
     * "{'id':3,'icon':'fa-globe','name':'我的菜单','childs':[{'name':'子菜单','href':'http://www.baidu.com'}]}" +
     * "]";
     *
     * @param menusList 原始菜单列表JSON数组
     * @return 转换后的菜单信息响应实体
     */
    private ResponseEntity<String> refreshMenusInfo(JSONArray menusList) {
        // 创建临时菜单数组用于存储转换后的菜单结构
        JSONArray tempMenus = new JSONArray();
        JSONObject tempMenu = null;
        // 遍历原始菜单列表
        for (int menuIndex = 0; menuIndex < menusList.size(); menuIndex++) {
            JSONObject tMenu = menusList.getJSONObject(menuIndex);
            // 根据菜单组ID在临时菜单中查找是否已存在该菜单组
            tempMenu = this.getMenuFromMenus(tempMenus, tMenu.getString("gId"));
            if (tempMenu == null) {
                // 如果菜单组不存在，创建新的菜单组对象
                tempMenu = new JSONObject();
                tempMenu.put("id", tMenu.getString("gId"));
                tempMenu.put("icon", tMenu.getString("icon"));
                tempMenu.put("name", tMenu.getString("menuGroupName"));
                tempMenu.put("label", tMenu.getString("label"));
                tempMenu.put("seq", tMenu.getString("menuGroupSeq"));
                tempMenu.put("childs", new JSONArray());
                // 将新创建的菜单组添加到临时菜单数组中
                tempMenus.add(tempMenu);
            }
            // 获取当前菜单组的子菜单数组
            JSONArray childs = tempMenu.getJSONArray("childs");
            // 创建子菜单对象
            JSONObject childMenu = new JSONObject();
            childMenu.put("name", tMenu.getString("menuName"));
            childMenu.put("href", tMenu.getString("url"));
            childMenu.put("seq", tMenu.getString("menuSeq"));
            childMenu.put("isShow", tMenu.getString("isShow"));
            childMenu.put("description", tMenu.getString("description"));
            // 将子菜单添加到菜单组的子菜单数组中
            childs.add(childMenu);
        }
        // 返回转换后的菜单信息
        return new ResponseEntity<String>(tempMenus.toJSONString(), HttpStatus.OK);
    }

    /**
     * 在菜单列表中根据菜单组ID查询菜单
     * 
     * 遍历临时菜单数组，查找指定菜单组ID对应的菜单对象
     *
     * @param tempMenus 临时菜单数组
     * @param gId 菜单组ID
     * @return 找到的菜单对象，如果未找到则返回null
     */
    private JSONObject getMenuFromMenus(JSONArray tempMenus, String gId) {
        // 遍历临时菜单数组
        for (int tempIndex = 0; tempIndex < tempMenus.size(); tempIndex++) {
            // 比较菜单组ID，找到匹配的菜单对象
            if (tempMenus.getJSONObject(tempIndex).getString("id").equals(gId)) {
                return tempMenus.getJSONObject(tempIndex);
            }
        }

        return null;
    }
}