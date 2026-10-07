const { getCart, saveCart, totals } = require('../../utils/cart')
Page({
  data: { cart: { items: [] }, count: 0, amount: 0, amountText: '0.00' },
  onShow() { this.refresh() },
  refresh() { const cart = getCart(); cart.items = cart.items.map(i => ({ ...i, optionText: i.options.join(' · '), lineText: (i.unitPrice * i.quantity / 100).toFixed(2) })); const total = totals(cart); this.setData({ cart, count: total.count, amount: total.amount, amountText: (total.amount / 100).toFixed(2) }) },
  change(e) { const cart = getCart(); const item = cart.items.find(i => i.key === e.currentTarget.dataset.key); if (item) item.quantity += Number(e.currentTarget.dataset.delta); cart.items = cart.items.filter(i => i.quantity > 0); saveCart(cart); this.refresh() },
  menu() { wx.switchTab({ url: '/pages/menu/index' }) },
  confirm() { wx.navigateTo({ url: '/pages/confirm/index' }) }
})
