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
package com.java110.dev.cmd.dictSpec;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.dev.IDictSpecV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.dict.DictSpecDto;
import java.util.List;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询字典规格列表
 * 服务编码：dictSpec.listDictSpec
 * 请求路径：/app/dictSpec.ListDictSpec
 * add by 吴学文 at 2022-07-07 09:00:30 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "dictSpec.listDictSpec")
public class ListDictSpecCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(ListDictSpecCmd.class);
    
    /**
     * 字典规格服务接口
     */
    @Autowired
    private IDictSpecV1InnerServiceSMO dictSpecV1InnerServiceSMOImpl;

    /**
     * 参数校验方法
     * @param event 命令事件
     * @param cmdDataFlowContext 数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的分页信息校验方法
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法 - 查询字典规格列表
     * @param event 命令事件
     * @param cmdDataFlowContext 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为DictSpecDto对象
        DictSpecDto dictSpecDto = BeanConvertUtil.covertBean(reqJson, DictSpecDto.class);

        // 查询字典规格总数
        int count = dictSpecV1InnerServiceSMOImpl.queryDictSpecsCount(dictSpecDto);

        List<DictSpecDto> dictSpecDtos = null;

        // 根据查询结果处理数据
        if (count > 0) {
            // 如果存在数据，则查询字典规格列表
            dictSpecDtos = dictSpecV1InnerServiceSMOImpl.queryDictSpecs(dictSpecDto);
        } else {
            // 如果没有数据，返回空列表
            dictSpecDtos = new ArrayList<>();
        }

        // 构造返回结果对象
        // 计算总页数：总记录数/每页记录数，向上取整
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, dictSpecDtos);

        // 设置HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应实体设置到数据流上下文中
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}