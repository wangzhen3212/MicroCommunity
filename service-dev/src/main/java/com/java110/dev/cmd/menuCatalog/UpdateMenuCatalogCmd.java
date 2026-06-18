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
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.user.IMenuCatalogV1InnerServiceSMO;
import com.java110.po.menu.MenuCatalogPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * 类表述：更新菜单目录信息
 * 服务编码：menuCatalog.updateMenuCatalog
 * 请求路径：/app/menuCatalog.UpdateMenuCatalog
 * 
 * 功能描述：
 * 该命令类用于处理菜单目录信息的更新操作，包括参数验证和业务逻辑处理
 * 
 * 作者：吴学文 
 * 创建时间：2022-02-26 10:12:36
 * 联系方式：928255095@qq.com
 * 开源地址：https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 
 * 修改记录：
 * 如需修改此文件，请保留原有作者及注释信息，并在下方补充修改原因和联系方式
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "menuCatalog.updateMenuCatalog")
public class UpdateMenuCatalogCmd extends Cmd {

    // 日志记录器
    private static Logger logger = LoggerFactory.getLogger(UpdateMenuCatalogCmd.class);

    // 注入菜单目录服务接口
    @Autowired
    private IMenuCatalogV1InnerServiceSMO menuCatalogV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 功能说明：
     * 验证请求参数是否完整和有效，确保必要的参数都存在
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * 
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证caId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "caId", "caId不能为空");
        // 验证storeType参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "storeType", "storeType不能为空");
    }

    /**
     * 命令执行方法
     * 
     * 功能说明：
     * 执行菜单目录信息的更新操作，包括数据转换、服务调用和结果处理
     * 使用@Java110Transactional注解确保事务性操作
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * 
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON对象转换为菜单目录POJO对象
        MenuCatalogPo menuCatalogPo = BeanConvertUtil.covertBean(reqJson, MenuCatalogPo.class);
        
        // 调用服务接口更新菜单目录信息
        int flag = menuCatalogV1InnerServiceSMOImpl.updateMenuCatalog(menuCatalogPo);

        // 检查更新结果，如果影响行数小于1则认为更新失败
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}