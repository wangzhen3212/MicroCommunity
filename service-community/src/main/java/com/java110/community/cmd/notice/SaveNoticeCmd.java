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
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.notice.NoticeDto;
import com.java110.dto.privilege.RoleCommunityDto;
import com.java110.dto.store.StoreDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.community.ICommunityInnerServiceSMO;
import com.java110.intf.community.INoticeV1InnerServiceSMO;
import com.java110.intf.store.IStoreV1InnerServiceSMO;
import com.java110.intf.user.*;
import com.java110.po.notice.NoticePo;
import com.java110.utils.constant.StateConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import com.java110.vo.api.community.ApiCommunityDataVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * 类表述：保存公告命令类
 * 服务编码：notice.saveNotice
 * 请求路劲：/app/notice.SaveNotice
 * add by 吴学文 at 2022-07-22 19:20:18 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * 
 * 功能描述：处理公告保存相关的业务逻辑，包括参数验证和公告信息保存
 * 主要职责：
 * 1. 验证公告保存请求参数的完整性
 * 2. 执行业务逻辑保存公告信息
 * 3. 处理事务管理确保数据一致性
 */
@Java110Cmd(serviceCode = "notice.saveNotice")
public class SaveNoticeCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveNoticeCmd.class);

    /**
     * 公告ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 小区服务接口
     */
    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;

    /**
     * 公告服务接口
     */
    @Autowired
    private INoticeV1InnerServiceSMO noticeV1InnerServiceSMOImpl;

    /**
     * 角色小区关联服务接口
     */
    @Autowired
    private IRoleCommunityV1InnerServiceSMO roleCommunityV1InnerServiceSMOImpl;

    /**
     * 商户服务接口
     */
    @Autowired
    private IStoreV1InnerServiceSMO storeV1InnerServiceSMOImpl;

    /**
     * 用户服务接口
     */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数中必须包含公告标题、公告类型和公告内容
     *
     * @param event CMD事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置上下文信息
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证标题参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "title", "必填，请填写标题");
        // 验证公告类型参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "noticeTypeCd", "必填，请选择公告类型");
        // 验证公告内容参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "context", "必填，请填写公告内容");
    }

    /**
     * 执行公告保存命令
     * 处理公告信息的保存逻辑，包括数据转换、字段设置和数据库操作
     *
     * @param event CMD事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当保存失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 从上下文中获取当前用户ID
        String userId = CmdContextUtils.getUserId(cmdDataFlowContext);
        
        // 将请求JSON转换为公告PO对象
        NoticePo noticePo = BeanConvertUtil.covertBean(reqJson, NoticePo.class);
        
        // 设置公告相关属性
        noticePo.setUserId(userId); // 设置创建用户ID
        noticePo.setStartTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A)); // 设置开始时间为当前时间
        noticePo.setEndTime(DateUtil.LAST_TIME); // 设置结束时间为最大时间
        noticePo.setNoticeId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID)); // 生成公告ID
        noticePo.setObjType(NoticeDto.OBJ_TYPE_ALL); // 设置对象类型为全部
        noticePo.setObjId(reqJson.getString("communityId")); // 设置对象ID为小区ID
        noticePo.setState(NoticeDto.STATE_WAIT); // 设置公告状态为待发布
        
        // 调用服务保存公告信息
        int flag = noticeV1InnerServiceSMOImpl.saveNotice(noticePo);
        
        // 检查保存结果，失败则抛出异常
        if (flag < 1) {
            throw new CmdException("发布失败");
        }
        
        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}