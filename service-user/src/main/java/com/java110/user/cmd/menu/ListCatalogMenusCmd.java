/*
 * Copyright 2017-2020 吴学文 and java110 team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.java110.user.cmd.menu;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.language.Language;
import com.java110.dto.menu.MenuGroupDto;
import com.java110.dto.store.StoreDto;
import com.java110.intf.store.IStoreInnerServiceSMO;
import com.java110.intf.user.IMenuCatalogV1InnerServiceSMO;
import com.java110.utils.constant.CommonConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.factory.ApplicationContextFactory;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;


/**
 * 类表述：查询菜单目录和菜单信息
 * 服务编码：menuCatalog.listMenuCatalog
 * 请求路劲：/app/menuCatalog.ListMenuCatalog
 * add by 吴学文 at 2022-02-26 10:12:36 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
/**
 * 菜单目录查询命令类
 * 负责处理菜单目录和菜单信息的查询请求，根据用户和商户信息返回对应的菜单结构
 * 支持多语言菜单显示，根据请求头中的语言标识进行菜单文本的国际化处理
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-02-26
 */
@Java110Cmd(serviceCode = "menu.listCatalogMenus")
public class ListCatalogMenusCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListCatalogMenusCmd.class);
    
    /**
     * 菜单目录服务接口
     */
    @Autowired
    private IMenuCatalogV1InnerServiceSMO menuCatalogV1InnerServiceSMOImpl;

    /**
     * 商户服务接口
     */
    @Autowired
    private IStoreInnerServiceSMO storeInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 验证请求中是否包含必要的用户信息和商户信息
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含用户ID
        Assert.hasKeyAndValue(reqJson, "userId", "未包含用户信息");
        // 验证请求参数中必须包含商户ID
        Assert.hasKeyAndValue(reqJson, "storeId", "未包含商户信息");
        // 目录信息参数可选，暂时注释掉验证
        //Assert.hasKeyAndValue(reqJson, "caId", "未包含目录信息");
    }

    /**
     * 执行菜单查询命令
     * 根据用户和商户信息查询对应的菜单数据，支持多语言处理
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行过程中出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为菜单组DTO对象
        MenuGroupDto menuCatalogDto = BeanConvertUtil.covertBean(reqJson, MenuGroupDto.class);
        // 设置菜单组类型为PC端
        menuCatalogDto.setGroupType(MenuGroupDto.GROUP_TYPE_PC);
        
        // 查询商户信息
        StoreDto storeDto = new StoreDto();
        storeDto.setStoreId(reqJson.getString("storeId"));
        List<StoreDto> storeDtos = storeInnerServiceSMOImpl.getStores(storeDto);

        // 验证商户存在且唯一
        Assert.listOnlyOne(storeDtos, "商户不存在");

        // 判断商户类型，如果是管理员或开发者商户，则清空小区ID
        if (StoreDto.STORE_TYPE_ADMIN.equals(storeDtos.get(0).getStoreTypeCd()) 
                || StoreDto.STORE_TYPE_DEV.equals(storeDtos.get(0).getStoreTypeCd())) {
            menuCatalogDto.setCommunityId("");
        }

        // 设置菜单查询的域和商户类型
        menuCatalogDto.setDomain(storeDtos.get(0).getStoreTypeCd());
        menuCatalogDto.setStoreType(storeDtos.get(0).getStoreTypeCd());

        // 查询菜单数据
        List<Map> menus = menuCatalogV1InnerServiceSMOImpl.queryMenus(menuCatalogDto);
        
        // 获取请求头中的语言标识，进行多语言处理
        String lang = cmdDataFlowContext.getReqHeaders().get(CommonConstant.JAVA110_LANG);
        if (!StringUtil.isEmpty(lang) && !CommonConstant.LANG_ZH_CN.equals(lang)) {
            // 根据语言标识获取对应的语言处理器
            Language language = ApplicationContextFactory.getBean(lang, Language.class);
            if (language != null) {
                // 使用语言处理器处理菜单数据
                menus = language.getMenuDto(menus);
            }
        }

        // 设置响应数据
        cmdDataFlowContext.setResponseEntity(refreshMenusInfo(menus));
    }

    /**
     * 刷新菜单信息，将扁平化的菜单数据转换为树形结构
     * 按照菜单组进行分组，构建父子关系的菜单树
     *
     * @param menusList 原始菜单数据列表
     * @return 包含树形菜单结构的响应实体
     */
    private ResponseEntity<String> refreshMenusInfo(List<Map> menusList) {
        JSONArray tempMenus = new JSONArray();
        JSONObject tempMenu = null;
        
        // 遍历所有菜单项，构建树形结构
        for (Map tmpMenu : menusList) {
            // 将Map转换为JSONObject便于处理
            JSONObject tMenu = JSONObject.parseObject(JSONObject.toJSONString(tmpMenu));
            
            // 根据菜单组ID查找或创建菜单组节点
            tempMenu = this.getMenuFromMenus(tempMenus, tMenu.getString("gId"));
            if (tempMenu == null) {
                // 创建新的菜单组节点
                tempMenu = new JSONObject();
                tempMenu.put("id", tMenu.getString("gId"));
                tempMenu.put("icon", tMenu.getString("icon"));
                tempMenu.put("name", tMenu.getString("menuGroupName"));
                tempMenu.put("label", tMenu.getString("label"));
                tempMenu.put("seq", tMenu.getString("menuGroupSeq"));
                tempMenu.put("childs", new JSONArray());
                tempMenus.add(tempMenu);
            }
            
            // 获取当前菜单组的子菜单列表
            JSONArray childs = tempMenu.getJSONArray("childs");
            
            // 创建子菜单项
            JSONObject childMenu = new JSONObject();
            childMenu.put("mId", tMenu.getString("mId"));
            childMenu.put("name", tMenu.getString("menuName"));
            childMenu.put("href", tMenu.getString("url"));
            childMenu.put("seq", tMenu.getString("menuSeq"));
            childMenu.put("isShow", tMenu.getString("isShow"));
            childMenu.put("description", tMenu.getString("description"));
            
            // 将子菜单添加到菜单组中
            childs.add(childMenu);
        }

        // 返回构建好的菜单树结构
        return ResultVo.createResponseEntity(tempMenus);
    }

    /**
     * 在菜单列表中根据菜单组ID查找对应的菜单组
     *
     * @param tempMenus 菜单组列表
     * @param gId 菜单组ID
     * @return 找到的菜单组JSON对象，如果未找到则返回null
     */
    private JSONObject getMenuFromMenus(JSONArray tempMenus, String gId) {
        // 遍历菜单组列表，查找指定ID的菜单组
        for (int tempIndex = 0; tempIndex < tempMenus.size(); tempIndex++) {
            if (tempMenus.getJSONObject(tempIndex).getString("id").equals(gId)) {
                return tempMenus.getJSONObject(tempIndex);
            }
        }

        return null;
    }
}