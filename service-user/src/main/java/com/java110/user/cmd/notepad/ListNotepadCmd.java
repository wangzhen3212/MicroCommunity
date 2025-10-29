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
import com.java110.intf.user.INotepadV1InnerServiceSMO;
import com.java110.po.notepad.NotepadPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.notepad.NotepadDto;
import java.util.List;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询
 * 服务编码：notepad.listNotepad
 * 请求路劲：/app/notepad.ListNotepad
 * add by 吴学文 at 2022-08-16 00:02:21 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 便签列表查询命令类
 * 负责处理便签数据的查询请求，支持分页查询和按小区ID筛选
 * 服务编码：notepad.listNotepad
 * 请求路径：/app/notepad.ListNotepad
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-08-16
 */
@Java110Cmd(serviceCode = "notepad.listNotepad")
public class ListNotepadCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListNotepadCmd.class);
    
    /**
     * 便签服务接口，用于查询便签数据
     */
    @Autowired
    private INotepadV1InnerServiceSMO notepadV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性，包括分页参数和必填字段
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于处理请求和响应
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页参数（page和row）
        super.validatePageInfo(reqJson);
        // 验证必填参数：小区ID
        Assert.hasKeyAndValue(reqJson,"communityId","未包含小区ID");
    }

    /**
     * 命令执行方法
     * 处理便签列表查询业务逻辑，包括数据查询、分页处理和响应构建
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为NotepadDto对象
        NotepadDto notepadDto = BeanConvertUtil.covertBean(reqJson, NotepadDto.class);

        // 查询符合条件的便签记录总数
        int count = notepadV1InnerServiceSMOImpl.queryNotepadsCount(notepadDto);

        List<NotepadDto> notepadDtos = null;

        // 根据查询结果数量决定返回数据
        if (count > 0) {
            // 查询便签列表数据
            notepadDtos = notepadV1InnerServiceSMOImpl.queryNotepads(notepadDto);
        } else {
            // 无数据时返回空列表
            notepadDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 构建返回结果对象
        ResultVo resultVo = new ResultVo(totalPage, count, notepadDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应到数据流上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}