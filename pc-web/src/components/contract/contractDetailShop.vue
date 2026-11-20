<template>
  <div>
    <div class="margin-top">
      <el-table :data="contractDetailShopInfo.shops" style="width: 100%" border stripe>
        <el-table-column prop="roomName" :label="$t('contractDetailShop.shopNum')" align="center"></el-table-column>
        <el-table-column prop="locationName" :label="$t('contractDetailShop.location')" align="center"></el-table-column>
        <el-table-column :label="$t('contractDetailShop.area')" align="center">
          <template slot-scope="scope">
            {{ scope.row.builtUpArea }}/{{ scope.row.roomArea }}
          </template>
        </el-table-column>
        <el-table-column prop="roomRent" :label="$t('contractDetailShop.rent')" align="center"></el-table-column>
        <el-table-column :label="$t('contractDetailShop.shopStatus')" align="center">
          <template slot-scope="scope">
            {{ _shopStateName(scope.row) }}
          </template>
        </el-table-column>
        <template slot="empty">{{ $t('contractDetailShop.empty') }}</template>
      </el-table>
      <el-row class="margin-top">
        <el-col :span="24" class="text-right">
          <el-pagination @current-change="handlePageChange" :current-page="pagination.currentPage"
            :page-size="pagination.pageSize" layout="total, prev, pager, next"
            :total="pagination.total"></el-pagination>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script>
import { queryContractRoom } from '@/api/contract/contractDetailRoomApi'

export default {
  name: 'ContractDetailShop',
  data() {
    return {
      contractDetailShopInfo: {
        shops: [],
        contractId: '',
        payerObjType: '4444'
      },
      pagination: {
        currentPage: 1,
        pageSize: 10,
        total: 0
      }
    }
  },
  methods: {
    open(data) {
      const params = data || {}
      this.contractDetailShopInfo.contractId = params.contractId
      this.contractDetailShopInfo.payerObjType = params.payerObjType || '4444'
      this.pagination.currentPage = 1
      this._loadContractDetailShopData(1, this.pagination.pageSize)
    },
    _loadContractDetailShopData(page, row) {
      const params = {
        contractId: this.contractDetailShopInfo.contractId,
        payerObjType: this.contractDetailShopInfo.payerObjType,
        page: page || 1,
        row: row || 10
      }

      queryContractRoom(params).then(response => {
        this.contractDetailShopInfo.shops = response.data || []
        this.pagination.total = response.total || 0
      }).catch(error => {
        this.contractDetailShopInfo.shops = []
        this.pagination.total = 0
        this.$message.error(error.message)
      })
    },
    _shopStateName(row) {
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
    handlePageChange(currentPage) {
      this.pagination.currentPage = currentPage
      this._loadContractDetailShopData(currentPage, this.pagination.pageSize)
    }
  }
}
</script>

<style scoped>
.margin-top {
  margin-top: 20px;
}

.text-right {
  text-align: right;
}
</style>
