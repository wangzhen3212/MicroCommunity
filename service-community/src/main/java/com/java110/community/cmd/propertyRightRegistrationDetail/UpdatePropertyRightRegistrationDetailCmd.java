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
package com.java110.community.cmd.propertyRightRegistrationDetail;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.file.FileRelDto;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.community.IPropertyRightRegistrationDetailV1InnerServiceSMO;
import com.java110.intf.community.IPropertyRightRegistrationV1InnerServiceSMO;
import com.java110.po.file.FileRelPo;
import com.java110.po.room.PropertyRightRegistrationPo;
import com.java110.po.room.PropertyRightRegistrationDetailPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;

import java.util.Date;
import java.util.List;

/**
 * 产权登记详情更新命令类
 * 
 * 该类负责处理产权登记详情的更新操作，包括验证材料信息、更新产权登记详情数据、
 * 更新产权登记状态以及处理相关文件的上传和删除操作。
 * 
 * 服务编码：propertyRightRegistrationDetail.updatePropertyRightRegistrationDetail
 * 请求路径：/app/propertyRightRegistrationDetail.UpdatePropertyRightRegistrationDetail
 * 
 * @author 吴学文 at 2021-10-11 09:15:52 mail: 928255095@qq.com
 * @open source address: https://gitee.com/wuxw7/MicroCommunity
 * @官网：http://www.homecommunity.cn
 * @温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "propertyRightRegistrationDetail.updatePropertyRightRegistrationDetail")
public class UpdatePropertyRightRegistrationDetailCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(UpdatePropertyRightRegistrationDetailCmd.class);

    /** 产权登记详情服务接口 */
    @Autowired
    private IPropertyRightRegistrationDetailV1InnerServiceSMO propertyRightRegistrationDetailV1InnerServiceSMOImpl;

    /** 产权登记服务接口 */
    @Autowired
    private IPropertyRightRegistrationV1InnerServiceSMO propertyRightRegistrationV1InnerServiceSMOImpl;

    /** 文件关联服务接口 */
    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证prrId参数不能为空
        Assert.hasKeyAndValue(reqJson, "prrId", "prrId不能为空");
        // 验证communityId参数不能为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行产权登记详情更新命令
     * 
     * 该方法负责处理产权登记详情的完整更新流程，包括：
     * 1. 验证各种材料信息的完整性
     * 2. 更新产权登记详情数据
     * 3. 更新产权登记状态为未审核
     * 4. 处理相关文件的上传和删除
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当业务处理失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 获取材料类型(001身份证 002购房合同 003维修基金 004契税)
        String securities = reqJson.getString("securities");
        
        // 验证身份证照片信息
        if (securities.equals("001") && reqJson.containsKey("idCardPhotos") && !StringUtil.isEmpty(reqJson.getString("idCardPhotos"))) {
            String idCardPhotos = reqJson.getString("idCardPhotos");
            JSONArray objects = JSONArray.parseArray(idCardPhotos);
            // 身份证必须上传正反两张照片
            if (objects.size() != 2 || idCardPhotos.equals("[]")) {
                throw new CmdException("请上传身份证正反两张照片！");
            }
        }
        
        // 验证购房合同图片信息
        if (securities.equals("002") && reqJson.containsKey("housePurchasePhotos") && !StringUtil.isEmpty(reqJson.getString("housePurchasePhotos"))) {
            String housePurchasePhotos = reqJson.getString("housePurchasePhotos");
            // 购房合同图片不能为空
            if (housePurchasePhotos.length() < 1 || housePurchasePhotos.equals("[]")) {
                throw new CmdException("购房合同图片不能为空！");
            }
        }
        
        // 验证维修基金标识和图片
        if (securities.equals("003") && reqJson.containsKey("isTrue") && reqJson.getString("isTrue").equals("true")) {
            String repairPhotos = reqJson.getString("repairPhotos");
            // 维修基金图片不能为空
            if (repairPhotos.length() < 1 || repairPhotos.equals("[]")) {
                throw new CmdException("维修基金图片不能为空！");
            }
        } else if (securities.equals("003") && reqJson.containsKey("isTrue") && reqJson.getString("isTrue").equals("false")) {
            // 当维修基金为空时，清空维修基金图片并删除相关文件记录
            reqJson.put("repairPhotos", null);
            FileRelDto fileRelDto = new FileRelDto();
            fileRelDto.setObjId(reqJson.getString("prrId"));
            fileRelDto.setRelTypeCd("26000"); // 26000表示维修基金图片类型
            List<FileRelDto> fileRelDtos = fileRelInnerServiceSMOImpl.queryFileRels(fileRelDto);
            // 删除所有相关的维修基金文件记录
            if (fileRelDtos != null && fileRelDtos.size() > 0) {
                for (FileRelDto file : fileRelDtos) {
                    FileRelPo fileRel = new FileRelPo();
                    fileRel.setFileRelId(file.getFileRelId());
                    fileRelInnerServiceSMOImpl.deleteFileRel(fileRel);
                }
            }
        }
        
        // 验证契税标识和图片
        if (securities.equals("004") && reqJson.containsKey("isTrue") && reqJson.getString("isTrue").equals("true")) {
            String deedTaxPhotos = reqJson.getString("deedTaxPhotos");
            // 契税证明图片不能为空
            if (deedTaxPhotos.length() < 1 || deedTaxPhotos.equals("[]")) {
                throw new CmdException("契税证明图片不能为空！");
            }
        } else if (securities.equals("004") && reqJson.containsKey("isTrue") && reqJson.getString("isTrue").equals("false")) {
            // 当契税为空时，清空契税图片并删除相关文件记录
            reqJson.put("deedTaxPhotos", null);
            FileRelDto fileRelDto = new FileRelDto();
            fileRelDto.setObjId(reqJson.getString("prrId"));
            fileRelDto.setRelTypeCd("27000"); // 27000表示契税图片类型
            List<FileRelDto> fileRelDtos = fileRelInnerServiceSMOImpl.queryFileRels(fileRelDto);
            // 删除所有相关的契税文件记录
            if (fileRelDtos != null && fileRelDtos.size() > 0) {
                for (FileRelDto file : fileRelDtos) {
                    FileRelPo fileRel = new FileRelPo();
                    fileRel.setFileRelId(file.getFileRelId());
                    fileRelInnerServiceSMOImpl.deleteFileRel(fileRel);
                }
            }
        }
        
        // 将请求JSON数据转换为产权登记详情PO对象
        PropertyRightRegistrationDetailPo propertyRightRegistrationDetailPo = BeanConvertUtil.covertBean(reqJson, PropertyRightRegistrationDetailPo.class);
        // 更新产权登记详情数据
        int flag = propertyRightRegistrationDetailV1InnerServiceSMOImpl.updatePropertyRightRegistrationDetail(propertyRightRegistrationDetailPo);

        // 检查更新是否成功
        if (flag < 1) {
            throw new CmdException("更新详情数据失败");
        }
        
        // 产权登记详情修改成功后，产权登记状态要变为未审核状态
        PropertyRightRegistrationPo propertyRightRegistrationPo = new PropertyRightRegistrationPo();
        propertyRightRegistrationPo.setPrrId(reqJson.getString("prrId"));
        propertyRightRegistrationPo.setState("0"); // 0表示未审核状态
        int newFlag = propertyRightRegistrationV1InnerServiceSMOImpl.updatePropertyRightRegistration(propertyRightRegistrationPo);
        // 检查状态更新是否成功
        if (newFlag < 1) {
            throw new CmdException("更新数据失败");
        }
        
        // 初始化文件关联PO对象
        FileRelPo fileRelPo = new FileRelPo();
        fileRelPo.setObjId(propertyRightRegistrationDetailPo.getPrrId());
        // table表示表存储 ftp表示ftp文件存储
        fileRelPo.setSaveWay("ftp");
        fileRelPo.setCreateTime(new Date());
        
        // 处理身份证图片上传
        List<String> idCardPhotos = propertyRightRegistrationDetailPo.getIdCardPhotos();
        if (idCardPhotos != null && idCardPhotos.size() > 0) {
            // 查询并删除原有的身份证图片记录
            FileRelDto fileRelDto = new FileRelDto();
            fileRelDto.setObjId(propertyRightRegistrationDetailPo.getPrrId());
            fileRelDto.setRelTypeCd("60000"); // 60000表示身份证图片类型
            List<FileRelDto> fileRelDtos = fileRelInnerServiceSMOImpl.queryFileRels(fileRelDto);
            if (fileRelDtos != null && fileRelDtos.size() > 0) {
                for (FileRelDto file : fileRelDtos) {
                    FileRelPo fileRel = new FileRelPo();
                    fileRel.setFileRelId(file.getFileRelId());
                    fileRelInnerServiceSMOImpl.deleteFileRel(fileRel);
                }
            }
            // 保存新的身份证图片记录
            fileRelPo.setRelTypeCd("60000"); // 60000表示身份证图片
            for (String photo : idCardPhotos) {
                fileRelPo.setFileRelId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_relId));
                fileRelPo.setFileRealName(photo);
                fileRelPo.setFileSaveName(photo);
                fileRelInnerServiceSMOImpl.saveFileRel(fileRelPo);
            }
        }
        
        // 处理购房合同照片上传
        List<String> housePurchasePhotos = propertyRightRegistrationDetailPo.getHousePurchasePhotos();
        if (housePurchasePhotos != null && housePurchasePhotos.size() > 0) {
            // 查询并删除原有的购房合同图片记录
            FileRelDto fileRelDto = new FileRelDto();
            fileRelDto.setObjId(propertyRightRegistrationDetailPo.getPrrId());
            fileRelDto.setRelTypeCd("25000"); // 25000表示购房合同图片类型
            List<FileRelDto> fileRelDtos = fileRelInnerServiceSMOImpl.queryFileRels(fileRelDto);
            if (fileRelDtos != null && fileRelDtos.size() > 0) {
                for (FileRelDto file : fileRelDtos) {
                    FileRelPo fileRel = new FileRelPo();
                    fileRel.setFileRelId(file.getFileRelId());
                    fileRelInnerServiceSMOImpl.deleteFileRel(fileRel);
                }
            }
            // 保存新的购房合同图片记录
            fileRelPo.setRelTypeCd("25000"); // 25000表示购房合同图片
            for (String photo : housePurchasePhotos) {
                fileRelPo.setFileRelId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_relId));
                fileRelPo.setFileRealName(photo);
                fileRelPo.setFileSaveName(photo);
                fileRelInnerServiceSMOImpl.saveFileRel(fileRelPo);
            }
        }
        
        // 处理维修基金图片上传
        List<String> repairPhotos = propertyRightRegistrationDetailPo.getRepairPhotos();
        if (repairPhotos != null && repairPhotos.size() > 0) {
            // 查询并删除原有的维修基金图片记录
            FileRelDto fileRelDto = new FileRelDto();
            fileRelDto.setObjId(propertyRightRegistrationDetailPo.getPrrId());
            fileRelDto.setRelTypeCd("26000"); // 26000表示维修基金图片类型
            List<FileRelDto> fileRelDtos = fileRelInnerServiceSMOImpl.queryFileRels(fileRelDto);
            if (fileRelDtos != null && fileRelDtos.size() > 0) {
                for (FileRelDto file : fileRelDtos) {
                    FileRelPo fileRel = new FileRelPo();
                    fileRel.setFileRelId(file.getFileRelId());
                    fileRelInnerServiceSMOImpl.deleteFileRel(fileRel);
                }
            }
            // 保存新的维修基金图片记录
            fileRelPo.setRelTypeCd("26000"); // 26000表示维修基金图片
            for (String photo : repairPhotos) {
                fileRelPo.setFileRelId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_relId));
                fileRelPo.setFileRealName(photo);
                fileRelPo.setFileSaveName(photo);
                fileRelInnerServiceSMOImpl.saveFileRel(fileRelPo);
            }
        }
        
        // 处理契税证明图片上传
        List<String> deedTaxPhotos = propertyRightRegistrationDetailPo.getDeedTaxPhotos();
        if (deedTaxPhotos != null && deedTaxPhotos.size() > 0) {
            // 查询并删除原有的契税图片记录
            FileRelDto fileRelDto = new FileRelDto();
            fileRelDto.setObjId(propertyRightRegistrationDetailPo.getPrrId());
            fileRelDto.setRelTypeCd("27000"); // 27000表示契税图片类型
            List<FileRelDto> fileRelDtos = fileRelInnerServiceSMOImpl.queryFileRels(fileRelDto);
            if (fileRelDtos != null && fileRelDtos.size() > 0) {
                for (FileRelDto file : fileRelDtos) {
                    FileRelPo fileRel = new FileRelPo();
                    fileRel.setFileRelId(file.getFileRelId());
                    fileRelInnerServiceSMOImpl.deleteFileRel(fileRel);
                }
            }
            // 保存新的契税图片记录
            fileRelPo.setRelTypeCd("27000"); // 27000表示契税图片
            for (String photo : deedTaxPhotos) {
                fileRelPo.setFileRelId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_relId));
                fileRelPo.setFileRealName(photo);
                fileRelPo.setFileSaveName(photo);
                fileRelInnerServiceSMOImpl.saveFileRel(fileRelPo);
            }
        }
        
        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}