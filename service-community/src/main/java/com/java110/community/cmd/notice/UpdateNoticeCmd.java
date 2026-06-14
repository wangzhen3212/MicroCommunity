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
package com.java110.community.cmd.notice;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.community.INoticeV1InnerServiceSMO;
import com.java110.po.notice.NoticePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：更新
 * 服务编码：notice.updateNotice
 * 请求路劲：/app/notice.UpdateNotice
 * add by 吴学文 at 2022-07-22 19:20:18 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 公告更新命令类
 * 
 * 该类负责处理公告信息的更新操作，包括参数验证和业务逻辑处理
 * 通过注解@Java110Cmd标识服务编码为notice.updateNotice
 * 继承自Cmd基类，实现命令模式的处理流程
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-07-22
 */
@Java110Cmd(serviceCode = "notice.updateNotice")
public class UpdateNoticeCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(UpdateNoticeCmd.class);

    /**
     * 公告服务接口
     * 用于调用公告相关的业务操作
     */
    @Autowired
    private INoticeV1InnerServiceSMO noticeV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整和有效，确保必要的字段都存在
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws IllegalArgumentException 当必要参数缺失时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证公告ID不能为空
        Assert.hasKeyAndValue(reqJson, "noticeId", "公告ID不能为空");
        // 验证标题不能为空
        Assert.hasKeyAndValue(reqJson, "title", "必填，请填写标题");
        // 验证公告类型不能为空
        Assert.hasKeyAndValue(reqJson, "noticeTypeCd", "必填，请选择公告类型");
        // 验证公告内容不能为空
        Assert.hasKeyAndValue(reqJson, "context", "必填，请填写公告内容");
    }

    /**
     * 命令执行方法
     * 
     * 处理公告更新的核心业务逻辑，包括数据转换、服务调用和结果处理
     * 使用@Java110Transactional注解确保事务性操作
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON对象转换为公告POJO对象
        NoticePo noticePo = BeanConvertUtil.covertBean(reqJson, NoticePo.class);
        
        // 调用公告服务接口执行更新操作
        int flag = noticeV1InnerServiceSMOImpl.updateNotice(noticePo);

        // 检查更新操作是否成功，flag小于1表示更新失败
        if (flag < 1) {
            // 抛出命令异常，提示更新数据失败
            throw new CmdException("更新数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}