const DEFAULT_HABITS = [
  { id: 1, name: '晨间喝水', note: '起床后的一杯温水', icon: '💧', color: '#81a99c', streak: 12, done: true },
  { id: 2, name: '阅读 20 分钟', note: '把注意力还给自己', icon: '📖', color: '#d89372', streak: 7, done: false },
  { id: 3, name: '散步 6000 步', note: '去户外呼吸一下', icon: '🌿', color: '#a1a985', streak: 5, done: false },
  { id: 4, name: '23:30 前睡觉', note: '好好休息也是进步', icon: '☾', color: '#9293b2', streak: 9, done: true }
]

Page({
  data: {
    activeTab: 'today', habits: [], completed: 0, percent: 0, greeting: '早上好', dateText: '', week: [],
    showAdd: false, newHabitName: '', selectedPreset: 0,
    presets: [{ icon: '🧘', color: '#b38eac' }, { icon: '🏃', color: '#d48068' }, { icon: '✍️', color: '#6e9da8' }, { icon: '🥗', color: '#8fa875' }],
    bars: [52, 76, 64, 88, 72, 100, 50], weekLabels: ['一', '二', '三', '四', '五', '六', '日'], profileName: '慢慢变好的我'
  },
  onLoad() {
    const now = new Date()
    const hours = now.getHours()
    const greeting = hours < 11 ? '早上好' : hours < 18 ? '下午好' : '晚上好'
    const weekNames = ['日', '一', '二', '三', '四', '五', '六']
    const week = []
    for (let d = -3; d <= 3; d++) {
      const date = new Date(now); date.setDate(now.getDate() + d)
      week.push({ day: weekNames[date.getDay()], date: date.getDate(), today: d === 0 })
    }
    const habits = wx.getStorageSync('habit_items') || DEFAULT_HABITS
    this.setData({ greeting, dateText: `${now.getMonth() + 1}月${now.getDate()}日 · 星期${weekNames[now.getDay()]}`, week, habits })
    this.calculate(habits)
  },
  switchTab(e) { this.setData({ activeTab: e.currentTarget.dataset.tab }) },
  toggleHabit(e) {
    const id = Number(e.currentTarget.dataset.id)
    const habits = this.data.habits.map(h => h.id === id ? { ...h, done: !h.done, streak: h.done ? Math.max(0, h.streak - 1) : h.streak + 1 } : h)
    wx.setStorageSync('habit_items', habits); this.setData({ habits }); this.calculate(habits)
    if (habits.find(h => h.id === id).done) wx.vibrateShort({ type: 'light' })
  },
  calculate(habits) {
    const completed = habits.filter(h => h.done).length
    const percent = habits.length ? Math.round(completed / habits.length * 100) : 0
    this.setData({ completed, percent })
  },
  openAdd() { this.setData({ showAdd: true, newHabitName: '' }) },
  closeAdd() { this.setData({ showAdd: false }) },
  block() {},
  onNameInput(e) { this.setData({ newHabitName: e.detail.value }) },
  choosePreset(e) { this.setData({ selectedPreset: Number(e.currentTarget.dataset.index) }) },
  addHabit() {
    const name = this.data.newHabitName.trim()
    if (!name) return wx.showToast({ title: '先写下习惯名称', icon: 'none' })
    const preset = this.data.presets[this.data.selectedPreset]
    const habits = [...this.data.habits, { id: Date.now(), name, note: '今天开始，慢慢坚持', ...preset, streak: 0, done: false }]
    wx.setStorageSync('habit_items', habits); this.setData({ habits, showAdd: false }); this.calculate(habits)
    wx.showToast({ title: '习惯已创建', icon: 'success' })
  },
  deleteHabit(e) {
    const habits = this.data.habits.filter(h => h.id !== Number(e.currentTarget.dataset.id))
    wx.setStorageSync('habit_items', habits); this.setData({ habits }); this.calculate(habits)
  },
  resetData() {
    wx.removeStorageSync('habit_items'); const habits = DEFAULT_HABITS.map(h => ({ ...h }))
    this.setData({ habits }); this.calculate(habits); wx.showToast({ title: '已恢复示例数据', icon: 'success' })
  }
})

