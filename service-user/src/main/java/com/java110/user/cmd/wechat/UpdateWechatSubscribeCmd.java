package com.java110.user.cmd.wechat;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.wechat.SmallWeChatDto;
import com.java110.intf.store.ISmallWeChatInnerServiceSMO;
import com.java110.intf.user.IWechatSubscribeV1InnerServiceSMO;
import com.java110.po.wechat.WechatSubscribePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Java110Cmd(serviceCode = "wechat.updateWechatSubscribe")
public class UpdateWechatSubscribeCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(UpdateWechatSubscribeCmd.class);

    @Autowired
    private IWechatSubscribeV1InnerServiceSMO wechatSubscribeV1InnerServiceSMOImpl;

    @Autowired
    private ISmallWeChatInnerServiceSMO smallWeChatInnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        super.validateProperty(cmdDataFlowContext);
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区编号");
        Assert.hasKeyAndValue(reqJson, "subId", "未包含订阅ID");
    }

    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        SmallWeChatDto smallWeChatDto = new SmallWeChatDto();
        smallWeChatDto.setObjId(reqJson.getString("communityId"));
        smallWeChatDto.setWeChatType(SmallWeChatDto.WECHAT_TYPE_PUBLIC);

        List<SmallWeChatDto> smallWeChatDtos = smallWeChatInnerServiceSMOImpl.querySmallWeChats(smallWeChatDto);
        if (ListUtil.isNull(smallWeChatDtos)) {
            throw new CmdException("未配置公众号");
        }

        WechatSubscribePo wechatSubscribePo = BeanConvertUtil.covertBean(reqJson, WechatSubscribePo.class);
        wechatSubscribePo.setAppId(smallWeChatDtos.get(0).getAppId());

        int flag = wechatSubscribeV1InnerServiceSMOImpl.updateWechatSubscribe(wechatSubscribePo);
        if (flag < 1) {
            throw new CmdException("更新微信订阅信息失败");
        }

        cmdDataFlowContext.setResponseEntity(ResultVo.createResponseEntity(ResultVo.CODE_OK, "保存成功"));
    }
}