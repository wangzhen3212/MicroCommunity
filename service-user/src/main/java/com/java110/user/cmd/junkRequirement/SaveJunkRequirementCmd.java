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
package com.java110.user.cmd.junkRequirement;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.DataFlowContext;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.file.FileDto;
import com.java110.intf.common.IFileInnerServiceSMO;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.user.IJunkRequirementV1InnerServiceSMO;
import com.java110.po.file.FileRelPo;
import com.java110.po.junkRequirement.JunkRequirementPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：保存
 * 服务编码：junkRequirement.saveJunkRequirement
 * 请求路劲：/app/junkRequirement.SaveJunkRequirement
 * add by 吴学文 at 2022-08-08 08:53:50 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 垃圾需求保存命令类
 * 
 * 该类负责处理垃圾需求信息的保存操作，包括参数验证、数据转换、图片处理和数据持久化等功能。
 * 通过注解标识服务编码，与前端请求路径对应，实现垃圾需求信息的完整保存流程。
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-08-08
 */
@Java110Cmd(serviceCode = "junkRequirement.saveJunkRequirement")
public class SaveJunkRequirementCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveJunkRequirementCmd.class);

    /**
     * 代码前缀ID常量，用于生成垃圾需求ID
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 垃圾需求服务接口
     */
    @Autowired
    private IJunkRequirementV1InnerServiceSMO junkRequirementV1InnerServiceSMOImpl;

    /**
     * 文件关联服务接口
     */
    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    /**
     * 文件服务接口
     */
    @Autowired
    private IFileInnerServiceSMO fileInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段，确保请求数据的完整性。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置上下文信息
     * @param reqJson 请求JSON对象，包含前端传递的参数
     * @throws IllegalArgumentException 当必要参数缺失时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填字段是否存在
        Assert.hasKeyAndValue(reqJson, "typeCd", "请求报文中未包含typeCd");
        Assert.hasKeyAndValue(reqJson, "classification", "请求报文中未包含classification");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        Assert.hasKeyAndValue(reqJson, "context", "请求报文中未包含context");
        Assert.hasKeyAndValue(reqJson, "referencePrice", "请求报文中未包含referencePrice");
        Assert.hasKeyAndValue(reqJson, "publishUserId", "请求报文中未包含publishUserId");
        Assert.hasKeyAndValue(reqJson, "publishUserName", "请求报文中未包含publishUserName");
        Assert.hasKeyAndValue(reqJson, "publishUserLink", "请求报文中未包含publishUserLink");
    }

    /**
     * 执行命令方法
     * 
     * 处理垃圾需求保存的核心业务逻辑，包括状态设置、ID生成、图片处理和数据持久化。
     * 使用事务注解确保数据操作的原子性。
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 设置垃圾需求状态为待处理状态
        //reqJson.put("state", "12001");
        reqJson.put("state", "13001");
        
        // 生成垃圾需求ID
        String junkRequirementId = GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_junkRequirementId);
        reqJson.put("junkRequirementId", junkRequirementId);

        // 如果请求中包含图片，处理图片信息
        if (reqJson.containsKey("photos")) {
            dealPhotos(reqJson);
        }

        // 将JSON对象转换为实体对象
        JunkRequirementPo junkRequirementPo = BeanConvertUtil.covertBean(reqJson, JunkRequirementPo.class);
        // 设置垃圾需求ID
        junkRequirementPo.setJunkRequirementId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 调用服务保存垃圾需求信息
        int flag = junkRequirementV1InnerServiceSMOImpl.saveJunkRequirement(junkRequirementPo);

        // 检查保存结果
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }

    /**
     * 处理图片信息方法
     * 
     * 遍历请求中的图片数组，保存图片文件信息并建立文件关联关系。
     * 包括文件保存和文件关联记录创建两个主要步骤。
     * 
     * @param reqJson 请求JSON对象，包含图片数组信息
     * @throws CmdException 当保存图片关联关系失败时抛出异常
     */
    private void dealPhotos(JSONObject reqJson) {
        // 获取图片数组
        JSONArray photos = reqJson.getJSONArray("photos");
        JSONObject photo = null;
        int flag = 0;
        
        // 遍历所有图片
        for (int photoIndex = 0; photoIndex < photos.size(); photoIndex++) {
            photo = photos.getJSONObject(photoIndex);
            
            // 创建文件DTO对象并设置文件信息
            FileDto fileDto = new FileDto();
            fileDto.setFileId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_file_id));
            fileDto.setFileName(fileDto.getFileId());
            fileDto.setContext(photo.getString("photo")); // 图片base64编码内容
            fileDto.setSuffix("jpeg"); // 文件后缀
            fileDto.setCommunityId(reqJson.getString("communityId")); // 小区ID
            
            // 保存文件并获取文件名
            String fileName = fileInnerServiceSMOImpl.saveFile(fileDto);
            reqJson.put("photoId", fileDto.getFileId());
            reqJson.put("fileSaveName", fileName);

            // 创建文件关联业务对象
            JSONObject businessUnit = new JSONObject();
            businessUnit.put("fileRelId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_fileRelId));
            businessUnit.put("relTypeCd", "80000"); // 关联类型编码
            businessUnit.put("saveWay", "ftp"); // 保存方式
            businessUnit.put("objId", reqJson.getString("junkRequirementId")); // 关联对象ID
            businessUnit.put("fileRealName", reqJson.getString("photoId")); // 文件真实名称
            businessUnit.put("fileSaveName", reqJson.getString("fileSaveName")); // 文件保存名称
            
            // 转换为文件关联PO对象
            FileRelPo fileRelPo = BeanConvertUtil.covertBean(businessUnit, FileRelPo.class);

            // 保存文件关联关系
            flag = fileRelInnerServiceSMOImpl.saveFileRel(fileRelPo);
            if(flag <1){
                throw new CmdException("报错图片异常");
            }
        }
    }
}