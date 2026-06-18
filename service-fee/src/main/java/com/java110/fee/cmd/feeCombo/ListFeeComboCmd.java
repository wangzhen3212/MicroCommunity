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
package com.java110.fee.cmd.feeCombo;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.fee.IFeeComboV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.fee.FeeComboDto;
import java.util.List;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询
 * 服务编码：feeCombo.listFeeCombo
 * 请求路劲：/app/feeCombo.ListFeeCombo
 * add by 吴学文 at 2022-05-07 15:31:19 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 费用套餐查询命令类
 * 负责处理费用套餐列表查询请求，实现分页查询功能
 * 服务编码：feeCombo.listFeeCombo
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-05-07
 */
@Java110Cmd(serviceCode = "feeCombo.listFeeCombo")
public class ListFeeComboCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListFeeComboCmd.class);
    
    /**
     * 费用套餐服务接口，用于查询费用套餐数据
     */
    @Autowired
    private IFeeComboV1InnerServiceSMO feeComboV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性，主要验证分页参数
     *
     * @param event CMD事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的分页信息验证方法，验证page和row参数
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 处理费用套餐查询请求，返回分页查询结果
     *
     * @param event CMD事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 将请求JSON对象转换为FeeComboDto对象
        FeeComboDto feeComboDto = BeanConvertUtil.covertBean(reqJson, FeeComboDto.class);

        // 查询符合条件的费用套餐总数
        int count = feeComboV1InnerServiceSMOImpl.queryFeeCombosCount(feeComboDto);

        List<FeeComboDto> feeComboDtos = null;

        // 根据查询结果数量决定返回的数据
        if (count > 0) {
            // 如果存在数据，则查询具体的费用套餐列表
            feeComboDtos = feeComboV1InnerServiceSMOImpl.queryFeeCombos(feeComboDto);
        } else {
            // 如果没有数据，返回空列表
            feeComboDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页显示数量，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        
        // 构建返回结果对象，包含分页信息和数据
        ResultVo resultVo = new ResultVo(totalPage, count, feeComboDtos);

        // 创建HTTP响应实体，状态码为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应设置到数据流上下文中
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}