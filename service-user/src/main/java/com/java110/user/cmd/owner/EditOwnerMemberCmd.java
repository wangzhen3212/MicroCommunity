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
import com.java110.dto.owner.OwnerAppUserDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.intf.acct.IAccountInnerServiceSMO;
import com.java110.intf.common.IFileInnerServiceSMO;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.user.*;
import com.java110.po.account.AccountPo;
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
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 修改业主信息命令类
 * 主要用于第三方系统（如招商系统）同步业主信息，包括业主基本信息、账户信息、属性信息和绑定用户信息的修改
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023
 */
@Java110CmdDoc(title = "修改业主",
        description = "第三方系统，比如招商系统同步业主信息",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/owner.editOwnerMember",
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
@Java110Cmd(serviceCode = "owner.editOwnerMember")
public class EditOwnerMemberCmd extends Cmd {

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
     * 验证请求参数的有效性
     * 检查必填字段、手机号重复性、身份证号重复性、图片地址长度等
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 验证必填字段是否存在
        Assert.jsonObjectHaveKey(reqJson, "memberId", "请求报文中未包含ownerId");
        Assert.jsonObjectHaveKey(reqJson, "name", "请求报文中未包含name");
        Assert.jsonObjectHaveKey(reqJson, "link", "请求报文中未包含link");
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求报文中未包含communityId");
        Assert.judgeAttrValue(reqJson);
        
        // 处理手机号脱敏情况：如果手机号包含*号，则置为空
        String link = reqJson.getString("link");
        if (!StringUtil.isEmpty(link) && link.contains("*")) {
            reqJson.put("link", "");
        }
        
        // 处理身份证号脱敏情况：如果身份证号包含*号，则置为空
        String idCard = reqJson.getString("idCard");
        if (!StringUtil.isEmpty(idCard) && idCard.contains("*")) {
            reqJson.put("idCard", "");
        }

        // 验证图片地址长度
        if (reqJson.containsKey("ownerPhotoUrl")) {
            String ownerPhotoUrl = reqJson.getString("ownerPhotoUrl");
            if (!StringUtil.isEmpty(ownerPhotoUrl) && ownerPhotoUrl.length() > 500) {
                throw new CmdException("图片地址太长");
            }
            if (!StringUtil.isEmpty(ownerPhotoUrl)) {
                reqJson.put("faceUrl", ownerPhotoUrl); // 设置人脸识别URL
            }
        }

        // 检查是否开启用户验证
        String userValidate = MappingCache.getValue("USER_VALIDATE");
        if (!"ON".equals(userValidate)) {
            return; // 未开启用户验证，直接返回
        }

        // 根据memberId查询当前业主信息
        OwnerDto curOwner = new OwnerDto();
        curOwner.setMemberId(reqJson.getString("memberId"));
        List<OwnerDto> curOwners = ownerInnerServiceSMOImpl.queryOwners(curOwner);
        Assert.listOnlyOne(curOwners, "未查询到业主信息或查询到多条");

        // 检查手机号是否重复
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setLink(curOwners.get(0).getLink()); // 使用原始手机号进行查询
        ownerDto.setCommunityId(reqJson.getString("communityId"));
        List<OwnerDto> ownerDtos = ownerInnerServiceSMOImpl.queryAllOwners(ownerDto);
        
        // 如果查询到多个业主记录，检查是否有重复手机号
        if (ownerDtos != null && ownerDtos.size() > 1) {
            throw new IllegalArgumentException("手机号重复，请重新输入");
        } else if (ownerDtos != null && ownerDtos.size() == 1) {
            // 如果只有一个记录，检查是否是当前业主本人
            for (OwnerDto owner : ownerDtos) {
                if (!reqJson.getString("memberId").equals(owner.getMemberId())) {
                    throw new IllegalArgumentException("手机号重复，请重新输入");
                }
            }
        }
    }

    /**
     * 执行修改业主信息的核心业务逻辑
     * 包括修改业主基本信息、账户信息、属性信息和绑定用户信息
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当业务操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 修改业主基本信息
        OwnerPo ownerPo = BeanConvertUtil.covertBean(reqJson, OwnerPo.class);
        int flag = ownerV1InnerServiceSMOImpl.updateOwner(ownerPo);
        if (flag < 1) {
            throw new CmdException("修改业主失败");
        }

        // 修改账户信息
        updateAccount(reqJson);

        // 处理业主属性信息
        JSONArray attrs = reqJson.getJSONArray("attrs");
        if (ListUtil.isNull(attrs)) {
            return; // 没有属性信息需要处理，直接返回
        }
        
        JSONObject attr = null;
        for (int attrIndex = 0; attrIndex < attrs.size(); attrIndex++) {
            attr = attrs.getJSONObject(attrIndex);
            // 设置属性关联的业主ID和小区ID
            attr.put("memberId", reqJson.getString("memberId"));
            attr.put("communityId", reqJson.getString("communityId"));
            
            // 判断是新增属性还是修改属性
            if (!attr.containsKey("attrId") || StringUtil.isEmpty(attr.getString("attrId")) || attr.getString("attrId").startsWith("-")) {
                // 新增属性：生成属性ID并保存
                attr.put("attrId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_attrId));
                OwnerAttrPo ownerAttrPo = BeanConvertUtil.covertBean(attr, OwnerAttrPo.class);
                flag = ownerAttrInnerServiceSMOImpl.saveOwnerAttr(ownerAttrPo);
                if (flag < 1) {
                    throw new CmdException("添加业主属性失败");
                }
                continue;
            }
            
            // 修改现有属性
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
     * 更新业主关联的账户信息
     * 当业主手机号或姓名发生变化时，同步更新账户信息
     *
     * @param paramInJson 请求参数JSON对象
     */
    public void updateAccount(JSONObject paramInJson) {
        // 查询业主对应的账户信息
        AccountDto accountDto = new AccountDto();
        accountDto.setObjId(paramInJson.getString("memberId")); // 设置业主ID
        accountDto.setPartId(paramInJson.getString("communityId")); // 设置小区ID
        List<AccountDto> accountDtos = accountInnerServiceSMOImpl.queryAccounts(accountDto);
        
        if (ListUtil.isNull(accountDtos)) {
            return; // 没有找到对应的账户信息，直接返回
        }
        
        // 查询更新后的业主信息
        OwnerDto owner = new OwnerDto();
        owner.setMemberId(paramInJson.getString("memberId"));
        List<OwnerDto> owners = ownerInnerServiceSMOImpl.queryOwnerMembers(owner);
        Assert.listOnlyOne(owners, "未查询到业主信息或查询到多条");
        
        // 检查账户信息是否需要更新（手机号或姓名发生变化）
        if (!accountDtos.get(0).getLink().equals(owners.get(0).getLink()) || !accountDtos.get(0).getAcctName().equals(owners.get(0).getName())) {
            AccountPo accountPo = new AccountPo();
            accountPo.setAcctName(owners.get(0).getName()); // 设置账户姓名
            accountPo.setoLink(owners.get(0).getLink()); // 设置账户手机号
            accountPo.setAcctId(accountDtos.get(0).getAcctId()); // 设置账户ID
            accountInnerServiceSMOImpl.updateAccount(accountPo); // 执行账户更新
        }
    }

    /**
     * 更新业主绑定的APP用户信息
     * 当业主手机号发生变化时，同步更新绑定的APP用户手机号
     *
     * @param reqJson 请求参数JSON对象
     */
    private void updateOwnerAppUser(JSONObject reqJson) {
        // 查询业主绑定的APP用户信息
        OwnerAppUserDto ownerAppUserDto = new OwnerAppUserDto();
        ownerAppUserDto.setMemberId(reqJson.getString("memberId"));
        List<OwnerAppUserDto> ownerAppUserDtos = ownerAppUserInnerServiceSMOImpl.queryOwnerAppUsers(ownerAppUserDto);
        
        if (ListUtil.isNull(ownerAppUserDtos)) {
            return; // 没有绑定的APP用户，直接返回
        }
        
        // 遍历所有绑定的APP用户，更新手机号信息
        for (OwnerAppUserDto ownerAppUser : ownerAppUserDtos) {
            OwnerAppUserPo ownerAppUserPo = BeanConvertUtil.covertBean(ownerAppUser, OwnerAppUserPo.class);
            ownerAppUserPo.setLink(reqJson.getString("link")); // 更新手机号
            ownerAppUserV1InnerServiceSMOImpl.updateOwnerAppUser(ownerAppUserPo);
            
            // 检查用户ID是否有效
            if (StringUtil.isEmpty(ownerAppUser.getUserId())) {
                continue;
            }
            if (ownerAppUser.getUserId().startsWith("-")) {
                continue; // 跳过临时用户ID
            }
            
            // 更新用户基本信息中的手机号
            UserPo userPo = new UserPo();
            userPo.setUserId(ownerAppUserDtos.get(0).getUserId());
            userPo.setTel(reqJson.getString("link")); // 更新用户手机号
            userV1InnerServiceSMOImpl.updateUser(userPo);
        }
    }
}