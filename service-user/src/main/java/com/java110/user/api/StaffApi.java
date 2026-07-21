package com.java110.user.api;

import com.alibaba.fastjson.JSONObject;
import com.java110.dto.app.AppDto;
import com.java110.dto.wechat.SmallWeChatDto;
import com.java110.dto.user.StaffAppAuthDto;
import com.java110.intf.store.ISmallWeChatInnerServiceSMO;
import com.java110.po.user.StaffAppAuthPo;
import com.java110.user.bmo.staffAppAuth.IDeleteStaffAppAuthBMO;
import com.java110.user.bmo.staffAppAuth.IGetStaffAppAuthBMO;
import com.java110.user.bmo.staffAppAuth.ISaveStaffAppAuthBMO;
import com.java110.user.bmo.staffAppAuth.IUpdateStaffAppAuthBMO;
import com.java110.utils.cache.UrlCache;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping(value = "/staff")
public class StaffApi {

    @Autowired
    private ISaveStaffAppAuthBMO saveStaffAppAuthBMOImpl;
    @Autowired
    private IUpdateStaffAppAuthBMO updateStaffAppAuthBMOImpl;
    @Autowired
    private IDeleteStaffAppAuthBMO deleteStaffAppAuthBMOImpl;

    @Autowired
    private IGetStaffAppAuthBMO getStaffAppAuthBMOImpl;

    @Autowired
    private ISmallWeChatInnerServiceSMO smallWeChatInnerServiceSMOImpl;

    /**
     * 微信保存消息模板
     *
     * @param reqJson
     * @return
     * @serviceCode /staff/saveStaffAppAuth
     * @path /app/staff/saveStaffAppAuth
     */
    @RequestMapping(value = "/saveStaffAppAuth", method = RequestMethod.POST)
    public ResponseEntity<String> saveStaffAppAuth(@RequestBody JSONObject reqJson) {

        Assert.hasKeyAndValue(reqJson, "staffId", "请求报文中未包含staffId");
        Assert.hasKeyAndValue(reqJson, "appType", "请求报文中未包含appType");


        StaffAppAuthPo staffAppAuthPo = BeanConvertUtil.covertBean(reqJson, StaffAppAuthPo.class);
        return saveStaffAppAuthBMOImpl.save(staffAppAuthPo);
    }

    /**
     * 微信修改消息模板
     *
     * @param param
     * @return
     * @serviceCode /staff/updateStaffAppAuth
     * @path /app/staff/updateStaffAppAuth
     * <p>
     * {"openId":"oxeFbwJzDG8t87dVDcmrlRoPsTfM","appType":"WECHAT","state":"2002","storeId":"402019032924930007","staffId":"30518940136629616640"}
     */
    @RequestMapping(value = "/updateStaffAppAuth", method = RequestMethod.POST)
    public ResponseEntity<String> updateStaffAppAuth(@RequestBody String param) {
        JSONObject reqJson = JSONObject.parseObject(param);
        Assert.hasKeyAndValue(reqJson, "staffId", "请求报文中未包含staffId");
        Assert.hasKeyAndValue(reqJson, "appType", "请求报文中未包含appType");
        Assert.hasKeyAndValue(reqJson, "openId", "请求报文中未包含openId");
        Assert.hasKeyAndValue(reqJson, "state", "请求报文中未包含state");

        StaffAppAuthPo staffAppAuthPo = BeanConvertUtil.covertBean(reqJson, StaffAppAuthPo.class);
        return updateStaffAppAuthBMOImpl.update(staffAppAuthPo);
    }

    /**
     * 微信删除消息模板
     *
     * @param reqJson
     * @return
     * @serviceCode /staff/deleteStaffAppAuth
     * @path /app/staff/deleteStaffAppAuth
     */
    @RequestMapping(value = "/deleteStaffAppAuth", method = RequestMethod.POST)
    public ResponseEntity<String> deleteStaffAppAuth(@RequestBody JSONObject reqJson) {
        Assert.hasKeyAndValue(reqJson, "communityId", "小区ID不能为空");

        Assert.hasKeyAndValue(reqJson, "auId", "auId不能为空");


        StaffAppAuthPo staffAppAuthPo = BeanConvertUtil.covertBean(reqJson, StaffAppAuthPo.class);
        return deleteStaffAppAuthBMOImpl.delete(staffAppAuthPo);
    }

    /**
     * 微信删除消息模板
     *
     * @param storeId 小区ID
     * @return
     * @serviceCode /staff/queryStaffAppAuth
     * @path /app/staff/queryStaffAppAuth
     */
    @RequestMapping(value = "/queryStaffAppAuth", method = RequestMethod.GET)
    public ResponseEntity<String> queryStaffAppAuth(@RequestHeader(value = "store-id") String storeId,
                                                    @RequestHeader(value = "user-id") String userId,
                                                    @RequestParam(value = "staffId",required = false) String staffId,
                                                    @RequestParam(value = "page") int page,
                                                    @RequestParam(value = "row") int row) {
        StaffAppAuthDto staffAppAuthDto = new StaffAppAuthDto();
        staffAppAuthDto.setPage(page);
        staffAppAuthDto.setRow(row);
        staffAppAuthDto.setStoreId(storeId);

        if (!StringUtil.isEmpty(staffId)) {
            // 查询指定员工认证信息
            staffAppAuthDto.setStaffId(staffId);
            return getStaffAppAuthBMOImpl.get(staffAppAuthDto);
        } else {
            // 查询商户下所有员工认证信息
            return getStaffAppAuthBMOImpl.getList(staffAppAuthDto);
        }
    }

    /**
     * 生成员工认证二维码
     * 支持两种模式：
     * 1. 指定员工模式（默认）：URL 带 staffId + storeId，扫码后直接绑定当前员工
     * 2. 通用认证模式（common=true）：URL 不带 staffId 和 storeId，
     *    任何员工扫码后跳转员工端登录页，登录成功后自动绑定 openId
     *
     * @serviceCode /staff/generatorQrCode
     * @path /app/staff/generatorQrCode
     */
    @RequestMapping(value = "/generatorQrCode", method = RequestMethod.GET)
    public ResponseEntity<String> generatorQrCode(@RequestHeader(value = "store-id") String storeId,
                                                  @RequestHeader(value = "user-id") String userId,
                                                  @RequestParam(value = "communityId") String communityId,
                                                  @RequestParam(value = "common", required = false, defaultValue = "false") boolean common,
                                                  @RequestParam(value = "staffId", required = false) String staffId) {
        SmallWeChatDto smallWeChatDto = new SmallWeChatDto();
        smallWeChatDto.setObjId(communityId);
        smallWeChatDto.setObjType(SmallWeChatDto.OBJ_TYPE_COMMUNITY);
        smallWeChatDto.setWechatType(SmallWeChatDto.WECHAT_TYPE_PUBLIC);
        List<SmallWeChatDto> smallWeChatDtos = smallWeChatInnerServiceSMOImpl.querySmallWeChats(smallWeChatDto);
        String staffAuthUrl;
        if (common) {
            // 通用认证模式：不带 staffId 和 storeId，回调后跳转员工端登录页（带 openId）
            // 员工登录成功后由 PcUserLoginCmd 自动写入 staff_app_auth 绑定
            staffAuthUrl = UrlCache.getPropertyPhoneUrl()
                    + "/app/staffAuth?communityId=" + communityId
                    + "&appId=" + AppDto.WECHAT_OWNER_APP_ID;
        } else {
            // 指定员工模式：URL 带 staffId + storeId
            // 优先使用传入的 staffId，否则使用当前登录用户的 userId
            String targetStaffId = StringUtil.isEmpty(staffId) ? userId : staffId;
            staffAuthUrl = UrlCache.getPropertyPhoneUrl()
                    + "/app/staffAuth?storeId=" + storeId + "&staffId=" + targetStaffId
                    + "&communityId=" + communityId + "&appId=" + AppDto.WECHAT_OWNER_APP_ID;
        }

        if (smallWeChatDtos != null && smallWeChatDtos.size() > 0) {
            staffAuthUrl += ("&wAppId=" + smallWeChatDtos.get(0).getAppId());
        }
        return ResultVo.createResponseEntity(staffAuthUrl);
    }
}
