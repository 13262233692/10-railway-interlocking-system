// WebSocket配置 - 原生WebSocket
const WS_URL = '/api/ws/interlocking'

// 消息类型
const MESSAGE_TYPES = {
  CONNECTED: 'CONNECTED',
  PONG: 'PONG',
  STATUS_UPDATE: 'STATUS_UPDATE',
  SECTION_UPDATE: 'SECTION_UPDATE',
  SIGNAL_UPDATE: 'SIGNAL_UPDATE',
  SWITCH_UPDATE: 'SWITCH_UPDATE',
  ROUTE_UPDATE: 'ROUTE_UPDATE',
  SYSTEM_ALARM: 'SYSTEM_ALARM',
  MESSAGE_RECEIVED: 'MESSAGE_RECEIVED',
  ERROR: 'ERROR'
}

class WebSocketService {
  constructor() {
    this.socket = null
    this.connected = false
    this.listeners = new Map()
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 10
    this.reconnectInterval = 3000
    this.heartbeatInterval = null
    this.heartbeatIntervalTime = 30000
  }

  connect() {
    return new Promise((resolve, reject) => {
      try {
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
        const host = window.location.host
        const wsUrl = `${protocol}//${host}${WS_URL}`

        console.log('正在连接WebSocket:', wsUrl)
        this.socket = new WebSocket(wsUrl)

        this.socket.onopen = () => {
          console.log('WebSocket连接成功')
          this.connected = true
          this.reconnectAttempts = 0
          this.startHeartbeat()
          this.notifyListeners('connect', null)
          resolve(this.socket)
        }

        this.socket.onmessage = (event) => {
          try {
            const message = JSON.parse(event.data)
            this.handleMessage(message)
          } catch (error) {
            console.error('解析WebSocket消息失败:', error, event.data)
          }
        }

        this.socket.onerror = (error) => {
          console.error('WebSocket连接错误:', error)
          this.connected = false
          this.stopHeartbeat()
          reject(error)
        }

        this.socket.onclose = (event) => {
          console.log('WebSocket连接关闭:', event.code, event.reason)
          this.connected = false
          this.stopHeartbeat()
          this.notifyListeners('disconnect', event)
          this.handleReconnect()
        }
      } catch (error) {
        console.error('创建WebSocket连接失败:', error)
        reject(error)
      }
    })
  }

  disconnect() {
    this.stopHeartbeat()
    if (this.socket) {
      this.socket.close()
      this.socket = null
    }
    this.connected = false
    this.listeners.clear()
    console.log('WebSocket已断开连接')
  }

  handleMessage(message) {
    const { type, data, level, content } = message

    switch (type) {
      case MESSAGE_TYPES.CONNECTED:
        console.log('WebSocket已连接:', content)
        break

      case MESSAGE_TYPES.PONG:
        break

      case MESSAGE_TYPES.STATUS_UPDATE:
        this.notifyListeners('status', data)
        this.notifyListeners('sections', data.sections)
        this.notifyListeners('signals', data.signals)
        this.notifyListeners('switches', data.switches)
        this.notifyListeners('routes', data.routes)
        break

      case MESSAGE_TYPES.SECTION_UPDATE:
        this.notifyListeners('section', data)
        break

      case MESSAGE_TYPES.SIGNAL_UPDATE:
        this.notifyListeners('signal', data)
        break

      case MESSAGE_TYPES.SWITCH_UPDATE:
        this.notifyListeners('switch', data)
        break

      case MESSAGE_TYPES.ROUTE_UPDATE:
        this.notifyListeners('route', data)
        break

      case MESSAGE_TYPES.SYSTEM_ALARM:
        this.notifyListeners('alarm', message)
        break

      case MESSAGE_TYPES.ERROR:
        console.error('WebSocket错误消息:', content)
        this.notifyListeners('error', message)
        break

      default:
        break
    }

    this.notifyListeners('message', message)
  }

  startHeartbeat() {
    this.stopHeartbeat()
    this.heartbeatInterval = setInterval(() => {
      if (this.connected && this.socket && this.socket.readyState === WebSocket.OPEN) {
        this.send('PING')
      }
    }, this.heartbeatIntervalTime)
  }

  stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval)
      this.heartbeatInterval = null
    }
  }

  handleReconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++
      console.log(`尝试重连 (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`)
      setTimeout(() => {
        this.connect().catch(() => {
          console.warn('重连失败')
        })
      }, this.reconnectInterval)
    } else {
      console.error('已达到最大重连次数，停止重连')
      this.notifyListeners('reconnect_failed', null)
    }
  }

  isConnected() {
    return this.connected && this.socket && this.socket.readyState === WebSocket.OPEN
  }

  send(data) {
    if (!this.isConnected()) {
      console.warn('WebSocket未连接，无法发送消息')
      return false
    }

    try {
      const message = typeof data === 'string' ? data : JSON.stringify(data)
      this.socket.send(message)
      return true
    } catch (error) {
      console.error('发送消息失败:', error)
      return false
    }
  }

  requestStatus() {
    return this.send('GET_STATUS')
  }

  subscribe(eventType, callback) {
    if (!this.listeners.has(eventType)) {
      this.listeners.set(eventType, new Set())
    }
    this.listeners.get(eventType).add(callback)
    return () => this.unsubscribe(eventType, callback)
  }

  unsubscribe(eventType, callback) {
    if (this.listeners.has(eventType)) {
      this.listeners.get(eventType).delete(callback)
      if (this.listeners.get(eventType).size === 0) {
        this.listeners.delete(eventType)
      }
    }
  }

  notifyListeners(eventType, data) {
    if (this.listeners.has(eventType)) {
      this.listeners.get(eventType).forEach((callback) => {
        try {
          callback(data)
        } catch (error) {
          console.error(`事件监听器错误 [${eventType}]:`, error)
        }
      })
    }
  }

  subscribeSectionStatus(callback) {
    return this.subscribe('section', callback)
  }

  subscribeSignalStatus(callback) {
    return this.subscribe('signal', callback)
  }

  subscribeSwitchStatus(callback) {
    return this.subscribe('switch', callback)
  }

  subscribeRouteStatus(callback) {
    return this.subscribe('route', callback)
  }

  subscribeSystemAlarm(callback) {
    return this.subscribe('alarm', callback)
  }

  subscribeStatusUpdate(callback) {
    return this.subscribe('status', callback)
  }
}

const websocketService = new WebSocketService()

export default websocketService

export { MESSAGE_TYPES, WebSocketService }
