const { request } = require('../../utils/api')
Page({
  data: { loading: false, error: '' },
  async login(e) {
    this.setData({ loading: true, error: '' })
    try {
      const session = await request('/auth/dev-login', { method: 'POST', data: { email: e.currentTarget.dataset.email } })
      wx.setStorageSync('yike_session', session)
      wx.reLaunch({ url: '/pages/menu/index' })
    } catch (error) { this.setData({ error: error.message }) }
    finally { this.setData({ loading: false }) }
  }
})
