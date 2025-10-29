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
package com.java110.fee.cmd.feePrintPageTemplate;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.fee.IFeePrintPageTemplateV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.fee.FeePrintPageTemplateDto;
import java.util.List;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;


/**
 * 类表述：查询
 * 服务编码：feePrintPageTemplate.listFeePrintPageTemplate
 * 请求路劲：/app/feePrintPageTemplate.ListFeePrintPageTemplate
 * add by 吴学文 at 2021-09-16 23:10:01 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 费用打印页面模板查询命令类
 * 
 * 该类负责处理费用打印页面模板的列表查询请求，包括参数验证、数据查询和结果返回等功能
 * 通过调用内部服务接口获取费用打印页面模板数据，并按照分页要求返回查询结果
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2021-09-16
 */
@Java110Cmd(serviceCode = "feePrintPageTemplate.listFeePrintPageTemplate")
public class ListFeePrintPageTemplateCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListFeePrintPageTemplateCmd.class);
    
    /**
     * 费用打印页面模板内部服务接口
     * 用于查询费用打印页面模板的相关数据
     */
    @Autowired
    private IFeePrintPageTemplateV1InnerServiceSMO feePrintPageTemplateV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，主要验证分页参数是否合法
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的分页信息验证方法，确保分页参数合法
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 
     * 处理费用打印页面模板的查询请求，包括数据查询、结果封装和响应设置
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于设置响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 将请求JSON对象转换为费用打印页面模板数据传输对象
        FeePrintPageTemplateDto feePrintPageTemplateDto = BeanConvertUtil.covertBean(reqJson, FeePrintPageTemplateDto.class);

        // 查询符合条件的费用打印页面模板总数
        int count = feePrintPageTemplateV1InnerServiceSMOImpl.queryFeePrintPageTemplatesCount(feePrintPageTemplateDto);

        // 声明费用打印页面模板数据列表
        List<FeePrintPageTemplateDto> feePrintPageTemplateDtos = null;

        // 根据查询结果数量决定如何处理数据
        if (count > 0) {
            // 如果存在数据，则查询具体的费用打印页面模板列表
            feePrintPageTemplateDtos = feePrintPageTemplateV1InnerServiceSMOImpl.queryFeePrintPageTemplates(feePrintPageTemplateDto);
        } else {
            // 如果没有数据，返回空列表
            feePrintPageTemplateDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页显示行数，并向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 创建结果值对象，包含分页信息和数据列表
        ResultVo resultVo = new ResultVo(totalPage, count, feePrintPageTemplateDtos);

        // 创建HTTP响应实体，将结果值对象转换为字符串并设置HTTP状态为200
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应实体设置到数据流上下文中，返回给调用方
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}