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
package com.java110.acct.cmd.payment;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.payment.PaymentPoolConfigDto;
import com.java110.dto.payment.PaymentPoolDto;
import com.java110.dto.payment.PaymentPoolValueDto;
import com.java110.dto.repair.RepairDto;
import com.java110.intf.acct.IPaymentPoolConfigV1InnerServiceSMO;
import com.java110.intf.acct.IPaymentPoolV1InnerServiceSMO;
import com.java110.intf.acct.IPaymentPoolValueV1InnerServiceSMO;
import com.java110.intf.community.ICommunityV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;


/**
 * 类表述：查询
 * 服务编码：paymentPool.listPaymentPool
 * 请求路劲：/app/paymentPool.ListPaymentPool
 * add by 吴学文 at 2023-10-25 11:52:42 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 支付池管理查询命令类
 * 负责处理管理员查询支付池信息的请求，包括支付池基本信息、配置信息和值信息的查询
 * 服务编码：payment.listAdminPayment
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023-10-25
 */
@Java110Cmd(serviceCode = "payment.listAdminPayment")
public class ListAdminPaymentCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListAdminPaymentCmd.class);
    
    /**
     * 支付池服务接口
     */
    @Autowired
    private IPaymentPoolV1InnerServiceSMO paymentPoolV1InnerServiceSMOImpl;

    /**
     * 支付池值服务接口
     */
    @Autowired
    private IPaymentPoolValueV1InnerServiceSMO paymentPoolValueV1InnerServiceSMOImpl;

    /**
     * 支付池配置服务接口
     */
    @Autowired
    private IPaymentPoolConfigV1InnerServiceSMO paymentPoolConfigV1InnerServiceSMOImpl;

    /**
     * 小区服务接口
     */
    @Autowired
    private ICommunityV1InnerServiceSMO communityV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性，包括分页参数验证和管理员权限验证
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证管理员权限
        super.validateAdmin(cmdDataFlowContext);
    }

    /**
     * 命令执行方法
     * 查询支付池信息，包括基本信息、小区名称、配置信息和值信息
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为支付池DTO对象
        PaymentPoolDto paymentPoolDto = BeanConvertUtil.covertBean(reqJson, PaymentPoolDto.class);

        // 查询支付池总数
        int count = paymentPoolV1InnerServiceSMOImpl.queryPaymentPoolsCount(paymentPoolDto);

        List<PaymentPoolDto> paymentPoolDtos = null;

        if (count > 0) {
            // 查询支付池列表
            paymentPoolDtos = paymentPoolV1InnerServiceSMOImpl.queryPaymentPools(paymentPoolDto);
        } else {
            // 如果没有数据，返回空列表
            paymentPoolDtos = new ArrayList<>();
        }
        
        // 刷新小区名称信息
        refreshCommunityName(paymentPoolDtos);

        // 补充配置信息和值信息
        computeConfigAndValues(paymentPoolDtos);

        // 构造返回结果
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, paymentPoolDtos);

        // 设置响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 刷新小区名称信息
     * 根据小区ID查询小区名称并设置到支付池DTO中
     *
     * @param paymentPoolDtos 支付池DTO列表
     */
    private void refreshCommunityName(List<PaymentPoolDto> paymentPoolDtos) {
        // 如果支付池列表为空，直接返回
        if(ListUtil.isNull(paymentPoolDtos)){
            return;
        }

        // 收集所有小区ID
        List<String> communityIds = new ArrayList<>();
        for (PaymentPoolDto paymentPoolDto : paymentPoolDtos) {
            communityIds.add(paymentPoolDto.getCommunityId());
        }

        // 如果小区ID列表为空，直接返回
        if(ListUtil.isNull(communityIds)){
            return ;
        }
        
        // 查询小区信息
        CommunityDto communityDto = new CommunityDto();
        communityDto.setCommunityIds(communityIds.toArray(new String[communityIds.size()]));
        List<CommunityDto> communityDtos = communityV1InnerServiceSMOImpl.queryCommunitys(communityDto);
        
        // 如果小区信息为空，直接返回
        if(ListUtil.isNull(communityDtos)){
            return;
        }
        
        // 设置小区名称到支付池DTO中
        for (PaymentPoolDto paymentPoolDto : paymentPoolDtos) {
            for (CommunityDto tCommunityDto : communityDtos) {
                // 匹配小区ID
                if (!paymentPoolDto.getCommunityId().equals(tCommunityDto.getCommunityId())) {
                    continue;
                }
                // 设置小区名称
                paymentPoolDto.setCommunityName(tCommunityDto.getName());
            }
        }
    }

    /**
     * 计算配置信息和值信息
     * 查询支付池的配置信息和值信息并设置到支付池DTO中
     * 注意：当前实现只处理第一个支付池的配置和值信息
     *
     * @param paymentPoolDtos 支付池DTO列表
     */
    private void computeConfigAndValues(List<PaymentPoolDto> paymentPoolDtos) {
        // 如果支付池列表为空，直接返回
        if(ListUtil.isNull(paymentPoolDtos)){
            return;
        }

        // 查询支付池值信息
        PaymentPoolValueDto paymentPoolValueDto = new PaymentPoolValueDto();
        paymentPoolValueDto.setPpId(paymentPoolDtos.get(0).getPpId()); // 设置支付池ID
        paymentPoolValueDto.setCommunityId(paymentPoolDtos.get(0).getCommunityId()); // 设置小区ID
        List<PaymentPoolValueDto> values = paymentPoolValueV1InnerServiceSMOImpl.queryPaymentPoolValues(paymentPoolValueDto);
        paymentPoolDtos.get(0).setValues(values); // 设置值信息到第一个支付池

        // 查询支付池配置信息
        PaymentPoolConfigDto paymentPoolConfigDto = new PaymentPoolConfigDto();
        paymentPoolConfigDto.setPpId(paymentPoolDtos.get(0).getPpId()); // 设置支付池ID
        paymentPoolConfigDto.setCommunityId(paymentPoolDtos.get(0).getCommunityId()); // 设置小区ID
        List<PaymentPoolConfigDto> configs = paymentPoolConfigV1InnerServiceSMOImpl.queryPaymentPoolConfigs(paymentPoolConfigDto);
        paymentPoolDtos.get(0).setConfigs(configs); // 设置配置信息到第一个支付池
    }
}