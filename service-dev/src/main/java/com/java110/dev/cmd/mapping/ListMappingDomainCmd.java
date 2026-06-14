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
package com.java110.dev.cmd.mapping;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.dev.IMappingDomainV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.mapping.MappingDomainDto;
import java.util.List;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询映射域列表
 * 服务编码：mappingDomain.listMappingDomain
 * 请求路径：/app/mappingDomain.ListMappingDomain
 * add by 吴学文 at 2023-01-29 14:09:04 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "mapping.listMappingDomain")
public class ListMappingDomainCmd extends Cmd {

    // 日志记录器
    private static Logger logger = LoggerFactory.getLogger(ListMappingDomainCmd.class);
    
    // 映射域服务接口
    @Autowired
    private IMappingDomainV1InnerServiceSMO mappingDomainV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 主要验证分页参数
     * @param event 命令事件
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        super.validatePageInfo(reqJson);
    }

    /**
     * 执行命令方法
     * 查询映射域列表数据，支持分页查询
     * @param event 命令事件
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 将请求参数转换为映射域DTO对象
        MappingDomainDto mappingDomainDto = BeanConvertUtil.covertBean(reqJson, MappingDomainDto.class);

        // 查询映射域总记录数
        int count = mappingDomainV1InnerServiceSMOImpl.queryMappingDomainsCount(mappingDomainDto);

        // 映射域数据列表
        List<MappingDomainDto> mappingDomainDtos = null;

        // 如果存在数据，则查询数据列表；否则返回空列表
        if (count > 0) {
            mappingDomainDtos = mappingDomainV1InnerServiceSMOImpl.queryMappingDomains(mappingDomainDto);
        } else {
            mappingDomainDtos = new ArrayList<>();
        }

        // 计算总页数并构造返回结果
        // 总页数 = ceil(总记录数 / 每页记录数)
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, mappingDomainDtos);

        // 设置HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应设置到数据流上下文中
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}