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
package com.java110.fee.cmd.feeComboMember;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.fee.FeeComboMemberDto;
import com.java110.intf.fee.IFeeComboMemberV1InnerServiceSMO;
import com.java110.po.fee.FeeComboMemberPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 费用套餐成员保存命令类
 * 
 * 该类用于处理费用套餐成员的保存操作，包括参数验证和业务逻辑处理。
 * 服务编码：feeComboMember.saveFeeComboMember
 * 请求路径：/app/feeComboMember.SaveFeeComboMember
 * 
 * @author 吴学文 at 2022-05-07 15:37:34 mail: 928255095@qq.com
 * @open source address: https://gitee.com/wuxw7/MicroCommunity
 * @官网：http://www.homecommunity.cn
 * @温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "feeComboMember.saveFeeComboMember")
public class SaveFeeComboMemberCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveFeeComboMemberCmd.class);

    /**
     * ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 费用套餐成员服务接口
     */
    @Autowired
    private IFeeComboMemberV1InnerServiceSMO feeComboMemberV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段：comboId、configId、communityId
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求报文中是否包含comboId字段
        Assert.hasKeyAndValue(reqJson, "comboId", "请求报文中未包含comboId");
        // 验证请求报文中是否包含configId字段
        Assert.hasKeyAndValue(reqJson, "configId", "请求报文中未包含configId");
        // 验证请求报文中是否包含communityId字段
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
    }

    /**
     * 执行保存费用套餐成员命令
     * 
     * 该方法执行具体的业务逻辑，包括：
     * 1. 检查费用套餐成员是否已存在
     * 2. 生成成员ID
     * 3. 保存费用套餐成员信息
     * 4. 设置响应结果
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 创建费用套餐成员DTO对象用于查询
        FeeComboMemberDto feeComboMemberDto = new FeeComboMemberDto();
        feeComboMemberDto.setConfigId(reqJson.getString("configId"));
        feeComboMemberDto.setComboId(reqJson.getString("comboId"));
        feeComboMemberDto.setCommunityId(reqJson.getString("communityId"));
        
        // 查询是否已存在相同的费用套餐成员
        List<FeeComboMemberDto> feeComboMemberDtos = feeComboMemberV1InnerServiceSMOImpl.queryFeeComboMembers(feeComboMemberDto);
        
        // 如果已存在相同的费用套餐成员，则抛出异常
        if (feeComboMemberDtos != null && feeComboMemberDtos.size() > 0) {
            throw new IllegalArgumentException("该费用套餐下已添加过该费用项！");
        }
        
        // 将请求JSON对象转换为费用套餐成员PO对象
        FeeComboMemberPo feeComboMemberPo = BeanConvertUtil.covertBean(reqJson, FeeComboMemberPo.class);
        // 生成成员ID
        feeComboMemberPo.setMemberId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 调用服务保存费用套餐成员信息
        int flag = feeComboMemberV1InnerServiceSMOImpl.saveFeeComboMember(feeComboMemberPo);

        // 检查保存操作是否成功
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置成功的响应结果
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}