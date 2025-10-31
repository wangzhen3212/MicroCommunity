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
package com.java110.dev.cmd.menuCatalog;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.user.IMenuCatalogV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.menu.MenuCatalogDto;
import java.util.List;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询菜单目录列表
 * 服务编码：menuCatalog.listMenuCatalog
 * 请求路径：/app/menuCatalog.ListMenuCatalog
 * add by 吴学文 at 2022-02-26 10:12:36 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "menuCatalog.listMenuCatalog")
public class ListMenuCatalogCmd extends Cmd {

    // 日志记录器
    private static Logger logger = LoggerFactory.getLogger(ListMenuCatalogCmd.class);
    
    // 菜单目录服务接口
    @Autowired
    private IMenuCatalogV1InnerServiceSMO menuCatalogV1InnerServiceSMOImpl;

    /**
     * 参数校验方法
     * 主要验证分页参数
     * @param event 命令事件
     * @param cmdDataFlowContext 数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的分页信息验证方法
        super.validatePageInfo(reqJson);
    }

    /**
     * 执行查询菜单目录列表命令
     * 1. 将请求参数转换为MenuCatalogDto对象
     * 2. 查询菜单目录总数
     * 3. 根据分页信息查询菜单目录列表
     * 4. 构建返回结果
     * @param event 命令事件
     * @param cmdDataFlowContext 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON参数转换为MenuCatalogDto对象
        MenuCatalogDto menuCatalogDto = BeanConvertUtil.covertBean(reqJson, MenuCatalogDto.class);

        // 查询菜单目录总数
        int count = menuCatalogV1InnerServiceSMOImpl.queryMenuCatalogsCount(menuCatalogDto);

        List<MenuCatalogDto> menuCatalogDtos = null;

        // 根据查询结果处理数据
        if (count > 0) {
            // 如果存在数据，则查询菜单目录列表
            menuCatalogDtos = menuCatalogV1InnerServiceSMOImpl.queryMenuCatalogs(menuCatalogDto);
        } else {
            // 如果没有数据，返回空列表
            menuCatalogDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数/每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        
        // 构建返回结果对象
        ResultVo resultVo = new ResultVo(totalPage, count, menuCatalogDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应结果到数据流上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}