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
package com.java110.acct.cmd.integral;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.acct.IIntegralGiftDetailV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.integral.IntegralGiftDetailDto;

import java.util.List;
import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询积分礼品明细列表的命令类
 * 服务编码：integralGiftDetail.listIntegralGiftDetail
 * 请求路劲：/app/integralGiftDetail.ListIntegralGiftDetail
 * add by 吴学文 at 2022-12-12 01:54:09 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "integral.listIntegralGiftDetail")
public class ListIntegralGiftDetailCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(ListIntegralGiftDetailCmd.class);
    
    /**
     * 积分礼品明细服务接口
     */
    @Autowired
    private IIntegralGiftDetailV1InnerServiceSMO integralGiftDetailV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性和完整性
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于处理数据流
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类方法验证分页信息
        super.validatePageInfo(reqJson);
        // 验证communityId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行命令方法
     * 处理积分礼品明细列表查询业务逻辑
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为积分礼品明细DTO对象
        IntegralGiftDetailDto integralGiftDetailDto = BeanConvertUtil.covertBean(reqJson, IntegralGiftDetailDto.class);

        // 查询积分礼品明细总记录数
        int count = integralGiftDetailV1InnerServiceSMOImpl.queryIntegralGiftDetailsCount(integralGiftDetailDto);

        List<IntegralGiftDetailDto> integralGiftDetailDtos = null;

        // 根据查询结果数量判断是否查询数据
        if (count > 0) {
            // 查询积分礼品明细列表数据
            integralGiftDetailDtos = integralGiftDetailV1InnerServiceSMOImpl.queryIntegralGiftDetails(integralGiftDetailDto);
        } else {
            // 如果没有数据，返回空列表
            integralGiftDetailDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 构建返回结果对象
        ResultVo resultVo = new ResultVo(totalPage, count, integralGiftDetailDtos);

        // 创建HTTP响应实体，状态码为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应实体到数据流上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}