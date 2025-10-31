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
import com.java110.core.factory.GenerateCodeFactory;
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
 * 类表述：保存菜单目录信息
 * 服务编码：menuCatalog.saveMenuCatalog
 * 请求路径：/app/menuCatalog.SaveMenuCatalog
 * add by 吴学文 at 2022-02-26 10:12:36 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "menuCatalog.saveMenuCatalog")
public class SaveMenuCatalogCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveMenuCatalogCmd.class);

    /**
     * ID生成前缀，用于生成菜单目录的唯一标识
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 菜单目录服务接口，用于操作菜单目录数据
     */
    @Autowired
    private IMenuCatalogV1InnerServiceSMO menuCatalogV1InnerServiceSMOImpl;

    /**
     * 参数校验方法
     * 验证请求参数是否完整和有效
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填参数是否存在
        Assert.hasKeyAndValue(reqJson, "name", "请求报文中未包含name");
        Assert.hasKeyAndValue(reqJson, "icon", "请求报文中未包含icon");
        Assert.hasKeyAndValue(reqJson, "seq", "请求报文中未包含seq");
        Assert.hasKeyAndValue(reqJson, "url", "请求报文中未包含url");
        Assert.hasKeyAndValue(reqJson, "isShow", "请求报文中未包含isShow");
        
        // 可以在这里添加更多的业务逻辑验证
        // 例如：验证seq是否为数字，url格式是否正确等
    }

    /**
     * 执行保存菜单目录命令
     * 将请求数据转换为菜单目录对象并保存到数据库
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 记录开始保存的日志
        logger.debug("开始保存菜单目录信息，请求参数：{}", reqJson.toJSONString());

        // 将JSON数据转换为菜单目录POJO对象
        MenuCatalogPo menuCatalogPo = BeanConvertUtil.covertBean(reqJson, MenuCatalogPo.class);
        
        // 生成菜单目录的唯一ID
        menuCatalogPo.setCaId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 调用服务保存菜单目录信息
        int flag = menuCatalogV1InnerServiceSMOImpl.saveMenuCatalog(menuCatalogPo);

        // 检查保存结果
        if (flag < 1) {
            logger.error("保存菜单目录数据失败，受影响行数：{}", flag);
            throw new CmdException("保存数据失败");
        }

        // 记录保存成功的日志
        logger.info("菜单目录保存成功，生成的ID：{}", menuCatalogPo.getCaId());
        
        // 设置响应结果
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}