const { request } = require('../../utils/api')
const LABELS = { PENDING_ACCEPTANCE: '等待商家接单', PREPARING: '咖啡正在制作', READY: '请到店取餐', COMPLETED: '订单已完成', REJECTED: '商家未能接单', CANCELLED: '订单已取消' }
Page({
  data: { id: '', loading: true, error: '', order: null },
  onLoad(options) { this.setData({ id: options.id }); this.load() },
  onShow() { if (this.data.id && this.data.order) this.load() },
  async load() { try { const order = await request(`/customer/orders/${this.data.id}`); order.statusText = LABELS[order.status]; order.amountText = (order.totalAmount / 100).toFixed(2); order.items = order.items.map(i => ({ ...i, amountText: (i.lineAmount / 100).toFixed(2), optionText: i.options.map(o => `${o.groupName}：${o.optionName}`).join(' · ') })); this.setData({ order, error: '' }) } catch (error) { this.setData({ error: error.message }) } finally { this.setData({ loading: false }) } },
  async cancel() { const ok = await new Promise(resolve => wx.showModal({ title: '取消订单', content: '仅待接单订单可以取消，确认继续？', success: r => resolve(r.confirm) })); if (!ok) return; try { await request(`/customer/orders/${this.data.id}/cancel`, { method: 'POST' }); wx.showToast({ title: '订单已取消' }); this.load() } catch (error) { wx.showToast({ title: error.message, icon: 'none' }) } },
  menu() { wx.switchTab({ url: '/pages/menu/index' }) }
})
