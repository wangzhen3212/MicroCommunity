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
package com.java110.community.cmd.visit;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.parking.ParkingAreaDto;
import com.java110.intf.community.IParkingAreaInnerServiceSMO;
import com.java110.intf.community.IVisitSettingV1InnerServiceSMO;
import com.java110.po.visit.VisitSettingPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 访客设置更新命令类
 * 
 * 该类负责处理访客设置信息的更新操作，包括参数验证、业务逻辑处理和数据库更新。
 * 服务编码：visit.updateVisitSetting
 * 请求路径：/app/visitSetting.UpdateVisitSetting
 * 
 * @author 吴学文 at 2023-01-18 14:43:29 mail: 928255095@qq.com
 * @open source address: https://gitee.com/wuxw7/MicroCommunity
 * @官网：http://www.homecommunity.cn
 * @温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "visit.updateVisitSetting")
public class UpdateVisitSettingCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(UpdateVisitSettingCmd.class);

    /**
     * 访客设置服务接口
     */
    @Autowired
    private IVisitSettingV1InnerServiceSMO visitSettingV1InnerServiceSMOImpl;

    /**
     * 停车场区域服务接口
     */
    @Autowired
    private IParkingAreaInnerServiceSMO parkingAreaInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段：settingId和communityId
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含settingId字段
        Assert.hasKeyAndValue(reqJson, "settingId", "settingId不能为空");
        // 验证请求参数中必须包含communityId字段
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行更新访客设置命令
     * 
     * 处理访客设置的更新逻辑，包括参数处理、停车场信息验证和数据库更新操作
     * 该方法使用事务注解确保数据一致性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为访客设置POJO对象
        VisitSettingPo visitSettingPo = BeanConvertUtil.covertBean(reqJson, VisitSettingPo.class);
        
        // 处理访客数量字段，如果为空则设置为默认值0
        if (StringUtil.isEmpty(reqJson.getString("visitNumber"))) {
            visitSettingPo.setVisitNumber("0");
        }
        
        // 如果请求中包含停车场ID，则验证停车场信息并设置停车场编号
        if (reqJson.containsKey("paId")) {
            // 创建停车场查询条件对象
            ParkingAreaDto parkingAreaDto = new ParkingAreaDto();
            parkingAreaDto.setCommunityId(reqJson.getString("communityId"));
            parkingAreaDto.setPaId(reqJson.getString("paId"));
            
            // 查询停车场信息
            List<ParkingAreaDto> parkingAreaDtos = parkingAreaInnerServiceSMOImpl.queryParkingAreas(parkingAreaDto);
            
            // 验证查询结果，确保只存在一个匹配的停车场
            Assert.listOnlyOne(parkingAreaDtos, "停车场不存在");
            
            // 设置访客设置中的停车场编号
            visitSettingPo.setPaNum(parkingAreaDtos.get(0).getNum());
        }
        
        // 执行数据库更新操作
        int flag = visitSettingV1InnerServiceSMOImpl.updateVisitSetting(visitSettingPo);
        
        // 检查更新结果，如果更新失败则抛出异常
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }
        
        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}