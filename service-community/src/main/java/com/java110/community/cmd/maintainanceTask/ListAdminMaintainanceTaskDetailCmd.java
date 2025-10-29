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
import com.java110.dto.maintainance.MaintainanceTaskDetailDto;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.community.IMaintainanceTaskDetailV1InnerServiceSMO;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import com.java110.vo.api.junkRequirement.PhotoVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 类表述：查询
 * 服务编码：maintainanceTaskDetail.listMaintainanceTaskDetail
 * 请求路劲：/app/maintainanceTaskDetail.ListMaintainanceTaskDetail
 * add by 吴学文 at 2022-11-08 16:02:23 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 维护任务详情管理命令类
 * 负责处理维护任务详情的查询请求，包括分页查询任务详情列表和关联的照片信息
 * 主要功能：验证请求参数、查询任务详情数据、处理照片信息并返回结果
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-11-08
 */
@Java110Cmd(serviceCode = "maintainanceTask.listAdminMaintainanceTaskDetail")
public class ListAdminMaintainanceTaskDetailCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListAdminMaintainanceTaskDetailCmd.class);

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
     * 验证请求参数
     * 验证分页信息和管理员权限
     *
     * @param event               命令事件对象
     * @param cmdDataFlowContext  命令数据流上下文
     * @param reqJson             请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证管理员权限
        super.validateAdmin(cmdDataFlowContext);
    }

    /**
     * 执行命令主方法
     * 查询维护任务详情列表，处理照片信息并返回分页结果
     *
     * @param event               命令事件对象
     * @param cmdDataFlowContext  命令数据流上下文
     * @param reqJson             请求参数JSON对象
     * @throws CmdException       命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为维护任务详情DTO对象
        MaintainanceTaskDetailDto maintainanceTaskDetailDto = BeanConvertUtil.covertBean(reqJson, MaintainanceTaskDetailDto.class);

        // 查询符合条件的维护任务详情总数
        int count = maintainanceTaskDetailV1InnerServiceSMOImpl.queryMaintainanceTaskDetailsCount(maintainanceTaskDetailDto);
        List<MaintainanceTaskDetailDto> maintainanceTaskDetailDtos = null;
        
        if (count > 0) {
            // 如果有数据，查询维护任务详情列表
            maintainanceTaskDetailDtos = maintainanceTaskDetailV1InnerServiceSMOImpl.queryMaintainanceTaskDetails(maintainanceTaskDetailDto);
            // 刷新任务详情的照片信息
            refreshPhotos(maintainanceTaskDetailDtos);
        } else {
            // 如果没有数据，返回空列表
            maintainanceTaskDetailDtos = new ArrayList<>();
        }
        
        // 计算总页数并构建返回结果
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, maintainanceTaskDetailDtos);
        // 设置HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 刷新维护任务详情的照片信息
     * 为状态为"20200407"的任务详情查询并设置关联的照片URL
     *
     * @param maintainanceTaskDetailDtos 维护任务详情列表
     */
    private void refreshPhotos(List<MaintainanceTaskDetailDto> maintainanceTaskDetailDtos) {
        List<PhotoVo> photoVos = null;
        PhotoVo photoVo = null;
        // 从缓存中获取图片域名路径
        String imgUrl = MappingCache.getValue(MappingConstant.FILE_DOMAIN, "IMG_PATH");
        
        // 遍历所有任务详情
        for (MaintainanceTaskDetailDto inspectionTaskDetail : maintainanceTaskDetailDtos) {
            // 只处理状态为"20200407"的任务详情
            if (!"20200407".equals(inspectionTaskDetail.getState())) {
                continue;
            }
            
            // 构建文件关联查询条件
            FileRelDto fileRelDto = new FileRelDto();
            fileRelDto.setObjId(inspectionTaskDetail.getTaskDetailId());
            // 查询该任务详情关联的所有文件
            List<FileRelDto> fileRelDtos = fileRelInnerServiceSMOImpl.queryFileRels(fileRelDto);
            
            photoVos = new ArrayList<>();
            // 处理每个关联的文件
            for (FileRelDto tmpFileRelDto : fileRelDtos) {
                photoVo = new PhotoVo();
                // 构建完整的图片URL：如果是http开头的直接使用，否则拼接域名路径
                photoVo.setUrl(tmpFileRelDto.getFileRealName().startsWith("http") ? 
                              tmpFileRelDto.getFileRealName() : 
                              imgUrl + tmpFileRelDto.getFileRealName());
                photoVos.add(photoVo);
            }
            // 设置任务详情的照片列表
            inspectionTaskDetail.setPhotos(photoVos);
        }
    }
}