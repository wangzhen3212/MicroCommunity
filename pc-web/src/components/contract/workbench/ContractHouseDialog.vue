<template>
  <el-dialog
    :title="$t('contractWorkbench.target.addRent')"
    :visible.sync="visible"
    width="860px"
    @close="handleClose"
  >
    <el-tabs v-model="tab" @tab-click="handleTabChange">
      <el-tab-pane :label="$t('contractWorkbench.target.tabRoom')" name="room" />
      <el-tab-pane :label="$t('contractWorkbench.target.tabShop')" name="shop" />
    </el-tabs>

    <div class="cc-toolbar">
      <template v-if="isShop">
        <el-input
          style="width: 180px"
          v-model="shopNum"
          :placeholder="$t('contractWorkbench.target.shopName')"
          clearable
          @keyup.enter.native="handleSearch"
        />
      </template>
      <template v-else>
        <el-input
          style="width: 130px"
          v-model="floorNum"
          :placeholder="$t('contractWorkbench.target.floorNum')"
          clearable
          @keyup.enter.native="handleSearch"
        />
        <el-input
          style="width: 150px"
          v-model="roomNumLike"
          :placeholder="$t('contractWorkbench.target.roomNum')"
          clearable
          @keyup.enter.native="handleSearch"
        />
      </template>
      <el-button type="primary" @click="handleSearch">
        <i class="el-icon-search"></i>
        {{ $t('common.search') }}
      </el-button>
      <span class="cc-toolbar-tip">{{ $t('contractWorkbench.target.noOwnerTip') }}</span>
    </div>

    <el-table
      :data="rows"
      size="mini"
      border
      v-loading="loading"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="46" />
      <el-table-column :label="isShop ? $t('contractWorkbench.target.shopName') : $t('contractWorkbench.target.roomName')">
        <template slot-scope="scope">{{ rowName(scope.row) }}</template>
      </el-table-column>
      <el-table-column v-if="isShop" prop="locationName" :label="$t('contractWorkbench.target.shopLocation')" />
      <el-table-column :label="$t('contractWorkbench.target.state')" align="center" width="100">
        <template slot-scope="scope">{{ rowStateName(scope.row) }}</template>
      </el-table-column>
      <el-table-column :label="$t('contractWorkbench.target.area')" align="center" width="100">
        <template slot-scope="scope">{{ scope.row.builtUpArea || '-' }}</template>
      </el-table-column>
      <el-table-column
        :label="isShop ? $t('contractWorkbench.target.shopArea') : $t('contractWorkbench.target.roomArea')"
        align="center"
        width="100"
      >
        <template slot-scope="scope">{{ rowAreaValue(scope.row) || '-' }}</template>
      </el-table-column>
      <el-table-column :label="$t('contractWorkbench.target.rent')" align="center" width="110">
        <template slot-scope="scope">{{ formatAmount(rowRentValue(scope.row)) }}</template>
      </el-table-column>
    </el-table>

    <el-pagination
      :current-page="page.current"
      :page-size="page.size"
      :total="page.total"
      layout="total, prev, pager, next"
      style="margin-top: 12px; text-align: right"
      @current-change="handleCurrentChange"
    />

    <div class="cc-selected">
      <div class="cc-selected-title">{{ $t('contractWorkbench.target.house') }}（{{ selected.length }}）</div>
      <div v-if="selected.length" class="cc-selected-list">
        <el-tag
          v-for="(item, index) in selected"
          :key="'obj' + index"
          size="small"
          closable
          @close="removeSelected(index)"
        >
          {{ rowName(item) }}
        </el-tag>
      </div>
      <div v-else class="cc-empty">{{ $t('contractWorkbench.target.empty') }}</div>
    </div>

    <span slot="footer">
      <el-button @click="handleClose">{{ $t('common.cancel') }}</el-button>
      <el-button type="primary" @click="handleConfirm">{{ $t('common.confirm') }}</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { getCommunityId } from '@/api/community/communityApi'
import { queryLeasableRooms } from '@/api/contract/addContractApi'
import { listShops } from '@/api/shop/shopCreateFeeApi'
import { PAYER_OBJ_TYPE, formatAmount } from '@/utils/contractUtil'

export default {
  name: 'ContractHouseDialog',
  data() {
    return {
      visible: false,
      loading: false,
      tab: 'room',
      floorNum: '',
      roomNumLike: '',
      shopNum: '',
      rows: [],
      selected: [],
      page: {
        current: 1,
        size: 10,
        total: 0
      }
    }
  },
  computed: {
    isShop() {
      return this.tab === 'shop'
    },
    payerObjType() {
      return this.isShop ? PAYER_OBJ_TYPE.SHOP : PAYER_OBJ_TYPE.ROOM
    }
  },
  methods: {
    formatAmount,
    open(exists) {
      this.visible = true
      this.selected = exists ? exists.slice() : []
      this.floorNum = ''
      this.roomNumLike = ''
      this.shopNum = ''
      this.page.current = 1
      this.tab = this.selected.length && this.selected[0].payerObjType === PAYER_OBJ_TYPE.SHOP ? 'shop' : 'room'
      this.queryData()
    },
    handleClose() {
      this.visible = false
    },
    handleTabChange() {
      if (this.selected.length && this.selected[0].payerObjType !== this.payerObjType) {
        this.selected = []
        this.$message.warning(this.$t('contractWorkbench.msg.mixPayerObjType'))
      }
      this.page.current = 1
      this.queryData()
    },
    handleSearch() {
      this.page.current = 1
      this.queryData()
    },
    queryData() {
      if (this.isShop) {
        this.queryShops()
        return
      }
      this.queryRooms()
    },
    queryRooms() {
      this.loading = true
      queryLeasableRooms({
        communityId: getCommunityId(),
        roomType: '1010301',
        floorNum: this.floorNum,
        roomNumLike: this.roomNumLike,
        page: this.page.current,
        row: this.page.size
      }).then(res => {
        const payload = res || {}
        this.rows = payload.data || []
        this.page.total = payload.total || this.rows.length
      }).catch(error => {
        this.rows = []
        this.page.total = 0
        this.$message.error(error.message || this.$t('contractWorkbench.msg.loadError'))
      }).finally(() => {
        this.loading = false
      })
    },
    queryShops() {
      this.loading = true
      listShops({
        shopNum: this.shopNum,
        page: this.page.current,
        row: this.page.size
      }).then(res => {
        const payload = res || {}
        this.rows = payload.data || []
        this.page.total = payload.total || this.rows.length
      }).catch(error => {
        this.rows = []
        this.page.total = 0
        this.$message.error(error.message || this.$t('contractWorkbench.msg.loadError'))
      }).finally(() => {
        this.loading = false
      })
    },
    rowName(row) {
      if (!row) {
        return ''
      }
      if (row.objName) {
        return row.objName
      }
      if (row.shopNum || row.locationName) {
        return row.shopNum || row.locationName
      }
      return row.roomName || ((row.floorNum || '') + '-' + (row.unitNum || '') + '-' + (row.roomNum || ''))
    },
    rowAreaValue(row) {
      return row.areaValue || (this.isShop ? row.shopArea : row.roomArea)
    },
    rowRentValue(row) {
      return row.rentValue || (this.isShop ? row.shopRent : row.roomRent)
    },
    rowStateName(row) {
      if (row && row.stateName) {
        return row.stateName
      }
      const state = String((row && row.state) || '')
      if (state === '2008') {
        return this.$t('contractWorkbench.target.stateIdle')
      }
      if (state === '2006') {
        return this.$t('contractWorkbench.target.stateRented')
      }
      if (state === '2007') {
        return this.$t('contractWorkbench.target.stateSold')
      }
      return '-'
    },
    rowPayerObjType(row) {
      return row && row.payerObjType ? row.payerObjType : this.payerObjType
    },
    rowObjId(row) {
      return row.objId || row.roomId || row.shopId
    },
    handleSelectionChange(selection) {
      this.rows.forEach(row => {
        const index = this.indexOfSelected(this.rowObjId(row))
        if (index > -1 && selection.indexOf(row) < 0) {
          this.selected.splice(index, 1)
        }
      })
      selection.forEach(row => {
        if (this.indexOfSelected(this.rowObjId(row)) < 0) {
          this.selected.push(row)
        }
      })
    },
    indexOfSelected(objId) {
      for (let index = 0; index < this.selected.length; index++) {
        if (this.rowObjId(this.selected[index]) === objId) {
          return index
        }
      }
      return -1
    },
    removeSelected(index) {
      this.selected.splice(index, 1)
    },
    handleCurrentChange(current) {
      this.page.current = current
      this.queryData()
    },
    buildTarget(row) {
      const objId = this.rowObjId(row)
      const payerObjType = this.rowPayerObjType(row)
      return {
        ...row,
        payerObjType: payerObjType,
        objId: objId,
        objName: this.rowName(row),
        roomId: objId,
        builtUpArea: row.builtUpArea,
        areaValue: this.rowAreaValue(row),
        rentValue: this.rowRentValue(row),
        stateName: this.rowStateName(row)
      }
    },
    handleConfirm() {
      const objs = this.selected.map(row => this.buildTarget(row))
      this.$emit('choose', objs)
      this.handleClose()
    }
  }
}
</script>

<style lang="scss" scoped>
.cc-toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 12px;

  .el-input {
    margin-right: 8px;
  }

  .el-button {
    margin-right: 8px;
  }

  .cc-toolbar-tip {
    font-size: 12px;
    color: #909399;
  }
}

.cc-selected {
  margin-top: 14px;

  .cc-selected-title {
    font-size: 13px;
    color: #606266;
    margin-bottom: 8px;
  }

  .cc-selected-list {
    .el-tag {
      margin: 0 8px 8px 0;
    }
  }
}
</style>
