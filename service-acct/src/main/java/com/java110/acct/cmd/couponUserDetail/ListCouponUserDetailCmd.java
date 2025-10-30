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
package com.java110.acct.cmd.couponUserDetail;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.acct.ICouponUserDetailV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.coupon.CouponUserDetailDto;

import java.util.List;
import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;


/**
 * 类表述：查询优惠券用户详情列表命令类
 * 服务编码：couponUserDetail.listCouponUserDetail
 * 请求路劲：/app/couponUserDetail.ListCouponUserDetail
 * add by 吴学文 at 2021-11-24 00:20:57 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * 
 * 功能描述：该命令类用于处理优惠券用户详情的列表查询请求，包括分页查询和数据返回
 */
@Java110Cmd(serviceCode = "couponUserDetail.listCouponUserDetail")
public class ListCouponUserDetailCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListCouponUserDetailCmd.class);
    
    /**
     * 优惠券用户详情服务接口
     */
    @Autowired
    private ICouponUserDetailV1InnerServiceSMO couponUserDetailV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 功能描述：验证请求参数，主要验证分页参数
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于处理请求和响应
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的分页信息验证方法，验证分页参数
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 功能描述：执行优惠券用户详情的查询操作，包括数据查询、分页处理和响应返回
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为CouponUserDetailDto对象
        CouponUserDetailDto couponUserDetailDto = BeanConvertUtil.covertBean(reqJson, CouponUserDetailDto.class);

        // 查询优惠券用户详情总数
        int count = couponUserDetailV1InnerServiceSMOImpl.queryCouponUserDetailsCount(couponUserDetailDto);

        List<CouponUserDetailDto> couponUserDetailDtos = null;

        // 根据查询结果数量判断是否进行数据查询
        if (count > 0) {
            // 查询优惠券用户详情列表数据
            couponUserDetailDtos = couponUserDetailV1InnerServiceSMOImpl.queryCouponUserDetails(couponUserDetailDto);
        } else {
            // 如果没有数据，返回空列表
            couponUserDetailDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页显示行数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 创建结果对象，包含总页数、总记录数和数据列表
        ResultVo resultVo = new ResultVo(totalPage, count, couponUserDetailDtos);

        // 创建HTTP响应实体，设置响应状态为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应实体设置到数据流上下文中
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}