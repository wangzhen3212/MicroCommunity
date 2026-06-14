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
package com.java110.dev.cmd.menuGroupCatalog;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.menu.MenuGroupCatalogDto;
import com.java110.intf.user.IMenuGroupCatalogV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;


/**
 * 类表述：查询菜单组目录列表
 * 服务编码：menuGroupCatalog.listMenuGroupCatalog
 * 请求路径：/app/menuGroupCatalog.ListMenuGroupCatalog
 * add by 吴学文 at 2022-02-26 10:18:54 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "menuGroupCatalog.listMenuGroupCatalog")
public class ListMenuGroupCatalogCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListMenuGroupCatalogCmd.class);
    
    @Autowired
    private IMenuGroupCatalogV1InnerServiceSMO menuGroupCatalogV1InnerServiceSMOImpl;

    /**
     * 参数校验方法
     * 主要功能：验证请求参数的有效性，包括分页参数等
     * @param event Cmd事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        super.validatePageInfo(reqJson);
    }

    /**
     * 业务处理主方法
     * 主要功能：查询菜单组目录列表，支持分页查询
     * 执行流程：
     * 1. 将请求JSON转换为MenuGroupCatalogDto对象
     * 2. 查询符合条件的记录总数
     * 3. 根据分页参数查询具体数据列表
     * 4. 构建返回结果并设置响应实体
     * 
     * @param event Cmd事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当业务处理出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为MenuGroupCatalogDto对象
        MenuGroupCatalogDto menuGroupCatalogDto = BeanConvertUtil.covertBean(reqJson, MenuGroupCatalogDto.class);

        // 查询符合条件的记录总数
        int count = menuGroupCatalogV1InnerServiceSMOImpl.queryMenuGroupCatalogsCount(menuGroupCatalogDto);

        List<MenuGroupCatalogDto> menuGroupCatalogDtos = null;

        // 根据查询结果数量决定返回的数据
        if (count > 0) {
            // 查询具体的菜单组目录列表数据
            menuGroupCatalogDtos = menuGroupCatalogV1InnerServiceSMOImpl.queryMenuGroupCatalogs(menuGroupCatalogDto);
        } else {
            // 如果没有数据，返回空列表
            menuGroupCatalogDtos = new ArrayList<>();
        }

        // 构建返回结果对象，包含总页数、总记录数和数据列表
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, menuGroupCatalogDtos);

        // 设置响应实体，返回HTTP 200状态码
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}