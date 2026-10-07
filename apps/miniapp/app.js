App({
  globalData: { name: '一刻咖啡' },
  onLaunch() {
    if (!wx.getStorageSync('yike_session')) wx.reLaunch({ url: '/pages/login/index' })
  }
})
