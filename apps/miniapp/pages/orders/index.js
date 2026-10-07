const { request } = require('../../utils/api')
const LABELS = { PENDING_ACCEPTANCE: '待商家接单', PREPARING: '制作中', READY: '待取餐', COMPLETED: '已完成', REJECTED: '商家已拒单', CANCELLED: '已取消' }
Page({
  data: { loading: true, error: '', orders: [] },
  onShow() { this.load() },
  async load() { this.setData({ loading: true, error: '' }); try { const result = await request('/customer/orders?page=1&pageSize=50'); this.setData({ orders: result.items.map(i => ({ ...i, statusText: LABELS[i.status], amountText: (i.totalAmount / 100).toFixed(2) })) }) } catch (error) { this.setData({ error: error.message }) } finally { this.setData({ loading: false }) } },
  open(e) { wx.navigateTo({ url: `/pages/order-detail/index?id=${e.currentTarget.dataset.id}` }) },
  menu() { wx.switchTab({ url: '/pages/menu/index' }) }
})
