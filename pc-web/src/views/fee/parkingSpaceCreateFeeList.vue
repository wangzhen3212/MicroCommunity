<template>
  <create-fee-layout :title="parkingSpaceCreateFeeInfo.psName" :empty="isTreeEmpty"
    :empty-text="$t('parkingSpaceCreateFee.parkingSpaceEmpty')" storage-key="parkingSpaceCreateFeeTreeWidth">
    <div slot="tree">
      <parking-space-tree-has-owner ref="parkingSpaceTree" :auto-select-first="false"
        @selectParkingSpace="handleSelectParkingSpace" @treeEmpty="handleTreeEmpty"></parking-space-tree-has-owner>
    </div>

    <template slot="tools">
      <el-button v-if="hasPrivilege('502022073096990007')" size="small" @click="_openFeeImportExcel">
        <i class="el-icon-download"></i>
        {{ $t('parkingSpaceCreateFee.customTemplate') }}
      </el-button>
      <el-button v-if="hasPrivilege('502022073096990007')" size="small" @click="_openDoCreateRoomFee">
        <i class="el-icon-plus"></i>
        {{ $t('parkingSpaceCreateFee.customImport') }}
      </el-button>
    </template>

    <div slot="content">
      <el-tabs v-model="parkingSpaceCreateFeeInfo._currentTab"
        @tab-click="changeTab(parkingSpaceCreateFeeInfo._currentTab)">
        <el-tab-pane :label="$t('parkingSpaceCreateFee.parkingSpaceFee')" name="simplifyPayerObjFee">
          <simplify-parking-space-fee v-if="parkingSpaceCreateFeeInfo._currentTab === 'simplifyPayerObjFee'"
            ref="simplifyPayerObjFee" :key="'simplifyPayerObjFee' + parkingSpaceCreateFeeInfo.psId"></simplify-parking-space-fee>
        </el-tab-pane>
        <el-tab-pane :label="$t('parkingSpaceCreateFee.paymentHistory')" name="simplifyHisFee">
          <simplify-his-fee v-if="parkingSpaceCreateFeeInfo._currentTab === 'simplifyHisFee'" ref="simplifyHisFee"
            :key="'simplifyHisFee' + parkingSpaceCreateFeeInfo.psId"></simplify-his-fee>
        </el-tab-pane>
        <el-tab-pane :label="$t('parkingSpaceCreateFee.parkingSpaceCar')" name="parkingSpaceCarDetail">
          <parking-space-car-detail v-if="parkingSpaceCreateFeeInfo._currentTab === 'parkingSpaceCarDetail'"
            ref="parkingSpaceCarDetail" :key="'parkingSpaceCarDetail' + parkingSpaceCreateFeeInfo.psId"></parking-space-car-detail>
        </el-tab-pane>
        <el-tab-pane :label="$t('parkingSpaceCreateFee.prepayment')" name="prePayFeeDetail">
          <pre-pay-fee-detail v-if="parkingSpaceCreateFeeInfo._currentTab === 'prePayFeeDetail'" ref="prePayFeeDetail"
            :key="'prePayFeeDetail' + parkingSpaceCreateFeeInfo.psId"></pre-pay-fee-detail>
        </el-tab-pane>
        <el-tab-pane v-if="parkingSpaceCreateFeeInfo.ownerId" :label="$t('parkingSpaceCreateFee.house')"
          name="simplifyOwnerRooms">
          <simplify-owner-rooms v-if="parkingSpaceCreateFeeInfo._currentTab === 'simplifyOwnerRooms'"
            ref="simplifyOwnerRooms" :key="'simplifyOwnerRooms' + parkingSpaceCreateFeeInfo.psId"></simplify-owner-rooms>
        </el-tab-pane>
        <el-tab-pane v-if="parkingSpaceCreateFeeInfo.ownerId" :label="$t('parkingSpaceCreateFee.owner')"
          name="carDetailOwner">
          <car-detail-owner v-if="parkingSpaceCreateFeeInfo._currentTab === 'carDetailOwner'" ref="carDetailOwner"
            :key="'carDetailOwner' + parkingSpaceCreateFeeInfo.psId"></car-detail-owner>
        </el-tab-pane>
        <el-tab-pane v-if="hasPrivilege('502020092373407363')" :label="$t('parkingSpaceCreateFee.reprintReceipt')"
          name="simplifyFeeReceipt">
          <simplify-fee-receipt v-if="parkingSpaceCreateFeeInfo._currentTab === 'simplifyFeeReceipt'"
            ref="simplifyFeeReceipt" :key="'simplifyFeeReceipt' + parkingSpaceCreateFeeInfo.psId"></simplify-fee-receipt>
        </el-tab-pane>
      </el-tabs>
    </div>

    <delete-fee ref="deleteFee" @success="handleSuccess" />
    <edit-fee ref="editFee" @success="handleSuccess" />
    <split-fee ref="splitFee" @success="handleSuccess" />
    <export-parking-space-fee-import-excel ref="exportParkingSpaceFeeImportExcel" />
    <do-import-create-fee ref="doImportCreateFee" />
  </create-fee-layout>
</template>

<script>
import { getCommunityId } from '@/api/community/communityApi'

import CreateFeeLayout from '@/components/fee/createFeeLayout'
import ParkingSpaceTreeHasOwner from '@/components/car/parkingSpaceTreeHasOwner'
import SimplifyParkingSpaceFee from '@/components/simplify/simplifyParkingSpaceFee'
import SimplifyHisFee from '@/components/fee/simplifyHisFee'
import PrePayFeeDetail from '@/components/fee/prePayFeeDetail'
import SimplifyOwnerRooms from '@/components/fee/simplifyOwnerRooms'
import CarDetailOwner from '@/components/fee/carDetailOwner'
import SimplifyFeeReceipt from '@/components/fee/simplifyFeeReceipt'
import ParkingSpaceCarDetail from '@/components/fee/parkingSpaceCarDetail'
import DeleteFee from '@/components/fee/deleteFee'
import EditFee from '@/components/fee/editFee'
import SplitFee from '@/components/fee/splitFee'

import ExportParkingSpaceFeeImportExcel from '@/components/fee/exportParkingSpaceFeeImportExcel'
import DoImportCreateFee from '@/components/fee/doImportCreateFee'

import { openFeeTabPane } from '@/utils/feeTabUtil'

export default {
  name: 'ParkingSpaceCreateFeeList',
  components: {
    CreateFeeLayout,
    ParkingSpaceTreeHasOwner,
    SimplifyParkingSpaceFee,
    SimplifyHisFee,
    PrePayFeeDetail,
    SimplifyOwnerRooms,
    CarDetailOwner,
    SimplifyFeeReceipt,
    ParkingSpaceCarDetail,
    DeleteFee,
    EditFee,
    SplitFee,
    ExportParkingSpaceFeeImportExcel,
    DoImportCreateFee
  },
  data() {
    return {
      communityId: '',
      isTreeEmpty: false,
      parkingSpaceCreateFeeInfo: {
        psId: '',
        psName: '',
        paId: '',
        areaNum: '',
        sectionId: '',
        sectionName: '',
        num: '',
        ownerId: '',
        ownerName: '',
        carId: '',
        carNum: '',
        startTime: '',
        endTime: '',
        _currentTab: 'simplifyPayerObjFee'
      }
    }
  },
  async created() {
    this.communityId = await getCommunityId()
  },
  methods: {
    handleTreeEmpty() {
      this.isTreeEmpty = true
    },
    handleSelectParkingSpace(param) {
      this.parkingSpaceCreateFeeInfo.psId = param.psId
      this.parkingSpaceCreateFeeInfo.psName = param.psName
      this.parkingSpaceCreateFeeInfo.paId = param.paId
      this.parkingSpaceCreateFeeInfo.areaNum = param.areaNum
      this.parkingSpaceCreateFeeInfo.sectionId = param.sectionId
      this.parkingSpaceCreateFeeInfo.sectionName = param.sectionName
      this.parkingSpaceCreateFeeInfo.num = param.num
      this.parkingSpaceCreateFeeInfo.ownerId = param.ownerId
      this.parkingSpaceCreateFeeInfo.ownerName = param.ownerName
      this.parkingSpaceCreateFeeInfo.carId = param.carId
      this.parkingSpaceCreateFeeInfo.carNum = param.carNum
      this.parkingSpaceCreateFeeInfo.startTime = param.startTime
      this.parkingSpaceCreateFeeInfo.endTime = param.endTime
      this.changeTab(this.parkingSpaceCreateFeeInfo._currentTab)
    },
    changeTab(tabName) {
      this.parkingSpaceCreateFeeInfo._currentTab = tabName
      openFeeTabPane(this, tabName, {
        psId: this.parkingSpaceCreateFeeInfo.psId,
        psName: this.parkingSpaceCreateFeeInfo.psName,
        paId: this.parkingSpaceCreateFeeInfo.paId,
        sectionId: this.parkingSpaceCreateFeeInfo.sectionId,
        num: this.parkingSpaceCreateFeeInfo.num,
        ownerId: this.parkingSpaceCreateFeeInfo.ownerId,
        ownerName: this.parkingSpaceCreateFeeInfo.ownerName,
        carId: this.parkingSpaceCreateFeeInfo.carId,
        carNum: this.parkingSpaceCreateFeeInfo.carNum,
        startTime: this.parkingSpaceCreateFeeInfo.startTime,
        endTime: this.parkingSpaceCreateFeeInfo.endTime,
        payerObjId: this.parkingSpaceCreateFeeInfo.psId,
        payerObjName: this.parkingSpaceCreateFeeInfo.psName,
        payerObjType: '6666'
      })
    },
    _openFeeImportExcel() {
      this.$refs.exportParkingSpaceFeeImportExcel.open()
    },
    _openDoCreateRoomFee() {
      this.$refs.doImportCreateFee.open()
    },
    handleSuccess() {
      this.changeTab(this.parkingSpaceCreateFeeInfo._currentTab)
    }
  }
}
</script>
