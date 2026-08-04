const PRODUCTS = [
  { id: 1, category: '本周推荐', name: '橙香冰美式', desc: '鲜橙 · 双份浓缩 · 气泡', price: 26, icon: '🍊', tone: '#F3B277', badge: '新品' },
  { id: 2, category: '本周推荐', name: '桂花拿铁', desc: '杭白桂 · 丝滑鲜奶', price: 29, icon: '☕', tone: '#D7B28A', badge: '人气' },
  { id: 3, category: '经典咖啡', name: '燕麦澳白', desc: '中深烘 · 燕麦奶', price: 28, icon: '🌾', tone: '#C9B99B' },
  { id: 4, category: '经典咖啡', name: '海盐焦糖拿铁', desc: '海盐奶盖 · 焦糖', price: 31, icon: '🧂', tone: '#B88B68' },
  { id: 5, category: '轻食甜点', name: '开心果可颂', desc: '每日现烤 · 开心果酱', price: 22, icon: '🥐', tone: '#C9A66B' },
  { id: 6, category: '轻食甜点', name: '巴斯克芝士', desc: '焦香浓郁 · 低甜', price: 25, icon: '🍰', tone: '#E5C784' }
]

Page({
  data: {
    activeTab: 'menu', categories: ['本周推荐', '经典咖啡', '轻食甜点'], activeCategory: '本周推荐',
    products: PRODUCTS.filter(p => p.category === '本周推荐'), allProducts: PRODUCTS,
    cart: [], cartCount: 0, cartTotal: 0, showSku: false, selected: null, temperature: '冰', sugar: '标准',
    order: null, userName: '咖啡旅人', temperatureOptions: ['冰', '热'], sugarOptions: ['标准', '半糖', '不另外加糖']
  },
  onLoad() {
    const cart = wx.getStorageSync('brew_cart') || []
    const order = wx.getStorageSync('brew_order') || null
    this.setData({ cart, order })
    this.refreshCart(cart)
  },
  switchTab(e) { this.setData({ activeTab: e.currentTarget.dataset.tab }) },
  selectCategory(e) {
    const activeCategory = e.currentTarget.dataset.name
    this.setData({ activeCategory, products: PRODUCTS.filter(p => p.category === activeCategory) })
  },
  openSku(e) {
    const selected = PRODUCTS.find(p => p.id === Number(e.currentTarget.dataset.id))
    this.setData({ selected, showSku: true, temperature: '冰', sugar: '标准' })
  },
  closeSku() { this.setData({ showSku: false }) },
  block() {},
  chooseOption(e) { this.setData({ [e.currentTarget.dataset.type]: e.currentTarget.dataset.value }) },
  addToCart() {
    const { selected, temperature, sugar, cart } = this.data
    const key = `${selected.id}-${temperature}-${sugar}`
    const next = cart.slice()
    const existing = next.find(item => item.key === key)
    if (existing) existing.qty += 1
    else next.push({ ...selected, key, temperature, sugar, qty: 1 })
    this.setData({ showSku: false })
    this.refreshCart(next)
    wx.showToast({ title: '已加入购物袋', icon: 'success' })
  },
  changeQty(e) {
    const { key, delta } = e.currentTarget.dataset
    const next = this.data.cart.map(i => ({ ...i }))
    const item = next.find(i => i.key === key)
    if (item) item.qty += Number(delta)
    this.refreshCart(next.filter(i => i.qty > 0))
  },
  refreshCart(cart) {
    const cartCount = cart.reduce((sum, i) => sum + i.qty, 0)
    const cartTotal = cart.reduce((sum, i) => sum + i.price * i.qty, 0)
    wx.setStorageSync('brew_cart', cart)
    this.setData({ cart, cartCount, cartTotal })
  },
  checkout() {
    if (!this.data.cart.length) return
    const order = { no: `YK${Date.now().toString().slice(-6)}`, total: this.data.cartTotal, count: this.data.cartCount, status: '制作中', time: '预计 12 分钟后完成' }
    wx.setStorageSync('brew_order', order)
    wx.removeStorageSync('brew_cart')
    this.setData({ order, activeTab: 'orders' })
    this.refreshCart([])
    wx.showToast({ title: '下单成功', icon: 'success' })
  },
  reorder() { this.setData({ activeTab: 'menu' }) },
  clearOrder() { wx.removeStorageSync('brew_order'); this.setData({ order: null }) }
})

