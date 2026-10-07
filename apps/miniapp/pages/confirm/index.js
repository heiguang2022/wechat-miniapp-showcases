const { request } = require('../../utils/api')
const { getCart, clearCart, totals } = require('../../utils/cart')
Page({
  data: { cart: { items: [] }, amountText: '0.00', submitting: false, error: '' },
  onLoad() { const cart = getCart(); if (!cart.items.length) return wx.navigateBack(); cart.items = cart.items.map(i => ({ ...i, optionText: i.options.join(' · '), lineText: (i.unitPrice * i.quantity / 100).toFixed(2) })); this.setData({ cart, amountText: (totals(cart).amount / 100).toFixed(2) }) },
  async submit() {
    if (this.data.submitting) return
    this.setData({ submitting: true, error: '' })
    try {
      const order = await request('/customer/orders', { method: 'POST', header: { 'X-Idempotency-Key': `${Date.now()}-${Math.random().toString(36).slice(2)}` }, data: { storeId: this.data.cart.storeId, items: this.data.cart.items.map(i => ({ productId: i.productId, quantity: i.quantity, optionIds: i.optionIds })) } })
      clearCart(); wx.redirectTo({ url: `/pages/order-detail/index?id=${order.id}` })
    } catch (error) { this.setData({ error: error.message, submitting: false }) }
  }
})
