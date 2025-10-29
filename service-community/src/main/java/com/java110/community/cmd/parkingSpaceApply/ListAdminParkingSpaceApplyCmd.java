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
package com.java110.community.cmd.parkingSpaceApply;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.log.LoggerFactory;
import com.java110.dto.parking.ParkingSpaceApplyDto;
import com.java110.intf.community.IParkingSpaceApplyV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;


/**
 * 类表述：查询
 * 服务编码：parkingSpaceApply.listParkingSpaceApply
 * 请求路劲：/app/parkingSpaceApply.ListParkingSpaceApply
 * add by 吴学文 at 2021-10-18 13:00:02 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 管理员停车位申请列表查询命令类
 * 负责处理管理员查询停车位申请列表的业务逻辑
 * 服务编码：parkingSpaceApply.listAdminParkingSpaceApply
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2021-10-18
 */
@Java110Cmd(serviceCode = "parkingSpaceApply.listAdminParkingSpaceApply")
public class ListAdminParkingSpaceApplyCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListAdminParkingSpaceApplyCmd.class);
    
    /**
     * 停车位申请服务接口
     */
    @Autowired
    private IParkingSpaceApplyV1InnerServiceSMO parkingSpaceApplyV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性和管理员权限
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
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
     * 处理管理员查询停车位申请列表的核心业务逻辑
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 将请求JSON转换为停车位申请数据传输对象
        ParkingSpaceApplyDto parkingSpaceApplyDto = BeanConvertUtil.covertBean(reqJson, ParkingSpaceApplyDto.class);

        // 查询停车位申请记录总数
        int count = parkingSpaceApplyV1InnerServiceSMOImpl.queryParkingSpaceApplysCount(parkingSpaceApplyDto);

        List<ParkingSpaceApplyDto> parkingSpaceApplyDtos = null;

        // 根据查询结果数量决定如何处理数据
        if (count > 0) {
            // 如果有数据，查询具体的停车位申请列表
            parkingSpaceApplyDtos = parkingSpaceApplyV1InnerServiceSMOImpl.queryParkingSpaceApplys(parkingSpaceApplyDto);
        } else {
            // 如果没有数据，返回空列表
            parkingSpaceApplyDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页显示行数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 构建返回结果对象
        ResultVo resultVo = new ResultVo(totalPage, count, parkingSpaceApplyDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应结果到数据流上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}