import React, { useState, useEffect, useCallback } from 'react'
import StationCanvas from './components/StationCanvas'
import ControlPanel from './components/ControlPanel'
import { stationLayout, routes as presetRoutes, SectionStatus, SignalStatus, SwitchPosition } from './data/stationLayout'
import apiService from './services/apiService'
import websocketService from './services/websocketService'
import './App.css'

function App() {
  const [trackSections, setTrackSections] = useState([...stationLayout.trackSections])
  const [switches, setSwitches] = useState([...stationLayout.switches])
  const [signals, setSignals] = useState([...stationLayout.signals])
  const [routes, setRoutes] = useState([...presetRoutes])
  const [allRoutes, setAllRoutes] = useState([])

  const [selectedSignal, setSelectedSignal] = useState(null)
  const [activeRoutes, setActiveRoutes] = useState([])
  const [lockedSections, setLockedSections] = useState([])
  const [systemStatus, setSystemStatus] = useState('OFFLINE')
  const [alarms, setAlarms] = useState([])
  const [wsConnected, setWsConnected] = useState(false)
  const [currentTime, setCurrentTime] = useState(new Date())

  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentTime(new Date())
    }, 1000)
    return () => clearInterval(timer)
  }, [])

  useEffect(() => {
    const initWebSocket = async () => {
      try {
        await websocketService.connect()
        setWsConnected(true)
        setSystemStatus('ONLINE')

        websocketService.subscribeStatusUpdate((data) => {
          handleFullStatusUpdate(data)
        })

        websocketService.subscribe('connect', () => {
          setWsConnected(true)
          setSystemStatus('ONLINE')
          websocketService.requestStatus()
        })

        websocketService.subscribe('disconnect', () => {
          setWsConnected(false)
          setSystemStatus('OFFLINE')
        })
      } catch (error) {
        console.warn('WebSocket连接失败，使用本地模拟模式')
        setWsConnected(false)
        setSystemStatus('OFFLINE')
      }
    }

    initWebSocket()

    return () => {
      websocketService.disconnect()
    }
  }, [])

  useEffect(() => {
    const loadInitialData = async () => {
      try {
        const [sectionData, signalData, switchData, routesData, statusData] = await Promise.all([
          apiService.getAllSectionStatus(),
          apiService.getAllSignalStatus(),
          apiService.getAllSwitchStatus(),
          apiService.getAllRoutes(),
          apiService.getSystemStatus()
        ])

        if (sectionData && sectionData.success && sectionData.data) {
          const sections = sectionData.data
          setTrackSections(prev => prev.map(section => {
            const updated = sections.find(s => s.id === section.id)
            if (updated) {
              const status = mapSectionStatus(updated.status)
              return { ...section, status, locked: updated.locked, occupied: updated.occupied }
            }
            return section
          }))
        }

        if (signalData && signalData.success && signalData.data) {
          const signalsList = signalData.data
          setSignals(prev => prev.map(signal => {
            const updated = signalsList.find(s => s.id === signal.id)
            if (updated) {
              const status = mapSignalAspect(updated.aspect)
              return { ...signal, status, aspect: updated.aspect, cleared: updated.cleared, name: updated.name || signal.name }
            }
            return signal
          }))
        }

        if (switchData && switchData.success && switchData.data) {
          const switchesList = switchData.data
          setSwitches(prev => prev.map(sw => {
            const updated = switchesList.find(s => s.id === sw.id)
            if (updated) {
              const position = mapSwitchPosition(updated.position)
              return { ...sw, position, locked: updated.locked, name: updated.name || sw.name }
            }
            return sw
          }))
        }

        if (routesData && routesData.success && routesData.data) {
          setAllRoutes(routesData.data)
          const active = routesData.data.filter(r => r.locked || r.status === 'LOCKED' || r.status === 'CLEARED' || r.status === 'OCCUPIED')
          setActiveRoutes(active)
          const locked = active.flatMap(r => r.sectionIds || [])
          setLockedSections(locked)
        }

        if (statusData && statusData.success && statusData.data) {
          setSystemStatus('ONLINE')
        }
      } catch (error) {
        console.warn('加载后端数据失败，使用本地数据:', error.message)
      }
    }

    loadInitialData()
  }, [])

  const mapSectionStatus = (backendStatus) => {
    const statusMap = {
      'IDLE': SectionStatus.IDLE,
      'OCCUPIED': SectionStatus.OCCUPIED,
      'LOCKED': SectionStatus.LOCKED,
      'RESERVED': SectionStatus.LOCKED,
      'FAULT': SectionStatus.FAULT
    }
    return statusMap[backendStatus] || SectionStatus.IDLE
  }

  const mapSignalAspect = (aspect) => {
    const aspectMap = {
      'RED': SignalStatus.RED,
      'YELLOW': SignalStatus.YELLOW,
      'GREEN': SignalStatus.GREEN,
      'DOUBLE_YELLOW': SignalStatus.DOUBLE_YELLOW,
      'OFF': SignalStatus.OFF
    }
    return aspectMap[aspect] || SignalStatus.RED
  }

  const mapSwitchPosition = (position) => {
    const positionMap = {
      'STRAIGHT': SwitchPosition.STRAIGHT,
      'REVERSE': SwitchPosition.DIVERGING,
      'MOVING': SwitchPosition.MOVING,
      'FOUR_WAY': SwitchPosition.FAULT
    }
    return positionMap[position] || SwitchPosition.STRAIGHT
  }

  const handleFullStatusUpdate = useCallback((data) => {
    if (!data) return

    if (data.sections) {
      setTrackSections(prev => prev.map(section => {
        const updated = data.sections.find(s => s.id === section.id)
        if (updated) {
          const status = mapSectionStatus(updated.status)
          return { ...section, status, locked: updated.locked, occupied: updated.occupied }
        }
        return section
      }))
    }

    if (data.signals) {
      setSignals(prev => prev.map(signal => {
        const updated = data.signals.find(s => s.id === signal.id)
        if (updated) {
          const status = mapSignalAspect(updated.aspect)
          return { ...signal, status, aspect: updated.aspect, cleared: updated.cleared }
        }
        return signal
      }))
    }

    if (data.switches) {
      setSwitches(prev => prev.map(sw => {
        const updated = data.switches.find(s => s.id === sw.id)
        if (updated) {
          const position = mapSwitchPosition(updated.position)
          return { ...sw, position, locked: updated.locked }
        }
        return sw
      }))
    }

    if (data.routes) {
      const active = data.routes.filter(r => r.locked || r.status === 'LOCKED' || r.status === 'CLEARED' || r.status === 'OCCUPIED')
      setActiveRoutes(active)
      const locked = active.flatMap(r => r.sectionIds || [])
      setLockedSections(locked)
    }
  }, [])

  const handleEstablishRoute = useCallback(async (routeId, operator = '调度员') => {
    try {
      const result = await apiService.establishRoute(routeId, operator)

      if (result && result.success) {
        const routeData = result.data
        addAlarm({
          level: 'INFO',
          message: `进路办理成功: ${routeData.routeName || routeId}`,
          time: new Date().toLocaleTimeString()
        })

        setTimeout(() => {
          loadCurrentStatus()
        }, 500)
      } else {
        addAlarm({
          level: 'WARNING',
          message: result.message || '办理进路失败',
          time: new Date().toLocaleTimeString()
        })
      }
    } catch (error) {
      console.error('办理进路失败:', error)
      const errorMsg = error.response?.data?.message || error.message || '办理进路失败'
      addAlarm({
        level: 'WARNING',
        message: errorMsg,
        time: new Date().toLocaleTimeString()
      })
    }
  }, [])

  const handleCancelRoute = useCallback(async (routeId, operator = '调度员') => {
    try {
      const result = await apiService.cancelRoute(routeId, operator)

      if (result && result.success) {
        addAlarm({
          level: 'INFO',
          message: `进路已取消: ${routeId}`,
          time: new Date().toLocaleTimeString()
        })

        setTimeout(() => {
          loadCurrentStatus()
        }, 500)
      } else {
        addAlarm({
          level: 'WARNING',
          message: result.message || '取消进路失败',
          time: new Date().toLocaleTimeString()
        })
      }
    } catch (error) {
      console.error('取消进路失败:', error)
      const errorMsg = error.response?.data?.message || error.message || '取消进路失败'
      addAlarm({
        level: 'WARNING',
        message: errorMsg,
        time: new Date().toLocaleTimeString()
      })
    }
  }, [])

  const loadCurrentStatus = async () => {
    try {
      const statusData = await apiService.getSystemStatus()
      if (statusData && statusData.success && statusData.data) {
        handleFullStatusUpdate(statusData.data)
      }
    } catch (error) {
      console.warn('刷新状态失败:', error.message)
    }
  }

  const handleSendDepartureCommand = useCallback(async (trainNumber, trackId) => {
    try {
      const departureRoute = allRoutes.find(r =>
        r.routeType === 'DEPARTURE' &&
        r.name && r.name.includes(`${trackId}道发`)
      )

      if (departureRoute) {
        await handleEstablishRoute(departureRoute.id, '发车调度')
      }

      addAlarm({
        level: 'INFO',
        message: `发车指令已下达: ${trainNumber}次 第${trackId}道`,
        time: new Date().toLocaleTimeString()
      })
    } catch (error) {
      console.error('发送发车指令失败:', error)
      addAlarm({
        level: 'WARNING',
        message: '发送发车指令失败: ' + error.message,
        time: new Date().toLocaleTimeString()
      })
    }
  }, [allRoutes, handleEstablishRoute])

  const handleSignalClick = useCallback((signal) => {
    setSelectedSignal(signal)
  }, [])

  const addAlarm = useCallback((alarm) => {
    setAlarms(prev => [alarm, ...prev].slice(0, 20))
  }, [])

  return (
    <div className="app-container">
      <header className="app-header">
        <div className="header-left">
          <h1 className="app-title">
            <span className="title-icon">🚄</span>
            高铁车站联锁系统 - 调度指挥中心
          </h1>
          <div className="station-info">
            <span className="station-name">{stationLayout.stationInfo.name}</span>
            <span className="station-code">[{stationLayout.stationInfo.code}]</span>
            <span className="station-type">{stationLayout.stationInfo.type}</span>
          </div>
        </div>
        <div className="header-right">
          <div className="time-display">
            <span className="time">{currentTime.toLocaleTimeString()}</span>
            <span className="date">{currentTime.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', weekday: 'short' })}</span>
          </div>
          <div className="connection-status">
            <span className={`ws-dot ${wsConnected ? 'connected' : 'disconnected'}`}></span>
            <span>{wsConnected ? '实时连接' : '离线模式'}</span>
          </div>
        </div>
      </header>

      <main className="main-content">
        <div className="station-area">
          <StationCanvas
            trackSections={trackSections}
            switches={switches}
            signals={signals}
            lockedSections={lockedSections}
            onSignalClick={handleSignalClick}
            selectedSignal={selectedSignal?.id}
          />
        </div>

        <div className="control-area">
          <ControlPanel
            routes={routes}
            allRoutes={allRoutes}
            selectedSignal={selectedSignal}
            activeRoutes={activeRoutes}
            onEstablishRoute={handleEstablishRoute}
            onCancelRoute={handleCancelRoute}
            onSendDepartureCommand={handleSendDepartureCommand}
            systemStatus={systemStatus}
            alarms={alarms}
            onRefresh={loadCurrentStatus}
          />
        </div>
      </main>

      <footer className="app-footer">
        <div className="footer-left">
          <span>区段总数: {trackSections.length}</span>
          <span>道岔总数: {switches.length}</span>
          <span>信号机总数: {signals.length}</span>
        </div>
        <div className="footer-right">
          <span>活跃进路: {activeRoutes.length}</span>
          <span>锁闭区段: {lockedSections.length}</span>
          <span>当前告警: {alarms.length}</span>
        </div>
      </footer>
    </div>
  )
}

export default App
