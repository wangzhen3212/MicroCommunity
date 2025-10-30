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
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.wechat.WechatMenuDto;
import com.java110.intf.store.IWechatMenuInnerServiceSMO;
import com.java110.intf.store.IWechatMenuV1InnerServiceSMO;
import com.java110.po.wechat.WechatMenuPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 类表述：保存微信菜单命令类
 * 服务编码：wechatMenu.saveWechatMenu
 * 请求路径：/app/wechatMenu.SaveWechatMenu
 * 功能描述：处理微信菜单的保存操作，包括参数验证和业务逻辑处理
 * add by 吴学文 at 2022-07-29 11:48:22 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "smallWeChat.saveWechatMenu")
public class SaveWechatMenuCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveWechatMenuCmd.class);

    /**
     * 微信菜单ID前缀，用于生成唯一标识
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 微信菜单V1版本内部服务接口
     */
    @Autowired
    private IWechatMenuV1InnerServiceSMO wechatMenuV1InnerServiceSMOImpl;

    /**
     * 微信菜单内部服务接口
     */
    @Autowired
    private IWechatMenuInnerServiceSMO wechatMenuInnerServiceSMOImpl;

    /**
     * 验证请求参数的有效性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws IllegalArgumentException 当参数验证失败时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填参数是否存在
        Assert.hasKeyAndValue(reqJson, "menuName", "请求报文中未包含menuName");
        Assert.hasKeyAndValue(reqJson, "menuType", "请求报文中未包含menuType");
        Assert.hasKeyAndValue(reqJson, "menuLevel", "请求报文中未包含menuLevel");
        //Assert.hasKeyAndValue(reqJson, "menuValue", "请求报文中未包含menuValue");
        Assert.hasKeyAndValue(reqJson, "seq", "请求报文中未包含顺序");
        Assert.isInteger(reqJson.getString("seq"),"顺序不是有效数字");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        
        String menuName = reqJson.getString("menuName");

        // 根据菜单级别进行不同的验证逻辑
        if (WechatMenuDto.MENU_LEVEL_ONE.equals(reqJson.getString("menuLevel"))) {
            // 验证一级菜单名称长度（1-4个字符）
            if (menuName.length() > 4 && menuName.length() < 1) {
                throw new IllegalArgumentException("一级菜单必须1至4位");
            }
            
            // 查询当前社区下的一级菜单数量，确保不超过3个
            WechatMenuDto wechatMenuDto = new WechatMenuDto();
            wechatMenuDto.setCommunityId(reqJson.getString("communityId"));
            wechatMenuDto.setMenuLevel(reqJson.getString("menuLevel"));
            List<WechatMenuDto> wechatMenuDtos = wechatMenuInnerServiceSMOImpl.queryWechatMenus(wechatMenuDto);
            if(wechatMenuDtos.size() > 2){
                throw new IllegalArgumentException("一级菜单最多3个");
            }
        } else {
            // 二级菜单需要验证父菜单ID和名称长度
            Assert.hasKeyAndValue(reqJson, "parentMenuId", "未包含一级菜单信息");
            if (menuName.length() > 7 && menuName.length() < 1) {
                throw new IllegalArgumentException("二级菜单必须1至7位");
            }

            // 查询当前父菜单下的二级菜单数量，确保不超过5个
            WechatMenuDto wechatMenuDto = new WechatMenuDto();
            wechatMenuDto.setCommunityId(reqJson.getString("communityId"));
            wechatMenuDto.setMenuLevel(reqJson.getString("menuLevel"));
            wechatMenuDto.setParentMenuId(reqJson.getString("parentMenuId"));
            List<WechatMenuDto> wechatMenuDtos = wechatMenuInnerServiceSMOImpl.queryWechatMenus(wechatMenuDto);
            if(wechatMenuDtos.size() > 4){
                throw new IllegalArgumentException("二级菜单最多5个");
            }
        }
    }

    /**
     * 执行保存微信菜单的业务逻辑
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当保存数据失败时抛出
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为微信菜单PO对象
        WechatMenuPo wechatMenuPo = BeanConvertUtil.covertBean(reqJson, WechatMenuPo.class);
        // 生成微信菜单唯一ID
        wechatMenuPo.setWechatMenuId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        // 调用服务保存微信菜单
        int flag = wechatMenuV1InnerServiceSMOImpl.saveWechatMenu(wechatMenuPo);

        // 检查保存结果
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}