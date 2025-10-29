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
package com.java110.user.cmd.menuUser;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.menu.MenuDto;
import com.java110.intf.community.IMenuInnerServiceSMO;
import com.java110.intf.user.IMenuUserV1InnerServiceSMO;
import com.java110.po.menu.MenuUserPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 菜单用户关系保存命令类
 * 
 * 该类负责处理菜单用户关系的保存操作，包括参数验证、数据转换和业务逻辑处理
 * 服务编码：menuUser.saveMenuUser
 * 请求路径：/app/menuUser.SaveMenuUser
 * 
 * @author 吴学文 at 2022-02-27 12:21:33 mail: 928255095@qq.com
 * @open source address: https://gitee.com/wuxw7/MicroCommunity
 * @官网：http://www.homecommunity.cn
 * @温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "menuUser.saveMenuUser")
public class SaveMenuUserCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveMenuUserCmd.class);

    /**
     * ID生成前缀，用于生成菜单用户关系ID
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 菜单用户关系服务接口
     */
    @Autowired
    private IMenuUserV1InnerServiceSMO menuUserV1InnerServiceSMOImpl;

    /**
     * 菜单服务接口
     */
    @Autowired
    private IMenuInnerServiceSMO menuInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整，确保必要的参数都存在
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含mId
        Assert.hasKeyAndValue(reqJson, "mId", "请求报文中未包含mId");
        // 验证请求参数中必须包含userId
        Assert.hasKeyAndValue(reqJson, "userId", "请求报文中未包含userId");
        // 验证请求参数中必须包含icon
        Assert.hasKeyAndValue(reqJson, "icon", "请求报文中未包含icon");
        // 验证请求参数中必须包含seq
        Assert.hasKeyAndValue(reqJson, "seq", "请求报文中未包含seq");
    }

    /**
     * 执行保存菜单用户关系命令
     * 
     * 该方法负责处理菜单用户关系的保存逻辑，包括数据转换、菜单验证和数据持久化
     * 使用事务注解确保操作的原子性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为菜单用户关系PO对象
        MenuUserPo menuUserPo = BeanConvertUtil.covertBean(reqJson, MenuUserPo.class);
        // 生成菜单用户关系ID
        menuUserPo.setMuId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));

        // 根据菜单ID查询菜单信息
        MenuDto menuDto = new MenuDto();
        menuDto.setmId(reqJson.getString("mId"));
        List<MenuDto> menuDtos = menuInnerServiceSMOImpl.queryMenus(menuDto);

        // 验证菜单存在且唯一
        Assert.listOnlyOne(menuDtos, "菜单不存在");

        // 设置菜单用户关系的相关属性
        menuUserPo.setmId(menuDtos.get(0).getmId());      // 设置菜单ID
        menuUserPo.setUrl(menuDtos.get(0).getUrl());      // 设置菜单URL
        menuUserPo.setName(menuDtos.get(0).getName());    // 设置菜单名称
        menuUserPo.setStaffId(reqJson.getString("userId")); // 设置用户ID

        // 保存菜单用户关系到数据库
        int flag = menuUserV1InnerServiceSMOImpl.saveMenuUser(menuUserPo);

        // 检查保存结果，失败则抛出异常
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}