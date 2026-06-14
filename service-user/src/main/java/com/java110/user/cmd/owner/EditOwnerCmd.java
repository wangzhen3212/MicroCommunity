/**
 * 业主信息编辑命令类
 * 
 * 该类负责处理业主信息的修改操作，包括业主基本信息、账户信息、属性信息和绑定用户信息的更新。
 * 主要用于第三方系统（如招商系统）同步业主信息到本系统。
 * 
 * @author 吴学文
 * @version 1.0
 * @see Cmd
 */
package com.java110.user.cmd.owner;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.doc.annotation.*;
import com.java110.dto.account.AccountDto;
import com.java110.dto.file.FileDto;
import com.java110.dto.file.FileRelDto;
import com.java110.dto.owner.OwnerAppUserDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.intf.acct.IAccountInnerServiceSMO;
import com.java110.intf.common.IFileInnerServiceSMO;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.user.*;
import com.java110.po.account.AccountPo;
import com.java110.po.file.FileRelPo;
import com.java110.po.owner.OwnerAppUserPo;
import com.java110.po.owner.OwnerAttrPo;
import com.java110.po.owner.OwnerPo;
import com.java110.po.user.UserPo;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Java110CmdDoc(title = "修改业主",
        description = "第三方系统，比如招商系统同步业主信息",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/owner.editOwner",
        resource = "userDoc",
        author = "吴学文",
        serviceCode = "owner.editOwner",
        seq = 10)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "name", length = 64, remark = "业主名称"),
        @Java110ParamDoc(name = "link", length = 11, remark = "业主手机号"),
        @Java110ParamDoc(name = "idCard", length = 30, remark = "业主身份证号"),
        @Java110ParamDoc(name = "address", length = 512, remark = "地址"),
        @Java110ParamDoc(name = "sex", length = 12, remark = "性别 男 1 女 0"),
        @Java110ParamDoc(name = "ownerTypeCd", length = 12, remark = "业主类型 1001 业主 2002 家庭成员 家庭成员 需要传业主的ownerId"),
        @Java110ParamDoc(name = "remark", length = 512, remark = "备注"),
        @Java110ParamDoc(name = "memberId", length = 30, remark = "业主ID"),
        @Java110ParamDoc(name = "ownerPhoto", length = -1, remark = "业主人脸 用于同步门禁 人脸开门"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        })

@Java110ExampleDoc(
        reqBody = "{\n" +
                "\t\"name\": \"王王\",\n" +
                "\t\"age\": \"\",\n" +
                "\t\"link\": \"18909718888\",\n" +
                "\t\"address\": \"张三\",\n" +
                "\t\"sex\": \"0\",\n" +
                "\t\"ownerTypeCd\": \"1001\",\n" +
                "\t\"remark\": \"\",\n" +
                "\t\"memberId\": 123123123,\n" +
                "\t\"ownerPhoto\": \"\",\n" +
                "\t\"idCard\": \"\",\n" +
                "\t\"communityId\": \"2022121921870161\"\n" +
                "}",
        resBody = "{\"code\":0,\"msg\":\"成功\"}"
)
@Java110Cmd(serviceCode = "owner.editOwner")
public class EditOwnerCmd extends Cmd {

    @Autowired
    private IOwnerInnerServiceSMO ownerInnerServiceSMOImpl;

    @Autowired
    private IOwnerAttrInnerServiceSMO ownerAttrInnerServiceSMOImpl;

    @Autowired
    private IOwnerV1InnerServiceSMO ownerV1InnerServiceSMOImpl;

    @Autowired
    private IOwnerAppUserInnerServiceSMO ownerAppUserInnerServiceSMOImpl;

    @Autowired
    private IOwnerAppUserV1InnerServiceSMO ownerAppUserV1InnerServiceSMOImpl;

    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    @Autowired
    private IFileInnerServiceSMO fileInnerServiceSMOImpl;

    @Autowired
    private IAccountInnerServiceSMO accountInnerServiceSMOImpl;

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数的有效性，包括必填字段检查、手机号重复性检查、身份证号处理等
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 验证必填字段是否存在
        Assert.jsonObjectHaveKey(reqJson, "memberId", "请求报文中未包含ownerId");
        Assert.jsonObjectHaveKey(reqJson, "name", "请求报文中未包含name");
        Assert.jsonObjectHaveKey(reqJson, "link", "请求报文中未包含link");
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求报文中未包含communityId");
        Assert.judgeAttrValue(reqJson);
        
        // 处理手机号中的掩码字符（*），如果有掩码则清空手机号
        String link = reqJson.getString("link");
        if (!StringUtil.isEmpty(link) && link.contains("*")) {
            reqJson.put("link", "");
        }
        
        // 处理身份证号中的掩码字符（*），如果有掩码则清空身份证号
        String idCard = reqJson.getString("idCard");
        if (!StringUtil.isEmpty(idCard) && idCard.contains("*")) {
            reqJson.put("idCard", "");
        }

        // 处理业主照片URL，检查长度并设置人脸URL
        if (reqJson.containsKey("ownerPhotoUrl")) {
            String ownerPhotoUrl = reqJson.getString("ownerPhotoUrl");
            if (!StringUtil.isEmpty(ownerPhotoUrl) && ownerPhotoUrl.length() > 500) {
                throw new CmdException("图片地址太长");
            }
            if (!StringUtil.isEmpty(ownerPhotoUrl)) {
                reqJson.put("faceUrl", ownerPhotoUrl);
            }
        }

        // 检查用户验证配置，如果未开启验证则直接返回
        String userValidate = MappingCache.getValue("USER_VALIDATE");
        if (!"ON".equals(userValidate)) {
            return;
        }

        // 根据业主ID查询当前业主信息，确保存在且唯一
        OwnerDto curOwner = new OwnerDto();
        curOwner.setMemberId(reqJson.getString("memberId"));
        List<OwnerDto> curOwners = ownerInnerServiceSMOImpl.queryOwners(curOwner);
        Assert.listOnlyOne(curOwners, "未查询到业主信息或查询到多条");

        // 检查手机号是否重复
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setLink(link);
        ownerDto.setCommunityId(reqJson.getString("communityId"));
        List<OwnerDto> ownerDtos = ownerInnerServiceSMOImpl.queryAllOwners(ownerDto);
        if (ownerDtos != null && ownerDtos.size() > 1) {
            throw new IllegalArgumentException("手机号重复，请重新输入");
        } else if (ownerDtos != null && ownerDtos.size() == 1) {
            for (OwnerDto owner : ownerDtos) {
                // 如果手机号已被其他业主使用，则抛出异常
                if (!reqJson.getString("memberId").equals(owner.getMemberId())) {
                    throw new IllegalArgumentException("手机号重复，请重新输入");
                }
            }
        }
    }

    /**
     * 执行业主信息编辑命令
     * 
     * 主要功能包括：
     * 1. 更新业主基本信息
     * 2. 更新业主账户信息
     * 3. 处理业主属性信息（新增或修改）
     * 4. 更新业主绑定的APP用户信息
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 更新业主基本信息
        OwnerPo ownerPo = BeanConvertUtil.covertBean(reqJson, OwnerPo.class);
        int flag = ownerV1InnerServiceSMOImpl.updateOwner(ownerPo);
        if (flag < 1) {
            throw new CmdException("修改业主失败");
        }

        // 更新业主账户信息
        updateAccount(reqJson);

        // 处理业主属性信息
        JSONArray attrs = reqJson.getJSONArray("attrs");
        if (ListUtil.isNull(attrs)) {
            return;
        }
        
        JSONObject attr = null;
        for (int attrIndex = 0; attrIndex < attrs.size(); attrIndex++) {
            attr = attrs.getJSONObject(attrIndex);
            // 设置属性关联的业主ID和小区ID
            attr.put("memberId", reqJson.getString("memberId"));
            attr.put("communityId", reqJson.getString("communityId"));
            
            // 判断属性ID是否存在，如果不存在则生成新的属性ID并新增属性
            if (!attr.containsKey("attrId") || StringUtil.isEmpty(attr.getString("attrId")) || attr.getString("attrId").startsWith("-")) {
                attr.put("attrId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_attrId));
                OwnerAttrPo ownerAttrPo = BeanConvertUtil.covertBean(attr, OwnerAttrPo.class);
                flag = ownerAttrInnerServiceSMOImpl.saveOwnerAttr(ownerAttrPo);
                if (flag < 1) {
                    throw new CmdException("添加业主属性失败");
                }
                continue;
            }
            
            // 更新已存在的属性信息
            OwnerAttrPo ownerAttrPo = BeanConvertUtil.covertBean(attr, OwnerAttrPo.class);
            flag = ownerAttrInnerServiceSMOImpl.updateOwnerAttrInfoInstance(ownerAttrPo);
            if (flag < 1) {
                throw new CmdException("修改业主属性失败");
            }
        }
        
        // 更新业主绑定的APP用户信息
        updateOwnerAppUser(reqJson);
    }

    /**
     * 更新业主账户信息
     * 
     * 检查业主手机号和账户手机号是否一致，如果不一致则更新账户信息
     * 
     * @param paramInJson 请求参数JSON对象
     */
    public void updateAccount(JSONObject paramInJson) {
        // 查询业主对应的账户信息
        AccountDto accountDto = new AccountDto();
        accountDto.setObjId(paramInJson.getString("memberId"));
        accountDto.setPartId(paramInJson.getString("communityId"));
        List<AccountDto> accountDtos = accountInnerServiceSMOImpl.queryAccounts(accountDto);
        
        // 如果没有账户信息则直接返回
        if (ListUtil.isNull(accountDtos)) {
            return;
        }
        
        // 查询更新后的业主信息
        OwnerDto owner = new OwnerDto();
        owner.setMemberId(paramInJson.getString("memberId"));
        List<OwnerDto> owners = ownerInnerServiceSMOImpl.queryOwnerMembers(owner);
        Assert.listOnlyOne(owners, "未查询到业主信息或查询到多条");
        
        // 如果账户的手机号或姓名与业主信息不一致，则更新账户信息
        if (!accountDtos.get(0).getLink().equals(owners.get(0).getLink()) || !accountDtos.get(0).getAcctName().equals(owners.get(0).getName())) {
            AccountPo accountPo = new AccountPo();
            accountPo.setAcctName(owners.get(0).getName());
            accountPo.setoLink(owners.get(0).getLink());
            accountPo.setAcctId(accountDtos.get(0).getAcctId());
            accountInnerServiceSMOImpl.updateAccount(accountPo);
        }
    }

    /**
     * 更新业主绑定的APP用户信息
     * 
     * 如果业主绑定了APP用户，则更新绑定用户的手机号信息
     * 
     * @param reqJson 请求JSON对象
     */
    private void updateOwnerAppUser(JSONObject reqJson) {
        // 查询业主绑定的APP用户信息
        OwnerAppUserDto ownerAppUserDto = new OwnerAppUserDto();
        ownerAppUserDto.setMemberId(reqJson.getString("memberId"));
        List<OwnerAppUserDto> ownerAppUserDtos = ownerAppUserInnerServiceSMOImpl.queryOwnerAppUsers(ownerAppUserDto);
        
        // 如果没有绑定信息则直接返回
        if (ListUtil.isNull(ownerAppUserDtos)) {
            return;
        }
        
        // 遍历所有绑定的APP用户，更新手机号信息
        for (OwnerAppUserDto ownerAppUser : ownerAppUserDtos) {
            OwnerAppUserPo ownerAppUserPo = BeanConvertUtil.covertBean(ownerAppUser, OwnerAppUserPo.class);
            ownerAppUserPo.setLink(reqJson.getString("link"));
            ownerAppUserV1InnerServiceSMOImpl.updateOwnerAppUser(ownerAppUserPo);
            
            // 如果用户ID为空或为临时ID，则跳过用户信息更新
            if (StringUtil.isEmpty(ownerAppUser.getUserId())) {
                continue;
            }
            if (ownerAppUser.getUserId().startsWith("-")) {
                continue;
            }
            
            // 更新用户基本信息中的手机号
            UserPo userPo = new UserPo();
            userPo.setUserId(ownerAppUserDtos.get(0).getUserId());
            userPo.setTel(reqJson.getString("link"));
            userV1InnerServiceSMOImpl.updateUser(userPo);
        }
    }
}