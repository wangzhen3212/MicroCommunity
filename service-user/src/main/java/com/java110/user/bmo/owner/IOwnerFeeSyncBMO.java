package com.java110.user.bmo.owner;

import com.java110.dto.owner.OwnerDto;

public interface IOwnerFeeSyncBMO {

    /**
     * 业主名称 / owner_id 变更后同步 pay_fee_rule、pay_fee 的业主信息
     *
     * @param oldOwner 变更前业主信息
     * @param newOwner 变更后业主信息
     * @return 同步影响行数
     */
    int syncOwnerToFee(OwnerDto oldOwner, OwnerDto newOwner);
}
