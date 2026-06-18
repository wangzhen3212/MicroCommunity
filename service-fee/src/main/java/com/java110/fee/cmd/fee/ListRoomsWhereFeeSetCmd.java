package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.room.RoomDto;
import com.java110.dto.privilege.BasePrivilegeDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.owner.OwnerRoomRelDto;
import com.java110.intf.community.IMenuInnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.user.IOwnerInnerServiceSMO;
import com.java110.intf.user.IOwnerRoomRelInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.api.ApiRoomDataVo;
import com.java110.vo.api.ApiRoomVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 费用设置房屋列表查询命令类
 * 功能：查询小区中可以进行费用设置的房屋列表，支持按业主信息查询和普通房屋查询两种方式
 * 用途：在费用设置页面中展示可设置费用的房屋信息
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "fee.listRoomsWhereFeeSet")
public class ListRoomsWhereFeeSetCmd extends Cmd {

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private IOwnerInnerServiceSMO ownerInnerServiceSMOImpl;

    @Autowired
    private IOwnerRoomRelInnerServiceSMO ownerRoomRelInnerServiceSMOImpl;

    @Autowired
    private IMenuInnerServiceSMO menuInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否合法，包括分页信息和必要参数
     *
     * @param event             命令事件对象
     * @param cmdDataFlowContext 数据流上下文
     * @param reqJson           请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        super.validatePageInfo(reqJson); // 验证分页参数
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区ID"); // 验证小区ID是否存在
    }

    /**
     * 命令执行方法
     * 根据不同的查询条件执行房屋查询逻辑
     *
     * @param event             命令事件对象
     * @param cmdDataFlowContext 数据流上下文
     * @param reqJson           请求参数JSON对象
     * @throws CmdException 当命令执行失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        ApiRoomVo apiRoomVo = new ApiRoomVo(); // 创建返回VO对象

        String ownerName = reqJson.getString("ownerName"); // 业主姓名精确查询
        String idCard = reqJson.getString("idCard"); // 身份证号查询
        String ownerNameLike = reqJson.getString("ownerNameLike"); // 业主姓名模糊查询
        
        // 这里优化 userId 问题，设置为空字符串避免干扰查询
        reqJson.put("userId", "");
        
        // 根据业主信息来定位房屋信息（精确查询或模糊查询）
        if (!StringUtil.isEmpty(ownerName) || !StringUtil.isEmpty(idCard) || !StringUtil.isEmpty(ownerNameLike)) {
            queryRoomByOwnerInfo(apiRoomVo, reqJson, cmdDataFlowContext); // 调用业主信息查询方法
            return;
        }

        // 普通房屋查询逻辑
        RoomDto roomDto = BeanConvertUtil.covertBean(reqJson, RoomDto.class); // 转换请求参数为RoomDto

        // 查询总记录数
        int total = roomInnerServiceSMOImpl.queryRoomsCount(BeanConvertUtil.covertBean(reqJson, RoomDto.class));
        apiRoomVo.setTotal(total); // 设置总记录数
        
        if (total > 0) {
            List<RoomDto> roomDtoList = roomInnerServiceSMOImpl.queryRooms(roomDto); // 查询房屋列表
            
            // 获取手机号、身份证号加密标识
            String flag = reqJson.getString("flag");
            // 刷新房屋的业主信息，包括脱敏处理
            refreshRoomOwners(reqJson.getString("loginUserId"), reqJson.getString("communityId"), roomDtoList, flag);

            // 转换房屋数据为API返回格式
            apiRoomVo.setRooms(BeanConvertUtil.covertBeanList(roomDtoList, ApiRoomDataVo.class));
        } else {
            throw new IllegalArgumentException("查询业主房屋错误！"); // 无数据时抛出异常
        }
        
        // 计算总页数
        int row = reqJson.getInteger("row");
        apiRoomVo.setRecords((int) Math.ceil((double) total / (double) row));

        // 设置响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiRoomVo), HttpStatus.OK);
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 根据业主信息查询房屋信息
     * 通过业主姓名、身份证号等条件查询对应的房屋信息
     *
     * @param apiRoomVo          返回数据VO对象
     * @param reqJson            请求参数JSON对象
     * @param cmdDataFlowContext 数据流上下文
     */
    private void queryRoomByOwnerInfo(ApiRoomVo apiRoomVo, JSONObject reqJson, ICmdDataFlowContext cmdDataFlowContext) {
        // 转换请求参数为业主房屋关系DTO
        OwnerRoomRelDto ownerRoomRelDto = BeanConvertUtil.covertBean(reqJson, OwnerRoomRelDto.class);
        ownerRoomRelDto.setByOwnerInfo(true); // 设置为按业主信息查询模式
        ownerRoomRelDto.setCommunityId(reqJson.getString("communityId")); // 设置小区ID
        
        // 查询业主房屋关系记录数
        int total = ownerRoomRelInnerServiceSMOImpl.queryOwnerRoomRelsCount(ownerRoomRelDto);
        apiRoomVo.setTotal(total); // 设置总记录数
        
        if (total > 0) {
            // 查询业主房屋关系列表
            List<OwnerRoomRelDto> ownerRoomRelDtos = ownerRoomRelInnerServiceSMOImpl.queryOwnerRoomRels(ownerRoomRelDto);
            List<RoomDto> roomDtoList = null;

            // 刷新业主房屋信息
            roomDtoList = refreshOwnerRooms(reqJson.getString("communityId"), ownerRoomRelDtos);

            // 转换房屋数据为API返回格式
            apiRoomVo.setRooms(BeanConvertUtil.covertBeanList(roomDtoList, ApiRoomDataVo.class));
        }
        
        // 计算总页数（基于实际返回的房屋数量）
        int row = reqJson.getInteger("row");
        apiRoomVo.setRecords((int) Math.ceil((double) apiRoomVo.getRooms().size() / (double) row));

        // 设置响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiRoomVo), HttpStatus.OK);
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 刷新业主房屋信息
     * 根据业主房屋关系数据获取完整的房屋信息，并补充业主信息
     *
     * @param communityId       小区ID
     * @param ownerRoomRelDtos 业主房屋关系数据列表
     * @return List<RoomDto> 完整的房屋信息列表
     */
    private List<RoomDto> refreshOwnerRooms(String communityId, List<OwnerRoomRelDto> ownerRoomRelDtos) {
        List<String> roomIds = new ArrayList<>(); // 房屋ID集合

        // 提取所有房屋ID
        for (OwnerRoomRelDto ownerRoomRelDto : ownerRoomRelDtos) {
            roomIds.add(ownerRoomRelDto.getRoomId());
        }
        
        // 构建房屋查询条件
        RoomDto roomDto = new RoomDto();
        roomDto.setCommunityId(communityId);
        roomDto.setRoomIds(roomIds.toArray(new String[roomIds.size()]));
        
        // 查询房屋详细信息
        List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);

        // 将业主信息补充到房屋信息中
        for (RoomDto tmpRoomDto : roomDtos) {
            for (OwnerRoomRelDto ownerRoomRelDto : ownerRoomRelDtos) {
                if (tmpRoomDto.getRoomId().equals(ownerRoomRelDto.getRoomId())) {
                    // 设置业主相关信息
                    tmpRoomDto.setOwnerId(ownerRoomRelDto.getOwnerId());
                    tmpRoomDto.setOwnerName(ownerRoomRelDto.getOwnerName());
                    tmpRoomDto.setIdCard(ownerRoomRelDto.getIdCard());
                    tmpRoomDto.setLink(ownerRoomRelDto.getLink());
                }
            }
        }
        return roomDtos;
    }

    /**
     * 刷新房屋业主信息
     * 为房屋列表补充业主信息，并对敏感信息进行脱敏处理
     *
     * @param userId     用户ID，用于权限验证
     * @param communityId 小区ID
     * @param roomDtos   房屋数据列表
     * @param flag       脱敏标识，不为空时不进行脱敏
     */
    private void refreshRoomOwners(String userId, String communityId, List<RoomDto> roomDtos, String flag) {
        List<String> roomIds = new ArrayList<>();
        
        // 提取所有房屋ID
        for (RoomDto roomDto : roomDtos) {
            roomIds.add(roomDto.getRoomId());
        }
        
        // 构建业主查询条件
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setCommunityId(communityId);
        ownerDto.setRoomIds(roomIds.toArray(new String[roomIds.size()]));
        
        // 检查用户权限，确定是否需要对敏感信息脱敏
        List<Map> mark = getPrivilegeOwnerList("/roomCreateFee", userId);
        
        // 查询房屋对应的业主信息
        List<OwnerDto> ownerDtos = ownerInnerServiceSMOImpl.queryOwnersByRoom(ownerDto);
        
        // 为每个房屋设置业主信息并进行脱敏处理
        for (RoomDto roomDto : roomDtos) {
            for (OwnerDto tmpOwnerDto : ownerDtos) {
                if (roomDto.getRoomId().equals(tmpOwnerDto.getRoomId())) {
                    roomDto.setOwnerId(tmpOwnerDto.getOwnerId());
                    roomDto.setOwnerName(tmpOwnerDto.getName());
                    
                    // 对业主身份证号隐藏处理
                    String idCard = tmpOwnerDto.getIdCard();
                    if (mark.size() == 0 && idCard != null && !idCard.equals("") && StringUtil.isEmpty(flag)) {
                        // 身份证号脱敏：保留前6位和后4位，中间用*代替
                        idCard = idCard.substring(0, 6) + "**********" + idCard.substring(16);
                    }
                    
                    // 对业主手机号隐藏处理
                    String link = tmpOwnerDto.getLink();
                    if (mark.size() == 0 && link != null && !link.equals("") && StringUtil.isEmpty(flag)) {
                        // 手机号脱敏：保留前3位和后4位，中间用*代替
                        link = link.substring(0, 3) + "****" + link.substring(7);
                    }
                    
                    roomDto.setIdCard(idCard);
                    roomDto.setLink(link);
                }
            }
        }
    }

    /**
     * 检查用户权限方法
     * 验证用户是否具有指定资源的访问权限，用于脱敏处理判断
     *
     * @param resource 资源路径
     * @param userId   用户ID
     * @return List<Map> 权限列表，如果为空表示没有权限
     */
    public List<Map> getPrivilegeOwnerList(String resource, String userId) {
        BasePrivilegeDto basePrivilegeDto = new BasePrivilegeDto();
        basePrivilegeDto.setResource(resource); // 设置资源路径
        basePrivilegeDto.setUserId(userId); // 设置用户ID
        // 检查用户是否具有指定资源的权限
        List<Map> privileges = menuInnerServiceSMOImpl.checkUserHasResource(basePrivilegeDto);
        return privileges;
    }
}