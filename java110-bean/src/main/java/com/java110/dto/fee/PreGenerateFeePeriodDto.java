package com.java110.dto.fee;

import java.io.Serializable;

/**
 * 预生成账单期次
 */
public class PreGenerateFeePeriodDto implements Serializable {

    /**
     * 期号，从 1 开始连续编号
     */
    private int seq;

    /**
     * 账期开始时间
     */
    private String startTime;

    /**
     * 账期结束时间
     */
    private String endTime;

    public PreGenerateFeePeriodDto() {
    }

    public PreGenerateFeePeriodDto(int seq, String startTime, String endTime) {
        this.seq = seq;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
}
