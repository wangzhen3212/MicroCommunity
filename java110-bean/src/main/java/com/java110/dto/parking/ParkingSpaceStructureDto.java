package com.java110.dto.parking;

/**
 * 车位结构图数据封装（车位台账 + 欠费汇总）
 */
public class ParkingSpaceStructureDto extends ParkingSpaceDto {

    private String oweAmount;

    public String getOweAmount() {
        return oweAmount;
    }

    public void setOweAmount(String oweAmount) {
        this.oweAmount = oweAmount;
    }
}
