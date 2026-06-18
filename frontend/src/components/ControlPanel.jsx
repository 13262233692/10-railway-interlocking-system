import React, { useState, useMemo } from 'react'

const ControlPanel = ({
  routes,
  allRoutes,
  selectedSignal,
  activeRoutes,
  onEstablishRoute,
  onCancelRoute,
  onSendDepartureCommand,
  systemStatus,
  alarms,
  onRefresh
}) => {
  const [selectedRouteId, setSelectedRouteId] = useState('')
  const [trainNumber, setTrainNumber] = useState('')
  const [selectedTrack, setSelectedTrack] = useState('')
  const [activeTab, setActiveTab] = useState('route')

  const receptionRoutes = useMemo(() => {
    return allRoutes.filter(r => r.routeType === 'RECEPTION')
  }, [allRoutes])

  const departureRoutes = useMemo(() => {
    return allRoutes.filter(r => r.routeType === 'DEPARTURE')
  }, [allRoutes])

  const throughRoutes = useMemo(() => {
    return allRoutes.filter(r => r.routeType === 'THROUGH')
  }, [allRoutes])

  const tracks = [
    { id: '1', name: '1道' },
    { id: '2', name: '2道' },
    { id: '3', name: '3道' },
    { id: '4', name: '4道' },
    { id: '5', name: '5道' },
    { id: '6', name: '6道' },
    { id: '7', name: '7道' },
    { id: '8', name: '8道' },
    { id: '9', name: '9道' },
    { id: '10', name: '10道' },
    { id: '11', name: '11道' },
    { id: '12', name: '12道' }
  ]

  const handleEstablishRoute = () => {
    if (!selectedRouteId) {
      alert('请选择要办理的进路')
      return
    }
    onEstablishRoute(selectedRouteId, '调度员')
    setSelectedRouteId('')
  }

  const handleDeparture = () => {
    if (!trainNumber || !selectedTrack) {
      alert('请输入车次号和选择股道')
      return
    }
    onSendDepartureCommand(trainNumber, selectedTrack)
    setTrainNumber('')
    setSelectedTrack('')
  }

  const handleQuickRoute = (route) => {
    onEstablishRoute(route.id, '调度员')
  }

  const getRouteTypeName = (type) => {
    const names = {
      RECEPTION: '接车进路',
      DEPARTURE: '发车进路',
      THROUGH: '通过进路',
      SHUNT: '调车进路'
    }
    return names[type] || type
  }

  const getRouteStatusName = (status) => {
    const names = {
      NOT_ESTABLISHED: '未办理',
      ESTABLISHING: '办理中',
      LOCKED: '已锁闭',
      CLEARED: '已开放',
      OCCUPIED: '占用中',
      RELEASING: '解锁中',
      UNLOCKED: '已解锁',
      CANCELLED: '已取消'
    }
    return names[status] || status
  }

  const getAlarmLevelClass = (level) => {
    const classes = {
      CRITICAL: 'alarm-critical',
      ERROR: 'alarm-critical',
      WARNING: 'alarm-warning',
      WARN: 'alarm-warning',
      INFO: 'alarm-info'
    }
    return classes[level] || 'alarm-info'
  }

  const getRouteStatusClass = (status) => {
    const classes = {
      LOCKED: 'status-locked',
      CLEARED: 'status-cleared',
      OCCUPIED: 'status-occupied'
    }
    return classes[status] || 'status-established'
  }

  return (
    <div className="control-panel">
      <div className="system-status-bar">
        <div className="status-indicator">
          <span className={`status-dot ${systemStatus === 'ONLINE' ? 'online' : 'offline'}`}></span>
          <span className="status-text">
            系统状态: {systemStatus === 'ONLINE' ? '在线' : '离线'}
          </span>
        </div>
        <button className="refresh-btn" onClick={onRefresh} title="刷新状态">
          🔄 刷新
        </button>
        {alarms.length > 0 && (
          <div className="alarm-indicator">
            <span className="alarm-icon">⚠</span>
            <span className="alarm-count">{alarms.length}</span>
          </div>
        )}
      </div>

      <div className="tab-buttons">
        <button
          className={`tab-btn ${activeTab === 'route' ? 'active' : ''}`}
          onClick={() => setActiveTab('route')}
        >
          进路办理
        </button>
        <button
          className={`tab-btn ${activeTab === 'departure' ? 'active' : ''}`}
          onClick={() => setActiveTab('departure')}
        >
          发车指令
        </button>
        <button
          className={`tab-btn ${activeTab === 'active' ? 'active' : ''}`}
          onClick={() => setActiveTab('active')}
        >
          活跃进路 ({activeRoutes.length})
        </button>
        <button
          className={`tab-btn ${activeTab === 'alarm' ? 'active' : ''}`}
          onClick={() => setActiveTab('alarm')}
        >
          告警信息 ({alarms.length})
        </button>
      </div>

      {activeTab === 'route' && (
        <div className="panel-content">
          <div className="panel-section">
            <h3 className="section-title">办理进路</h3>
            <div className="form-group">
              <label>选择进路:</label>
              <select
                value={selectedRouteId}
                onChange={(e) => setSelectedRouteId(e.target.value)}
                className="form-select"
              >
                <option value="">请选择进路...</option>
                {allRoutes.length > 0 ? (
                  <>
                    <optgroup label="接车进路">
                      {receptionRoutes.map((r) => (
                        <option key={r.id} value={r.id}>
                          {r.name} ({r.number})
                        </option>
                      ))}
                    </optgroup>
                    <optgroup label="发车进路">
                      {departureRoutes.map((r) => (
                        <option key={r.id} value={r.id}>
                          {r.name} ({r.number})
                        </option>
                      ))}
                    </optgroup>
                    <optgroup label="通过进路">
                      {throughRoutes.map((r) => (
                        <option key={r.id} value={r.id}>
                          {r.name} ({r.number})
                        </option>
                      ))}
                    </optgroup>
                  </>
                ) : (
                  routes.map((r) => (
                    <option key={r.id} value={r.id}>
                      {r.name}
                    </option>
                  ))
                )}
              </select>
            </div>
            {selectedSignal && (
              <div className="selected-signal-info">
                <span className="info-label">已选择信号机:</span>
                <span className="signal-name">{selectedSignal.name || selectedSignal.id}</span>
              </div>
            )}
            <button
              onClick={handleEstablishRoute}
              className="action-btn primary-btn"
              disabled={!selectedRouteId}
            >
              办理进路
            </button>
          </div>

          <div className="panel-section">
            <h3 className="section-title">快捷进路</h3>
            <div className="quick-routes">
              {(allRoutes.length > 0 ? allRoutes.slice(0, 12) : routes).map((route) => (
                <button
                  key={route.id}
                  onClick={() => handleQuickRoute(route)}
                  className="quick-route-btn"
                  title={route.name}
                >
                  <span className="route-name">{route.name || route.number}</span>
                  <span className={`route-type ${route.routeType || route.type}`}>
                    {getRouteTypeName(route.routeType || route.type)}
                  </span>
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {activeTab === 'departure' && (
        <div className="panel-content">
          <div className="panel-section">
            <h3 className="section-title">下达发车指令</h3>
            <div className="form-group">
              <label>车次号:</label>
              <input
                type="text"
                value={trainNumber}
                onChange={(e) => setTrainNumber(e.target.value.toUpperCase())}
                placeholder="例如: G1234"
                className="form-input"
                maxLength={10}
              />
            </div>
            <div className="form-group">
              <label>发车股道:</label>
              <select
                value={selectedTrack}
                onChange={(e) => setSelectedTrack(e.target.value)}
                className="form-select"
              >
                <option value="">请选择股道...</option>
                {tracks.map((track) => (
                  <option key={track.id} value={track.id}>{track.name}</option>
                ))}
              </select>
            </div>
            <button
              onClick={handleDeparture}
              className="action-btn success-btn"
              disabled={!trainNumber || !selectedTrack}
            >
              下达发车指令
            </button>
          </div>

          <div className="panel-section">
            <h3 className="section-title">操作说明</h3>
            <div className="instructions">
              <p>1. 在站场图上点击信号机可查看详情</p>
              <p>2. 办理进路需选择具体进路后点击"办理进路"</p>
              <p>3. 进路锁闭后区段显示白光带</p>
              <p>4. 进路开放后信号机显示绿灯/黄灯</p>
              <p>5. 可通过鼠标滚轮缩放视图</p>
              <p>6. 按住鼠标左键可拖动视图</p>
            </div>
          </div>
        </div>
      )}

      {activeTab === 'active' && (
        <div className="panel-content">
          <div className="panel-section">
            <h3 className="section-title">当前活跃进路</h3>
            {activeRoutes.length === 0 ? (
              <div className="empty-state">
                <p>暂无活跃进路</p>
              </div>
            ) : (
              <div className="active-routes-list">
                {activeRoutes.map((route) => (
                  <div key={route.id} className="route-card">
                    <div className="route-header">
                      <span className="route-name">{route.name || route.number}</span>
                      <span className={`route-status ${getRouteStatusClass(route.status)}`}>
                        {getRouteStatusName(route.status)}
                      </span>
                    </div>
                    <div className="route-details">
                      <div className="detail-row">
                        <span className="detail-label">类型:</span>
                        <span>{getRouteTypeName(route.routeType)}</span>
                      </div>
                      <div className="detail-row">
                        <span className="detail-label">方向:</span>
                        <span>{route.direction || '-'}</span>
                      </div>
                      <div className="detail-row">
                        <span className="detail-label">区段数:</span>
                        <span>{route.sectionIds ? route.sectionIds.length : 0} 个</span>
                      </div>
                      {route.sectionIds && route.sectionIds.length > 0 && (
                        <div className="detail-row">
                          <span className="detail-label">区段:</span>
                          <span className="sections-list">{route.sectionIds.slice(0, 5).join(', ')}{route.sectionIds.length > 5 ? '...' : ''}</span>
                        </div>
                      )}
                      {route.operator && (
                        <div className="detail-row">
                          <span className="detail-label">操作员:</span>
                          <span>{route.operator}</span>
                        </div>
                      )}
                    </div>
                    <button
                      onClick={() => onCancelRoute(route.id, '调度员')}
                      className="action-btn danger-btn small-btn"
                    >
                      取消进路
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {activeTab === 'alarm' && (
        <div className="panel-content">
          <div className="panel-section">
            <h3 className="section-title">告警信息</h3>
            {alarms.length === 0 ? (
              <div className="empty-state">
                <p>当前无告警</p>
              </div>
            ) : (
              <div className="alarms-list">
                {alarms.map((alarm, index) => (
                  <div key={index} className={`alarm-item ${getAlarmLevelClass(alarm.level)}`}>
                    <div className="alarm-header">
                      <span className="alarm-level">{alarm.level === 'CRITICAL' || alarm.level === 'ERROR' ? '严重' : alarm.level === 'WARNING' || alarm.level === 'WARN' ? '警告' : '提示'}</span>
                      <span className="alarm-time">{alarm.time || new Date().toLocaleTimeString()}</span>
                    </div>
                    <div className="alarm-message">{alarm.message}</div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      <div className="stats-bar">
        <div className="stat-item">
          <span className="stat-value">{allRoutes.length > 0 ? 113 : 120}</span>
          <span className="stat-label">区段</span>
        </div>
        <div className="stat-item">
          <span className="stat-value">{allRoutes.length > 0 ? 32 : 20}</span>
          <span className="stat-label">道岔</span>
        </div>
        <div className="stat-item">
          <span className="stat-value">{allRoutes.length > 0 ? 49 : 24}</span>
          <span className="stat-label">信号机</span>
        </div>
        <div className="stat-item">
          <span className="stat-value">{activeRoutes.length}</span>
          <span className="stat-label">活跃进路</span>
        </div>
      </div>
    </div>
  )
}

export default ControlPanel
