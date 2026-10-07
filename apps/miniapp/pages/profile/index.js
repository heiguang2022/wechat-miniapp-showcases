const { request } = require('../../utils/api')
Page({
  data: { user: null },
  onShow() { request('/auth/me').then(user => this.setData({ user })).catch(() => {}) },
  logout() { const session = wx.getStorageSync('yike_session') || {}; request('/auth/logout', { method: 'POST', data: { refreshToken: session.refreshToken || '' } }).catch(() => {}).finally(() => { wx.removeStorageSync('yike_session'); wx.removeStorageSync('yike_cart'); wx.reLaunch({ url: '/pages/login/index' }) }) }
})
