<template>
  <div>
    <el-row>
      <el-col :span="4">
        <el-date-picker v-model="startDate" type="date" :placeholder="$t('dataReport.startDate')"
          value-format="yyyy-MM-dd" style="width: 100%" />
      </el-col>
      <el-col :span="4">
        <el-date-picker v-model="endDate" type="date" :placeholder="$t('dataReport.endDate')" value-format="yyyy-MM-dd"
          style="width: 100%" />
      </el-col>
      <el-col :span="4">
        <el-button type="primary" size="small" @click="_qureyParkingFeeStatistics">
          <i class="el-icon-search"></i>
          {{ $t('dataReport.search') }}
        </el-button>
      </el-col>
      <el-col :span="8" class="text-right">
        <el-button type="primary" size="small" @click="_exportParkingFeeExcel">
          <i class="el-icon-download"></i>
          <span>{{ $t('dataReport.export') }}</span>
        </el-button>
      </el-col>
    </el-row>
    <div class="margin-top-sm">
      <el-table :data="fees" border style="width: 100%">
        <el-table-column prop="paName" :label="$t('dataReport.parkingArea')" align="center" />
        <el-table-column prop="psCount" :label="$t('dataReport.parkingSpaceCount')" align="center" />
        <el-table-column prop="feePsCount" :label="$t('dataReport.feeParkingSpaceCount')" align="center" />
        <el-table-column prop="hisMonthOweFee" :label="$t('dataReport.historyOwe')" align="center" />
        <el-table-column prop="oweFee" :label="$t('dataReport.totalOwe')" align="center" />
        <el-table-column prop="todayReceivedRoomCount" :label="$t('dataReport.todayPaidCount')" align="center" />
        <el-table-column prop="todayReceivedRoomAmount" :label="$t('dataReport.todayPaidAmount')" align="center" />
        <el-table-column prop="hisOweReceivedRoomCount" :label="$t('dataReport.historyPaidCount')" align="center" />
        <el-table-column prop="hisOweReceivedRoomAmount" :label="$t('dataReport.historyPaidAmount')" align="center" />
        <el-table-column prop="monthReceivedRoomCount" :label="$t('dataReport.monthPaidCount')" align="center" />
        <el-table-column :label="$t('dataReport.parkingRemainingCount')" align="center">
          <template slot-scope="scope">
            {{ (scope.row.feePsCount - scope.row.monthReceivedRoomCount).toFixed(0) }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('dataReport.paidRatio')" align="center">
          <template slot-scope="scope">
            {{ scope.row.feePsCount > 0 ?
              ((scope.row.monthReceivedRoomCount) / (scope.row.feePsCount) * 100).toFixed(2) + '%' : '0%' }}
          </template>
        </el-table-column>
        <el-table-column prop="monthReceivedRoomAmount" :label="$t('dataReport.monthPaidAmount')" align="center" />
        <el-table-column prop="curMonthOweFee" :label="$t('dataReport.monthRemaining')" align="center" />
        <el-table-column :label="$t('dataReport.collectionRate')" align="center">
          <template slot-scope="scope">
            {{ scope.row.curReceivableFee > 0 ?
              ((scope.row.monthReceivedRoomAmount) / (scope.row.curReceivableFee) * 100).toFixed(2) + '%' : '0%' }}
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script>
import { queryParkingFeeStatistics, exportData } from '@/api/report/dataReportApi'

export default {
  name: 'DataReportFeeStatisticsParking',

  data() {
    return {
      fees: [],
      communityId: '',
      feeTypeCd: '',
      startDate: '',
      endDate: ''
    }
  },
  methods: {
    open(param) {
      if (!param) {
        return
      }
      this.communityId = param.communityId || this.communityId
      this.feeTypeCd = param.feeTypeCd || this.feeTypeCd
      this.startDate = param.startDate || this.startDate
      this.endDate = param.endDate || this.endDate
      this._loadParkingFeeStatisticsData()
    },
    async _loadParkingFeeStatisticsData() {
      try {
        const params = {
          communityId: this.communityId,
          startDate: this.startDate,
          endDate: this.endDate,
          feeTypeCd: this.feeTypeCd
        }
        const { data } = await queryParkingFeeStatistics(params)
        this.fees = data || []
      } catch (error) {
        console.error('Failed to load data:', error)
      }
    },
    _qureyParkingFeeStatistics() {
      this._loadParkingFeeStatisticsData()
    },
    async _exportParkingFeeExcel() {
      try {
        if (!this.fees || this.fees.length < 1) {
          this.$message.warning(this.$t('dataReport.noExportData'))
          return
        }

        const params = {
          communityId: this.communityId,
          startDate: this.startDate,
          endDate: this.endDate,
          feeTypeCd: this.feeTypeCd,
          pagePath: 'dataReportParkingFeeStatistics'
        }
        const res = await exportData(params)
        this.$message.success(res.msg)
        if (res.code === 0) {
          this.$router.push('/pages/property/downloadTempFile?tab=下载中心')
        }
      } catch (error) {
        console.error('Export failed:', error)
      }
    }
  }
}
</script>

<style scoped>
.text-right {
  text-align: right;
}
</style>
