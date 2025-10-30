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
import com.java110.dto.parking.ParkingCouponCarDto;
import com.java110.intf.acct.IParkingCouponCarV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;


@Java110CmdDoc(title = "查询商户赠送停车劵",
        description = "根据商户ID 查询商户赠送停车劵",
        httpMethod = "get",
        url = "http://{ip}:{port}/app/parkingCoupon.listParkingCouponCar",
        resource = "acctDoc",
        author = "吴学文",
        serviceCode = "parkingCoupon.listParkingCouponCar"
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
                @Java110ParamDoc(parentNodeName = "data",name = "stateName", type = "String", remark = "状态"),
                @Java110ParamDoc(parentNodeName = "data",name = "shopName", type = "String", remark = "商家名称"),
                @Java110ParamDoc(parentNodeName = "data",name = "carNum", type = "String", remark = "车牌号"),
        }
)

@Java110ExampleDoc(
        reqBody="http://{ip}:{port}/app/parkingCoupon.listParkingCouponCar?page=1&row=100&communityId=2022081539020475&shopId=502022101140520018",
        resBody="{\"code\":0,\"data\":[{\"carNum\":\"青A88888\",\"communityId\":\"2022081539020475\",\"couponId\":\"102022101024650009\",\"couponName\":\"时卡优惠券\",\"couponShopId\":\"102022101112890007\",\"createTime\":\"2022-10-15 18:41:33\",\"endTime\":\"2022-10-16 18:41:32\",\"giveWay\":\"2002\",\"paId\":\"102022082382290048\",\"paName\":\"\",\"page\":-1,\"pccId\":\"102022101586160004\",\"records\":0,\"row\":0,\"shopId\":\"502022101140520018\",\"shopName\":\"测试商家\",\"startTime\":\"2022-10-15 18:41:32\",\"state\":\"1001\",\"stateName\":\"未使用\",\"statusCd\":\"0\",\"total\":0,\"typeCd\":\"1001\",\"value\":\"3\"}],\"msg\":\"成功\",\"page\":0,\"records\":1,\"rows\":0,\"total\":1}"
)

/**
 * 查询商户赠送停车券命令类
 * 该类负责处理查询商户赠送停车券的请求，根据商户ID、小区ID等条件查询停车券信息
 * 支持分页查询，返回停车券的详细信息列表
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-10-12
 */
@Java110Cmd(serviceCode = "parkingCoupon.listParkingCouponCar")
public class ListParkingCouponCarCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListParkingCouponCarCmd.class);
    
    /**
     * 停车券车辆服务接口
     * 用于查询停车券车辆相关数据
     */
    @Autowired
    private IParkingCouponCarV1InnerServiceSMO parkingCouponCarV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性，主要验证分页参数
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的分页信息验证方法，验证page和row参数
        super.validatePageInfo(reqJson);
    }

    /**
     * 执行命令方法
     * 处理查询商户赠送停车券的核心业务逻辑
     * 包括参数转换、数据查询、结果封装和响应设置
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为停车券车辆数据传输对象
        ParkingCouponCarDto parkingCouponCarDto = BeanConvertUtil.covertBean(reqJson, ParkingCouponCarDto.class);

        // 查询符合条件的停车券车辆记录总数
        int count = parkingCouponCarV1InnerServiceSMOImpl.queryParkingCouponCarsCount(parkingCouponCarDto);

        List<ParkingCouponCarDto> parkingCouponCarDtos = null;

        // 根据查询结果数量决定如何处理数据
        if (count > 0) {
            // 如果有数据，则查询具体的停车券车辆信息列表
            parkingCouponCarDtos = parkingCouponCarV1InnerServiceSMOImpl.queryParkingCouponCars(parkingCouponCarDto);
        } else {
            // 如果没有数据，返回空列表
            parkingCouponCarDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页显示数量，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 封装返回结果对象，包含分页信息和数据列表
        ResultVo resultVo = new ResultVo(totalPage, count, parkingCouponCarDtos);

        // 创建HTTP响应实体，设置响应状态为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应实体设置到命令数据流上下文中
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}