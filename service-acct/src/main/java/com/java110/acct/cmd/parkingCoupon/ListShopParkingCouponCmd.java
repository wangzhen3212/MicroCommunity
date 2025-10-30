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
package com.java110.acct.cmd.parkingCoupon;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.parking.ParkingCouponShopDto;
import com.java110.intf.acct.IParkingCouponShopV1InnerServiceSMO;
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


@Java110CmdDoc(title = "查询商户停车劵",
        description = "根据商户ID 查询商户停车劵",
        httpMethod = "get",
        url = "http://{ip}:{port}/app/parkingCoupon.listShopParkingCoupon",
        resource = "acctDoc",
        author = "吴学文",
        serviceCode = "parkingCoupon.listShopParkingCoupon"
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "page",type = "int",length = 11, remark = "页数"),
        @Java110ParamDoc(name = "row",type = "int", length = 11, remark = "行数"),
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "shopId", length = 30, remark = "商铺ID"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
                @Java110ParamDoc(name = "data", type = "Object", remark = "有效数据"),
                @Java110ParamDoc(parentNodeName = "data",name = "couponName", type = "String", remark = "优惠券名称"),
                @Java110ParamDoc(parentNodeName = "data",name = "startTime", type = "String", remark = "开始时间"),
                @Java110ParamDoc(parentNodeName = "data",name = "endTime", type = "String", remark = "结束时间"),
                @Java110ParamDoc(parentNodeName = "data",name = "paName", type = "String", remark = "停车场名称"),
                @Java110ParamDoc(parentNodeName = "data",name = "paId", type = "String", remark = "停车场ID"),
                @Java110ParamDoc(parentNodeName = "data",name = "quantity", type = "String", remark = "数量"),
                @Java110ParamDoc(parentNodeName = "data",name = "shopName", type = "String", remark = "商家名称"),
        }
)

@Java110ExampleDoc(
        reqBody="http://{ip}:{port}/app/parkingCoupon.listShopParkingCoupon?page=1&row=100&communityId=2022081539020475&shopId=502022101140520018",
        resBody="{\"code\":0,\"data\":[{\"communityId\":\"2022081539020475\",\"communityName\":\"测试小区\",\"couponId\":\"102022101024650009\",\"couponName\":\"时卡优惠券\",\"couponShopId\":\"102022101112890007\",\"endTime\":\"2050-01-01 00:00:00\",\"paId\":\"102022082382290048\",\"paName\":\"\",\"page\":-1,\"quantity\":\"200\",\"records\":0,\"row\":0,\"shopId\":\"502022101140520018\",\"shopName\":\"测试商家\",\"startTime\":\"2022-10-11 19:01:03\",\"statusCd\":\"0\",\"total\":0}],\"msg\":\"成功\",\"page\":0,\"records\":1,\"rows\":0,\"total\":1}"
)

/**
 * 查询商户停车券命令类
 * 
 * 该类负责处理查询商户停车券的请求，根据商户ID查询对应的停车券信息，
 * 支持分页查询，返回停车券的详细信息列表
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-10-10
 */
@Java110Cmd(serviceCode = "parkingCoupon.listShopParkingCoupon")
public class ListShopParkingCouponCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListShopParkingCouponCmd.class);
    
    /**
     * 停车券商户服务接口
     */
    @Autowired
    private IParkingCouponShopV1InnerServiceSMO parkingCouponShopV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，包括分页参数和必填参数
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类方法验证分页参数
        super.validatePageInfo(reqJson);
        // 验证请求中必须包含商铺ID参数
        Assert.hasKeyAndValue(reqJson, "shopId", "未包含商铺ID");
    }

    /**
     * 执行命令方法
     * 
     * 处理查询商户停车券的业务逻辑，包括数据查询、结果封装和响应设置
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行过程中发生异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为停车券商户数据传输对象
        ParkingCouponShopDto parkingCouponShopDto = BeanConvertUtil.covertBean(reqJson, ParkingCouponShopDto.class);

        // 查询符合条件的停车券商户记录总数
        int count = parkingCouponShopV1InnerServiceSMOImpl.queryParkingCouponShopsCount(parkingCouponShopDto);

        List<ParkingCouponShopDto> parkingCouponShopDtos = null;

        // 根据查询结果数量决定返回数据
        if (count > 0) {
            // 如果存在记录，查询具体的停车券商户列表
            parkingCouponShopDtos = parkingCouponShopV1InnerServiceSMOImpl.queryParkingCouponShops(parkingCouponShopDto);
        } else {
            // 如果没有记录，返回空列表
            parkingCouponShopDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 封装返回结果对象
        ResultVo resultVo = new ResultVo(totalPage, count, parkingCouponShopDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应实体到数据流上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}