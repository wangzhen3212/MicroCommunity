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
package com.java110.acct.cmd.couponUser;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.log.LoggerFactory;
import com.java110.dto.community.CommunityMemberDto;
import com.java110.dto.coupon.CouponUserDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.intf.acct.ICouponUserV1InnerServiceSMO;
import com.java110.intf.community.ICommunityMemberV1InnerServiceSMO;
import com.java110.intf.user.IOwnerV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;


/**
 * 类表述：查询用户优惠券列表命令类
 * 服务编码：couponUser.listCouponUser
 * 请求路劲：/app/couponUser.ListCouponUser
 * 功能描述：该命令类用于处理查询用户优惠券列表的请求，根据手机号和小区ID查询用户及其家庭成员的优惠券信息
 * add by 吴学文 at 2021-11-24 00:19:34 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "couponUser.listCouponUser")
public class ListCouponUserCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListCouponUserCmd.class);
    
    /**
     * 优惠券用户服务接口
     */
    @Autowired
    private ICouponUserV1InnerServiceSMO couponUserV1InnerServiceSMOImpl;

    /**
     * 小区成员服务接口
     */
    @Autowired
    private ICommunityMemberV1InnerServiceSMO communityMemberV1InnerServiceSMOImpl;

    /**
     * 业主服务接口
     */
    @Autowired
    private IOwnerV1InnerServiceSMO ownerV1InnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类方法验证分页信息
        super.validatePageInfo(reqJson);
        // 验证必须包含手机号参数
        Assert.hasKeyAndValue(reqJson, "tel", "未包含手机号");
        // 验证必须包含小区ID参数
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
    }

    /**
     * 执行查询用户优惠券列表命令
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行过程中出现错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为CouponUserDto对象
        CouponUserDto couponUserDto = BeanConvertUtil.covertBean(reqJson, CouponUserDto.class);

        // 验证小区物业是否存在
        CommunityMemberDto communityMemberDto = new CommunityMemberDto();
        communityMemberDto.setCommunityId(reqJson.getString("communityId"));
        communityMemberDto.setMemberTypeCd(CommunityMemberDto.MEMBER_TYPE_PROPERTY);
        communityMemberDto.setPage(1);
        communityMemberDto.setRow(1);
        List<CommunityMemberDto> communityMemberDtos = communityMemberV1InnerServiceSMOImpl.queryCommunityMembers(communityMemberDto);

        // 确保小区物业存在且唯一
        Assert.listOnlyOne(communityMemberDtos, "小区物业不存在");

        // 根据手机号查询业主信息
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setLink(reqJson.getString("tel"));
        ownerDto.setCommunityId(reqJson.getString("communityId"));
        ownerDto.setPage(1);
        ownerDto.setRow(1);
        List<OwnerDto> ownerDtos = ownerV1InnerServiceSMOImpl.queryOwners(ownerDto);

        // 如果未找到业主信息，直接返回空结果
        if (ownerDtos == null || ownerDtos.size() < 1) {
            return;
        }

        // 根据业主ID查询所有家庭成员信息
        ownerDto = new OwnerDto();
        ownerDto.setOwnerId(ownerDtos.get(0).getOwnerId());
        ownerDto.setCommunityId(reqJson.getString("communityId"));
        ownerDtos = ownerV1InnerServiceSMOImpl.queryOwners(ownerDto);
        if (ownerDtos == null || ownerDtos.size() < 1) {
            return;
        }
        
        // 收集所有家庭成员的联系电话
        List<String> tels = new ArrayList<>();
        for (OwnerDto tmpOwnerDto : ownerDtos) {
            tels.add(tmpOwnerDto.getLink());
        }

        // 设置查询条件：清空单个手机号，使用家庭成员手机号列表查询
        couponUserDto.setTel("");
        couponUserDto.setTels(tels.toArray(new String[tels.size()]));

        // 查询优惠券用户总数
        int count = couponUserV1InnerServiceSMOImpl.queryCouponUsersCount(couponUserDto);

        List<CouponUserDto> couponUserDtos = null;

        // 根据查询结果数量决定是否查询详细数据
        if (count > 0) {
            couponUserDtos = couponUserV1InnerServiceSMOImpl.queryCouponUsers(couponUserDto);
        } else {
            couponUserDtos = new ArrayList<>();
        }

        // 构建返回结果对象，包含分页信息和数据列表
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, couponUserDtos);

        // 设置HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}