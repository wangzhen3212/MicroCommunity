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
        <el-button type="primary" size="small" @click="_qureyDataReportFeeStatistics">
          <i class="el-icon-search"></i>
          {{ $t('dataReport.search') }}
        </el-button>
      </el-col>
      <el-col :span="8" class="text-right">
        <el-button type="primary" size="small" @click="_exportReportFeeExcel">
          <i class="el-icon-download"></i>
          <span>{{ $t('dataReport.export') }}</span>
        </el-button>
      </el-col>
    </el-row>
    <div class="margin-top-sm">
      <el-table :data="fees" border style="width: 100%">
        <el-table-column prop="floorNum" :label="$t('dataReport.building')" align="center" />
        <el-table-column prop="roomCount" :label="$t('dataReport.roomCount')" align="center" />
        <el-table-column prop="feeRoomCount" :label="$t('dataReport.feeRoomCount')" align="center" />
        <el-table-column prop="hisMonthOweFee" :label="$t('dataReport.historyOwe')" align="center" />
        <el-table-column prop="oweFee" :label="$t('dataReport.totalOwe')" align="center" />
        <el-table-column prop="todayReceivedRoomCount" :label="$t('dataReport.todayPaidCount')" align="center" />
        <el-table-column prop="todayReceivedRoomAmount" :label="$t('dataReport.todayPaidAmount')" align="center" />
        <el-table-column prop="hisOweReceivedRoomCount" :label="$t('dataReport.historyPaidCount')" align="center" />
        <el-table-column prop="hisOweReceivedRoomAmount" :label="$t('dataReport.historyPaidAmount')" align="center" />
        <el-table-column prop="monthReceivedRoomCount" :label="$t('dataReport.monthPaidCount')" align="center" />
        <el-table-column :label="$t('dataReport.remainingCount')" align="center">
          <template slot-scope="scope">
            {{ (scope.row.feeRoomCount - scope.row.monthReceivedRoomCount).toFixed(0) }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('dataReport.paidRatio')" align="center">
          <template slot-scope="scope">
            {{ scope.row.feeRoomCount > 0 ?
              ((scope.row.monthReceivedRoomCount) / (scope.row.feeRoomCount) * 100).toFixed(2) + '%' : '0%' }}
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
import { queryDataReportFeeStatistics, exportData, queryFloors } from '@/api/report/dataReportApi'

export default {
  name: 'DataReportFeeStatisticsRoom',

  data() {
    return {
      fees: [],
      floors: [],
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
      this._loadFloors()
    },
    async _loadFloors() {
      try {
        const params = {
          page: 1,
          row: 100,
          communityId: this.communityId
        }
        const { apiFloorDataVoList } = await queryFloors(params)
        this.floors = apiFloorDataVoList
        this.fees = []

        const _floorIds = []
        for (let i = 0; i < apiFloorDataVoList.length; i++) {
          _floorIds.push(apiFloorDataVoList[i].floorId)
          if (_floorIds.length >= 5) {
            await this._loadDataReportFeeStatisticsData(_floorIds.join(','))
            _floorIds.length = 0
          }
        }

        if (_floorIds.length > 0) {
          await this._loadDataReportFeeStatisticsData(_floorIds.join(','))
        }
      } catch (error) {
        console.error('Failed to load floors:', error)
      }
    },
    async _loadDataReportFeeStatisticsData(_floorIds) {
      try {
        const params = {
          communityId: this.communityId,
          startDate: this.startDate,
          endDate: this.endDate,
          feeTypeCd: this.feeTypeCd,
          floorIds: _floorIds
        }
        const { data } = await queryDataReportFeeStatistics(params)
        data.forEach(item => {
          this.fees.push(item)
        })
      } catch (error) {
        console.error('Failed to load data:', error)
      }
    },
    _qureyDataReportFeeStatistics() {
      this.fees = []
      this._loadFloors()
    },
    async _exportReportFeeExcel() {
      try {
        const _floorIds = this.floors.map(item => item.floorId)
        if (!_floorIds || _floorIds.length < 1) {
          this.$message.warning(this.$t('dataReport.noExportData'))
          return
        }

        const params = {
          communityId: this.communityId,
          startDate: this.startDate,
          endDate: this.endDate,
          feeTypeCd: this.feeTypeCd,
          floorIds: _floorIds.join(','),
          pagePath: 'dataReportFeeStatistics'
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
