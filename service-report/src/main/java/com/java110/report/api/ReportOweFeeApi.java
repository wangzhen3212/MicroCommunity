package com.java110.report.api;

import com.java110.dto.reportFee.ReportOweFeeDto;
import com.java110.report.bmo.reportOweFee.IGetReportOweFeeBMO;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/reportOweFee")
public class ReportOweFeeApi {

    @Autowired
    private IGetReportOweFeeBMO getReportOweFeeBMOImpl;

    /**
     * 微信删除消息模板
     *
     * @param communityId 小区ID
     * @return
     * @serviceCode /reportOweFee/queryReportOweFee
     * @path /app/reportOweFee/queryReportOweFee
     */
    @RequestMapping(value = "/queryReportOweFee", method = RequestMethod.GET)
    public ResponseEntity<String> queryReportOweFee(@RequestParam(value = "communityId") String communityId,
                                                    @RequestParam(value = "configIds", required = false) String configIds,
                                                    @RequestParam(value = "payObjType", required = false) String payObjType,
                                                    @RequestParam(value = "num", required = false) String num,
                                                    @RequestParam(value = "ownerName", required = false) String ownerName,
                                                    @RequestParam(value = "floorId", required = false) String floorId,
                                                    @RequestParam(value = "unitId", required = false) String unitId,
                                                    @RequestParam(value = "roomSubType", required = false) String roomSubType,
                                                    @RequestParam(value = "roomNum", required = false) String roomNum,
                                                    @RequestParam(value = "ownerId", required = false) String ownerId,
                                                    @RequestParam(value = "payerObjId", required = false) String payerObjId,
                                                    @RequestParam(value = "page") int page,
                                                    @RequestParam(value = "row") int row) {
        ReportOweFeeDto reportOweFeeDto = new ReportOweFeeDto();
        reportOweFeeDto.setPage(page);
        reportOweFeeDto.setRow(row);
        reportOweFeeDto.setPayerObjType(payObjType);
        reportOweFeeDto.setPayerObjName(num);
        reportOweFeeDto.setCommunityId(communityId);
        reportOweFeeDto.setOwnerName(ownerName);
        reportOweFeeDto.setFloorId(floorId);
        reportOweFeeDto.setUnitId(unitId);
        reportOweFeeDto.setRoomSubType(roomSubType);
        reportOweFeeDto.setRoomNum(roomNum);
        reportOweFeeDto.setOwnerId(ownerId);
        reportOweFeeDto.setPayerObjId(payerObjId);
        if (!StringUtil.isEmpty(configIds)) {
            String[] tmpConfigIds = configIds.split(",");
            reportOweFeeDto.setConfigIds(tmpConfigIds);
        }
        return getReportOweFeeBMOImpl.get(reportOweFeeDto);
    }

    /**
     * 查询所有欠费信息
     *
     * @param communityId 小区ID
     * @return
     * @serviceCode /reportOweFee/queryReportAllOweFee
     * @path /app/reportOweFee/queryReportAllOweFee
     */
    @RequestMapping(value = "/queryReportAllOweFee", method = RequestMethod.GET)
    public ResponseEntity<String> queryReportAllOweFee(@RequestParam(value = "communityId") String communityId,
                                                       @RequestParam(value = "configIds", required = false) String configIds,
                                                       @RequestParam(value = "payObjType", required = false) String payObjType,
                                                       @RequestParam(value = "hasOweFee", required = false) String hasOweFee,
                                                       @RequestParam(value = "ownerName", required = false) String ownerName,
                                                       @RequestParam(value = "floorId", required = false) String floorId,
                                                       @RequestParam(value = "unitId", required = false) String unitId,
                                                       @RequestParam(value = "roomSubType", required = false) String roomSubType,
                                                       @RequestParam(value = "roomNum", required = false) String roomNum,
                                                       @RequestParam(value = "num", required = false) String num) {
        ReportOweFeeDto reportOweFeeDto = new ReportOweFeeDto();
        reportOweFeeDto.setPayerObjType(payObjType);
        reportOweFeeDto.setPayerObjName(num);
        reportOweFeeDto.setCommunityId(communityId);
        reportOweFeeDto.setHasOweFee(hasOweFee);
        reportOweFeeDto.setOwnerName(ownerName);
        reportOweFeeDto.setFloorId(floorId);
        reportOweFeeDto.setUnitId(unitId);
        reportOweFeeDto.setRoomSubType(roomSubType);
        reportOweFeeDto.setRoomNum(roomNum);
        if (!StringUtil.isEmpty(configIds)) {
            String[] tmpConfigIds = configIds.split(",");
            reportOweFeeDto.setConfigIds(tmpConfigIds);
        }
        return getReportOweFeeBMOImpl.getAllFees(reportOweFeeDto);
    }
}
