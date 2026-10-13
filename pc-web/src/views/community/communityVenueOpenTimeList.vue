<template>
  <div v-loading="venueLoading" class="venue-open-time">
    <div v-if="venueLoaded && venues.length < 1" class="panel empty-panel">
      <div class="empty-tip">{{ $t('communityVenueOpenTime.addVenueTip') }}</div>
    </div>

    <el-row v-else-if="venueLoaded" :gutter="20">
      <el-col :span="4">
        <div class="panel venue-panel">
          <div class="panel-header">
            <span class="panel-title">{{ $t('communityVenueOpenTime.venueList') }}</span>
          </div>
          <ul class="venue-list">
            <li v-for="item in venues" :key="item.venueId" class="venue-item"
              :class="{ 'venue-item-active': venueId === item.venueId }" @click="handleSelectVenue(item)">
              {{ item.name }}
            </li>
          </ul>
        </div>
      </el-col>

      <el-col :span="20">
        <div class="panel config-panel">
          <div class="config-row">
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
              <div class="day-time" :class="{ 'day-time-rest': !day.enabled }">
                <el-time-picker v-model="day.startTime" size="small" class="time-picker" value-format="HH:mm"
                  format="HH:mm" :placeholder="$t('communityVenueOpenTime.openLabel')" />
                <span class="time-sep">{{ $t('communityVenueOpenTime.timeTo') }}</span>
                <el-time-picker v-model="day.endTime" size="small" class="time-picker" value-format="HH:mm"
                  format="HH:mm" :placeholder="$t('communityVenueOpenTime.closeLabel')" />
              </div>
              <span v-if="currentVenue" class="week-share">
                {{ $t('communityVenueOpenTime.sharedByVenue', { name: currentVenue.name }) }}
              </span>
            </div>
            <div v-if="weekConfig.length < 1" class="empty-tip">{{ $t('communityVenueOpenTime.noVenue') }}</div>
          </div>
        </div>
      </el-col>
    </el-row>
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
      venueLoading: false,
      venueLoaded: false,
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
      this.venueLoading = true
      try {
        const res = await listCommunityVenue({ page: 1, row: 100 })
        this.venues = res || []
        if (this.venues.length < 1) {
          this.weekConfig = []
          return
        }
        this.venueId = this.venues[0].venueId
        this.listSpaces()
        await this.loadWeekConfig()
      } catch (error) {
        this.venues = []
        this.weekConfig = []
        this.$message.error(error.message || this.$t('communityVenueOpenTime.loadFailed'))
      } finally {
        this.venueLoading = false
        this.venueLoaded = true
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
        this.startTime = venue.startTime || '08:00'
        this.endTime = venue.endTime || '22:00'
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
        config.push(this.buildDayConfig(dayRows, weekDay))
      }
      return config
    },
    buildDayConfig(dayRows, weekDay) {
      const openHours = []
      if (dayRows) {
        dayRows.forEach(item => {
          if (item.isOpen !== 'Y' || item.hours === null || item.hours === undefined) {
            return
          }
          const hour = parseInt(item.hours, 10)
          if (!isNaN(hour) && hour >= 0 && hour < HOUR_TOTAL) {
            openHours.push(hour)
          }
        })
      }
      if (openHours.length < 1) {
        return {
          weekDay: weekDay,
          enabled: false,
          startTime: this.startTime || '08:00',
          endTime: this.endTime || '22:00'
        }
      }
      openHours.sort((a, b) => a - b)
      return {
        weekDay: weekDay,
        enabled: true,
        startTime: this.pad(openHours[0]) + ':00',
        endTime: this.formatEndTime(openHours[openHours.length - 1] + 1)
      }
    },
    formatEndTime(hour) {
      if (hour >= HOUR_TOTAL) {
        return '23:59'
      }
      return this.pad(hour) + ':00'
    },
    getMinutes(time) {
      if (!time || time.indexOf(':') < 1) {
        return -1
      }
      const hour = parseInt(time.substring(0, time.indexOf(':')), 10)
      const minute = parseInt(time.substring(time.indexOf(':') + 1), 10)
      if (isNaN(hour) || isNaN(minute)) {
        return -1
      }
      return hour * 60 + minute
    },
    pad(value) {
      return value < 10 ? '0' + value : '' + value
    },
    handleSelectVenue(venue) {
      if (!venue || venue.venueId === this.venueId) {
        return
      }
      this.venueId = venue.venueId
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
      this.syncWeekDaysTime()
    },
    syncWeekDaysTime() {
      this.weekConfig.forEach(day => {
        if (!day.enabled) {
          return
        }
        day.startTime = this.startTime
        day.endTime = this.endTime
      })
    },
    buildOpenTimes(day) {
      const startHour = Math.floor(this.getMinutes(day.startTime) / 60)
      const endHour = Math.ceil(this.getMinutes(day.endTime) / 60)
      const openTimes = []
      for (let hourValue = 0; hourValue < HOUR_TOTAL; hourValue++) {
        openTimes.push({
          hours: '' + hourValue,
          isOpen: day.enabled && hourValue >= startHour && hourValue < endHour ? 'Y' : 'N'
        })
      }
      return openTimes
    },
    handleDayChange(day) {
      if (!day.enabled) {
        return
      }
      const startMin = this.getMinutes(day.startTime)
      const endMin = this.getMinutes(day.endTime)
      if (startMin < 0 || endMin < 0 || endMin <= startMin) {
        day.startTime = this.startTime || '08:00'
        day.endTime = this.endTime || '22:00'
      }
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
      const hasInvalidDay = this.weekConfig.some(day => {
        if (!day.enabled) {
          return false
        }
        const startMin = this.getMinutes(day.startTime)
        const endMin = this.getMinutes(day.endTime)
        return startMin < 0 || endMin < 0 || endMin <= startMin
      })
      if (hasInvalidDay) {
        this.$message.warning(this.$t('communityVenueOpenTime.dayTimeError'))
        return
      }
      this.saving = true
      try {
        const openTimeConfigs = this.weekConfig.map(day => ({
          weekDay: '' + day.weekDay,
          startTime: day.startTime,
          endTime: day.endTime,
          openTimes: this.buildOpenTimes(day)
        }))
        await saveCommunitySpaceOpenTimeBatch({
          venueId: this.venueId,
          startTime: this.startTime,
          endTime: this.endTime,
          openTimeConfigs: openTimeConfigs
        })
        this.$message.success(this.$t('communityVenueOpenTime.saveSuccess'))
        await this.loadWeekConfig()
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

.venue-panel {
  min-height: calc(100vh - 122px);
}

.venue-list {
  list-style: none;
  padding: 8px;
  margin: 0;
}

.venue-item {
  padding: 10px 12px;
  margin-bottom: 6px;
  border-radius: 8px;
  font-size: 13px;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s;
}

.venue-item:hover {
  background: #f1f5f9;
}

.venue-item-active {
  background: #2563eb;
  color: #fff;
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

.day-time {
  display: flex;
  align-items: center;
  gap: 6px;
}

.day-time-rest {
  opacity: 0.4;
}

.time-sep {
  font-size: 12px;
  color: #94a3b8;
}

.week-share {
  margin-left: auto;
  font-size: 11px;
  color: #94a3b8;
}

.empty-panel {
  padding: 60px 16px;
}

.empty-tip {
  padding: 30px 16px;
  text-align: center;
  font-size: 13px;
  color: #94a3b8;
}
</style>
