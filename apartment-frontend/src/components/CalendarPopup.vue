<template>
  <div class="calendar-popup" @click.stop>
    <div class="calendar-header">
      <button class="nav-btn" @click="prevMonth" :disabled="!canGoPrev">&lsaquo;</button>
      <span class="month-label">{{ year }}年{{ month + 1 }}月</span>
      <button class="nav-btn" @click="nextMonth">&rsaquo;</button>
    </div>
    <div class="calendar-grid">
      <div class="weekday" v-for="w in weekdays" :key="w">{{ w }}</div>
      <div
        v-for="(day, idx) in calendarDays"
        :key="idx"
        class="day-cell"
        :class="{
          'empty': !day,
          'disabled': day && isDisabled(day),
          'selected': day && isSelected(day),
          'today': day && isToday(day),
          'in-range': day && isInRange(day),
          'available': day && !isDisabled(day) && day >= todayDay
        }"
        @click="day && !isDisabled(day) && selectDate(day)"
      >
        <span v-if="day" class="day-number">{{ day.getDate() }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';

const props = defineProps<{
  /** Currently selected date string YYYY-MM-DD, for check-in */
  selectedDate?: string;
  /** For check-out mode: the check-in date string YYYY-MM-DD */
  checkInDate?: string;
  /** Mode: 'checkin' or 'checkout' */
  mode: 'checkin' | 'checkout';
  /** Function to determine if a date is disabled */
  isDateDisabled: (date: Date) => boolean;
  /** Minimum selectable date (YYYY-MM-DD) */
  minDate?: string;
}>();

const emit = defineEmits<{
  (e: 'select', dateStr: string): void;
}>();

const weekdays = ['一', '二', '三', '四', '五', '六', '日'];

const now = new Date();
const year = ref(now.getFullYear());
const month = ref(now.getMonth());

const todayDay = new Date(now.getFullYear(), now.getMonth(), now.getDate());

const canGoPrev = computed(() => {
  const firstOfMonth = new Date(year.value, month.value, 1);
  const min = props.minDate ? new Date(props.minDate + 'T00:00:00') : todayDay;
  return firstOfMonth > min;
});

const prevMonth = () => {
  if (month.value === 0) {
    year.value--;
    month.value = 11;
  } else {
    month.value--;
  }
};

const nextMonth = () => {
  if (month.value === 11) {
    year.value++;
    month.value = 0;
  } else {
    month.value++;
  }
};

const calendarDays = computed(() => {
  const firstDay = new Date(year.value, month.value, 1);
  const lastDay = new Date(year.value, month.value + 1, 0);

  // Monday=0 offset (Chinese convention)
  let startDow = firstDay.getDay() - 1;
  if (startDow < 0) startDow = 6;

  const days: (Date | null)[] = [];
  for (let i = 0; i < startDow; i++) days.push(null);

  for (let d = 1; d <= lastDay.getDate(); d++) {
    days.push(new Date(year.value, month.value, d));
  }

  return days;
});

const formatDay = (date: Date): string => {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
};

const isToday = (day: Date): boolean => {
  return day.getTime() === todayDay.getTime();
};

const isSelected = (day: Date): boolean => {
  if (!props.selectedDate) return false;
  return formatDay(day) === props.selectedDate;
};

const isInRange = (day: Date): boolean => {
  if (props.mode !== 'checkout' || !props.checkInDate || !props.selectedDate) return false;
  const checkIn = new Date(props.checkInDate + 'T00:00:00');
  const selected = new Date(props.selectedDate + 'T00:00:00');
  return day > checkIn && day <= selected;
};

const isDisabled = (day: Date): boolean => {
  const min = props.minDate ? new Date(props.minDate + 'T00:00:00') : todayDay;
  if (day < min) return true;

  // Check-out mode: must be after check-in date (at least one night)
  if (props.mode === 'checkout' && props.checkInDate) {
    const checkIn = new Date(props.checkInDate + 'T00:00:00');
    if (day.getTime() <= checkIn.getTime()) return true;
  }

  return props.isDateDisabled(day);
};

const selectDate = (day: Date) => {
  emit('select', formatDay(day));
};
</script>

<style scoped>
.calendar-popup {
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  padding: 12px;
  width: 280px;
  user-select: none;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

.calendar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  padding: 0 4px;
}

.nav-btn {
  width: 28px;
  height: 28px;
  border: none;
  background: #f1f5f9;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  font-weight: 700;
  color: #475569;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}
.nav-btn:hover:not(:disabled) {
  background: #e2e8f0;
  color: #0f172a;
}
.nav-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.month-label {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 1px;
}

.weekday {
  text-align: center;
  font-size: 11px;
  font-weight: 600;
  color: #94a3b8;
  padding: 6px 0;
}

.day-cell {
  text-align: center;
  padding: 0;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.1s;
}

.day-cell.empty {
  cursor: default;
}

.day-number {
  font-size: 13px;
  font-weight: 500;
  line-height: 1;
}

.day-cell.available:hover {
  background: #eff6ff;
}
.day-cell.available:hover .day-number {
  color: #2563eb;
}

.day-cell.today .day-number {
  font-weight: 700;
  color: #3b82f6;
}

.day-cell.selected {
  background: #3b82f6;
}
.day-cell.selected .day-number {
  color: #fff !important;
  font-weight: 700;
}

.day-cell.in-range {
  background: #eff6ff;
}
.day-cell.in-range .day-number {
  color: #2563eb;
}

.day-cell.disabled {
  cursor: not-allowed;
}
.day-cell.disabled .day-number {
  color: #cbd5e1;
  text-decoration: line-through;
  font-weight: 400;
}
</style>
