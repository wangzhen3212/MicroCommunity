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
package com.java110.user.cmd.notepad;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.notepad.NotepadDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.user.INotepadDetailV1InnerServiceSMO;
import com.java110.intf.user.INotepadV1InnerServiceSMO;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.po.notepad.NotepadPo;
import com.java110.po.notepad.NotepadDetailPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 类表述：保存
 * 服务编码：notepadDetail.saveNotepadDetail
 * 请求路劲：/app/notepadDetail.SaveNotepadDetail
 * add by 吴学文 at 2022-08-16 00:08:00 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 便签详情保存命令类
 * 
 * 该类负责处理便签详情的保存请求，包括参数验证、用户信息获取、便签详情保存和便签状态更新等功能
 * 实现了Cmd接口，遵循Java110框架的命令模式规范
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-08-16
 */
@Java110Cmd(serviceCode = "notepad.saveNotepadDetail")
public class SaveNotepadDetailCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveNotepadDetailCmd.class);

    /**
     * 便签详情ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 便签详情服务接口
     */
    @Autowired
    private INotepadDetailV1InnerServiceSMO notepadDetailV1InnerServiceSMOImpl;

    /**
     * 用户服务接口
     */
    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;

    /**
     * 便签服务接口
     */
    @Autowired
    private INotepadV1InnerServiceSMO notepadV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段，确保请求数据的完整性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求报文中必须包含noteId字段
        Assert.hasKeyAndValue(reqJson, "noteId", "请求报文中未包含noteId");
        // 验证请求报文中必须包含content字段
        Assert.hasKeyAndValue(reqJson, "content", "请求报文中未包含content");
    }

    /**
     * 执行命令方法
     * 
     * 处理便签详情的保存逻辑，包括获取用户信息、保存便签详情、更新便签状态等操作
     * 使用@Java110Transactional注解确保事务一致性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当保存或更新数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 从请求头中获取用户ID
        String userId = cmdDataFlowContext.getReqHeaders().get("user-id");
        
        // 构建用户查询条件
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        userDto.setPage(1);
        userDto.setRow(1);
        
        // 查询用户信息
        List<UserDto> userDtos = userInnerServiceSMOImpl.getUsers(userDto);
        // 验证用户存在且唯一
        Assert.listOnlyOne(userDtos, "用户不存在");

        // 将用户信息添加到请求参数中
        reqJson.put("createUserId", userDtos.get(0).getUserId());
        reqJson.put("createUserName", userDtos.get(0).getName());

        // 将请求参数转换为便签详情持久化对象
        NotepadDetailPo notepadDetailPo = BeanConvertUtil.covertBean(reqJson, NotepadDetailPo.class);
        // 生成便签详情ID
        notepadDetailPo.setDetailId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 保存便签详情数据
        int flag = notepadDetailV1InnerServiceSMOImpl.saveNotepadDetail(notepadDetailPo);

        // 检查保存操作是否成功
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 如果便签状态不是已完成状态，则直接返回成功响应
        if(!NotepadDto.STATE_FINISH.equals(reqJson.getString("state"))){
            return;
        }

        // 构建便签更新对象
        NotepadPo notepadPo = new NotepadPo();
        notepadPo.setNoteId(reqJson.getString("noteId"));
        notepadPo.setState(reqJson.getString("state"));
        
        // 更新便签状态
        flag = notepadV1InnerServiceSMOImpl.updateNotepad(notepadPo);

        // 检查更新操作是否成功
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}