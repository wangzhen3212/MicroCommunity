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
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.user.IMenuGroupCatalogV1InnerServiceSMO;
import com.java110.po.menu.MenuGroupCatalogPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 类表述：删除菜单组目录
 * 服务编码：menuGroupCatalog.deleteMenuGroupCatalog
 * 请求路径：/app/menuGroupCatalog.DeleteMenuGroupCatalog
 * add by 吴学文 at 2022-02-26 10:18:54 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "menuGroupCatalog.deleteMenuGroupCatalog")
public class DeleteMenuGroupCatalogCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteMenuGroupCatalogCmd.class);

    /**
     * 菜单组目录服务接口
     */
    @Autowired
    private IMenuGroupCatalogV1InnerServiceSMO menuGroupCatalogV1InnerServiceSMOImpl;

    /**
     * 参数校验方法
     * 
     * @param event 命令事件
     * @param cmdDataFlowContext 数据流上下文
     * @param reqJson 请求参数JSON对象
     * 
     * 功能说明：
     * 1. 校验必填参数是否存在
     * 2. 确保删除操作的必要参数完整
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 校验目录ID参数
        Assert.hasKeyAndValue(reqJson, "gcId", "gcId不能为空");
        // 校验店铺类型参数
        Assert.hasKeyAndValue(reqJson, "storeType", "storeType不能为空");
    }

    /**
     * 执行删除菜单组目录命令
     * 
     * @param event 命令事件
     * @param cmdDataFlowContext 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     * 
     * 功能说明：
     * 1. 将请求参数转换为菜单组目录PO对象
     * 2. 调用服务层执行删除操作
     * 3. 处理删除结果并返回响应
     * 4. 使用事务注解确保数据一致性
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为菜单组目录PO对象
        MenuGroupCatalogPo menuGroupCatalogPo = BeanConvertUtil.covertBean(reqJson, MenuGroupCatalogPo.class);
        
        // 调用服务层执行删除操作，返回影响的行数
        int flag = menuGroupCatalogV1InnerServiceSMOImpl.deleteMenuGroupCatalog(menuGroupCatalogPo);

        // 检查删除操作是否成功
        if (flag < 1) {
            // 删除失败时抛出异常
            throw new CmdException("删除数据失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}