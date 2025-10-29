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
package com.java110.user.cmd.wechat;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.data.DatabusDataDto;
import com.java110.dto.wechat.SmallWeChatDto;
import com.java110.intf.job.IDataBusInnerServiceSMO;
import com.java110.intf.store.ISmallWeChatInnerServiceSMO;
import com.java110.intf.user.IWechatSubscribeV1InnerServiceSMO;
import com.java110.po.wechat.WechatSubscribePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 类表述：保存
 * 服务编码：wechatSubscribe.saveWechatSubscribe
 * 请求路劲：/app/wechatSubscribe.SaveWechatSubscribe
 * add by 吴学文 at 2022-02-22 16:17:23 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 微信关注用户同步命令类
 * 
 * 该类负责处理微信关注用户的同步操作，通过数据总线服务将同步请求转发给相应的业务处理器
 * 主要用于从微信公众号平台同步关注用户信息到本地系统
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-02-22
 */
@Java110Cmd(serviceCode = "wechat.synchronizeWechatSubscribe")
public class SynchronizeWechatSubscribeCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SynchronizeWechatSubscribeCmd.class);

    /**
     * 代码前缀常量，用于生成唯一标识
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 数据总线服务接口，用于处理数据同步任务
     */
    @Autowired
    private IDataBusInnerServiceSMO dataBusInnerServiceSMOImpl;

    /**
     * 微信小程序/公众号信息服务接口，用于查询微信配置信息
     */
    @Autowired
    private ISmallWeChatInnerServiceSMO smallWeChatInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，主要检查小区编号是否存在以及是否配置了对应的公众号信息
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param cmdDataFlowContext 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的属性验证方法
        super.validateProperty(cmdDataFlowContext);
        // 验证请求参数中必须包含小区编号
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区编号");

        // 构建微信配置查询条件
        SmallWeChatDto smallWeChatDto = new SmallWeChatDto();
        smallWeChatDto.setObjId(reqJson.getString("communityId")); // 设置小区ID作为查询条件
        smallWeChatDto.setWeChatType(SmallWeChatDto.WECHAT_TYPE_PUBLIC); // 设置微信类型为公众号
        
        // 查询该小区下的公众号配置信息
        List<SmallWeChatDto> smallWeChatDtos = smallWeChatInnerServiceSMOImpl.querySmallWeChats(smallWeChatDto);
        
        // 验证是否配置了公众号，如果没有配置则抛出异常
        if (ListUtil.isNull(smallWeChatDtos)) {
            throw new CmdException("未配置公众号");
        }
    }

    /**
     * 执行微信关注用户同步命令
     * 
     * 该方法通过数据总线服务将同步请求转发给相应的业务处理器，实现微信关注用户信息的同步
     * 使用事务注解确保操作的原子性
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param cmdDataFlowContext 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当同步操作执行失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 创建数据总线数据传输对象，设置业务类型为同步微信关注用户
        DatabusDataDto databusDataDto = new DatabusDataDto(DatabusDataDto.BUSINESS_TYPE_SYNCHRONIZE_WECHAT_SUBSCRIBE, reqJson);
        
        // 调用数据总线服务处理微信关注用户同步任务
        dataBusInnerServiceSMOImpl.databusData(databusDataDto);

        // 设置响应结果，返回操作成功信息
        cmdDataFlowContext.setResponseEntity(ResultVo.createResponseEntity(ResultVo.CODE_OK, "已提交获取关注用户信息"));
    }
}