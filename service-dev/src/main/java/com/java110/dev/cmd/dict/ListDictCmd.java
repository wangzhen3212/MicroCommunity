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
package com.java110.dev.cmd.dict;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.dict.DictDto;
import com.java110.intf.dev.IDictV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询字典列表命令
 * 服务编码：dict.listDict
 * 请求路径：/app/dict.ListDict
 * add by 吴学文 at 2022-07-06 20:03:58 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "dict.listDict")
public class ListDictCmd extends Cmd {

    // 日志记录器
    private static Logger logger = LoggerFactory.getLogger(ListDictCmd.class);
    
    // 字典服务接口
    @Autowired
    private IDictV1InnerServiceSMO dictV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性
     * 
     * @param event 命令事件
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的分页信息验证方法
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 处理字典列表查询业务逻辑
     * 
     * @param event 命令事件
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为字典数据传输对象
        DictDto dictDto = BeanConvertUtil.covertBean(reqJson, DictDto.class);

        // 查询字典和规格的总记录数
        int count = dictV1InnerServiceSMOImpl.queryDictsAndSpecCount(dictDto);

        List<DictDto> dictDtos = null;

        // 根据查询结果处理数据
        if (count > 0) {
            // 如果存在记录，查询字典和规格的详细信息
            dictDtos = dictV1InnerServiceSMOImpl.queryDictAndSpecs(dictDto);
        } else {
            // 如果没有记录，返回空列表
            dictDtos = new ArrayList<>();
        }

        // 计算总页数并构建返回结果
        // 注意：这里使用Math.ceil进行向上取整，确保分页正确
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, dictDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应实体到数据流上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}