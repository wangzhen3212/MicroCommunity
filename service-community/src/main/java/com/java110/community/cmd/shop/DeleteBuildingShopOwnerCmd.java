package com.java110.community.cmd.shop;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.shop.BuildingShopDto;
import com.java110.dto.shop.BuildingShopOwnerDto;
import com.java110.intf.community.IBuildingShopOwnerInnerServiceSMO;
import com.java110.intf.community.IBuildingShopV1InnerServiceSMO;
import com.java110.po.shop.BuildingShopOwnerPo;
import com.java110.po.shop.BuildingShopPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Java110Cmd(serviceCode = "shop.deleteBuildingShopOwner")
public class DeleteBuildingShopOwnerCmd extends Cmd {

    @Autowired
    private IBuildingShopOwnerInnerServiceSMO buildingShopOwnerInnerServiceSMOImpl;

    @Autowired
    private IBuildingShopV1InnerServiceSMO buildingShopV1InnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        if (StringUtil.isEmpty(reqJson.getString("shopOwnerId"))) {
            Assert.hasKeyAndValue(reqJson, "shopId", "shopOwnerId 或 (shopId + ownerId) 不能为空");
            Assert.hasKeyAndValue(reqJson, "ownerId", "shopOwnerId 或 (shopId + ownerId) 不能为空");
        }
    }

    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        BuildingShopOwnerDto queryDto = new BuildingShopOwnerDto();
        queryDto.setShopOwnerId(reqJson.getString("shopOwnerId"));
        queryDto.setShopId(reqJson.getString("shopId"));
        queryDto.setOwnerId(reqJson.getString("ownerId"));
        queryDto.setStatusCd("0");
        List<BuildingShopOwnerDto> shopOwners = buildingShopOwnerInnerServiceSMOImpl.queryBuildingShopOwners(queryDto);
        if (shopOwners == null || shopOwners.isEmpty()) {
            throw new CmdException("未找到商铺业主记录");
        }

        String shopOwnerId = shopOwners.get(0).getShopOwnerId();
        String shopId = shopOwners.get(0).getShopId();

        BuildingShopOwnerPo buildingShopOwnerPo = new BuildingShopOwnerPo();
        buildingShopOwnerPo.setShopOwnerId(shopOwnerId);
        int flag = buildingShopOwnerInnerServiceSMOImpl.deleteBuildingShopOwner(buildingShopOwnerPo);
        if (flag < 1) {
            throw new CmdException("删除商铺租赁失败");
        }

        BuildingShopOwnerDto remainDto = new BuildingShopOwnerDto();
        remainDto.setShopId(shopId);
        remainDto.setStatusCd("0");
        List<BuildingShopOwnerDto> remainOwners = buildingShopOwnerInnerServiceSMOImpl.queryBuildingShopOwners(remainDto);
        String state = BuildingShopDto.STATE_FREE;
        if (remainOwners != null && !remainOwners.isEmpty()) {
            state = remainOwners.get(0).getState();
        }

        BuildingShopPo buildingShopPo = new BuildingShopPo();
        buildingShopPo.setShopId(shopId);
        buildingShopPo.setState(state);
        buildingShopPo.setStatusCd(null);
        int updateFlag = buildingShopV1InnerServiceSMOImpl.updateBuildingShop(buildingShopPo);
        if (updateFlag < 1) {
            throw new CmdException("更新商铺状态失败");
        }

        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}
