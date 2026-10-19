<template>
  <div>
    <div class="margin-top-lg flex justify-between">
      <div class="flex justify-start">
        <div :span="3" class="padding-right-xs padding-left-xl">
          <el-select v-model="simplifyFeeReceiptInfo.objType" size="small" @change="_changeSimplifyFeeReceiptFeeTypeCd"
            :placeholder="$t('simplifyFeeReceipt.selectFeeType')">
            <el-option v-for="(item, index) in simplifyFeeReceiptInfo.objTypes" :key="index" :label="item.name"
              :value="item.statusCd"></el-option>
          </el-select>
        </div>
        <div :span="3" class="padding-right-xs padding-left-xl" v-if="simplifyFeeReceiptInfo.objType == '4444'">
          <el-select v-model="simplifyFeeReceiptInfo.shopId" size="small" @change="changeSimplifyFeeReceiptObj"
            :placeholder="$t('simplifyFeeReceipt.selectShop')">
            <el-option v-for="(item, index) in simplifyFeeReceiptInfo.ownerShops" :key="index" :label="item.shopNum"
              :value="item.shopId"></el-option>
          </el-select>
        </div>
        <div :span="3" class="padding-right-xs padding-left-xl" v-else-if="simplifyFeeReceiptInfo.objType == '6666'">
          <el-select v-model="simplifyFeeReceiptInfo.carId" size="small" @change="changeSimplifyFeeReceiptObj"
            :placeholder="$t('simplifyFeeReceipt.selectCar')">
            <el-option v-for="(item, index) in simplifyFeeReceiptInfo.ownerCars" :key="index" :label="item.carNum"
              :value="item.carId"></el-option>
          </el-select>
        </div>
        <div :span="3" class="padding-right-xs padding-left-xl" v-else-if="simplifyFeeReceiptInfo.objType == '7777'">
          <el-select v-model="simplifyFeeReceiptInfo.contractId" size="small" @change="changeSimplifyFeeReceiptObj"
            :placeholder="$t('simplifyFeeReceipt.selectContract')">
            <el-option v-for="(item, index) in simplifyFeeReceiptInfo.ownerContracts" :key="index"
              :label="item.contractCode" :value="item.contractId"></el-option>
          </el-select>
        </div>
        <div :span="3" class="padding-right-xs padding-left-xl" v-if="simplifyFeeReceiptInfo.objType != '7777'">
          <el-select v-model="simplifyFeeReceiptInfo.ownerFlag" size="small" @change="_queryFeeReceiptMethod">
            <el-option :label="$t('simplifyFeeReceipt.current') + _getFeeObjName(simplifyFeeReceiptInfo.objType)"
              value="F"></el-option>
            <el-option :label="$t('simplifyFeeReceipt.all') + _getFeeObjName(simplifyFeeReceiptInfo.objType)"
              value="T"></el-option>
          </el-select>
        </div>
        <div :span="3" class="padding-right-xs padding-left-xl">
          <el-input v-model="simplifyFeeReceiptInfo.receiptCode" size="small"
            :placeholder="$t('simplifyFeeReceipt.receiptCode')"></el-input>
        </div>
        <div :span="2" class="padding-right-xs padding-left-xl">
          <el-button type="primary" size="small" @click="_queryFeeReceiptMethod">
            {{ $t('simplifyFeeReceipt.query') }}
          </el-button>
        </div>
      </div>
      <div :span="8" class="text-right padding-right-lg">
        <el-button type="primary" size="small" @click="_printFeeReceipt('OFF')">
          {{ $t('simplifyFeeReceipt.print') }}
        </el-button>
        <el-button type="primary" size="small" @click="_printFeeReceipt('ON')">
          {{ $t('simplifyFeeReceipt.mergePrint') }}
        </el-button>
        <el-button type="primary" size="small" @click="_printFeeSmallReceipt">
          {{ $t('simplifyFeeReceipt.printSmall') }}
        </el-button>
        <el-button type="primary" size="small" @click="_printApplyFeeReceipt">
          {{ $t('simplifyFeeReceipt.apply') }}
        </el-button>
      </div>
    </div>
    <div class="margin-top">
      <el-table :data="simplifyFeeReceiptInfo.feeReceipts" @selection-change="handleSelectionChange" border stripe>
        <el-table-column type="selection" width="55" align="center"></el-table-column>
        <el-table-column prop="receiptCode" :label="$t('simplifyFeeReceipt.receiptCode')"
          align="center"></el-table-column>
        <el-table-column :label="$t('simplifyFeeReceipt.feeType')" align="center">
          <template slot-scope="scope">
            {{ _getFeeObjName(scope.row.objType) }}{{ $t('simplifyFeeReceipt.fee') }}
          </template>
        </el-table-column>
        <el-table-column prop="objName" :label="_getFeeObjName(simplifyFeeReceiptInfo.objType)"
          align="center"></el-table-column>
        <el-table-column prop="payObjName" :label="$t('simplifyFeeReceipt.owner')" align="center"></el-table-column>
        <el-table-column prop="feeName" :label="$t('simplifyFeeReceipt.feeItem')" align="center"></el-table-column>
        <el-table-column :label="$t('simplifyFeeReceipt.timePeriod')" align="center">
          <template slot-scope="scope">
            {{ dateFormat(scope.row.startTime) }}~<br>
            <span v-if="scope.row.startTime >= scope.row.endTime">
              {{ dateFormat(scope.row.endTime) }}
            </span>
            <span v-else>
              {{ dateFormat(scope.row.endTime) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="amount" :label="$t('simplifyFeeReceipt.totalAmount')" align="center">
          <template slot-scope="scope">
            {{ scope.row.amount }}{{ $t('simplifyFeeReceipt.yuan') }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('simplifyFeeReceipt.feeMonth')" align="center">
          <template slot-scope="scope">
            {{ scope.row.feeMonth || '-' }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('simplifyFeeReceipt.payWay')" align="center">
          <template slot-scope="scope">
            {{ scope.row.primeRate || scope.row.payWay || '-' }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('simplifyFeeReceipt.deductionAmount')" align="center">
          <template slot-scope="scope">
            {{ scope.row.deductionAmount || '-' }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('simplifyFeeReceipt.lateAmount')" align="center">
          <template slot-scope="scope">
            {{ scope.row.lateAmount || '-' }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('simplifyFeeReceipt.acctAmount')" align="center">
          <template slot-scope="scope">
            {{ scope.row.acctAmount || '-' }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('simplifyFeeReceipt.degree')" align="center">
          <template slot-scope="scope">
            <span v-if="scope.row.startDegree || scope.row.endDegree">
              {{ scope.row.startDegree || '-' }} ~ {{ scope.row.endDegree || '-' }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="$t('simplifyFeeReceipt.payTime')" align="center"></el-table-column>
        <el-table-column prop="receiptId" :label="$t('simplifyFeeReceipt.receiptId')" align="center"></el-table-column>
      </el-table>
      <el-pagination @current-change="handleCurrentChange" :current-page="currentPage" :page-size="pageSize"
        layout="total, prev, pager, next" :total="total" class="text-right" style="margin-top: 20px">
      </el-pagination>
    </div>
  </div>
</template>

<script>
import { getCommunityId, getDict } from '@/api/community/communityApi'
import { queryFeeReceipt, listFeePrintPage, queryOwnerCars, queryContract, listOwnerShops } from '@/api/fee/simplifyFeeReceiptApi'
import { dateFormat } from '@/utils/dateUtil'

const DEFAULT_OBJ_TYPES = [
  { statusCd: '3333', nameKey: 'room' },
  { statusCd: '4444', nameKey: 'shop' },
  { statusCd: '6666', nameKey: 'car' },
  { statusCd: '7777', nameKey: 'contract' }
]

export default {
  name: 'SimplifyFeeReceipt',
  data() {
    return {
      DEFAULT_PAGE: 1,
      DEFAULT_ROWS: 50,
      simplifyFeeReceiptInfo: {
        feeReceipts: [],
        objType: '3333',
        objId: '',
        payObjId: '',
        roomId: '',
        shopId: '',
        carId: '',
        receiptCode: '',
        total: '',
        records: '',
        ownerId: '',
        objTypes: [],
        ownerCars: [],
        ownerContracts: [],
        ownerShops: [],
        selectReceipts: [],
        ownerFlag: 'F',
        contractId: '',
        quan: false,
        printUrl: '/#/pages/property/printPayFee'
      },
      currentPage: 1,
      pageSize: 10,
      total: 0
    }
  },
  watch: {
    'simplifyFeeReceiptInfo.selectReceipts': {
      handler(newVal) {
        this.simplifyFeeReceiptInfo.quan = newVal.length === this.simplifyFeeReceiptInfo.feeReceipts.length
      },
      deep: true
    }
  },
  created() {
    this._loadDictData()
  },
  methods: {
    handleSwitch(param) {
      this.clearSimplifyFeeReceiptInfo()
      this._loadDictData()
      this._listFeePrintPages()

      this.simplifyFeeReceiptInfo.ownerId = param.ownerId || ''
      this.simplifyFeeReceiptInfo.payObjId = param.ownerId || ''
      this.simplifyFeeReceiptInfo.roomId = param.roomId || ''
      this.simplifyFeeReceiptInfo.objType = '3333'

      if (!this.simplifyFeeReceiptInfo.ownerId) {
        return
      }
      this._listSimplifyFeeReceipt(this.DEFAULT_PAGE, this.DEFAULT_ROWS)
    },
    handleCurrentChange(val) {
      this._listSimplifyFeeReceipt(val, this.DEFAULT_ROWS)
    },
    async _loadDictData() {
      let dictList = []
      try {
        const data = await getDict('fee_receipt', 'obj_type')
        dictList = Array.isArray(data) ? data : []
      } catch (error) {
        console.error('Failed to load dict data:', error)
      }
      const objTypes = []
      DEFAULT_OBJ_TYPES.forEach(item => {
        const dictItem = dictList.find(dict => dict.statusCd === item.statusCd)
        objTypes.push({
          statusCd: item.statusCd,
          name: dictItem ? dictItem.name : this.$t('simplifyFeeReceipt.' + item.nameKey)
        })
      })
      dictList.forEach(dictItem => {
        const exists = objTypes.some(item => item.statusCd === dictItem.statusCd)
        if (!exists) {
          objTypes.push({ statusCd: dictItem.statusCd, name: dictItem.name })
        }
      })
      this.simplifyFeeReceiptInfo.objTypes = objTypes
    },
    async _listSimplifyFeeReceipt(page, rows) {
      this.simplifyFeeReceiptInfo.selectReceipts = []
      this.simplifyFeeReceiptInfo.quan = false

      const objType = this.simplifyFeeReceiptInfo.objType
      const objIdMap = {
        '3333': this.simplifyFeeReceiptInfo.roomId,
        '4444': this.simplifyFeeReceiptInfo.shopId,
        '6666': this.simplifyFeeReceiptInfo.carId,
        '7777': this.simplifyFeeReceiptInfo.contractId
      }

      const params = {
        page,
        row: rows,
        objType,
        objId: '',
        receiptCode: this.simplifyFeeReceiptInfo.receiptCode,
        communityId: getCommunityId()
      }

      if (this.simplifyFeeReceiptInfo.ownerFlag === 'F') {
        params.objId = objIdMap[objType] || ''
        if (!params.objId) {
          this.$message.warning(this.$t('simplifyFeeReceipt.needObj'))
          return
        }
      } else {
        params.payObjId = this.simplifyFeeReceiptInfo.payObjId
        if (!params.payObjId) {
          this.$message.warning(this.$t('simplifyFeeReceipt.needOwner'))
          return
        }
      }

      this.currentPage = page

      try {
        const res = await queryFeeReceipt(params)
        this.simplifyFeeReceiptInfo.total = res.total
        this.simplifyFeeReceiptInfo.records = res.records
        this.simplifyFeeReceiptInfo.feeReceipts = res.data
        this.total = res.total
      } catch (error) {
        console.error('Request failed:', error)
      }
    },
    _queryFeeReceiptMethod() {
      this._listSimplifyFeeReceipt(this.DEFAULT_PAGE, this.DEFAULT_ROWS)
    },
    _printFeeReceipt(merge) {
      if (this.simplifyFeeReceiptInfo.selectReceipts.length < 1) {
        this.$message.warning(this.$t('simplifyFeeReceipt.selectReceipt'))
        return
      }
      const receiptids = this.simplifyFeeReceiptInfo.selectReceipts.join(',')
      window.open(`${this.simplifyFeeReceiptInfo.printUrl}?receiptIds=${receiptids}&apply=N&merge=${merge}`)
    },
    _printApplyFeeReceipt() {
      if (this.simplifyFeeReceiptInfo.selectReceipts.length < 1) {
        this.$message.warning(this.$t('simplifyFeeReceipt.selectReceipt'))
        return
      }
      const receiptids = this.simplifyFeeReceiptInfo.selectReceipts.join(',')
      window.open(`/#/pages/property/printPayFee?receiptIds=${receiptids}&apply=Y`)
    },
    _printFeeSmallReceipt() {
      if (this.simplifyFeeReceiptInfo.selectReceipts.length < 1) {
        this.$message.warning(this.$t('simplifyFeeReceipt.selectReceipt'))
        return
      }
      const receiptids = this.simplifyFeeReceiptInfo.selectReceipts.join(',')
      window.open(`/#/pages/property/printSmallPayFee?receiptIds=${receiptids}`)
    },
    clearSimplifyFeeReceiptInfo() {
      this.simplifyFeeReceiptInfo = {
        feeReceipts: [],
        objType: '3333',
        objId: '',
        payObjId: '',
        roomId: '',
        shopId: '',
        carId: '',
        receiptCode: '',
        total: '',
        records: '',
        ownerId: '',
        objTypes: [],
        ownerCars: [],
        ownerContracts: [],
        ownerShops: [],
        selectReceipts: [],
        ownerFlag: 'F',
        contractId: '',
        quan: false,
        printUrl: '/#/pages/property/printPayFee'
      }
    },
    async _changeSimplifyFeeReceiptFeeTypeCd(objType) {
      this.simplifyFeeReceiptInfo.shopId = ''
      this.simplifyFeeReceiptInfo.carId = ''
      this.simplifyFeeReceiptInfo.contractId = ''
      if (!this.simplifyFeeReceiptInfo.ownerId) {
        this.$emit('notify', {})
        return
      }
      if (objType === '4444') {
        await this._listSimplifyFeeReceiptOwnerShop()
      } else if (objType === '6666') {
        await this._listSimplifyFeeReceiptOwnerCar()
      } else if (objType === '7777') {
        await this._listSimplifyFeeReceiptOwnerContract()
      }
      this.$emit('notify', {})
    },
    changeSimplifyFeeReceiptObj() {
      this._queryFeeReceiptMethod()
    },
    async _listSimplifyFeeReceiptOwnerShop() {
      try {
        const res = await listOwnerShops({
          page: 1,
          row: 100,
          ownerId: this.simplifyFeeReceiptInfo.ownerId
        })
        const shops = res && Array.isArray(res.data) ? res.data : []
        this.simplifyFeeReceiptInfo.ownerShops = shops
        if (shops.length > 0) {
          this.simplifyFeeReceiptInfo.shopId = shops[0].shopId
          return
        }
        this.$message.warning(this.$t('simplifyFeeReceipt.noShop'))
      } catch (error) {
        console.error('Request failed:', error)
      }
    },
    async _listSimplifyFeeReceiptOwnerCar() {
      try {
        const res = await queryOwnerCars({
          page: 1,
          row: 100,
          ownerId: this.simplifyFeeReceiptInfo.ownerId,
          carTypeCd: '1001',
          communityId: getCommunityId()
        })
        const cars = res && Array.isArray(res.data) ? res.data : []
        this.simplifyFeeReceiptInfo.ownerCars = cars
        if (cars.length > 0) {
          this.simplifyFeeReceiptInfo.carId = cars[0].carId
          return
        }
        this.$message.warning(this.$t('simplifyFeeReceipt.noCar'))
      } catch (error) {
        console.error('Request failed:', error)
      }
    },
    async _listSimplifyFeeReceiptOwnerContract() {
      try {
        const res = await queryContract({
          page: 1,
          row: 100,
          objPersonId: this.simplifyFeeReceiptInfo.ownerId,
          communityId: getCommunityId()
        })
        const contracts = res && Array.isArray(res.data) ? res.data : []
        this.simplifyFeeReceiptInfo.ownerContracts = contracts
        if (contracts.length > 0) {
          this.simplifyFeeReceiptInfo.contractId = contracts[0].contractId
          return
        }
        this.$message.warning(this.$t('simplifyFeeReceipt.noContract'))
      } catch (error) {
        console.error('Request failed:', error)
      }
    },
    checkAllReceipt(e) {
      const checkObj = document.querySelectorAll('.checReceiptItem')
      if (e.target.checked) {
        checkObj.forEach(item => {
          if (!item.checked) {
            this.simplifyFeeReceiptInfo.selectReceipts.push(item.value)
          }
        })
      } else {
        this.simplifyFeeReceiptInfo.selectReceipts = []
      }
    },
    _getFeeObjName(feeTypeCd) {
      if (feeTypeCd === '3333') {
        return this.$t('simplifyFeeReceipt.room')
      } else if (feeTypeCd === '4444') {
        return this.$t('simplifyFeeReceipt.shop')
      } else if (feeTypeCd === '6666') {
        return this.$t('simplifyFeeReceipt.car')
      } else {
        return this.$t('simplifyFeeReceipt.contract')
      }
    },
    async _listFeePrintPages() {
      try {
        const res = await listFeePrintPage({
          page: 1,
          row: 1,
          state: 'T',
          communityId: getCommunityId()
        })
        const feePrintPages = res.data
        if (feePrintPages && feePrintPages.length > 0) {
          this.simplifyFeeReceiptInfo.printUrl = feePrintPages[0].url
        }
      } catch (error) {
        console.error('Request failed:', error)
      }
    },
    open(params) {
      this.handleSwitch(params)
    },
    dateFormat(date) {
      return dateFormat(date)
    },
    handleSelectionChange(val) {
      this.simplifyFeeReceiptInfo.selectReceipts = val.map(item => item.receiptId)
    }
  }
}
</script>

<style scoped>
.margin-top-lg {
  margin-top: 20px;
}

.padding-right-xs {
  padding-right: 5px;
}

.padding-left-xl {
  padding-left: 20px;
}

.text-right {
  text-align: right;
}

.padding-right-lg {
  padding-right: 20px;
}
</style>