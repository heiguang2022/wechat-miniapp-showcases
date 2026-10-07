const { request } = require('../../utils/api')
const { getCart, saveCart, totals } = require('../../utils/cart')

Page({
  data: { loading: true, error: '', merchant: null, stores: [], store: null, categories: [], activeCategory: '', products: [], visibleProducts: [], selected: null, selections: {}, unitPrice: 0, showSku: false, cartCount: 0 },
  onShow() { this.refreshCart(); if (!this.data.merchant) this.load() },
  async load() {
    this.setData({ loading: true, error: '' })
    try {
      const merchants = await request('/public/merchants'); if (!merchants.length) throw new Error('暂无营业商家')
      const stores = await request(`/public/stores?merchantId=${merchants[0].id}`); const store = stores.find(i => i.status === 'OPEN') || stores[0]
      if (!store) throw new Error('暂无可用门店')
      const menu = await request(`/public/stores/${store.id}/menu`)
      const products = menu.products.map(p => ({ ...p, priceText: (p.basePrice / 100).toFixed(0), specGroups: groupSpecs(p.specs) }))
      const activeCategory = menu.categories[0] && menu.categories[0].id || ''
      this.setData({ merchant: menu.merchant, stores, store: menu.store, categories: menu.categories, products, activeCategory, visibleProducts: products.filter(p => p.categoryId === activeCategory) })
    } catch (error) { this.setData({ error: error.message }) }
    finally { this.setData({ loading: false }) }
  },
  chooseCategory(e) { const id = e.currentTarget.dataset.id; this.setData({ activeCategory: id, visibleProducts: this.data.products.filter(p => p.categoryId === id) }) },
  chooseStore(e) { const index = Number(e.detail.value); const store = this.data.stores[index]; request(`/public/stores/${store.id}/menu`).then(menu => { const products = menu.products.map(p => ({ ...p, priceText: (p.basePrice / 100).toFixed(0), specGroups: groupSpecs(p.specs) })); this.setData({ store, products, visibleProducts: products.filter(p => p.categoryId === this.data.activeCategory) }) }).catch(error => wx.showToast({ title: error.message, icon: 'none' })) },
  openSku(e) { const selected = this.data.products.find(p => p.id === e.currentTarget.dataset.id); const selections = {}; selected.specGroups.forEach(g => { if (g.options[0]) selections[g.id] = g.options[0].id }); this.setData({ selected, selections, unitPrice: selected.basePrice + optionTotal(selected, selections), showSku: true }) },
  chooseOption(e) { const selections = { ...this.data.selections, [e.currentTarget.dataset.group]: e.currentTarget.dataset.option }; this.setData({ selections, unitPrice: this.data.selected.basePrice + optionTotal(this.data.selected, selections) }) },
  closeSku() { this.setData({ showSku: false }) }, block() {},
  addToCart() {
    const add = () => { const cart = getCart(); cart.merchantId = this.data.merchant.id; cart.storeId = this.data.store.id; cart.storeName = this.data.store.name; const optionIds = Object.values(this.data.selections).sort(); const key = `${this.data.selected.id}:${optionIds.join(',')}`; const options = this.data.selected.specGroups.flatMap(g => g.options.filter(o => optionIds.includes(o.id)).map(o => `${g.name}：${o.name}`)); const existing = cart.items.find(i => i.key === key); if (existing) existing.quantity += 1; else cart.items.push({ key, productId: this.data.selected.id, name: this.data.selected.name, unitPrice: this.data.unitPrice, quantity: 1, optionIds, options }); saveCart(cart); this.setData({ showSku: false }); this.refreshCart(); wx.showToast({ title: '已加入购物袋' }) }
    const cart = getCart(); if (cart.items.length && cart.storeId !== this.data.store.id) wx.showModal({ title: '切换门店', content: '购物袋中已有其他门店商品，继续将清空原商品。', success: r => { if (r.confirm) { saveCart({ merchantId: '', storeId: '', items: [] }); add() } } }); else add()
  },
  refreshCart() { this.setData({ cartCount: totals(getCart()).count }) },
  goCart() { wx.switchTab({ url: '/pages/cart/index' }) }
})
function groupSpecs(rows) { const map = {}; rows.forEach(r => { if (!map[r.groupId]) map[r.groupId] = { id: r.groupId, name: r.groupName, required: r.requiredFlag, options: [] }; map[r.groupId].options.push({ id: r.optionId, name: r.optionName, priceDelta: Number(r.priceDelta) }) }); return Object.values(map) }
function optionTotal(product, selections) { return product.specGroups.flatMap(g => g.options).filter(o => Object.values(selections).includes(o.id)).reduce((s, o) => s + o.priceDelta, 0) }
