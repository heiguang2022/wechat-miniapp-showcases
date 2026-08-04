const EVENTS = [
  { id: 1, type: '展览', title: '看见风的形状', place: '西岸艺术中心', date: '08.08 — 09.22', day: '08', month: 'AUG', price: 68, color: '#608d86', icon: '◒', tags: ['沉浸式', '当代艺术'], liked: false },
  { id: 2, type: '市集', title: '梧桐树下生活节', place: '衡山路 8 号街区', date: '08.10 — 08.11', day: '10', month: 'AUG', price: 0, color: '#df765a', icon: '⌂', tags: ['周末', '宠物友好'], liked: false },
  { id: 3, type: '演出', title: '夏夜草坪爵士会', place: '浦东美术馆南广场', date: '08.16 19:30', day: '16', month: 'AUG', price: 128, color: '#365a72', icon: '♫', tags: ['音乐', '户外'], liked: false },
  { id: 4, type: '体验', title: '城市植物拓印工坊', place: '上生·新所', date: '08.18 14:00', day: '18', month: 'AUG', price: 88, color: '#84996f', icon: '❋', tags: ['手作', '小班'], liked: false }
]

Page({
  data: { activeTab: 'explore', filters: ['全部', '展览', '市集', '演出', '体验'], activeFilter: '全部', events: EVENTS, allEvents: EVENTS, selected: null, showDetail: false, tickets: [], favorites: [], toast: '' },
  onLoad() {
    const favoriteIds = wx.getStorageSync('city_favorites') || []
    const tickets = wx.getStorageSync('city_tickets') || []
    const allEvents = EVENTS.map(e => ({ ...e, liked: favoriteIds.includes(e.id) }))
    this.setData({ allEvents, events: allEvents, favorites: allEvents.filter(e => e.liked), tickets })
  },
  switchTab(e) { this.setData({ activeTab: e.currentTarget.dataset.tab }) },
  selectFilter(e) {
    const activeFilter = e.currentTarget.dataset.filter
    this.setData({ activeFilter, events: activeFilter === '全部' ? this.data.allEvents : this.data.allEvents.filter(i => i.type === activeFilter) })
  },
  openEvent(e) { this.setData({ selected: this.data.allEvents.find(i => i.id === Number(e.currentTarget.dataset.id)), showDetail: true }) },
  closeDetail() { this.setData({ showDetail: false }) },
  block() {},
  toggleLike(e) {
    const id = Number(e.currentTarget.dataset.id)
    const allEvents = this.data.allEvents.map(item => item.id === id ? { ...item, liked: !item.liked } : item)
    const favoriteIds = allEvents.filter(i => i.liked).map(i => i.id)
    wx.setStorageSync('city_favorites', favoriteIds)
    const activeFilter = this.data.activeFilter
    this.setData({ allEvents, events: activeFilter === '全部' ? allEvents : allEvents.filter(i => i.type === activeFilter), favorites: allEvents.filter(i => i.liked), selected: this.data.selected && allEvents.find(i => i.id === this.data.selected.id) })
  },
  book() {
    const event = this.data.selected
    if (this.data.tickets.some(t => t.id === event.id)) return wx.showToast({ title: '已在票夹中', icon: 'none' })
    const tickets = [{ ...event, code: `MY${Date.now().toString().slice(-6)}` }, ...this.data.tickets]
    wx.setStorageSync('city_tickets', tickets)
    this.setData({ tickets, showDetail: false, activeTab: 'tickets' })
    wx.showToast({ title: event.price ? '报名成功' : '预约成功', icon: 'success' })
  },
  removeTicket(e) {
    const tickets = this.data.tickets.filter(t => t.id !== Number(e.currentTarget.dataset.id))
    wx.setStorageSync('city_tickets', tickets); this.setData({ tickets })
  }
})
