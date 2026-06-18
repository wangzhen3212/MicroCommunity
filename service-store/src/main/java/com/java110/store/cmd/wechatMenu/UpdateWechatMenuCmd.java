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
package com.java110.store.cmd.wechatMenu;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.wechat.WechatMenuDto;
import com.java110.intf.store.IWechatMenuV1InnerServiceSMO;
import com.java110.po.wechat.WechatMenuPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：更新
 * 服务编码：wechatMenu.updateWechatMenu
 * 请求路劲：/app/wechatMenu.UpdateWechatMenu
 * add by 吴学文 at 2022-07-29 11:48:22 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 微信菜单更新命令类
 * 
 * 该类负责处理微信菜单的更新操作，包括参数验证和业务逻辑执行
 * 通过注解 @Java110Cmd 标识为微信菜单更新服务
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-07-29
 */
@Java110Cmd(serviceCode = "smallWeChat.updateWechatMenu")
public class UpdateWechatMenuCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(UpdateWechatMenuCmd.class);

    /**
     * 微信菜单服务接口
     */
    @Autowired
    private IWechatMenuV1InnerServiceSMO wechatMenuV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性和完整性，包括必填字段检查、数据类型验证和业务规则验证
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws IllegalArgumentException 当参数验证失败时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填字段是否存在
        Assert.hasKeyAndValue(reqJson, "wechatMenuId", "wechatMenuId不能为空");
        Assert.hasKeyAndValue(reqJson, "menuName", "请求报文中未包含menuName");
        Assert.hasKeyAndValue(reqJson, "menuType", "请求报文中未包含menuType");
        Assert.hasKeyAndValue(reqJson, "menuLevel", "请求报文中未包含menuLevel");
        Assert.hasKeyAndValue(reqJson, "seq", "请求报文中未包含顺序");
        Assert.isInteger(reqJson.getString("seq"),"顺序不是有效数字");
        //Assert.hasKeyAndValue(reqJson, "menuValue", "请求报文中未包含menuValue");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");

        // 获取菜单名称
        String menuName = reqJson.getString("menuName");

        // 根据菜单级别进行不同的验证
        if (WechatMenuDto.MENU_LEVEL_ONE.equals(reqJson.getString("menuLevel"))) {
            // 一级菜单名称长度验证：必须1-4个字符
            if (menuName.length() > 4 && menuName.length() < 1) {
                throw new IllegalArgumentException("一级菜单必须1至4位");
            }
        } else {
            // 二级菜单需要验证父菜单ID和名称长度
            Assert.hasKeyAndValue(reqJson, "parentMenuId", "未包含一级菜单信息");
            // 二级菜单名称长度验证：必须1-7个字符
            if (menuName.length() > 7 && menuName.length() < 1) {
                throw new IllegalArgumentException("二级菜单必须1至7位");
            }
        }
    }

    /**
     * 执行微信菜单更新命令
     * 
     * 将请求参数转换为微信菜单对象，调用服务层进行数据更新操作
     * 使用事务注解确保操作的原子性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当更新操作失败时抛出
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON对象转换为微信菜单POJO对象
        WechatMenuPo wechatMenuPo = BeanConvertUtil.covertBean(reqJson, WechatMenuPo.class);
        
        // 调用服务层更新微信菜单
        int flag = wechatMenuV1InnerServiceSMOImpl.updateWechatMenu(wechatMenuPo);

        // 检查更新结果
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}