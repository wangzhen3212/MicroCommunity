/**
 * 保存小区设置命令类
 * 
 * 该类负责处理小区设置信息的保存和更新操作，包括参数验证和业务逻辑处理。
 * 支持批量处理多个设置项，根据设置项是否存在决定执行新增或更新操作。
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.community.cmd.community;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.Environment;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.CommunitySettingFactory;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.community.CommunitySettingDto;
import com.java110.intf.community.ICommunitySettingInnerServiceSMO;
import com.java110.po.community.CommunitySettingPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

@Java110Cmd(serviceCode = "community.saveCommunitySetting")
public class SaveCommunitySettingCmd extends Cmd {

    @Autowired
    private ICommunitySettingInnerServiceSMO communitySettingInnerServiceSMOImpl;

    /**
     * 验证请求参数的有效性
     * 
     * 该方法用于验证传入的请求JSON对象是否包含必要的参数，包括：
     * - 小区ID（communityId）
     * - 设置类型（settingType）
     * - 设置项数组（keys）及其内部每个设置项的键值对
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求的JSON对象，包含需要验证的参数
     * @throws CmdException 当参数验证失败时抛出命令异常
     * @throws ParseException 当数据解析出现错误时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 检查当前环境是否为开发环境
        Environment.isDevEnv();
        
        // 验证必填参数：小区ID和设置类型
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
        Assert.hasKeyAndValue(reqJson, "settingType", "未包含类型");

        // 验证设置项数组是否存在
        if (!reqJson.containsKey("keys")) {
            throw new CmdException("未包内容");
        }

        // 获取设置项数组并验证其有效性
        JSONArray keys = reqJson.getJSONArray("keys");
        if (keys == null || keys.size() < 1) {
            throw new CmdException("未包内容");
        }

        // 遍历所有设置项，验证每个设置项是否包含必要的键值
        JSONObject keyObj = null;
        for (int keyIndex = 0; keyIndex < keys.size(); keyIndex++) {
            keyObj = keys.getJSONObject(keyIndex);
            Assert.hasKeyAndValue(keyObj, "settingValue", "未包含值");
            Assert.hasKeyAndValue(keyObj, "settingKey", "未包含键");
        }
    }

    /**
     * 执行小区设置保存命令
     * 
     * 该方法处理小区设置的新增和更新操作：
     * 1. 遍历所有设置项
     * 2. 检查设置项是否已存在
     * 3. 如果不存在则新增，存在则更新
     * 4. 更新缓存中的设置信息
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求的JSON对象，包含需要处理的设置数据
     * @throws CmdException 当命令执行过程中出现错误时抛出命令异常
     * @throws ParseException 当数据解析出现错误时抛出解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        // 获取设置项数组
        JSONArray keys = reqJson.getJSONArray("keys");

        JSONObject keyObj = null;
        List<CommunitySettingDto> settings = null;
        CommunitySettingPo communitySettingPo = null;
        
        // 遍历所有设置项进行处理
        for (int keyIndex = 0; keyIndex < keys.size(); keyIndex++) {
            keyObj = keys.getJSONObject(keyIndex);

            // 构建查询条件，检查设置项是否已存在
            CommunitySettingDto communitySettingDto = new CommunitySettingDto();
            communitySettingDto.setSettingKey(keyObj.getString("settingKey"));
            communitySettingDto.setSettingType(reqJson.getString("settingType"));
            communitySettingDto.setCommunityId(reqJson.getString("communityId"));
            settings = communitySettingInnerServiceSMOImpl.queryCommunitySettings(communitySettingDto);
            
            // 构建小区设置持久化对象
            communitySettingPo = new CommunitySettingPo();
            communitySettingPo.setSettingType(reqJson.getString("settingType"));
            communitySettingPo.setCommunityId(reqJson.getString("communityId"));
            communitySettingPo.setSettingKey(keyObj.getString("settingKey"));
            communitySettingPo.setSettingValue(keyObj.getString("settingValue"));
            communitySettingPo.setSettingName(keyObj.getString("settingName"));
            communitySettingPo.setRemark(keyObj.getString("remark"));

            // 根据设置项是否存在决定执行新增或更新操作
            if (settings == null || settings.size() < 1) {
                // 生成新的设置ID并保存新设置项
                communitySettingPo.setCsId(GenerateCodeFactory.getGeneratorId("11"));
                communitySettingInnerServiceSMOImpl.saveCommunitySetting(communitySettingPo);
                continue;
            }

            // 更新已存在的设置项
            communitySettingPo.setCsId(settings.get(0).getCsId());
            communitySettingInnerServiceSMOImpl.updateCommunitySetting(communitySettingPo);

            // 将更新后的设置信息写入缓存，提高后续查询性能
            CommunitySettingFactory.getCommunitySettingFromDb(communitySettingPo.getCommunityId(), communitySettingPo.getSettingKey());

        }
    }
}