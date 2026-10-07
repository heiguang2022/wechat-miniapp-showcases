const KEY = 'yike_cart'
function getCart() { return wx.getStorageSync(KEY) || { merchantId: '', storeId: '', items: [] } }
function saveCart(cart) { wx.setStorageSync(KEY, cart); return cart }
function clearCart() { wx.removeStorageSync(KEY) }
function totals(cart) { return cart.items.reduce((r, i) => ({ count: r.count + i.quantity, amount: r.amount + i.unitPrice * i.quantity }), { count: 0, amount: 0 }) }
module.exports = { getCart, saveCart, clearCart, totals }
