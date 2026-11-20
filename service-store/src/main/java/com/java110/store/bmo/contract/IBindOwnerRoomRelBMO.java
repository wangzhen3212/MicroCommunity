package com.java110.store.bmo.contract;

/**
 * 合同生效后的业主与资产关系绑定业务接口（审批通过时调用，可被变更单复用）
 */
public interface IBindOwnerRoomRelBMO {

    /**
     * 合同生效：按合同业主与关联房屋建立业主房屋关系
     *
     * @param contractId  合同ID
     * @param storeId     商户ID
     * @param communityId 小区ID
     * @return true 已执行绑定；false 无需绑定（非房屋合同/无房屋/业主缺失）
     */
    boolean bindOwnerRoomRel(String contractId, String storeId, String communityId);
}
