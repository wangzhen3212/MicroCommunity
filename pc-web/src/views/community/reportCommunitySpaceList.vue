<template>
  <div class="space-booking">
    <div class="panel select-panel">
      <div class="select-row">
        <span class="select-label">{{ $t('reportCommunitySpace.venue') }}</span>
        <div class="tab-list">
          <div v-for="item in venues" :key="item.venueId" class="tab-item"
            :class="{ active: currentVenueId === item.venueId }" @click="handleSelectVenue(item)">
            <span class="tab-name">{{ item.name }}</span>
            <span class="tab-meta">
              {{ $t('reportCommunitySpace.venueTabsMeta',
                { count: countSpace(item.venueId), start: item.startTime, end: item.endTime }) }}
            </span>
          </div>
          <span v-if="venues.length < 1" class="empty-tip">{{ $t('communitySpaceManage.noVenue') }}</span>
        </div>
      </div>

      <div class="select-row">
        <span class="select-label">{{ $t('reportCommunitySpace.space') }}</span>
        <div class="tab-list">
          <div v-for="item in venueSpaces" :key="item.spaceId" class="tab-item"
            :class="{ active: currentSpaceId === item.spaceId }" @click="handleSelectSpace(item)">
            <span class="tab-name">{{ item.name }}</span>
            <span class="tab-meta">￥{{ item.feeMoney }}/{{ $t('reportCommunitySpace.hour') }}</span>
          </div>
          <span v-if="venueSpaces.length < 1" class="empty-tip">{{ $t('reportCommunitySpace.noEnabledSpace') }}</span>
        </div>
      </div>

      <div class="select-row">
        <span class="select-label">{{ $t('reportCommunitySpace.appointmentDate') }}</span>
        <div class="date-strip">
          <div v-for="item in dateList" :key="item.date" class="date-item"
            :class="{ active: appointmentDate === item.date }" @click="handleSelectDate(item.date)">
            <span class="date-week">{{ item.week }}</span>
            <span class="date-day">{{ item.dayText }}{{ item.isToday ? $t('reportCommunitySpace.today') : '' }}</span>
          </div>
        </div>
      </div>
    </div>

    <el-row :gutter="20" class="booking-body">
      <el-col :span="16">
        <div class="panel slot-panel">
          <div class="panel-header">
            <span class="panel-title">{{ $t('reportCommunitySpace.availableSlotTitle') }}</span>
            <span class="panel-meta">{{ currentSpace ? currentSpace.name : '-' }}</span>
            <div class="legend">
              <span class="legend-item"><i class="legend-box legend-available"></i>{{ $t('reportCommunitySpace.legendAvailable') }}</span>
              <span class="legend-item"><i class="legend-box legend-booked"></i>{{ $t('reportCommunitySpace.legendBooked') }}</span>
              <span class="legend-item"><i class="legend-box legend-closed"></i>{{ $t('reportCommunitySpace.legendClosed') }}</span>
              <span class="legend-item"><i class="legend-box legend-selected"></i>{{ $t('reportCommunitySpace.legendSelected') }}</span>
            </div>
          </div>

          <div v-loading="loading" class="slot-grid">
            <el-row :gutter="10">
              <el-col v-for="slot in slots" :key="slot.hours" :span="4" class="slot-col">
                <div class="slot-item" :class="'slot-' + slot.status" @click="handleToggleSlot(slot)">
                  <span class="slot-hour">{{ slot.label }}</span>
                  <span class="slot-state">{{ slotStatusText(slot.status) }}</span>
                </div>
              </el-col>
            </el-row>
          </div>
          <div v-if="slots.length < 1" class="empty-tip">{{ $t('reportCommunitySpace.noSpace') }}</div>
          <div class="slot-tip">{{ $t('reportCommunitySpace.slotTip') }}</div>
        </div>
      </el-col>

      <el-col :span="8">
        <div class="side-panel">
          <space-booking-panel :venue="currentVenue" :space="currentSpace" :appointment-date="appointmentDate"
            :selected-hours="selectedHours" @success="handleBookingSuccess" @clear="handleClearSelection" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import {
  listCommunityVenue,
  listCommunitySpace,
  listCommunitySpaceOpenTime
} from '@/api/community/reportCommunitySpaceApi'
import { getCommunityId } from '@/api/community/communityApi'
import SpaceBookingPanel from '@/components/community/SpaceBookingPanel'

export default {
  name: 'ReportCommunitySpaceList',
  components: {
    SpaceBookingPanel
  },
  data() {
    return {
      loading: false,
      communityId: getCommunityId(),
      venues: [],
      allSpaces: [],
      openTimeConfig: [],
      availableHours: [],
      selectedHours: [],
      currentVenueId: '',
      currentSpaceId: '',
      appointmentDate: ''
    }
  },
  computed: {
    currentVenue() {
      return this.venues.find(item => item.venueId === this.currentVenueId)
    },
    venueSpaces() {
      return this.allSpaces.filter(item => item.venueId === this.currentVenueId && item.state === '1001')
    },
    currentSpace() {
      return this.venueSpaces.find(item => item.spaceId === this.currentSpaceId)
    },
    dateList() {
      const list = []
      const base = new Date()
      for (let index = 0; index < 7; index++) {
        const date = new Date(base.getFullYear(), base.getMonth(), base.getDate() + index)
        const day = date.getDay()
        list.push({
          date: this.formatDate(date),
          week: this.$t('reportCommunitySpace.week' + (day === 0 ? 7 : day)),
          dayText: this.formatMonthDay(date),
          isToday: index === 0
        })
      }
      return list
    },
    slotHours() {
      if (this.openTimeConfig.length < 1) {
        return []
      }
      const venue = this.currentVenue
      let startHour = 0
      let endHour = 24
      if (venue && venue.startTime) {
        startHour = Number(venue.startTime.split(':')[0])
      }
      if (venue && venue.endTime) {
        endHour = Number(venue.endTime.split(':')[0])
      }
      return this.openTimeConfig.filter(item => {
        const hours = Number(item.hours)
        return hours >= startHour && hours < endHour
      })
    },
    slots() {
      return this.slotHours.map(item => {
        const hours = Number(item.hours)
        return {
          hours: hours,
          label: this.formatHourRange(hours),
          status: this.getSlotStatus(item)
        }
      })
    }
  },
  created() {
    this.appointmentDate = this.formatDate(new Date())
    this.listVenues()
  },
  methods: {
    async listVenues() {
      try {
        const res = await listCommunityVenue({ page: 1, row: 100, communityId: this.communityId })
        this.venues = res || []
        if (this.venues.length > 0) {
          this.currentVenueId = this.venues[0].venueId
        }
        this.listSpaces()
      } catch (error) {
        this.$message.error(error.message || this.$t('reportCommunitySpace.loadFailed'))
      }
    },
    async listSpaces() {
      try {
        const res = await listCommunitySpace({ page: 1, row: 1000, communityId: this.communityId })
        this.allSpaces = res || []
        this.syncCurrentSpace()
      } catch (error) {
        this.$message.error(error.message || this.$t('reportCommunitySpace.loadFailed'))
      }
    },
    async loadSlots() {
      if (!this.currentSpaceId) {
        this.openTimeConfig = []
        this.availableHours = []
        return
      }
      this.loading = true
      try {
        const configRes = await listCommunitySpaceOpenTime({
          venueId: this.currentVenueId,
          appointmentTime: this.appointmentDate,
          communityId: this.communityId
        })
        this.openTimeConfig = configRes.data || []

        const availableRes = await listCommunitySpaceOpenTime({
          venueId: this.currentVenueId,
          spaceId: this.currentSpaceId,
          appointmentTime: this.appointmentDate,
          communityId: this.communityId
        })
        const hours = []
        const openTimeList = availableRes.data || []
        openTimeList.forEach(item => {
          if (item.isOpen === 'Y') {
            hours.push(Number(item.hours))
          }
        })
        this.availableHours = hours
      } catch (error) {
        this.$message.error(error.message || this.$t('reportCommunitySpace.loadFailed'))
      } finally {
        this.loading = false
      }
    },
    syncCurrentSpace() {
      const spaces = this.venueSpaces
      const exists = spaces.some(item => item.spaceId === this.currentSpaceId)
      if (!exists) {
        this.currentSpaceId = spaces.length > 0 ? spaces[0].spaceId : ''
      }
      this.selectedHours = []
      this.loadSlots()
    },
    getSlotStatus(item) {
      if (item.isOpen !== 'Y') {
        return 'closed'
      }
      const hours = Number(item.hours)
      if (this.availableHours.indexOf(hours) < 0) {
        return 'booked'
      }
      if (this.selectedHours.indexOf(hours) > -1) {
        return 'selected'
      }
      return 'available'
    },
    slotStatusText(status) {
      if (status === 'booked') {
        return this.$t('reportCommunitySpace.slotStatusBooked')
      }
      if (status === 'closed') {
        return this.$t('reportCommunitySpace.slotStatusClosed')
      }
      if (status === 'selected') {
        return this.$t('reportCommunitySpace.slotStatusSelected')
      }
      return this.$t('reportCommunitySpace.slotStatusAvailable')
    },
    countSpace(venueId) {
      return this.allSpaces.filter(item => item.venueId === venueId && item.state === '1001').length
    },
    handleSelectVenue(venue) {
      if (this.currentVenueId === venue.venueId) {
        return
      }
      this.currentVenueId = venue.venueId
      this.currentSpaceId = ''
      this.syncCurrentSpace()
    },
    handleSelectSpace(space) {
      if (this.currentSpaceId === space.spaceId) {
        return
      }
      this.currentSpaceId = space.spaceId
      this.selectedHours = []
      this.loadSlots()
    },
    handleSelectDate(date) {
      if (this.appointmentDate === date) {
        return
      }
      this.appointmentDate = date
      this.selectedHours = []
      this.loadSlots()
    },
    handleToggleSlot(slot) {
      if (slot.status === 'available') {
        this.selectedHours.push(slot.hours)
        return
      }
      if (slot.status === 'selected') {
        const index = this.selectedHours.indexOf(slot.hours)
        if (index > -1) {
          this.selectedHours.splice(index, 1)
        }
        return
      }
      if (slot.status === 'booked') {
        this.$message.warning(this.$t('reportCommunitySpace.slotStatusBooked'))
      }
    },
    handleClearSelection() {
      this.selectedHours = []
    },
    handleBookingSuccess() {
      this.selectedHours = []
      this.loadSlots()
    },
    formatDate(date) {
      return date.getFullYear() + '-' + this.pad(date.getMonth() + 1) + '-' + this.pad(date.getDate())
    },
    formatMonthDay(date) {
      return this.pad(date.getMonth() + 1) + '-' + this.pad(date.getDate())
    },
    formatHourRange(hours) {
      return this.pad(hours) + ':00~' + this.pad(Number(hours) + 1) + ':00'
    },
    pad(value) {
      return value < 10 ? '0' + value : '' + value
    }
  }
}
</script>

<style lang="scss" scoped>
.space-booking {
  padding: 16px;
  background: #f1f5f9;
  min-height: calc(100vh - 90px);
}

.panel {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px -4px rgba(15, 23, 42, 0.12);
}

.select-panel {
  padding: 12px 16px;
  margin-bottom: 16px;
}

.select-row {
  display: flex;
  align-items: flex-start;
  padding: 8px 0;
}

.select-row + .select-row {
  border-top: 1px solid #f1f5f9;
}

.select-label {
  width: 72px;
  flex: none;
  font-size: 13px;
  color: #64748b;
  line-height: 34px;
}

.tab-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tab-item {
  display: flex;
  flex-direction: column;
  padding: 6px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-item:hover {
  border-color: #93c5fd;
  background: #f8fafc;
}

.tab-item.active {
  border-color: #2563eb;
  background: #eff6ff;
}

.tab-name {
  font-size: 13px;
  color: #334155;
}

.tab-item.active .tab-name {
  color: #2563eb;
  font-weight: 600;
}

.tab-meta {
  margin-top: 2px;
  font-size: 11px;
  color: #94a3b8;
}

.date-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.date-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 72px;
  padding: 6px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.date-item:hover {
  border-color: #93c5fd;
  background: #f8fafc;
}

.date-item.active {
  border-color: #2563eb;
  background: #eff6ff;
}

.date-week {
  font-size: 12px;
  color: #64748b;
}

.date-item.active .date-week {
  color: #2563eb;
}

.date-day {
  margin-top: 2px;
  font-size: 12px;
  color: #334155;
}

.slot-panel {
  min-width: 0;
}

.panel-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f1f5f9;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}

.panel-meta {
  margin-left: 10px;
  font-size: 12px;
  color: #94a3b8;
}

.legend {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-left: auto;
  font-size: 11px;
  color: #64748b;
}

.legend-item {
  display: flex;
  align-items: center;
}

.legend-box {
  display: inline-block;
  width: 12px;
  height: 12px;
  margin-right: 4px;
  border-radius: 3px;
}

.legend-available {
  background: #fff;
  border: 1px solid #cbd5e1;
}

.legend-booked {
  background: #ffe4e6;
  border: 1px solid #fda4af;
}

.legend-closed {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
}

.legend-selected {
  background: #2563eb;
}

.slot-grid {
  padding: 16px 16px 6px 16px;
  min-height: 120px;
}

.slot-col {
  margin-bottom: 10px;
}

.slot-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 62px;
  padding: 12px 6px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  cursor: pointer;
  transition: all 0.15s;
  user-select: none;
}

.slot-item:hover {
  transform: translateY(-2px);
}

.slot-hour {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  white-space: nowrap;
}

.slot-state {
  margin-top: 4px;
  font-size: 11px;
  color: #94a3b8;
}

.slot-available {
  background: #fff;
  border-color: #cbd5e1;
}

.slot-booked {
  background: #ffe4e6;
  border-color: #fda4af;
  cursor: not-allowed;
}

.slot-booked .slot-hour,
.slot-booked .slot-state {
  color: #e11d48;
}

.slot-closed {
  background: #f1f5f9;
  border-color: #e2e8f0;
  cursor: not-allowed;
}

.slot-closed .slot-hour,
.slot-closed .slot-state {
  color: #94a3b8;
}

.slot-selected {
  background: #2563eb;
  border-color: #2563eb;
}

.slot-selected .slot-hour,
.slot-selected .slot-state {
  color: #fff;
}

.slot-tip {
  padding: 0 16px 16px 16px;
  font-size: 11px;
  color: #94a3b8;
}

.empty-tip {
  padding: 10px 4px;
  font-size: 13px;
  color: #94a3b8;
}
</style>
