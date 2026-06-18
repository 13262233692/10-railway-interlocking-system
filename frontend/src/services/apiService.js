import axios from 'axios'

// API基础配置
const API_BASE_URL = '/api/interlocking'

// 创建axios实例
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
apiClient.interceptors.request.use(
  (config) => {
    return config
  },
  (error) => {
    console.error('API请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
apiClient.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    console.error('API响应错误:', error)
    return Promise.reject(error)
  }
)

// ==================== 站场相关API ====================

// 获取站场布局数据
export const getStationLayout = async () => {
  try {
    return await apiClient.get('/station/layout')
  } catch (error) {
    console.warn('获取站场布局失败，使用本地数据:', error.message)
    return null
  }
}

// 获取所有区段状态
export const getAllSectionStatus = async () => {
  try {
    return await apiClient.get('/sections')
  } catch (error) {
    console.warn('获取区段状态失败:', error.message)
    return null
  }
}

// 获取单个区段状态
export const getSectionStatus = async (sectionId) => {
  try {
    return await apiClient.get(`/sections/${sectionId}`)
  } catch (error) {
    console.warn(`获取区段${sectionId}状态失败:`, error.message)
    return null
  }
}

// ==================== 信号机相关API ====================

// 获取所有信号机状态
export const getAllSignalStatus = async () => {
  try {
    return await apiClient.get('/signals')
  } catch (error) {
    console.warn('获取信号机状态失败:', error.message)
    return null
  }
}

// 获取单个信号机状态
export const getSignalStatus = async (signalId) => {
  try {
    return await apiClient.get(`/signals/${signalId}`)
  } catch (error) {
    console.warn(`获取信号机${signalId}状态失败:`, error.message)
    return null
  }
}

// ==================== 道岔相关API ====================

// 获取所有道岔状态
export const getAllSwitchStatus = async () => {
  try {
    return await apiClient.get('/switches')
  } catch (error) {
    console.warn('获取道岔状态失败:', error.message)
    return null
  }
}

// 获取单个道岔状态
export const getSwitchStatus = async (switchId) => {
  try {
    return await apiClient.get(`/switches/${switchId}`)
  } catch (error) {
    console.warn(`获取道岔${switchId}状态失败:`, error.message)
    return null
  }
}

// 操纵道岔
export const operateSwitch = async (switchId, position) => {
  try {
    return await apiClient.post(`/switches/${switchId}/operate`, { position })
  } catch (error) {
    console.error(`操纵道岔${switchId}失败:`, error.message)
    throw error
  }
}

// ==================== 进路相关API ====================

// 获取所有进路
export const getAllRoutes = async () => {
  try {
    return await apiClient.get('/routes')
  } catch (error) {
    console.warn('获取进路列表失败:', error.message)
    return null
  }
}

// 办理进路
export const establishRoute = async (routeId, operator = '调度员') => {
  try {
    return await apiClient.post('/route/establish', {
      routeId,
      operator
    })
  } catch (error) {
    console.error('办理进路失败:', error.message)
    throw error
  }
}

// 取消进路
export const cancelRoute = async (routeId, operator = '调度员') => {
  try {
    return await apiClient.post('/route/cancel', {
      routeId,
      operator
    })
  } catch (error) {
    console.error('取消进路失败:', error.message)
    throw error
  }
}

// 获取已办理的进路列表
export const getActiveRoutes = async () => {
  try {
    const result = await apiClient.get('/routes')
    if (result && result.success && result.data) {
      return result.data.filter(r => r.locked || r.status === 'LOCKED' || r.status === 'CLEARED' || r.status === 'OCCUPIED')
    }
    return []
  } catch (error) {
    console.warn('获取活跃进路失败:', error.message)
    return []
  }
}

// ==================== 调度命令相关API ====================

// 下达发车指令（模拟，后端暂无此接口）
export const sendDepartureCommand = async (trainNumber, trackId) => {
  try {
    console.log(`发车指令: ${trainNumber} 道 ${trackId}`)
    return { success: true, message: '发车指令已下达' }
  } catch (error) {
    console.error('下达发车指令失败:', error.message)
    throw error
  }
}

// 获取命令执行历史（模拟）
export const getCommandHistory = async (page = 0, size = 20) => {
  try {
    return []
  } catch (error) {
    console.warn('获取命令历史失败:', error.message)
    return []
  }
}

// ==================== 列车相关API ====================

// 获取站内列车列表（模拟）
export const getTrainsInStation = async () => {
  try {
    return []
  } catch (error) {
    console.warn('获取站内列车失败:', error.message)
    return []
  }
}

// 获取列车位置（模拟）
export const getTrainPosition = async (trainId) => {
  try {
    return null
  } catch (error) {
    console.warn(`获取列车${trainId}位置失败:`, error.message)
    return null
  }
}

// ==================== 系统状态API ====================

// 获取系统运行状态
export const getSystemStatus = async () => {
  try {
    return await apiClient.get('/status')
  } catch (error) {
    console.warn('获取系统状态失败:', error.message)
    return { status: 'OFFLINE', message: '后端服务未连接' }
  }
}

// 获取告警信息（模拟，后端暂无此接口）
export const getAlarms = async () => {
  try {
    return []
  } catch (error) {
    console.warn('获取告警信息失败:', error.message)
    return []
  }
}

export default {
  getStationLayout,
  getAllSectionStatus,
  getSectionStatus,
  getAllSignalStatus,
  getSignalStatus,
  getAllSwitchStatus,
  getSwitchStatus,
  operateSwitch,
  getAllRoutes,
  establishRoute,
  cancelRoute,
  getActiveRoutes,
  sendDepartureCommand,
  getCommandHistory,
  getTrainsInStation,
  getTrainPosition,
  getSystemStatus,
  getAlarms
}
