package com.java110.user.cmd.user;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.AuthenticationFactory;
import com.java110.core.log.LoggerFactory;
import com.java110.dto.owner.OwnerAppUserDto;
import com.java110.dto.user.UserAttrDto;
import com.java110.intf.user.IOwnerAppUserV1InnerServiceSMO;
import com.java110.intf.user.IUserAttrV1InnerServiceSMO;
import com.java110.po.owner.OwnerAppUserPo;
import com.java110.po.user.UserAttrPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Java110Cmd(serviceCode = "user.service.logout")
public class UserLogoutServiceCmd  extends Cmd {

    private final static Logger logger = LoggerFactory.getLogger(UserLogoutServiceCmd.class);

    @Autowired
    private IUserAttrV1InnerServiceSMO userAttrV1InnerServiceSMOImpl;

    @Autowired
    private IOwnerAppUserV1InnerServiceSMO ownerAppUserV1InnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        Assert.jsonObjectHaveKey(reqJson,"token","请求报文中未包含token 节点请检查");
    }

    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        ResponseEntity responseEntity= null;
        String token = CmdContextUtils.getTokenId(context);
        String userId = CmdContextUtils.getUserId(context);
        try {
            if (!StringUtil.isEmpty(userId) && !"-1".equals(userId)) {
                UserAttrPo userAttrPo = new UserAttrPo();
                userAttrPo.setUserId(userId);
                userAttrPo.setSpecCd(UserAttrDto.SPEC_OPEN_ID);
                int attrFlag = userAttrV1InnerServiceSMOImpl.deleteUserAttrInfo(userAttrPo);
                logger.info("退出登录解除openId绑定 userId={}, 删除属性记录数={}", userId, attrFlag);

                int appFlag = resetOwnerAppUserOpenId(userId);
                logger.info("退出登录重置业主绑定表openId userId={}, 更新记录数={}", userId, appFlag);
            }
            //删除 token 信息
            AuthenticationFactory.deleteToken(token);
            responseEntity = new ResponseEntity<String>("退出登录成功", HttpStatus.OK);
        } catch (Exception e) {
            logger.error("退出登录失败", e);
            //Invalid signature/claims
            responseEntity = new ResponseEntity<String>("退出登录失败，请联系管理员", HttpStatus.UNAUTHORIZED);
        }
        context.setResponseEntity(responseEntity);
    }

    private int resetOwnerAppUserOpenId(String userId) {
        OwnerAppUserDto queryDto = new OwnerAppUserDto();
        queryDto.setUserId(userId);
        queryDto.setStatusCd("0");
        List<OwnerAppUserDto> appUsers = ownerAppUserV1InnerServiceSMOImpl.queryOwnerAppUsers(queryDto);
        if (ListUtil.isNull(appUsers)) {
            return 0;
        }
        int flag = 0;
        for (OwnerAppUserDto appUser : appUsers) {
            if ("-1".equals(appUser.getOpenId())) {
                continue;
            }
            OwnerAppUserPo po = new OwnerAppUserPo();
            po.setAppUserId(appUser.getAppUserId());
            po.setStatusCd("0");
            po.setOpenId("-1");
            flag += ownerAppUserV1InnerServiceSMOImpl.updateOwnerAppUser(po);
        }
        return flag;
    }
}
