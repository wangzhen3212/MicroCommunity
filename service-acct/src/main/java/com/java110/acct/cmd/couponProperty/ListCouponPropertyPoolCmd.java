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
package com.java110.acct.cmd.couponProperty;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.coupon.CouponPropertyPoolConfigDto;
import com.java110.intf.acct.ICouponPropertyPoolConfigV1InnerServiceSMO;
import com.java110.intf.acct.ICouponPropertyPoolV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.coupon.CouponPropertyPoolDto;
import java.util.List;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询
 * 服务编码：couponProperty.listCouponPropertyPool
 * 请求路劲：/app/couponProperty.ListCouponPropertyPool
 * add by 吴学文 at 2022-11-19 23:00:42 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 优惠券资产池查询命令类
 * 负责处理优惠券资产池列表查询请求，包括查询资产池基本信息及其关联的配置信息
 * 服务编码：couponProperty.listCouponPropertyPool
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-11-19
 */
@Java110Cmd(serviceCode = "couponProperty.listCouponPropertyPool")
public class ListCouponPropertyPoolCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListCouponPropertyPoolCmd.class);
    
    /**
     * 优惠券资产池服务接口
     */
    @Autowired
    private ICouponPropertyPoolV1InnerServiceSMO couponPropertyPoolV1InnerServiceSMOImpl;

    /**
     * 优惠券资产池配置服务接口
     */
    @Autowired
    private ICouponPropertyPoolConfigV1InnerServiceSMO couponPropertyPoolConfigV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性，主要验证分页参数
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 处理优惠券资产池列表查询请求，包括查询资产池信息和关联的配置信息
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为优惠券资产池DTO对象
        CouponPropertyPoolDto couponPropertyPoolDto = BeanConvertUtil.covertBean(reqJson, CouponPropertyPoolDto.class);

        // 查询优惠券资产池总数
        int count = couponPropertyPoolV1InnerServiceSMOImpl.queryCouponPropertyPoolsCount(couponPropertyPoolDto);

        List<CouponPropertyPoolDto> couponPropertyPoolDtos = null;

        if (count > 0) {
            // 如果存在数据，查询优惠券资产池列表
            couponPropertyPoolDtos = couponPropertyPoolV1InnerServiceSMOImpl.queryCouponPropertyPools(couponPropertyPoolDto);
            // 查询每个资产池的配置信息
            queryConfigs(couponPropertyPoolDtos);
        } else {
            // 如果没有数据，返回空列表
            couponPropertyPoolDtos = new ArrayList<>();
        }

        // 计算总页数并构造返回结果
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, couponPropertyPoolDtos);

        // 设置HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 查询优惠券资产池配置信息
     * 为每个优惠券资产池查询其关联的配置信息
     *
     * @param couponPropertyPoolDtos 优惠券资产池数据列表
     */
    private void queryConfigs(List<CouponPropertyPoolDto> couponPropertyPoolDtos) {
        // 检查参数有效性
        if(couponPropertyPoolDtos == null || couponPropertyPoolDtos.size()<1){
            return ;
        }
        
        // 收集所有资产池ID
        List<String> cppIds = new ArrayList<>();
        for(CouponPropertyPoolDto couponPropertyPoolDto: couponPropertyPoolDtos){
            cppIds.add(couponPropertyPoolDto.getCppId());
        }

        // 构建查询条件
        CouponPropertyPoolConfigDto couponPropertyPoolConfigDto = new CouponPropertyPoolConfigDto();
        couponPropertyPoolConfigDto.setCouponIds(cppIds.toArray(new String[cppIds.size()]));
        
        // 批量查询配置信息
        List<CouponPropertyPoolConfigDto> couponPropertyPoolConfigDtos
                = couponPropertyPoolConfigV1InnerServiceSMOImpl.queryCouponPropertyPoolConfigs(couponPropertyPoolConfigDto);
        
        // 临时存储每个资产池的配置列表
        List<CouponPropertyPoolConfigDto> tmpCouponPropertyPoolConfigDtos = null;
        
        // 为每个资产池设置对应的配置信息
        for(CouponPropertyPoolDto couponPropertyPoolDto: couponPropertyPoolDtos){
            tmpCouponPropertyPoolConfigDtos = new ArrayList<>();
            for(CouponPropertyPoolConfigDto couponPropertyPoolConfigDto1: couponPropertyPoolConfigDtos){
                // 根据资产池ID匹配配置信息
                if(couponPropertyPoolDto.getCppId().equals(couponPropertyPoolConfigDto1.getCouponId())){
                    tmpCouponPropertyPoolConfigDtos.add(couponPropertyPoolConfigDto1);
                }
            }
            // 设置资产池的配置列表
            couponPropertyPoolDto.setConfigs(tmpCouponPropertyPoolConfigDtos);
        }
    }
}