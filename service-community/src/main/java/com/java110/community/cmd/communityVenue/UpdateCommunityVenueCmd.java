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
package com.java110.community.cmd.communityVenue;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.community.ICommunityVenueV1InnerServiceSMO;
import com.java110.po.communitySpace.CommunityVenuePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：更新
 * 服务编码：communityVenue.updateCommunityVenue
 * 请求路劲：/app/communityVenue.UpdateCommunityVenue
 * add by 吴学文 at 2022-10-03 03:05:16 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 社区场馆更新命令类
 * 
 * 该类负责处理社区场馆信息的更新操作，通过接收前端请求数据，
 * 验证数据有效性后调用服务层完成场馆信息的更新
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-10-03
 */
@Java110Cmd(serviceCode = "communityVenue.updateCommunityVenue")
public class UpdateCommunityVenueCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(UpdateCommunityVenueCmd.class);

    /**
     * 社区场馆服务接口
     * 用于调用场馆相关的业务逻辑操作
     */
    @Autowired
    private ICommunityVenueV1InnerServiceSMO communityVenueV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的场馆ID和社区ID字段
     * 如果缺少必要参数，会抛出参数验证异常
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含venueId字段
        Assert.hasKeyAndValue(reqJson, "venueId", "venueId不能为空");
        // 验证请求参数中必须包含communityId字段
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行命令方法
     * 
     * 将请求参数转换为场馆实体对象，调用服务层更新场馆信息，
     * 并根据更新结果设置响应数据
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当更新数据失败时抛出命令异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为社区场馆实体对象
        CommunityVenuePo communityVenuePo = BeanConvertUtil.covertBean(reqJson, CommunityVenuePo.class);
        
        // 调用服务层接口更新场馆信息，返回受影响的行数
        int flag = communityVenueV1InnerServiceSMOImpl.updateCommunityVenue(communityVenuePo);

        // 检查更新操作是否成功，如果受影响行数小于1表示更新失败
        if (flag < 1) {
            // 抛出命令异常，提示更新数据失败
            throw new CmdException("更新数据失败");
        }

        // 设置响应结果为成功状态
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}