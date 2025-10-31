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
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.dict.DictSpecDto;
import com.java110.intf.dev.IDictSpecV1InnerServiceSMO;
import com.java110.intf.dev.IDictV1InnerServiceSMO;
import com.java110.po.dict.DictPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 类表述：保存字典数据
 * 服务编码：dict.saveDict
 * 请求路径：/app/dict.SaveDict
 * add by 吴学文 at 2022-07-06 20:03:58 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "dict.saveDict")
public class SaveDictCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveDictCmd.class);

    /**
     * ID前缀常量
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 字典服务接口
     */
    @Autowired
    private IDictV1InnerServiceSMO dictV1InnerServiceSMOImpl;

    /**
     * 字典规格服务接口
     */
    @Autowired
    private IDictSpecV1InnerServiceSMO dictSpecV1InnerServiceSMOImpl;

    /**
     * 参数校验方法
     * 
     * @param event 命令事件
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 校验必填参数
        Assert.hasKeyAndValue(reqJson, "statusCd", "请求报文中未包含状态码");
        Assert.hasKeyAndValue(reqJson, "name", "请求报文中未包含字典名称");
        Assert.hasKeyAndValue(reqJson, "specId", "请求报文中未包含字典规格ID");
    }

    /**
     * 执行保存字典命令
     * 主要流程：
     * 1. 根据规格ID查询字典规格信息
     * 2. 设置字典数据的表名和表字段
     * 3. 保存字典数据
     * 4. 返回操作结果
     * 
     * @param event 命令事件
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 命令异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 根据规格ID查询字典规格信息
        DictSpecDto dictSpecDto = new DictSpecDto();
        dictSpecDto.setSpecId(reqJson.getString("specId"));
        List<DictSpecDto> dictSpecDtos = dictSpecV1InnerServiceSMOImpl.queryDictSpecs(dictSpecDto);

        // 验证查询结果唯一性
        Assert.listOnlyOne(dictSpecDtos, "未查询到对应的字典规格信息或查询到多条记录");

        // 转换请求参数为字典PO对象
        DictPo dictPo = BeanConvertUtil.covertBean(reqJson, DictPo.class);
        // 设置表名和表字段信息
        dictPo.setTableName(dictSpecDtos.get(0).getTableName());
        dictPo.setTableColumns(dictSpecDtos.get(0).getTableColumns());
        
        // 保存字典数据
        int flag = dictV1InnerServiceSMOImpl.saveDict(dictPo);

        // 检查保存结果
        if (flag < 1) {
            throw new CmdException("保存字典数据失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}