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
import com.java110.dto.notepad.NotepadDto;
import com.java110.dto.notepad.NotepadDetailDto;
import com.java110.intf.user.INotepadDetailV1InnerServiceSMO;
import com.java110.intf.user.INotepadV1InnerServiceSMO;
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
 * 类表述：删除记事本详情记录的命令类
 * 服务编码：notepadDetail.deleteNotepadDetail
 * 请求路劲：/app/notepadDetail.DeleteNotepadDetail
 * 功能描述：该类负责处理删除记事本详情记录的请求，包括参数验证、删除详情记录和更新记事本状态等操作
 * add by 吴学文 at 2022-08-16 00:08:00 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "notepad.deleteNotepadDetail")
public class DeleteNotepadDetailCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteNotepadDetailCmd.class);

    /**
     * 记事本详情服务接口
     */
    @Autowired
    private INotepadDetailV1InnerServiceSMO notepadDetailV1InnerServiceSMOImpl;

    /**
     * 记事本服务接口
     */
    @Autowired
    private INotepadV1InnerServiceSMO notepadV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 功能描述：验证请求参数是否包含必要的字段
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含detailId字段
        Assert.hasKeyAndValue(reqJson, "detailId", "detailId不能为空");
    }

    /**
     * 执行删除记事本详情记录的命令
     * 功能描述：删除指定的记事本详情记录，并更新对应的记事本状态为进行中
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除或更新操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 创建记事本详情DTO对象，用于查询要删除的记录
        NotepadDetailDto notepadDetailDto = new NotepadDetailDto();
        notepadDetailDto.setDetailId(reqJson.getString("detailId"));

        // 根据detailId查询记事本详情记录
        List<NotepadDetailDto> notepadDetailDtos = notepadDetailV1InnerServiceSMOImpl.queryNotepadDetails(notepadDetailDto);

        // 验证查询结果有且只有一条记录
        Assert.listOnlyOne(notepadDetailDtos, "跟进记录不存在");

        // 将请求参数转换为记事本详情PO对象，用于删除操作
        NotepadDetailPo notepadDetailPo = BeanConvertUtil.covertBean(reqJson, NotepadDetailPo.class);
        // 执行删除操作
        int flag = notepadDetailV1InnerServiceSMOImpl.deleteNotepadDetail(notepadDetailPo);

        // 检查删除操作是否成功
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 创建记事本PO对象，用于更新记事本状态
        NotepadPo notepadPo = new NotepadPo();
        // 从查询结果中获取对应的记事本ID
        notepadPo.setNoteId(reqJson.getString(notepadDetailDtos.get(0).getNoteId()));
        // 设置记事本状态为进行中
        notepadPo.setState(NotepadDto.STATE_DOING);
        // 更新记事本状态
        flag = notepadV1InnerServiceSMOImpl.updateNotepad(notepadPo);

        // 检查更新操作是否成功
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}