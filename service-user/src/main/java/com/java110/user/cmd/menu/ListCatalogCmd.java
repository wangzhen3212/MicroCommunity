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

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.language.Language;
import com.java110.dto.menu.MenuCatalogDto;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;


/**
 * 类表述：查询菜单目录列表的命令类
 * 服务编码：menuCatalog.listMenuCatalog
 * 请求路劲：/app/menuCatalog.ListMenuCatalog
 * 功能描述：该命令类用于处理菜单目录列表查询请求，支持分页查询和多语言处理
 * add by 吴学文 at 2022-02-26 10:12:36 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "menu.listCatalog")
public class ListCatalogCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListCatalogCmd.class);
    
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
     * 参数验证方法
     * 功能描述：验证请求参数的有效性，包括分页参数和商户ID参数
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文，包含请求和响应信息
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的分页信息验证方法
        super.validatePageInfo(reqJson);
        // 如果请求参数中没有storeId或者storeId为空，则从请求头中获取store-id作为storeId
        if(!reqJson.containsKey("storeId") || StringUtil.isEmpty(reqJson.getString("storeId"))){
            reqJson.put("storeId",cmdDataFlowContext.getReqHeaders().get("store-id"));
        }
    }

    /**
     * 命令执行方法
     * 功能描述：执行菜单目录列表查询业务逻辑，包括商户信息验证、数据查询、多语言处理和响应构建
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文，包含请求和响应信息
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 将请求JSON对象转换为MenuCatalogDto对象
        MenuCatalogDto menuCatalogDto = BeanConvertUtil.covertBean(reqJson, MenuCatalogDto.class);

        // 查询商户信息，用于获取商户类型
        StoreDto storeDto = new StoreDto();
        storeDto.setStoreId(reqJson.getString("storeId"));
        List<StoreDto> storeDtos = storeInnerServiceSMOImpl.getStores(storeDto);

        // 验证商户存在且唯一
        Assert.listOnlyOne(storeDtos, "商户不存在");

        // 设置菜单目录的商户类型
        menuCatalogDto.setStoreType(storeDtos.get(0).getStoreTypeCd());

        // 查询菜单目录总数
        int count = menuCatalogV1InnerServiceSMOImpl.queryMenuCatalogsCount(menuCatalogDto);

        List<MenuCatalogDto> menuCatalogDtos = null;

        // 根据查询结果数量决定返回数据
        if (count > 0) {
            // 查询菜单目录列表
            menuCatalogDtos = menuCatalogV1InnerServiceSMOImpl.queryMenuCatalogs(menuCatalogDto);
        } else {
            // 如果没有数据，返回空列表
            menuCatalogDtos = new ArrayList<>();
        }

        // 多语言处理：根据请求头中的语言标识进行国际化处理
        String lang = cmdDataFlowContext.getReqHeaders().get(CommonConstant.JAVA110_LANG);
        if (!StringUtil.isEmpty(lang) && !CommonConstant.LANG_ZH_CN.equals(lang)) {
            // 获取对应语言的处理器
            Language language = ApplicationContextFactory.getBean(lang, Language.class);
            if (language != null) {
                // 对菜单目录数据进行国际化处理
                menuCatalogDtos = language.getMenuCatalog(menuCatalogDtos);
            }
        }

        // 构建返回结果对象，包含分页信息和数据
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, menuCatalogDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应实体到数据流上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}