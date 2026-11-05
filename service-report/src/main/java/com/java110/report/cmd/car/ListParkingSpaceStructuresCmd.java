package com.java110.report.cmd.car;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.parking.ParkingSpaceStructureDto;
import com.java110.intf.report.IReportCommunityInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/*
     查询车位结构图数据（按停车区/分区）
 */
@Java110Cmd(serviceCode = "car.listParkingSpaceStructures")
public class ListParkingSpaceStructuresCmd extends Cmd {

    @Autowired
    private IReportCommunityInnerServiceSMO reportCommunityInnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        Assert.hasKeyAndValue(reqJson, "communityId", "未传入小区信息");
        super.validatePageInfo(reqJson);
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        ParkingSpaceStructureDto parkingSpaceStructureDto = BeanConvertUtil.covertBean(reqJson, ParkingSpaceStructureDto.class);

        int total = reportCommunityInnerServiceSMOImpl.queryParkingSpaceStructuresCount(parkingSpaceStructureDto);

        List<ParkingSpaceStructureDto> parkingSpaceStructureDtos = null;
        if (total > 0) {
            parkingSpaceStructureDtos = reportCommunityInnerServiceSMOImpl.queryParkingSpaceStructures(parkingSpaceStructureDto);
        } else {
            parkingSpaceStructureDtos = new ArrayList<>();
        }

        cmdDataFlowContext.setResponseEntity(ResultVo.createResponseEntity(
                (int) Math.ceil((double) total / (double) parkingSpaceStructureDto.getRow()),
                total,
                parkingSpaceStructureDtos));
    }


}
