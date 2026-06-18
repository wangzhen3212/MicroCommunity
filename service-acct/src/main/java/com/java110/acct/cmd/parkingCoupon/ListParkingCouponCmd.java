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
import com.java110.intf.acct.IParkingCouponV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.parking.ParkingCouponDto;
import java.util.List;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Java110CmdDoc(title = "查询停车卷",
        description = "查询可以购买停车卷",
        httpMethod = "get",
        url = "http://{ip}:{port}/app/parkingCoupon.listParkingCoupon",
        resource = "acctDoc",
        author = "吴学文",
        serviceCode = "parkingCoupon.listParkingCoupon"
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
                @Java110ParamDoc(name = "data", type = "Object", remark = "有效数据"),
                @Java110ParamDoc(parentNodeName = "data",name = "name", type = "String", remark = "优惠券名称"),
                @Java110ParamDoc(parentNodeName = "data",name = "typeCdName", type = "String", remark = "类型"),
                @Java110ParamDoc(parentNodeName = "data",name = "value", type = "String", remark = "面值"),
                @Java110ParamDoc(parentNodeName = "data",name = "valuePrice", type = "String", remark = "售价"),
        }
)

@Java110ExampleDoc(
        reqBody="http://{ip}:{port}/app/parkingCoupon.listParkingCoupon?page=1&row=100&communityId=2022081539020475",
        resBody="{\"code\":0,\"data\":[{\"communityId\":\"2022081539020475\",\"couponId\":\"102022101024650009\",\"name\":\"时卡优惠券\",\"paId\":\"102022082382290048\",\"paName\":\"\",\"page\":-1,\"records\":0,\"row\":0,\"statusCd\":\"0\",\"total\":0,\"typeCd\":\"1001\",\"typeCdName\":\"时长减免\",\"value\":\"3\",\"valuePrice\":\"2\"}],\"msg\":\"成功\",\"page\":0,\"records\":1,\"rows\":0,\"total\":1}"
)

/**
 * 停车券查询命令类
 * 
 * 该类负责处理停车券列表查询请求，包括参数验证、数据查询和结果返回等功能。
 * 服务编码：parkingCoupon.listParkingCoupon
 * 请求路径：/app/parkingCoupon.ListParkingCoupon
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-10-10
 * 
 * 类表述：查询
 * 服务编码：parkingCoupon.listParkingCoupon
 * 请求路劲：/app/parkingCoupon.ListParkingCoupon
 * add by 吴学文 at 2022-10-10 13:27:02 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "parkingCoupon.listParkingCoupon")
public class ListParkingCouponCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(ListParkingCouponCmd.class);
    
    /**
     * 停车券服务接口
     */
    @Autowired
    private IParkingCouponV1InnerServiceSMO parkingCouponV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，包括分页信息和必填字段
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于处理数据流
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类方法验证分页信息
        super.validatePageInfo(reqJson);

        // 验证小区ID参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson,"communityId","未包含小区ID");
    }

    /**
     * 命令执行方法
     * 
     * 执行停车券查询业务逻辑，包括数据查询、结果封装和响应设置
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为停车券DTO对象
        ParkingCouponDto parkingCouponDto = BeanConvertUtil.covertBean(reqJson, ParkingCouponDto.class);

        // 查询停车券总记录数
        int count = parkingCouponV1InnerServiceSMOImpl.queryParkingCouponsCount(parkingCouponDto);

        List<ParkingCouponDto> parkingCouponDtos = null;

        // 根据查询结果数量决定返回的数据
        if (count > 0) {
            // 如果有数据，查询停车券列表
            parkingCouponDtos = parkingCouponV1InnerServiceSMOImpl.queryParkingCoupons(parkingCouponDto);
        } else {
            // 如果没有数据，返回空列表
            parkingCouponDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 封装返回结果对象
        ResultVo resultVo = new ResultVo(totalPage, count, parkingCouponDtos);

        // 创建HTTP响应实体，状态码为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应实体到数据流上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}