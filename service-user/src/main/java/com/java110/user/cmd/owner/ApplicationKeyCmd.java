/**
 * 钥匙申请处理命令类
 * 
 * 该类负责处理业主钥匙申请的完整业务流程，包括：
 * 1. 验证申请信息的完整性
 * 2. 根据房屋信息查询业主信息
 * 3. 保存业主申请信息
 * 4. 处理业主照片文件的上传和关联
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.user.cmd.owner;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.file.FileDto;
import com.java110.dto.owner.OwnerRoomRelDto;
import com.java110.intf.common.IFileInnerServiceSMO;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.user.IOwnerRoomRelInnerServiceSMO;
import com.java110.intf.user.IOwnerV1InnerServiceSMO;
import com.java110.po.file.FileRelPo;
import com.java110.po.owner.OwnerPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Java110Cmd(serviceCode = "owner.applicationKey")
public class ApplicationKeyCmd extends Cmd {

    /** 业主房屋关系服务接口 */
    @Autowired
    private IOwnerRoomRelInnerServiceSMO ownerRoomRelInnerServiceSMOImpl;

    /** 业主信息服务接口 */
    @Autowired
    private IOwnerV1InnerServiceSMO ownerV1InnerServiceSMOImpl;

    /** 文件服务接口 */
    @Autowired
    private IFileInnerServiceSMO fileInnerServiceSMOImpl;

    /** 文件关系服务接口 */
    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    /**
     * 验证请求参数方法
     * 
     * 检查请求报文中是否包含必要的字段信息，确保请求数据的完整性
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 数据流上下文，用于获取和设置上下文数据
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证必填字段是否存在
        Assert.jsonObjectHaveKey(reqJson, "name", "请求报文中未包含name");
        Assert.jsonObjectHaveKey(reqJson, "roomId", "请求报文中未包含房屋信息");
        Assert.jsonObjectHaveKey(reqJson, "age", "请求报文中未包含age");
        Assert.jsonObjectHaveKey(reqJson, "link", "请求报文中未包含link");
        Assert.jsonObjectHaveKey(reqJson, "sex", "请求报文中未包含sex");
        //Assert.jsonObjectHaveKey(paramIn, "ownerTypeCd", "请求报文中未包含sex"); //这个不需要 这个直接写成钥匙申请，临时人员
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求报文中未包含communityId");
        Assert.jsonObjectHaveKey(reqJson, "idCard", "请求报文中未包含身份证号");
        Assert.jsonObjectHaveKey(reqJson, "ownerPhoto", "请求报文中未包含照片信息");
    }

    /**
     * 执行钥匙申请业务逻辑
     * 
     * 处理钥匙申请的完整业务流程，包括：
     * 1. 根据房屋ID查询业主信息
     * 2. 生成成员ID
     * 3. 保存业主申请信息
     * 4. 处理照片文件上传和关联
     * 
     * @param event 命令事件对象
     * @param context 数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当业务处理失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 根据房屋ID查询业主房屋关系信息
        OwnerRoomRelDto ownerRoomRelDto = new OwnerRoomRelDto();
        ownerRoomRelDto.setRoomId(reqJson.getString("roomId"));
        List<OwnerRoomRelDto> ownerRoomRelDtoList = ownerRoomRelInnerServiceSMOImpl.queryOwnerRoomRels(ownerRoomRelDto);

        // 验证查询结果，确保只查询到一条记录
        Assert.listOnlyOne(ownerRoomRelDtoList, "根据房屋查询不到业主信息或查询到多条");
        // 将业主ID设置到请求参数中
        reqJson.put("ownerId", ownerRoomRelDtoList.get(0).getOwnerId());

        // 生成成员ID
        String memberId = GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_ownerId);
        reqJson.put("memberId", memberId);

        // 准备业主业务数据
        JSONObject businessOwner = new JSONObject();
        businessOwner.putAll(reqJson);
        businessOwner.put("ownerTypeCd", "1004");// 临时人员类型编码
        businessOwner.put("state", "1000");// 待审核状态

        // 转换并保存业主信息
        OwnerPo ownerPo = BeanConvertUtil.covertBean(businessOwner, OwnerPo.class);
        int flag = ownerV1InnerServiceSMOImpl.saveOwner(ownerPo);

        // 检查保存结果
        if (flag < 1) {
            throw new IllegalArgumentException("保存业主失败");
        }

        // 处理照片文件信息
        FileDto fileDto = new FileDto();
        fileDto.setFileId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_file_id));
        fileDto.setFileName(fileDto.getFileId());
        fileDto.setContext(reqJson.getString("ownerPhoto"));// 照片的base64编码内容
        fileDto.setSuffix("jpeg");// 文件后缀
        fileDto.setCommunityId(reqJson.getString("communityId"));
        // 保存文件并获取文件保存名称
        String fileName = fileInnerServiceSMOImpl.saveFile(fileDto);
        reqJson.put("ownerPhotoId", fileDto.getFileId());
        reqJson.put("fileSaveName", fileName);

        // 准备文件关联业务数据
        JSONObject businessUnit = new JSONObject();
        businessUnit.put("fileRelId", "-1");// 文件关系ID，-1表示新增
        businessUnit.put("relTypeCd", "10000");// 关系类型编码
        businessUnit.put("saveWay", "table");// 保存方式
        businessUnit.put("objId", reqJson.getString("memberId"));// 关联对象ID
        businessUnit.put("fileRealName", reqJson.getString("ownerPhotoId"));// 文件真实名称
        businessUnit.put("fileSaveName", reqJson.getString("fileSaveName"));// 文件保存名称

        // 转换并保存文件关联信息
        FileRelPo fileRelPo = BeanConvertUtil.covertBean(businessUnit, FileRelPo.class);
        flag = fileRelInnerServiceSMOImpl.saveFileRel(fileRelPo);
        if (flag < 1) {
            throw new IllegalArgumentException("保存业主失败");
        }
    }
}