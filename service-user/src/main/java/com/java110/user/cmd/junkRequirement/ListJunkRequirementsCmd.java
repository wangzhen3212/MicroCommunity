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

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.file.FileRelDto;
import com.java110.dto.owner.OwnerAppUserDto;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.user.IJunkRequirementInnerServiceSMO;
import com.java110.intf.user.IJunkRequirementV1InnerServiceSMO;
import com.java110.intf.user.IOwnerAppUserInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.api.junkRequirement.ApiJunkRequirementDataVo;
import com.java110.vo.api.junkRequirement.ApiJunkRequirementVo;
import com.java110.vo.api.junkRequirement.PhotoVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.itemRelease.JunkRequirementDto;
import java.util.List;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询
 * 服务编码：junkRequirement.listJunkRequirement
 * 请求路劲：/app/junkRequirement.ListJunkRequirement
 * add by 吴学文 at 2022-08-08 08:53:50 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 垃圾需求列表查询命令类
 * 负责处理垃圾需求信息的查询请求，包括分页查询、图片信息获取和用户信息关联等功能
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-08-08
 */
@Java110Cmd(serviceCode = "junkRequirement.listJunkRequirements")
public class ListJunkRequirementsCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListJunkRequirementsCmd.class);
    
    /**
     * 垃圾需求V1版本内部服务接口
     */
    @Autowired
    private IJunkRequirementV1InnerServiceSMO junkRequirementV1InnerServiceSMOImpl;

    /**
     * 垃圾需求内部服务接口
     */
    @Autowired
    private IJunkRequirementInnerServiceSMO junkRequirementInnerServiceSMOImpl;
    
    /**
     * 文件关联内部服务接口
     */
    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    /**
     * 业主应用用户内部服务接口
     */
    @Autowired
    private IOwnerAppUserInnerServiceSMO ownerAppUserInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性，主要验证分页参数
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的分页信息验证方法
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 处理垃圾需求列表查询业务逻辑，包括数据查询、分页处理和响应返回
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为垃圾需求DTO对象
        JunkRequirementDto junkRequirementDto = BeanConvertUtil.covertBean(reqJson, JunkRequirementDto.class);

        // 查询垃圾需求总数
        int count = junkRequirementInnerServiceSMOImpl.queryJunkRequirementsCount(junkRequirementDto);

        List<ApiJunkRequirementDataVo> junkRequirements = null;

        if (count > 0) {
            // 查询垃圾需求列表数据并转换为VO对象
            junkRequirements = BeanConvertUtil.covertBeanList(
                junkRequirementInnerServiceSMOImpl.queryJunkRequirements(junkRequirementDto), 
                ApiJunkRequirementDataVo.class);
            // 刷新图片和业主信息
            refreshPhotoAndOwners(junkRequirements);
        } else {
            // 如果没有数据，返回空列表
            junkRequirements = new ArrayList<>();
        }

        // 构建返回VO对象
        ApiJunkRequirementVo apiJunkRequirementVo = new ApiJunkRequirementVo();
        apiJunkRequirementVo.setTotal(count);
        // 计算总页数
        apiJunkRequirementVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        apiJunkRequirementVo.setJunkRequirements(junkRequirements);

        // 构建HTTP响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(
            JSONObject.toJSONString(apiJunkRequirementVo), HttpStatus.OK);
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 刷新图片和业主信息方法
     * 为垃圾需求数据补充图片URL和业主成员ID信息
     *
     * @param junkRequirements 垃圾需求数据列表
     */
    private void refreshPhotoAndOwners(List<ApiJunkRequirementDataVo> junkRequirements) {
        List<PhotoVo> photoVos = null;
        PhotoVo photoVo = null;

        // 如果列表为空，直接返回
        if (junkRequirements == null || junkRequirements.size() < 1) {
            return;
        }

        // 收集所有发布用户ID
        List<String> userIds = new ArrayList<>();
        for (ApiJunkRequirementDataVo junkRequirementDataVo : junkRequirements) {
            // 查询文件关联信息
            FileRelDto fileRelDto = new FileRelDto();
            fileRelDto.setObjId(junkRequirementDataVo.getJunkRequirementId());
            List<FileRelDto> fileRelDtos = fileRelInnerServiceSMOImpl.queryFileRels(fileRelDto);
            
            // 构建图片VO列表
            photoVos = new ArrayList<>();
            for (FileRelDto tmpFileRelDto : fileRelDtos) {
                photoVo = new PhotoVo();
                // 构建图片访问URL
                photoVo.setUrl("/callComponent/download/getFile/file?fileId=" + 
                    tmpFileRelDto.getFileRealName() + "&communityId=" + 
                    junkRequirementDataVo.getCommunityId());
                photoVos.add(photoVo);
            }

            // 设置图片列表到垃圾需求数据
            junkRequirementDataVo.setPhotos(photoVos);
            // 收集用户ID用于后续查询
            userIds.add(junkRequirementDataVo.getPublishUserId());
        }

        // 查询业主应用用户信息
        OwnerAppUserDto ownerAppUserDto = new OwnerAppUserDto();
        ownerAppUserDto.setCommunityId(junkRequirements.get(0).getCommunityId());
        ownerAppUserDto.setUserIds(userIds.toArray(new String[userIds.size()]));

        List<OwnerAppUserDto> ownerAppUserDtos = ownerAppUserInnerServiceSMOImpl.queryOwnerAppUsers(ownerAppUserDto);
        if (ownerAppUserDtos == null || ownerAppUserDtos.size() < 1) {
            return;
        }

        // 关联业主成员ID到垃圾需求数据
        for (OwnerAppUserDto tmpOwnerAppUserdto : ownerAppUserDtos) {
            for (ApiJunkRequirementDataVo apiJunkRequirementDataVo : junkRequirements) {
                if (tmpOwnerAppUserdto.getUserId().equals(apiJunkRequirementDataVo.getPublishUserId())) {
                    apiJunkRequirementDataVo.setMemberId(tmpOwnerAppUserdto.getMemberId());
                }
            }
        }
    }
}