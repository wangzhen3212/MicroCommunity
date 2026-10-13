<template>
  <div class="venue-open-time">
    <div class="panel config-panel">
      <div class="config-row">
        <span class="config-label">{{ $t('communityVenueOpenTime.venue') }}</span>
        <el-select v-model="venueId" size="small" class="venue-select"
          :placeholder="$t('communityVenueOpenTime.venuePlaceholder')" @change="handleVenueChange">
          <el-option v-for="item in venues" :key="item.venueId" :label="item.name" :value="item.venueId" />
        </el-select>
        <span class="config-label">{{ $t('communityVenueOpenTime.openLabel') }}</span>
        <el-time-picker v-model="startTime" size="small" class="time-picker" value-format="HH:mm" format="HH:mm"
          :placeholder="$t('communityVenueOpenTime.openLabel')" @change="handleTimeChange" />
        <span class="config-label">{{ $t('communityVenueOpenTime.closeLabel') }}</span>
        <el-time-picker v-model="endTime" size="small" class="time-picker" value-format="HH:mm" format="HH:mm"
          :placeholder="$t('communityVenueOpenTime.closeLabel')" @change="handleTimeChange" />
        <el-button type="primary" size="small" class="save-button" :loading="saving" @click="handleSave">
          {{ $t('communityVenueOpenTime.saveConfig') }}
        </el-button>
      </div>
      <div class="config-tip">{{ $t('communityVenueOpenTime.configTip') }}</div>
    </div>

    <div class="panel week-panel">
      <div class="panel-header">
        <span class="panel-title">{{ $t('communityVenueOpenTime.weekDayTitle') }}</span>
        <span v-if="currentVenue" class="panel-meta">
          {{ $t('communityVenueOpenTime.currentVenueTip', { name: currentVenue.name, count: spaceCount }) }}
        </span>
      </div>
      <div v-loading="loading" class="week-list">
        <div v-for="day in weekConfig" :key="day.weekDay" class="week-row"
          :class="{ 'week-row-rest': !day.enabled }">
          <span class="week-name">{{ $t('communityVenueOpenTime.week' + day.weekDay) }}</span>
          <el-switch v-model="day.enabled" @change="handleDayChange(day)" />
          <span class="week-state" :class="day.enabled ? 'state-open' : 'state-rest'">
            {{ day.enabled ? $t('communityVenueOpenTime.dayOpen') : $t('communityVenueOpenTime.dayRest') }}
          </span>
          <div class="hour-list" :class="{ 'hour-list-rest': !day.enabled }">
            <span v-for="item in day.hours" :key="item.hours" class="hour-item"
              :class="{ 'hour-item-open': day.enabled && item.isOpen === 'Y' }" @click="handleToggleHour(day, item)">
              {{ formatHourRange(item.hours) }}
            </span>
          </div>
        </div>
        <div v-if="weekConfig.length < 1" class="empty-tip">{{ $t('communityVenueOpenTime.noVenue') }}</div>
      </div>
    </div>
  </div>
</template>

<script>
import {
  listCommunityVenue,
  listCommunitySpaceOpenTime,
  saveCommunitySpaceOpenTimeBatch
} from '@/api/community/communityVenueOpenTimeApi'
import { listCommunitySpace } from '@/api/community/communitySpaceManageApi'
import { getCommunityId } from '@/api/community/communityApi'

const HOUR_TOTAL = 24

export default {
  name: 'CommunityVenueOpenTimeList',
  data() {
    return {
      communityId: getCommunityId(),
      venues: [],
      allSpaces: [],
      venueId: '',
      startTime: '08:00',
      endTime: '22:00',
      weekConfig: [],
      loading: false,
      saving: false
    }
  },
  computed: {
    currentVenue() {
      return this.venues.find(item => item.venueId === this.venueId)
    },
    spaceCount() {
      return this.allSpaces.filter(item => item.venueId === this.venueId).length
    }
  },
  created() {
    this.listVenues()
  },
  methods: {
    async listVenues() {
      try {
        const res = await listCommunityVenue({ page: 1, row: 100 })
        this.venues = res || []
        if (this.venues.length < 1) {
          this.weekConfig = []
          this.$message.warning(this.$t('communityVenueOpenTime.noVenue'))
          return
        }
        this.venueId = this.venues[0].venueId
        this.listSpaces()
        this.loadWeekConfig()
      } catch (error) {
        this.$message.error(error.message || this.$t('communityVenueOpenTime.loadFailed'))
      }
    },
    async listSpaces() {
      try {
        const res = await listCommunitySpace({ page: 1, row: 1000, communityId: this.communityId })
        this.allSpaces = res.data || []
      } catch (error) {
        this.allSpaces = []
      }
    },
    async loadWeekConfig() {
      if (!this.venueId) {
        this.weekConfig = []
        return
      }
      const venue = this.currentVenue
      if (venue) {
        this.startTime = venue.startTime || this.startTime
        this.endTime = venue.endTime || this.endTime
      }
      this.loading = true
      try {
        const rows = await listCommunitySpaceOpenTime({
          venueId: this.venueId,
          communityId: this.communityId,
          page: 1,
          row: 1000
        })
        this.weekConfig = this.buildWeekConfig(rows || [])
      } catch (error) {
        this.$message.error(error.message || this.$t('communityVenueOpenTime.loadFailed'))
      } finally {
        this.loading = false
      }
    },
    buildWeekConfig(rows) {
      const rowMap = {}
      rows.forEach(item => {
        const weekDay = item.weekDay ? '' + item.weekDay : '0'
        if (!rowMap[weekDay]) {
          rowMap[weekDay] = []
        }
        rowMap[weekDay].push(item)
      })
      const commonRows = rowMap['0'] && rowMap['0'].length > 0 ? rowMap['0'] : null
      const config = []
      for (let weekDay = 1; weekDay <= 7; weekDay++) {
        let dayRows = rowMap['' + weekDay]
        if (!dayRows || dayRows.length < 1) {
          dayRows = commonRows
        }
        const hours = this.buildDayHours(dayRows, weekDay)
        config.push({
          weekDay: weekDay,
          enabled: hours.some(item => item.isOpen === 'Y'),
          hours: hours
        })
      }
      return config
    },
    buildDayHours(dayRows, weekDay) {
      const hours = []
      let isOpen = ''
      for (let hourValue = 0; hourValue < HOUR_TOTAL; hourValue++) {
        isOpen = ''
        if (dayRows) {
          dayRows.forEach(item => {
            if (item.hours !== null && item.hours !== undefined && '' + item.hours === '' + hourValue) {
              isOpen = item.isOpen === 'Y' ? 'Y' : 'N'
            }
          })
        }
        if (!isOpen && !dayRows) {
          isOpen = this.getDefaultIsOpen(hourValue)
        }
        hours.push({
          weekDay: weekDay,
          hours: hourValue,
          isOpen: isOpen === 'Y' ? 'Y' : 'N'
        })
      }
      return hours
    },
    getDefaultIsOpen(hourValue) {
      const startHour = this.getHour(this.startTime, 0)
      const endHour = this.getHour(this.endTime, 24)
      return hourValue >= startHour && hourValue < endHour ? 'Y' : 'N'
    },
    getHour(time, defaultValue) {
      if (!time || time.indexOf(':') < 1) {
        return defaultValue
      }
      const hour = parseInt(time.substring(0, time.indexOf(':')), 10)
      if (isNaN(hour) || hour < 0 || hour > HOUR_TOTAL) {
        return defaultValue
      }
      return hour
    },
    formatHourRange(hours) {
      return this.pad(hours) + ':00~' + this.pad(Number(hours) + 1) + ':00'
    },
    pad(value) {
      return value < 10 ? '0' + value : '' + value
    },
    handleVenueChange() {
      this.weekConfig = []
      this.loadWeekConfig()
    },
    handleTimeChange() {
      if (!this.startTime || !this.endTime) {
        return
      }
      if (this.endTime <= this.startTime) {
        this.$message.warning(this.$t('communityVenueOpenTime.closeTimeError'))
        return
      }
      this.weekConfig.forEach(day => {
        if (!day.enabled) {
          return
        }
        day.hours.forEach(item => {
          item.isOpen = this.getDefaultIsOpen(item.hours)
        })
      })
    },
    handleDayChange(day) {
      if (day.enabled && !day.hours.some(item => item.isOpen === 'Y')) {
        day.hours.forEach(item => {
          item.isOpen = this.getDefaultIsOpen(item.hours)
        })
      }
    },
    handleToggleHour(day, item) {
      if (!day.enabled) {
        this.$message.warning(this.$t('communityVenueOpenTime.restDayTip'))
        return
      }
      item.isOpen = item.isOpen === 'Y' ? 'N' : 'Y'
    },
    async handleSave() {
      if (!this.venueId) {
        this.$message.warning(this.$t('communityVenueOpenTime.noVenue'))
        return
      }
      if (!this.startTime || !this.endTime) {
        this.$message.warning(this.$t('communityVenueOpenTime.closeTimeError'))
        return
      }
      if (this.endTime <= this.startTime) {
        this.$message.warning(this.$t('communityVenueOpenTime.closeTimeError'))
        return
      }
      this.saving = true
      try {
        for (let index = 0; index < this.weekConfig.length; index++) {
          const day = this.weekConfig[index]
          const data = {
            venueId: this.venueId,
            weekDay: '' + day.weekDay,
            openTimes: day.hours.map(item => {
              return {
                hours: '' + item.hours,
                isOpen: day.enabled && item.isOpen === 'Y' ? 'Y' : 'N'
              }
            })
          }
          if (index === 0) {
            data.startTime = this.startTime
            data.endTime = this.endTime
          }
          await saveCommunitySpaceOpenTimeBatch(data)
        }
        this.$message.success(this.$t('communityVenueOpenTime.saveSuccess'))
        this.loadWeekConfig()
      } catch (error) {
        this.$message.error(error.message || this.$t('communityVenueOpenTime.loadFailed'))
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.venue-open-time {
  padding: 16px;
  background: #f1f5f9;
  min-height: calc(100vh - 90px);
}

.panel {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px -4px rgba(15, 23, 42, 0.12);
}

.config-panel {
  padding: 16px;
  margin-bottom: 16px;
}

.config-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.config-label {
  font-size: 13px;
  color: #475569;
}

.venue-select {
  width: 200px;
}

.time-picker {
  width: 120px;
}

.save-button {
  margin-left: auto;
  background-color: #2563eb;
  border-color: #2563eb;
}

.config-tip {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f1f5f9;
  font-size: 12px;
  color: #94a3b8;
}

.panel-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-bottom: 1px solid #f1f5f9;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}

.panel-meta {
  font-size: 12px;
  color: #94a3b8;
}

.week-list {
  min-height: 120px;
}

.week-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
}

.week-row + .week-row {
  border-top: 1px solid #f1f5f9;
}

.week-row-rest {
  background: #f8fafc;
}

.week-name {
  width: 48px;
  flex: none;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.week-state {
  width: 40px;
  flex: none;
  font-size: 12px;
}

.state-open {
  color: #059669;
}

.state-rest {
  color: #94a3b8;
}

.hour-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.hour-list-rest {
  opacity: 0.5;
}

.hour-item {
  padding: 4px 8px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #f8fafc;
  color: #94a3b8;
  font-size: 11px;
  cursor: pointer;
  user-select: none;
  transition: all 0.15s;
}

.hour-item:hover {
  border-color: #93c5fd;
}

.hour-item-open {
  background: #eff6ff;
  border-color: #2563eb;
  color: #2563eb;
  font-weight: 600;
}

.empty-tip {
  padding: 30px 16px;
  text-align: center;
  font-size: 13px;
  color: #94a3b8;
}
</style>
