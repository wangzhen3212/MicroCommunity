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
package com.java110.store.cmd.orgStaffRel;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.org.OrgStaffRelDto;
import com.java110.intf.store.IOrgStaffRelV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;


/**
 * 类表述：查询
 * 服务编码：orgStaffRel.listOrgStaffRel
 * 请求路劲：/app/orgStaffRel.ListOrgStaffRel
 * add by 吴学文 at 2021-10-08 16:25:38 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 组织员工关系查询命令类
 * 
 * 该类负责处理组织员工关系列表查询请求，实现分页查询组织与员工关联信息的功能
 * 通过调用组织员工关系服务接口获取数据，并返回标准格式的响应结果
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2021-10-08
 */
@Java110Cmd(serviceCode = "orgStaffRel.listOrgStaffRel")
public class ListOrgStaffRelCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListOrgStaffRelCmd.class);
    
    /**
     * 组织员工关系服务接口
     */
    @Autowired
    private IOrgStaffRelV1InnerServiceSMO orgStaffRelV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
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
     * 命令执行方法
     * 
     * 处理组织员工关系列表查询请求，包括数据查询、分页处理和响应构建
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 将请求JSON对象转换为组织员工关系数据传输对象
        OrgStaffRelDto orgStaffRelDto = BeanConvertUtil.covertBean(reqJson, OrgStaffRelDto.class);

        // 查询组织员工关系总记录数
        int count = orgStaffRelV1InnerServiceSMOImpl.queryOrgStaffRelsCount(orgStaffRelDto);

        List<OrgStaffRelDto> orgStaffRelDtos = null;

        // 根据查询结果数量决定返回数据
        if (count > 0) {
            // 如果存在记录，则查询具体的组织员工关系列表数据
            orgStaffRelDtos = orgStaffRelV1InnerServiceSMOImpl.queryOrgStaffRels(orgStaffRelDto);
        } else {
            // 如果没有记录，返回空列表
            orgStaffRelDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数 / 每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        
        // 构建返回结果对象，包含分页信息和数据
        ResultVo resultVo = new ResultVo(totalPage, count, orgStaffRelDtos);

        // 创建HTTP响应实体，设置状态码为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应实体设置到数据流上下文中
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}