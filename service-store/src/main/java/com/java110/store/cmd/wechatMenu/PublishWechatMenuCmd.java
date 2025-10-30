package com.java110.store.cmd.wechatMenu;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.WechatFactory;
import com.java110.dto.wechat.SmallWeChatDto;
import com.java110.dto.wechat.WechatMenuDto;
import com.java110.intf.store.ISmallWeChatInnerServiceSMO;
import com.java110.intf.store.IWechatMenuInnerServiceSMO;
import com.java110.utils.constant.WechatConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * 微信公众号菜单发布命令类
 * 负责处理微信公众号菜单的发布操作，包括菜单数据的查询、组装和向微信服务器提交菜单配置
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "smallWeChat.publicWechatMenu")
public class PublishWechatMenuCmd extends Cmd {

    @Autowired
    private RestTemplate outRestTemplate;

    @Autowired
    private IWechatMenuInnerServiceSMO wechatMenuInnerServiceSMOImpl;

    @Autowired
    private ISmallWeChatInnerServiceSMO smallWeChatInnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 检查请求报文中是否包含必要的参数
     *
     * @param event   命令事件对象
     * @param context 数据流上下文对象，包含请求和响应数据
     * @param reqJson 请求报文JSON对象
     * @throws CmdException 当参数验证失败时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求报文中必须包含communityId参数
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
    }

    /**
     * 执行微信公众号菜单发布命令
     * 1. 查询一级菜单数据
     * 2. 验证菜单数量是否符合微信规范（1-3个）
     * 3. 组装菜单数据结构
     * 4. 获取微信访问令牌
     * 5. 调用微信API发布菜单
     * 6. 处理响应结果
     *
     * @param event   命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求报文JSON对象，包含小区ID等参数
     * @throws CmdException 当菜单发布过程中出现错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 查询一级菜单数据
        WechatMenuDto wechatMenuDto = BeanConvertUtil.covertBean(reqJson, WechatMenuDto.class);
        wechatMenuDto.setMenuLevel(WechatMenuDto.MENU_LEVEL_ONE);
        List<WechatMenuDto> wechatMenuDtos = wechatMenuInnerServiceSMOImpl.queryWechatMenus(wechatMenuDto);

        // 验证一级菜单数量是否符合微信规范（1-3个）
        if (wechatMenuDtos == null || wechatMenuDtos.size() < 1 || wechatMenuDtos.size() > 3) {
            throw new IllegalArgumentException("一级菜单数量为" + wechatMenuDtos.size() + "不符合公众号菜单数量要求");
        }
        
        // 组装微信菜单数据结构
        JSONObject wechatMenuObj = new JSONObject();
        JSONArray button = new JSONArray();
        
        // 遍历一级菜单，构建菜单结构
        for (WechatMenuDto tmpWechatMenuDto : wechatMenuDtos) {
            wechatMenuDto = new WechatMenuDto();
            wechatMenuDto.setMenuLevel(WechatMenuDto.MENU_LEVEL_TWO);
            wechatMenuDto.setCommunityId(tmpWechatMenuDto.getCommunityId());
            wechatMenuDto.setParentMenuId(tmpWechatMenuDto.getWechatMenuId());
            
            // 查询当前一级菜单下的二级菜单
            List<WechatMenuDto> tmpWechatMenuDtos = wechatMenuInnerServiceSMOImpl.queryWechatMenus(wechatMenuDto);
            JSONObject menuButton = new JSONObject();
            menuButton.put("name", tmpWechatMenuDto.getMenuName());
            
            // 如果存在二级菜单，则添加子菜单
            if (tmpWechatMenuDtos != null && tmpWechatMenuDtos.size() > 0) {
                addSubMenu(menuButton, tmpWechatMenuDtos);
                button.add(menuButton);
                continue;
            }

            // 设置一级菜单的属性
            menuButton.put("type", tmpWechatMenuDto.getMenuType());
            if (WechatMenuDto.MENU_TYPE_VIEW.equals(tmpWechatMenuDto.getMenuType())) {
                // 视图类型菜单，设置跳转URL
                menuButton.put("url", tmpWechatMenuDto.getMenuValue());
            } else if (WechatMenuDto.MENU_TYPE_MINIPROGRAM.equals(tmpWechatMenuDto.getMenuType())) {
                // 小程序类型菜单，设置小程序相关参数
                menuButton.put("url", tmpWechatMenuDto.getMenuValue());
                menuButton.put("appid", tmpWechatMenuDto.getAppId());
                menuButton.put("pagepath", tmpWechatMenuDto.getPagepath());
            }
            button.add(menuButton);
        }
        wechatMenuObj.put("button", button);
        
        // 查询小区对应的微信公众号配置
        SmallWeChatDto smallWeChatDto = new SmallWeChatDto();
        smallWeChatDto.setObjType(SmallWeChatDto.OBJ_TYPE_COMMUNITY);
        smallWeChatDto.setObjId(reqJson.getString("communityId"));
        smallWeChatDto.setWeChatType(SmallWeChatDto.WECHAT_TYPE_PUBLIC);
        List<SmallWeChatDto> smallWeChatDtos = smallWeChatInnerServiceSMOImpl.querySmallWeChats(smallWeChatDto);

        // 验证是否配置了微信公众号
        if (smallWeChatDtos == null || smallWeChatDtos.size() < 1) {
            throw new IllegalArgumentException("当前小区未配置公众号 请先配置");
        }
        
        // 获取微信访问令牌
        String token = WechatFactory.getAccessToken(smallWeChatDtos.get(0).getAppId(), smallWeChatDtos.get(0).getAppSecret());
        
        // 调用微信API创建菜单
        String url = WechatConstant.CREATE_MENU.replace("ACCESS_TOKEN", token);
        ResponseEntity<String> responseEntity = outRestTemplate.postForEntity(url, wechatMenuObj.toJSONString(), String.class);

        // 解析微信API响应并设置返回结果
        JSONObject wechatOutObj = JSONObject.parseObject(responseEntity.getBody());
        context.setResponseEntity(ResultVo.createResponseEntity(wechatOutObj.getInteger("errcode"), wechatOutObj.getString("errmsg")));
    }

    /**
     * 添加子菜单到父菜单对象中
     * 为包含二级菜单的一级菜单构建子菜单结构
     *
     * @param wechatMenuObj 父菜单JSON对象，用于添加子菜单
     * @param wechatMenuDtos 二级菜单数据列表
     */
    private void addSubMenu(JSONObject wechatMenuObj, List<WechatMenuDto> wechatMenuDtos) {
        JSONArray subButtons = new JSONArray();
        JSONObject subButton = null;
        
        // 遍历二级菜单数据，构建子菜单结构
        for (WechatMenuDto wechatMenuDto : wechatMenuDtos) {
            subButton = new JSONObject();
            subButton.put("name", wechatMenuDto.getMenuName());
            subButton.put("type", wechatMenuDto.getMenuType());
            
            // 根据菜单类型设置不同的属性
            if (WechatMenuDto.MENU_TYPE_VIEW.equals(wechatMenuDto.getMenuType())) {
                // 视图类型子菜单，设置跳转URL
                subButton.put("url", wechatMenuDto.getMenuValue());
            } else if (WechatMenuDto.MENU_TYPE_MINIPROGRAM.equals(wechatMenuDto.getMenuType())) {
                // 小程序类型子菜单，设置小程序相关参数
                subButton.put("url", wechatMenuDto.getMenuValue());
                subButton.put("appid", wechatMenuDto.getAppId());
                subButton.put("pagepath", wechatMenuDto.getPagepath());
            }
            subButtons.add(subButton);
        }
        
        // 将子菜单数组添加到父菜单对象中
        wechatMenuObj.put("sub_button", subButtons);
    }
}