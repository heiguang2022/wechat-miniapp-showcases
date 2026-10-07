const { apiBase } = require('../config')

function request(path, options = {}) {
  const session = wx.getStorageSync('yike_session') || {}
  return new Promise((resolve, reject) => wx.request({
    url: `${apiBase}${path}`,
    method: options.method || 'GET',
    data: options.data,
    header: { 'content-type': 'application/json', ...(session.accessToken ? { Authorization: `Bearer ${session.accessToken}` } : {}), ...(options.header || {}) },
    success(res) {
      if (res.statusCode >= 200 && res.statusCode < 300) return resolve(res.data.data)
      if (res.statusCode === 401) { wx.removeStorageSync('yike_session'); wx.reLaunch({ url: '/pages/login/index' }) }
      reject(new Error(res.data && res.data.message || `请求失败 (${res.statusCode})`))
    },
    fail: reject
  }))
}
module.exports = { request }
