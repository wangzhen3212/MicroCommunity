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
 * 类表述：删除菜单目录
 * 服务编码：menuCatalog.deleteMenuCatalog
 * 请求路径：/app/menuCatalog.DeleteMenuCatalog
 * 
 * 功能说明：该命令类用于处理删除菜单目录的业务逻辑，包括参数验证和数据库删除操作
 * 
 * 主要流程：
 * 1. 验证请求参数完整性
 * 2. 将JSON参数转换为PO对象
 * 3. 调用服务层执行删除操作
 * 4. 处理删除结果并返回响应
 * 
 * 注意事项：
 * - 删除操作需要验证caId和storeType参数
 * - 采用事务注解保证数据一致性
 * - 删除失败时抛出CmdException异常
 * 
 * @author 吴学文 at 2022-02-26 10:12:36 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "menuCatalog.deleteMenuCatalog")
public class DeleteMenuCatalogCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteMenuCatalogCmd.class);

    /**
     * 菜单目录服务接口，用于执行数据库操作
     */
    @Autowired
    private IMenuCatalogV1InnerServiceSMO menuCatalogV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 功能：验证请求参数是否完整和有效
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * 
     * 验证规则：
     * - caId不能为空：菜单目录ID是删除操作的必要标识
     * - storeType不能为空：店铺类型用于区分不同的业务场景
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证菜单目录ID参数
        Assert.hasKeyAndValue(reqJson, "caId", "caId不能为空");
        // 验证店铺类型参数
        Assert.hasKeyAndValue(reqJson, "storeType", "storeType不能为空");
    }

    /**
     * 命令执行方法
     * 
     * 功能：执行菜单目录的删除操作
     * 
     * 执行流程：
     * 1. 将请求JSON转换为菜单目录PO对象
     * 2. 调用服务层执行删除操作
     * 3. 检查删除结果，失败时抛出异常
     * 4. 设置成功响应
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当删除操作失败时抛出
     * 
     * 事务说明：
     * - 使用@Java110Transactional注解保证操作的事务性
     * - 删除失败时自动回滚
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为菜单目录PO对象
        MenuCatalogPo menuCatalogPo = BeanConvertUtil.covertBean(reqJson, MenuCatalogPo.class);
        
        // 调用服务层执行删除操作，返回影响的行数
        int flag = menuCatalogV1InnerServiceSMOImpl.deleteMenuCatalog(menuCatalogPo);

        // 检查删除结果，如果影响行数小于1说明删除失败
        if (flag < 1) {
            // 记录错误日志
            logger.error("删除菜单目录失败，caId: {}, storeType: {}", 
                        menuCatalogPo.getCaId(), menuCatalogPo.getStoreType());
            // 抛出删除失败异常
            throw new CmdException("删除数据失败");
        }

        // 记录成功日志
        logger.info("删除菜单目录成功，caId: {}, storeType: {}", 
                   menuCatalogPo.getCaId(), menuCatalogPo.getStoreType());
        
        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}