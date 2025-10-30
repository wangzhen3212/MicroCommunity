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
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.coupon.CouponPropertyPoolDto;
import com.java110.dto.coupon.CouponPropertyPoolDetailDto;
import com.java110.dto.coupon.CouponPropertyUserDto;
import com.java110.intf.acct.ICouponPropertyPoolDetailV1InnerServiceSMO;
import com.java110.intf.acct.ICouponPropertyPoolV1InnerServiceSMO;
import com.java110.intf.acct.ICouponPropertyUserV1InnerServiceSMO;
import com.java110.po.coupon.CouponPropertyPoolPo;
import com.java110.po.coupon.CouponPropertyPoolDetailPo;
import com.java110.po.coupon.CouponPropertyUserPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.lock.DistributedLock;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 类表述：删除优惠券赠送记录命令类
 * 服务编码：couponProperty.deleteCouponPropertyPool
 * 请求路劲：/app/couponProperty.DeleteCouponPropertyPool
 * add by 吴学文 at 2022-11-19 23:00:42 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
/**
 * 删除优惠券赠送记录命令类
 * 主要功能：处理删除优惠券赠送记录的请求，包括验证参数、删除赠送记录、恢复优惠券库存等操作
 * 涉及业务：优惠券管理、赠送记录管理、库存管理
 * @author 吴学文
 * @version 1.0
 * @since 2022-11-19
 */
@Java110Cmd(serviceCode = "couponProperty.deleteCouponPropertyPoolDetail")
public class DeleteCouponPropertyPoolDetailCmd extends Cmd {
    private static Logger logger = LoggerFactory.getLogger(DeleteCouponPropertyPoolDetailCmd.class);

    /**
     * 优惠券池服务接口
     */
    @Autowired
    private ICouponPropertyPoolV1InnerServiceSMO couponPropertyPoolV1InnerServiceSMOImpl;

    /**
     * 优惠券赠送记录服务接口
     */
    @Autowired
    private ICouponPropertyPoolDetailV1InnerServiceSMO couponPropertyPoolDetailV1InnerServiceSMOImpl;

    /**
     * 用户优惠券服务接口
     */
    @Autowired
    private ICouponPropertyUserV1InnerServiceSMO couponPropertyUserV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证赠送记录ID不能为空
        Assert.hasKeyAndValue(reqJson, "detailId", "赠送记录不能为空");
        // 验证小区ID不能为空
        Assert.hasKeyAndValue(reqJson, "communityId", "小区不能为空");
    }

    /**
     * 执行删除优惠券赠送记录命令
     * 主要逻辑：删除赠送记录、恢复优惠券库存、删除用户优惠券记录
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当业务处理失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 构建查询条件，查询赠送记录详情
        CouponPropertyPoolDetailDto couponPropertyPoolDetailDto = new CouponPropertyPoolDetailDto();
        couponPropertyPoolDetailDto.setCommunityId(reqJson.getString("communityId"));
        couponPropertyPoolDetailDto.setDetailId(reqJson.getString("detailId"));
        List<CouponPropertyPoolDetailDto> details = couponPropertyPoolDetailV1InnerServiceSMOImpl.queryCouponPropertyPoolDetails(couponPropertyPoolDetailDto);

        // 验证赠送记录存在且唯一
        Assert.listOnlyOne(details, "未包含赠送记录");

        // 检查用户优惠券记录是否存在
        CouponPropertyUserDto couponPropertyUserDto = new CouponPropertyUserDto();
        couponPropertyUserDto.setCppId(details.get(0).getCppId());
        couponPropertyUserDto.setStock(details.get(0).getSendCount());
        List<CouponPropertyUserDto> couponPropertyUserDtos = couponPropertyUserV1InnerServiceSMOImpl.queryCouponPropertyUsers(couponPropertyUserDto);

        // 如果用户优惠券记录不存在，说明优惠券已被使用，无法退回
        if (couponPropertyUserDtos == null || couponPropertyUserDtos.size() < 1) {
            throw new CmdException("优惠券已经被使用，无法退回");
        }

        // 获取分布式锁，防止并发操作
        String requestId = DistributedLock.getLockUUID();
        String key = this.getClass().getSimpleName() + reqJson.getString("cppId");
        try {
            // 等待获取分布式锁
            DistributedLock.waitGetDistributedLock(key, requestId);

            // 查询优惠券池信息
            CouponPropertyPoolDto couponPropertyPoolDto = new CouponPropertyPoolDto();
            couponPropertyPoolDto.setCppId(details.get(0).getCppId());
            List<CouponPropertyPoolDto> couponPropertyPoolDtos = couponPropertyPoolV1InnerServiceSMOImpl.queryCouponPropertyPools(couponPropertyPoolDto);

            // 验证优惠券池记录存在
            if (couponPropertyPoolDtos == null || couponPropertyPoolDtos.size() < 1) {
                throw new CmdException("优惠券不存在");
            }

            // 获取当前库存和要恢复的数量
            int stock = Integer.parseInt(couponPropertyPoolDtos.get(0).getStock());
            int quantity = Integer.parseInt(details.get(0).getSendCount());

            // 删除赠送记录
            CouponPropertyPoolDetailPo couponPropertyPoolDetailPo = new CouponPropertyPoolDetailPo();
            couponPropertyPoolDetailPo.setCommunityId(couponPropertyPoolDtos.get(0).getCommunityId());
            couponPropertyPoolDetailPo.setDetailId(details.get(0).getDetailId());
            int flag = couponPropertyPoolDetailV1InnerServiceSMOImpl.deleteCouponPropertyPoolDetail(couponPropertyPoolDetailPo);
            if (flag < 1) {
                throw new CmdException("删除赠送记录失败");
            }

            // 恢复优惠券库存
            CouponPropertyPoolPo couponPropertyPoolPo = new CouponPropertyPoolPo();
            couponPropertyPoolPo.setCppId(couponPropertyPoolDtos.get(0).getCppId());
            couponPropertyPoolPo.setStock((stock + quantity) + ""); // 计算新的库存数量
            flag = couponPropertyPoolV1InnerServiceSMOImpl.updateCouponPropertyPool(couponPropertyPoolPo);
            if (flag < 1) {
                throw new CmdException("赠送失败");
            }

            // 删除用户优惠券记录
            CouponPropertyUserPo couponPropertyUserPo = new CouponPropertyUserPo();
            couponPropertyUserPo.setCommunityId(couponPropertyPoolDtos.get(0).getCommunityId());
            couponPropertyUserPo.setCouponId(couponPropertyUserDtos.get(0).getCouponId());
            flag = couponPropertyUserV1InnerServiceSMOImpl.deleteCouponPropertyUser(couponPropertyUserPo);

            if (flag < 1) {
                throw new CmdException("保存赠送业主记录失败");
            }
        } finally {
            // 释放分布式锁
            DistributedLock.releaseDistributedLock(requestId, key);
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}