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
package com.java110.fee.cmd.tempCarFeeConfigAttr;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.fee.TempCarFeeConfigAttrDto;
import com.java110.intf.fee.ITempCarFeeConfigAttrV1InnerServiceSMO;
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


/**
 * 类表述：查询
 * 服务编码：tempCarFeeConfigAttr.listTempCarFeeConfigAttr
 * 请求路劲：/app/tempCarFeeConfigAttr.ListTempCarFeeConfigAttr
 * add by 吴学文 at 2022-06-27 13:39:52 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 临时车费用配置属性查询命令类
 * 该类负责处理临时车费用配置属性的列表查询请求，包括分页查询和数据返回
 * 通过服务编码 tempCarFeeConfigAttr.listTempCarFeeConfigAttr 对外提供服务
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-06-27
 */
@Java110Cmd(serviceCode = "tempCarFeeConfigAttr.listTempCarFeeConfigAttr")
public class ListTempCarFeeConfigAttrCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(ListTempCarFeeConfigAttrCmd.class);
    
    /**
     * 临时车费用配置属性服务接口
     */
    @Autowired
    private ITempCarFeeConfigAttrV1InnerServiceSMO tempCarFeeConfigAttrV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 主要验证分页参数的有效性
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
     * 执行查询命令的主要业务逻辑
     * 根据查询条件获取临时车费用配置属性列表，支持分页查询
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为数据传输对象
        TempCarFeeConfigAttrDto tempCarFeeConfigAttrDto = BeanConvertUtil.covertBean(reqJson, TempCarFeeConfigAttrDto.class);

        // 查询符合条件的记录总数
        int count = tempCarFeeConfigAttrV1InnerServiceSMOImpl.queryTempCarFeeConfigAttrsCount(tempCarFeeConfigAttrDto);

        List<TempCarFeeConfigAttrDto> tempCarFeeConfigAttrDtos = null;

        // 根据查询结果数量决定返回数据
        if (count > 0) {
            // 有数据时，查询具体的属性列表
            tempCarFeeConfigAttrDtos = tempCarFeeConfigAttrV1InnerServiceSMOImpl.queryTempCarFeeConfigAttrs(tempCarFeeConfigAttrDto);
        } else {
            // 无数据时返回空列表
            tempCarFeeConfigAttrDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 构建返回结果对象
        ResultVo resultVo = new ResultVo(totalPage, count, tempCarFeeConfigAttrDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应设置到数据流上下文中
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}