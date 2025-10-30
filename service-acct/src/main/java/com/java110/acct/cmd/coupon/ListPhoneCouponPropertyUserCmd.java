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
package com.java110.acct.cmd.coupon;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.coupon.CouponPropertyUserDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.acct.ICouponPropertyUserV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
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
 * 服务编码：couponPropertyUser.listCouponPropertyUser
 * 请求路劲：/app/couponPropertyUser.ListCouponPropertyUser
 * add by 吴学文 at 2022-11-21 12:08:05 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 手机端优惠券查询命令类
 * 该类用于处理手机端用户查询自己拥有的优惠券信息的请求
 * 通过用户登录信息获取对应的优惠券列表，并返回分页结果
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-11-21
 */
@Java110Cmd(serviceCode = "coupon.listPhoneCouponPropertyUser")
public class ListPhoneCouponPropertyUserCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(ListPhoneCouponPropertyUserCmd.class);
    
    /**
     * 优惠券用户关系服务接口，用于查询优惠券相关信息
     */
    @Autowired
    private ICouponPropertyUserV1InnerServiceSMO couponPropertyUserV1InnerServiceSMOImpl;

    /**
     * 用户服务接口，用于查询用户相关信息
     */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性，主要验证分页参数
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数的JSON对象，包含分页参数等信息
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的分页信息验证方法，验证page和row参数的有效性
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 根据当前登录用户信息查询其拥有的优惠券列表
     * 主要流程：获取用户信息 -> 验证用户 -> 查询优惠券 -> 构建返回结果
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于设置响应结果
     * @param reqJson 请求参数的JSON对象，包含查询条件等信息
     * @throws CmdException 当命令执行过程中出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 从上下文中获取当前登录用户的ID
        String userId = CmdContextUtils.getUserId(cmdDataFlowContext);

        // 构建用户查询条件对象
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        // 根据用户ID查询用户信息
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);

        // 验证用户存在且唯一，如果用户不存在或存在多个匹配用户则抛出异常
        Assert.listOnlyOne(userDtos, "用户未登录");

        // 将请求参数转换为优惠券用户关系DTO对象
        CouponPropertyUserDto couponPropertyUserDto = BeanConvertUtil.covertBean(reqJson, CouponPropertyUserDto.class);
        // 设置查询条件中的电话号码为当前用户的电话号码
        couponPropertyUserDto.setTel(userDtos.get(0).getTel());

        // 查询符合条件的优惠券总数，用于分页计算
        int count = couponPropertyUserV1InnerServiceSMOImpl.queryCouponPropertyUsersCount(couponPropertyUserDto);

        List<CouponPropertyUserDto> couponPropertyUserDtos = null;

        // 根据查询结果数量决定返回的数据
        if (count > 0) {
            // 如果存在数据，查询具体的优惠券列表
            couponPropertyUserDtos = couponPropertyUserV1InnerServiceSMOImpl.queryCouponPropertyUsers(couponPropertyUserDto);
        } else {
            // 如果没有数据，返回空列表避免空指针异常
            couponPropertyUserDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 构建返回结果对象，包含分页信息和数据列表
        ResultVo resultVo = new ResultVo(totalPage, count, couponPropertyUserDtos);

        // 创建HTTP响应实体，设置响应状态为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应结果到上下文，供框架后续处理
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}