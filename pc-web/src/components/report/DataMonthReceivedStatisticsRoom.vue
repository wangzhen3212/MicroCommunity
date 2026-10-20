<template>
  <div>
    <el-row>
      <el-col :span="4">
        <el-select v-model="floorId" :placeholder="$t('dataReport.selectBuilding')" clearable style="width: 100%">
          <el-option v-for="(item, index) in floors" :key="index" :label="item.floorNum" :value="item.floorId" />
        </el-select>
      </el-col>
      <el-col :span="4">
        <el-date-picker v-model="feeStartDate" type="date" :placeholder="$t('dataReport.feeStartDate')"
          value-format="yyyy-MM-dd" style="width: 100%" />
      </el-col>
      <el-col :span="4">
        <el-date-picker v-model="feeEndDate" type="date" :placeholder="$t('dataReport.feeEndDate')"
          value-format="yyyy-MM-dd" style="width: 100%" />
      </el-col>
      <el-col :span="4">
        <el-button type="primary" size="small" @click="_qureyDataMonthReceivedStatistics">
          <i class="el-icon-search"></i>
          {{ $t('dataReport.search') }}
        </el-button>
      </el-col>
      <el-col :span="8" class="text-right">
        <el-button type="primary" size="small" @click="_exportReportMonthReceivedFeeExcel">
          <i class="el-icon-download"></i>
          <span>{{ $t('dataReport.export') }}</span>
        </el-button>
      </el-col>
    </el-row>

    <div class="margin-top-sm">
      <el-table :data="fees" border style="width: 100%">
        <el-table-column prop="objName" :label="$t('dataReport.room')" align="center" />
        <el-table-column :label="$t('dataReport.owner')" align="center">
          <template slot-scope="scope">
            {{ scope.row.ownerName }}({{ scope.row.link }})
          </template>
        </el-table-column>
        <el-table-column prop="receivedAmount" :label="$t('dataReport.receivedAmount')" align="center">
          <template slot-scope="scope">
            {{ scope.row.receivedAmount || 0 }}
          </template>
        </el-table-column>
        <el-table-column prop="feeName" :label="$t('dataReport.feeName')" align="center" />
        <el-table-column prop="curYearMonth" :label="$t('dataReport.receivedPeriod')" align="center" />
        <el-table-column prop="cashierName" :label="$t('dataReport.cashier')" align="center" />
        <el-table-column prop="createTime" :label="$t('dataReport.paymentTime')" align="center" />
      </el-table>

      <el-row class="margin-top">
        <el-col :span="8">
          <span>{{ $t('dataReport.totalReceived') }}：</span>
          <span>{{ feeAmount }}</span>
        </el-col>
        <el-col :span="16" class="text-right">
          <el-pagination :current-page.sync="page.current" :page-sizes="[10, 20, 30, 50]" :page-size="page.size"
            :total="page.total" layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange"
            @current-change="handleCurrentChange" />
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script>
import { queryMonthReceivedDetail, exportData, queryFloors } from '@/api/report/dataReportApi'

export default {
  name: 'DataMonthReceivedStatisticsRoom',

  data() {
    return {
      fees: [],
      floors: [],
      floorId: '',
      feeStartDate: '',
      feeEndDate: '',
      feeTypeCd: '',
      feeAmount: '0',
      communityId: '',
      page: {
        current: 1,
        size: 10,
        total: 0
      }
    }
  },
  methods: {
    open(param) {
      if (!param) {
        return
      }
      this.communityId = param.communityId || this.communityId
      this.feeTypeCd = param.feeTypeCd || this.feeTypeCd
      if (!this.feeStartDate) {
        this.feeStartDate = param.startDate || this._getDefaultStartDate()
      }
      if (!this.feeEndDate) {
        this.feeEndDate = param.endDate || this._getDefaultEndDate()
      }
      this.page.current = 1
      this._loadMonthReceivedFloors()
      this._loadDataMonthReceivedStatisticsData()
    },
    _getDefaultStartDate() {
      const now = new Date()
      return this._formatDate(new Date(now.getFullYear(), now.getMonth(), 1))
    },
    _getDefaultEndDate() {
      const now = new Date()
      return this._formatDate(new Date(now.getFullYear(), now.getMonth() + 1, 0))
    },
    _formatDate(date) {
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return year + '-' + month + '-' + day
    },
    async _loadMonthReceivedFloors() {
      try {
        const params = {
          page: 1,
          row: 100,
          communityId: this.communityId
        }
        const { apiFloorDataVoList } = await queryFloors(params)
        this.floors = apiFloorDataVoList
      } catch (error) {
        console.error('Failed to load floors:', error)
      }
    },
    async _loadDataMonthReceivedStatisticsData() {
      try {
        const params = {
          page: this.page.current,
          row: this.page.size,
          communityId: this.communityId,
          feeStartDate: this.feeStartDate,
          feeEndDate: this.feeEndDate,
          feeTypeCd: this.feeTypeCd,
          floorId: this.floorId
        }
        const { data, records, sumTotal } = await queryMonthReceivedDetail(params)
        this.fees = data
        this.page.total = records
        this.feeAmount = sumTotal
      } catch (error) {
        console.error('Failed to load data:', error)
      }
    },
    async _exportReportMonthReceivedFeeExcel() {
      try {
        const params = {
          communityId: this.communityId,
          feeStartDate: this.feeStartDate,
          feeEndDate: this.feeEndDate,
          feeTypeCd: this.feeTypeCd,
          floorId: this.floorId,
          pagePath: 'dataMonthReceivedStatistics'
        }
        const res = await exportData(params)
        this.$message.success(res.msg)
        if (res.code === 0) {
          this.$router.push('/pages/property/downloadTempFile?tab=下载中心')
        }
      } catch (error) {
        console.error('Export failed:', error)
      }
    },
    _qureyDataMonthReceivedStatistics() {
      this.page.current = 1
      this._loadDataMonthReceivedStatisticsData()
    },
    handleSizeChange(val) {
      this.page.size = val
      this._loadDataMonthReceivedStatisticsData()
    },
    handleCurrentChange(val) {
      this.page.current = val
      this._loadDataMonthReceivedStatisticsData()
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
