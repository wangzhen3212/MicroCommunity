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
package com.java110.fee.cmd.oweFeeCallable;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.data.DatabusDataDto;
import com.java110.dto.oweFeeCallable.OweFeeCallableDto;
import com.java110.dto.wechat.SmallWeChatDto;
import com.java110.intf.fee.IOweFeeCallableV1InnerServiceSMO;
import com.java110.intf.job.IDataBusInnerServiceSMO;
import com.java110.intf.store.ISmallWechatV1InnerServiceSMO;
import com.java110.po.oweFeeCallable.OweFeeCallablePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：保存欠费催缴信息命令类
 * 服务编码：oweFeeCallable.saveOweFeeCallable
 * 请求路劲：/app/oweFeeCallable.SaveOweFeeCallable
 * 功能描述：该命令类用于处理欠费催缴信息的保存操作，包括参数验证和业务逻辑处理
 * add by 吴学文 at 2023-08-17 15:05:16 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "oweFeeCallable.saveOweFeeCallable")
public class SaveOweFeeCallableCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveOweFeeCallableCmd.class);

    /**
     * ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 数据总线服务接口
     */
    @Autowired
    private IDataBusInnerServiceSMO dataBusInnerServiceSMOImpl;

    /**
     * 微信小程序服务接口
     */
    @Autowired
    private ISmallWechatV1InnerServiceSMO smallWechatV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 功能描述：验证请求参数的有效性和完整性，确保业务逻辑执行前数据的正确性
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填字段是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        Assert.hasKeyAndValue(reqJson, "callableWay", "请求报文中未包含callableWay");

        // 验证房屋信息是否存在
        if (!reqJson.containsKey("roomIds")) {
            throw new CmdException("未包含房屋信息");
        }

        JSONArray roomIds = reqJson.getJSONArray("roomIds");

        // 验证房屋信息是否为空
        if (ListUtil.isNull(roomIds)) {
            throw new CmdException("未包含房屋信息");
        }

        // 验证费用信息是否存在（三种可能的费用标识字段）
        if (!reqJson.containsKey("feeId") && !reqJson.containsKey("feeIds") && !reqJson.containsKey("configIds")) {
            throw new CmdException("未包含需要催缴的费用");
        }

        // 公众号催缴方式校验
        if (OweFeeCallableDto.CALLABLE_WAY_WECHAT.equals(reqJson.getString("callableWay"))) {
            SmallWeChatDto smallWeChatDto = new SmallWeChatDto();
            smallWeChatDto.setObjId(reqJson.getString("communityId"));
            smallWeChatDto.setWeChatType(SmallWeChatDto.WECHAT_TYPE_PUBLIC);
            smallWeChatDto.setWechatType(smallWeChatDto.WECHAT_TYPE_PUBLIC);
            // 查询社区是否配置了公众号
            int count = smallWechatV1InnerServiceSMOImpl.querySmallWechatsCount(smallWeChatDto);
            if (count < 1) {
                throw new CmdException("未配置公众号");
            }
        }

        // 短信催缴方式暂不进行校验
        // todo 短信暂不校验
    }

    /**
     * 执行命令方法
     * 功能描述：处理欠费催缴信息的保存业务逻辑，通过数据总线发送催缴数据
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当业务处理失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 从请求头中获取用户ID
        String userId = cmdDataFlowContext.getReqHeaders().get("user-id");

        // 将用户ID设置为操作人员ID
        reqJson.put("staffId", userId);
        
        // 通过数据总线发送欠费催缴数据
        dataBusInnerServiceSMOImpl.databusData(new DatabusDataDto(DatabusDataDto.BUSINESS_TYPE_OWE_FEE_CALLABLE, reqJson));

        // 注释掉的原有保存逻辑，现改为通过数据总线处理
//        OweFeeCallablePo oweFeeCallablePo = BeanConvertUtil.covertBean(reqJson, OweFeeCallablePo.class);
//        oweFeeCallablePo.setOfcId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
//        int flag = oweFeeCallableV1InnerServiceSMOImpl.saveOweFeeCallable(oweFeeCallablePo);
//
//        if (flag < 1) {
//            throw new CmdException("保存数据失败");
//        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}