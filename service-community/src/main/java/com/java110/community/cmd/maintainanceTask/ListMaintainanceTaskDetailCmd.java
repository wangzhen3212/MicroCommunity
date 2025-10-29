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
package com.java110.community.cmd.maintainanceTask;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.file.FileRelDto;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.community.IMaintainanceTaskDetailV1InnerServiceSMO;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import com.java110.vo.api.junkRequirement.PhotoVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.maintainance.MaintainanceTaskDetailDto;

import java.util.List;
import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 维护任务详情查询命令类
 * 
 * 该类负责处理维护任务详情的列表查询请求，包括数据验证、业务处理和数据返回。
 * 主要功能包括查询维护任务详情列表、分页处理、图片信息刷新等。
 * 
 * 服务编码：maintainanceTask.listMaintainanceTaskDetail
 * 请求路径：/app/maintainanceTaskDetail.ListMaintainanceTaskDetail
 * 
 * @author 吴学文 at 2022-11-08 16:02:23 mail: 928255095@qq.com
 * @version 1.0
 * 
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "maintainanceTask.listMaintainanceTaskDetail")
public class ListMaintainanceTaskDetailCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListMaintainanceTaskDetailCmd.class);

    /**
     * 维护任务详情服务接口
     */
    @Autowired
    private IMaintainanceTaskDetailV1InnerServiceSMO maintainanceTaskDetailV1InnerServiceSMOImpl;

    /**
     * 文件关联服务接口
     */
    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数的有效性，包括分页参数和必要字段的检查
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证小区ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
    }

    /**
     * 命令执行方法
     * 
     * 处理维护任务详情的查询请求，包括数据查询、分页处理和图片信息刷新
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为维护任务详情DTO对象
        MaintainanceTaskDetailDto maintainanceTaskDetailDto = BeanConvertUtil.covertBean(reqJson, MaintainanceTaskDetailDto.class);
        
        // 查询维护任务详情总数
        int count = maintainanceTaskDetailV1InnerServiceSMOImpl.queryMaintainanceTaskDetailsCount(maintainanceTaskDetailDto);
        
        List<MaintainanceTaskDetailDto> maintainanceTaskDetailDtos = null;
        
        // 根据查询结果数量处理数据
        if (count > 0) {
            // 查询维护任务详情列表
            maintainanceTaskDetailDtos = maintainanceTaskDetailV1InnerServiceSMOImpl.queryMaintainanceTaskDetails(maintainanceTaskDetailDto);
            // 刷新图片信息
            refreshPhotos(maintainanceTaskDetailDtos);
        } else {
            // 如果没有数据，返回空列表
            maintainanceTaskDetailDtos = new ArrayList<>();
        }
        
        // 计算总页数并构建返回结果
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, maintainanceTaskDetailDtos);
        
        // 设置HTTP响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 刷新维护任务详情图片信息
     * 
     * 为符合条件的维护任务详情查询并设置相关的图片信息
     * 
     * @param maintainanceTaskDetailDtos 维护任务详情列表
     */
    private void refreshPhotos(List<MaintainanceTaskDetailDto> maintainanceTaskDetailDtos) {
        List<PhotoVo> photoVos = null;
        PhotoVo photoVo = null;
        
        // 获取图片域名配置
        String imgUrl = MappingCache.getValue(MappingConstant.FILE_DOMAIN, "IMG_PATH");
        
        // 遍历维护任务详情列表
        for (MaintainanceTaskDetailDto inspectionTaskDetail : maintainanceTaskDetailDtos) {
            // 只处理状态为"20200407"的任务详情
            if (!"20200407".equals(inspectionTaskDetail.getState())) {
                continue;
            }
            
            // 构建文件关联查询条件
            FileRelDto fileRelDto = new FileRelDto();
            fileRelDto.setObjId(inspectionTaskDetail.getTaskDetailId());
            
            // 查询文件关联信息
            List<FileRelDto> fileRelDtos = fileRelInnerServiceSMOImpl.queryFileRels(fileRelDto);
            
            photoVos = new ArrayList<>();
            // 处理每个文件关联信息
            for (FileRelDto tmpFileRelDto : fileRelDtos) {
                photoVo = new PhotoVo();
                // 设置图片URL，如果已经是完整URL则直接使用，否则拼接域名
                photoVo.setUrl(tmpFileRelDto.getFileRealName().startsWith("http") ? 
                              tmpFileRelDto.getFileRealName() : imgUrl + tmpFileRelDto.getFileRealName());
                photoVos.add(photoVo);
            }
            
            // 将图片信息设置到任务详情中
            inspectionTaskDetail.setPhotos(photoVos);
        }
    }
}