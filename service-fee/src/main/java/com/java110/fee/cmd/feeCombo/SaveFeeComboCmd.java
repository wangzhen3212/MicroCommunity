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
package com.java110.fee.cmd.feeCombo;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.fee.IFeeComboMemberV1InnerServiceSMO;
import com.java110.intf.fee.IFeeComboV1InnerServiceSMO;
import com.java110.po.fee.FeeComboPo;
import com.java110.po.fee.FeeComboMemberPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：保存费用套餐信息
 * 服务编码：feeCombo.saveFeeCombo
 * 请求路劲：/app/feeCombo.SaveFeeCombo
 * add by 吴学文 at 2022-05-07 15:31:19 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
/**
 * 费用套餐保存命令类
 * 负责处理费用套餐及其成员信息的保存操作
 * 包括套餐基本信息保存和套餐包含的费用配置项保存
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-05-07
 */
@Java110Cmd(serviceCode = "feeCombo.saveFeeCombo")
public class SaveFeeComboCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveFeeComboCmd.class);

    /**
     * ID生成前缀，用于生成套餐ID和成员ID
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 费用套餐服务接口
     */
    @Autowired
    private IFeeComboV1InnerServiceSMO feeComboV1InnerServiceSMOImpl;
    
    /**
     * 费用套餐成员服务接口
     */
    @Autowired
    private IFeeComboMemberV1InnerServiceSMO feeComboMemberV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数中是否包含必要的字段
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求报文中必须包含comboName字段
        Assert.hasKeyAndValue(reqJson, "comboName", "请求报文中未包含comboName");
        // 验证请求报文中必须包含communityId字段
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
    }

    /**
     * 执行保存费用套餐命令
     * 保存费用套餐基本信息及其包含的费用配置项
     * 使用事务保证数据一致性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 将请求JSON转换为FeeComboPo对象
        FeeComboPo feeComboPo = BeanConvertUtil.covertBean(reqJson, FeeComboPo.class);
        // 生成套餐ID
        feeComboPo.setComboId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        // 保存费用套餐基本信息
        int flag = feeComboV1InnerServiceSMOImpl.saveFeeCombo(feeComboPo);

        // 检查保存是否成功
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }
        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());

        // 检查请求中是否包含配置项ID列表
        if(!reqJson.containsKey("configIds")){
            return ;
        }

        // 获取配置项ID数组
        JSONArray configIds = reqJson.getJSONArray("configIds");
        FeeComboMemberPo feeComboMemberPo = null;
        // 遍历配置项ID列表，保存每个配置项到套餐成员表
        for(int configIndex = 0; configIndex < configIds.size() ; configIndex ++){
            feeComboMemberPo = new FeeComboMemberPo();
            // 设置套餐ID
            feeComboMemberPo.setComboId(feeComboPo.getComboId());
            // 设置小区ID
            feeComboMemberPo.setCommunityId(reqJson.getString("communityId"));
            // 设置配置项ID
            feeComboMemberPo.setConfigId(configIds.getString(configIndex));
            // 生成成员ID
            feeComboMemberPo.setMemberId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));

            // 保存套餐成员信息
            flag = feeComboMemberV1InnerServiceSMOImpl.saveFeeComboMember(feeComboMemberPo);
            // 检查保存是否成功
            if (flag < 1) {
                throw new CmdException("保存数据失败");
            }
        }
    }
}