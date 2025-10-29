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
package com.java110.user.cmd.examine;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.examine.ExamineStaffIntroductionDto;
import com.java110.dto.examine.ExamineStaffProjectDto;
import com.java110.dto.file.FileRelDto;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.user.IExamineStaffIntroductionV1InnerServiceSMO;
import com.java110.intf.user.IExamineStaffProjectV1InnerServiceSMO;
import com.java110.intf.user.IExamineStaffV1InnerServiceSMO;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.examine.ExamineStaffDto;

import java.util.List;
import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询审核人员列表命令类
 * 服务编码：examineStaff.listExamineStaff
 * 请求路劲：/app/examineStaff.ListExamineStaff
 * 功能描述：该类负责处理审核人员列表查询请求，包括查询审核人员基本信息、项目信息和简介信息
 * add by 吴学文 at 2023-03-07 15:57:29 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "examine.listExamineStaff")
public class ListExamineStaffCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListExamineStaffCmd.class);
    
    /**
     * 审核人员服务接口
     */
    @Autowired
    private IExamineStaffV1InnerServiceSMO examineStaffV1InnerServiceSMOImpl;

    /**
     * 审核人员项目服务接口
     */
    @Autowired
    private IExamineStaffProjectV1InnerServiceSMO examineStaffProjectV1InnerServiceSMOImpl;

    /**
     * 审核人员简介服务接口
     */
    @Autowired
    private IExamineStaffIntroductionV1InnerServiceSMO examineStaffIntroductionV1InnerServiceSMOImpl;

    /**
     * 文件关联服务接口
     */
    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 功能描述：验证请求参数的有效性，包括分页参数和必填参数
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证社区ID参数是否为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 命令执行方法
     * 功能描述：执行审核人员列表查询，包括基本信息、项目信息和简介信息的查询和组装
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为审核人员DTO对象
        ExamineStaffDto examineStaffDto = BeanConvertUtil.covertBean(reqJson, ExamineStaffDto.class);

        // 查询审核人员总数
        int count = examineStaffV1InnerServiceSMOImpl.queryExamineStaffsCount(examineStaffDto);

        List<ExamineStaffDto> examineStaffDtos = null;

        if (count > 0) {
            // 查询审核人员列表
            examineStaffDtos = examineStaffV1InnerServiceSMOImpl.queryExamineStaffs(examineStaffDto);

            // 刷新审核人员的项目信息
            freshStaffProjects(examineStaffDtos);
            // 更新审核人员的头像信息
            updatePhone(examineStaffDtos);
            // 刷入审核人员的简介信息
            updateIntroduction(examineStaffDtos);
        } else {
            // 如果没有数据，返回空列表
            examineStaffDtos = new ArrayList<>();
        }

        // 计算总页数并构建返回结果
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, examineStaffDtos);

        // 设置响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 更新审核人员简介信息
     * 功能描述：为审核人员设置简介信息，仅当查询结果为单条记录时设置
     * @param examineStaffDtos 审核人员列表
     */
    private void updateIntroduction(List<ExamineStaffDto> examineStaffDtos) {
        // 仅当查询结果为单条记录时处理简介信息
        if(examineStaffDtos == null || examineStaffDtos.size() != 1){
            return ;
        }

        // 构建简介查询条件
        ExamineStaffIntroductionDto examineStaffIntroductionDto = new ExamineStaffIntroductionDto();
        examineStaffIntroductionDto.setStaffId(examineStaffDtos.get(0).getStaffId());
        examineStaffIntroductionDto.setCommunityId(examineStaffDtos.get(0).getCommunityId());
        
        // 查询审核人员简介信息
        List<ExamineStaffIntroductionDto> introductionDtos = examineStaffIntroductionV1InnerServiceSMOImpl.queryExamineStaffIntroductions(examineStaffIntroductionDto);

        // 如果有简介信息，设置到审核人员对象中
        if(introductionDtos == null || introductionDtos.size() < 1){
            return ;
        }

        examineStaffDtos.get(0).setIntroduction(introductionDtos.get(0).getIntroduction());
    }

    /**
     * 更新审核人员头像信息
     * 功能描述：为审核人员设置头像URL，包括本地路径和网络路径的处理
     * @param examineStaffDtos 审核人员列表
     * @return boolean 处理结果，true表示处理完成或跳过处理
     */
    private boolean updatePhone(List<ExamineStaffDto> examineStaffDtos) {
        // 如果数据量过大，跳过处理
        if (examineStaffDtos.size() > 100) {
            return true;
        }

        // 收集所有审核人员ID
        List<String> esIds = new ArrayList<>();
        for (ExamineStaffDto tmpExamineStaffDto : examineStaffDtos) {
            esIds.add(tmpExamineStaffDto.getEsId());
        }

        // 构建文件查询条件
        FileRelDto fileRelDto = new FileRelDto();
        fileRelDto.setObjIds(esIds.toArray(new String[esIds.size()]));
        
        // 查询文件关联信息
        List<FileRelDto> fileRelDtos = fileRelInnerServiceSMOImpl.queryFileRels(fileRelDto);

        // 如果没有文件关联信息，直接返回
        if (fileRelDtos == null || fileRelDtos.size() < 1) {
            return true;
        }

        // 获取图片域名配置
        String imgUrl = MappingCache.getValue(MappingConstant.FILE_DOMAIN,"IMG_PATH");

        // 为每个审核人员设置头像URL
        for (ExamineStaffDto tmpExamineStaffDto : examineStaffDtos) {
            for (FileRelDto tmpFileRelDto : fileRelDtos) {
                // 匹配审核人员ID和文件关联对象ID
                if (!tmpExamineStaffDto.getEsId().equals(tmpFileRelDto.getObjId())) {
                    continue;
                }

                // 根据文件保存名称判断是网络路径还是本地路径
                if (tmpFileRelDto.getFileSaveName().startsWith("http")) {
                    // 直接使用网络路径
                    tmpExamineStaffDto.setHeaderImg(tmpFileRelDto.getFileSaveName());
                } else {
                    // 拼接本地路径
                    tmpExamineStaffDto.setHeaderImg(imgUrl + tmpFileRelDto.getFileSaveName());
                }
            }
        }

        return false;
    }

    /**
     * 刷新审核人员项目信息
     * 功能描述：为每个审核人员设置相关的项目信息列表
     * @param examineStaffDtos 审核人员列表
     */
    private void freshStaffProjects(List<ExamineStaffDto> examineStaffDtos) {
        // 如果审核人员列表为空，直接返回
        if(examineStaffDtos == null || examineStaffDtos.size() < 1){
            return ;
        }

        // 收集所有审核人员ID
        List<String> esIds = new ArrayList<>();
        for(ExamineStaffDto tmpExamineStaffDto : examineStaffDtos){
            esIds.add(tmpExamineStaffDto.getEsId());
        }

        // 构建项目查询条件
        ExamineStaffProjectDto examineStaffProjectDto = new ExamineStaffProjectDto();
        examineStaffProjectDto.setEsIds(esIds.toArray(new String[esIds.size()]));

        // 批量查询审核人员项目信息
        List<ExamineStaffProjectDto> examineStaffProjectDtos = examineStaffProjectV1InnerServiceSMOImpl.queryExamineStaffProjects(examineStaffProjectDto);

        List<ExamineStaffProjectDto> staffProjectDtos = null;
        
        // 为每个审核人员设置对应的项目列表
        for(ExamineStaffDto tmpExamineStaffDto : examineStaffDtos){
            staffProjectDtos = new ArrayList<>();
            
            // 筛选属于当前审核人员的项目
            for(ExamineStaffProjectDto tmpExamineStaffProjectDto: examineStaffProjectDtos){
                if(tmpExamineStaffProjectDto.getEsId().equals(tmpExamineStaffDto.getEsId())){
                    staffProjectDtos.add(tmpExamineStaffProjectDto);
                }
            }

            // 设置项目列表到审核人员对象
            tmpExamineStaffDto.setProjects(staffProjectDtos);
        }
    }
}