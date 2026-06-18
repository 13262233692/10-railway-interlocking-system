import React, { useRef, useEffect, useState, useCallback } from 'react'

// 区段状态颜色映射
const SECTION_COLORS = {
  IDLE: '#4a5568',        // 空闲-灰色
  OCCUPIED: '#e53e3e',    // 占用-红色
  LOCKED: '#f6e05e',      // 锁闭-白光带
  FAULT: '#805ad5'        // 故障-紫色
}

// 信号机状态颜色映射
const SIGNAL_COLORS = {
  RED: '#e53e3e',         // 红灯
  YELLOW: '#ecc94b',      // 黄灯
  GREEN: '#48bb78',       // 绿灯
  OFF: '#2d3748'          // 灭灯
}

// 道岔状态颜色映射
const SWITCH_COLORS = {
  STRAIGHT: '#48bb78',    // 定位-绿色
  CURVED: '#ed8936',      // 反位-橙色
  UNKNOWN: '#718096'      // 未知-灰色
}

const StationCanvas = ({
  trackSections,
  switches,
  signals,
  lockedSections,
  onSignalClick,
  selectedSignal
}) => {
  const canvasRef = useRef(null)
  const containerRef = useRef(null)
  const [scale, setScale] = useState(0.8)
  const [offset, setOffset] = useState({ x: 20, y: 20 })
  const [isDragging, setIsDragging] = useState(false)
  const [dragStart, setDragStart] = useState({ x: 0, y: 0 })
  const [hoveredElement, setHoveredElement] = useState(null)

  // 绘制轨道区段
  const drawSection = useCallback((ctx, section, isLocked) => {
    const { x, y, length, angle = 0 } = section
    const status = isLocked ? 'LOCKED' : section.status

    ctx.save()
    ctx.translate(x + offset.x, y + offset.y)
    ctx.rotate((angle * Math.PI) / 180)
    ctx.scale(scale, scale)

    // 区段主体
    ctx.beginPath()
    ctx.lineWidth = 8
    ctx.lineCap = 'round'

    // 区段颜色
    if (isLocked) {
      ctx.strokeStyle = SECTION_COLORS.LOCKED
      ctx.shadowColor = SECTION_COLORS.LOCKED
      ctx.shadowBlur = 10
    } else {
      ctx.strokeStyle = SECTION_COLORS[status] || SECTION_COLORS.IDLE
      ctx.shadowBlur = 0
    }

    ctx.moveTo(0, 0)
    ctx.lineTo(length, 0)
    ctx.stroke()

    // 区段名称
    ctx.shadowBlur = 0
    ctx.fillStyle = '#ffffff'
    ctx.font = 'bold 10px Arial'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(section.name, length / 2, -12)

    // 钢轨纹理
    ctx.beginPath()
    ctx.lineWidth = 2
    ctx.strokeStyle = isLocked ? '#faf089' : '#718096'
    ctx.setLineDash([5, 5])
    ctx.moveTo(0, 0)
    ctx.lineTo(length, 0)
    ctx.stroke()
    ctx.setLineDash([])

    ctx.restore()
  }, [offset, scale])

  // 绘制道岔
  const drawSwitch = useCallback((ctx, sw) => {
    const { x, y, position } = sw

    ctx.save()
    ctx.translate(x + offset.x, y + offset.y)
    ctx.scale(scale, scale)

    // 道岔底座
    ctx.beginPath()
    ctx.arc(0, 0, 12, 0, Math.PI * 2)
    ctx.fillStyle = '#2d3748'
    ctx.fill()
    ctx.strokeStyle = SWITCH_COLORS[position] || SWITCH_COLORS.UNKNOWN
    ctx.lineWidth = 3
    ctx.stroke()

    // 道岔位置指示器
    if (position === 'STRAIGHT') {
      ctx.beginPath()
      ctx.moveTo(-8, 0)
      ctx.lineTo(8, 0)
      ctx.strokeStyle = SWITCH_COLORS.STRAIGHT
      ctx.lineWidth = 3
      ctx.stroke()
    } else if (position === 'CURVED') {
      ctx.beginPath()
      ctx.arc(0, 0, 8, -Math.PI / 4, Math.PI / 4)
      ctx.strokeStyle = SWITCH_COLORS.CURVED
      ctx.lineWidth = 3
      ctx.stroke()
    }

    // 道岔名称
    ctx.fillStyle = '#ffffff'
    ctx.font = 'bold 9px Arial'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(sw.name, 0, 22)

    ctx.restore()
  }, [offset, scale])

  // 绘制信号机
  const drawSignal = useCallback((ctx, signal) => {
    const { x, y, status, direction, type } = signal
    const isSelected = selectedSignal === signal.id

    ctx.save()
    ctx.translate(x + offset.x, y + offset.y)
    ctx.scale(scale, scale)

    // 信号机立柱
    ctx.beginPath()
    ctx.moveTo(0, 0)
    ctx.lineTo(0, 25)
    ctx.strokeStyle = '#4a5568'
    ctx.lineWidth = 3
    ctx.stroke()

    // 信号机灯箱
    ctx.beginPath()
    ctx.rect(-12, -12, 24, 24)
    ctx.fillStyle = isSelected ? '#2b6cb0' : '#1a202c'
    ctx.fill()
    ctx.strokeStyle = isSelected ? '#63b3ed' : '#4a5568'
    ctx.lineWidth = isSelected ? 3 : 2
    ctx.stroke()

    // 信号机灯
    ctx.beginPath()
    ctx.arc(0, 0, 8, 0, Math.PI * 2)
    ctx.fillStyle = SIGNAL_COLORS[status] || SIGNAL_COLORS.OFF

    if (status !== 'OFF') {
      ctx.shadowColor = SIGNAL_COLORS[status]
      ctx.shadowBlur = 15
    }
    ctx.fill()
    ctx.shadowBlur = 0

    // 信号机类型标识
    if (type === 'ENTRY') {
      ctx.fillStyle = '#ffffff'
      ctx.font = 'bold 8px Arial'
      ctx.textAlign = 'center'
      ctx.fillText('进', 0, -18)
    } else if (type === 'EXIT') {
      ctx.fillStyle = '#ffffff'
      ctx.font = 'bold 8px Arial'
      ctx.textAlign = 'center'
      ctx.fillText('出', 0, -18)
    } else if (type === 'SHUNT') {
      ctx.fillStyle = '#ffffff'
      ctx.font = 'bold 8px Arial'
      ctx.textAlign = 'center'
      ctx.fillText('调', 0, -18)
    }

    // 信号机名称
    ctx.fillStyle = '#e2e8f0'
    ctx.font = 'bold 10px Arial'
    ctx.textAlign = direction === 'LEFT' ? 'right' : 'left'
    ctx.textBaseline = 'middle'
    const textX = direction === 'LEFT' ? -18 : 18
    ctx.fillText(signal.name, textX, 0)

    // 方向箭头
    ctx.beginPath()
    ctx.fillStyle = SIGNAL_COLORS[status] || '#718096'
    if (direction === 'RIGHT') {
      ctx.moveTo(14, 0)
      ctx.lineTo(10, -4)
      ctx.lineTo(10, 4)
    } else {
      ctx.moveTo(-14, 0)
      ctx.lineTo(-10, -4)
      ctx.lineTo(-10, 4)
    }
    ctx.closePath()
    ctx.fill()

    ctx.restore()
  }, [offset, scale, selectedSignal])

  // 绘制站场背景
  const drawBackground = useCallback((ctx, width, height) => {
    // 背景色
    ctx.fillStyle = '#0f172a'
    ctx.fillRect(0, 0, width, height)

    // 网格线
    ctx.strokeStyle = '#1e293b'
    ctx.lineWidth = 1
    const gridSize = 50 * scale

    for (let x = 0; x < width; x += gridSize) {
      ctx.beginPath()
      ctx.moveTo(x, 0)
      ctx.lineTo(x, height)
      ctx.stroke()
    }

    for (let y = 0; y < height; y += gridSize) {
      ctx.beginPath()
      ctx.moveTo(0, y)
      ctx.lineTo(width, y)
      ctx.stroke()
    }

    // 站场名称
    ctx.fillStyle = '#94a3b8'
    ctx.font = 'bold 14px Arial'
    ctx.textAlign = 'left'
    ctx.fillText('南站 - 站场示意图', 15, 25)
  }, [scale])

  // 绘制图例
  const drawLegend = useCallback((ctx, width) => {
    const legendX = width - 180
    const legendY = 15
    const itemHeight = 20

    ctx.save()
    ctx.fillStyle = 'rgba(15, 23, 42, 0.9)'
    ctx.fillRect(legendX - 10, legendY - 10, 170, 140)
    ctx.strokeStyle = '#334155'
    ctx.lineWidth = 1
    ctx.strokeRect(legendX - 10, legendY - 10, 170, 140)

    ctx.font = 'bold 12px Arial'
    ctx.fillStyle = '#e2e8f0'
    ctx.textAlign = 'left'
    ctx.fillText('图例', legendX, legendY + 5)

    // 区段状态
    const statusItems = [
      { color: SECTION_COLORS.IDLE, label: '空闲' },
      { color: SECTION_COLORS.OCCUPIED, label: '占用' },
      { color: SECTION_COLORS.LOCKED, label: '锁闭' },
      { color: SECTION_COLORS.FAULT, label: '故障' }
    ]

    statusItems.forEach((item, index) => {
      const y = legendY + 25 + index * itemHeight
      ctx.beginPath()
      ctx.rect(legendX, y - 6, 20, 10)
      ctx.fillStyle = item.color
      ctx.fill()
      ctx.fillStyle = '#94a3b8'
      ctx.font = '11px Arial'
      ctx.fillText(item.label, legendX + 28, y + 3)
    })

    // 信号机状态
    const signalItems = [
      { color: SIGNAL_COLORS.RED, label: '停车' },
      { color: SIGNAL_COLORS.YELLOW, label: '注意' },
      { color: SIGNAL_COLORS.GREEN, label: '通行' }
    ]

    signalItems.forEach((item, index) => {
      const y = legendY + 25 + (index + 5) * itemHeight
      ctx.beginPath()
      ctx.arc(legendX + 10, y - 1, 5, 0, Math.PI * 2)
      ctx.fillStyle = item.color
      ctx.fill()
      ctx.fillStyle = '#94a3b8'
      ctx.font = '11px Arial'
      ctx.fillText(item.label, legendX + 28, y + 3)
    })

    ctx.restore()
  }, [])

  // 主绘制函数
  const draw = useCallback(() => {
    const canvas = canvasRef.current
    if (!canvas) return

    const ctx = canvas.getContext('2d')
    const { width, height } = canvas

    ctx.clearRect(0, 0, width, height)

    drawBackground(ctx, width, height)
    drawLegend(ctx, width)

    // 绘制轨道区段
    trackSections.forEach((section) => {
      const isLocked = lockedSections.includes(section.id)
      drawSection(ctx, section, isLocked)
    })

    // 绘制道岔
    switches.forEach((sw) => {
      drawSwitch(ctx, sw)
    })

    // 绘制信号机
    signals.forEach((signal) => {
      drawSignal(ctx, signal)
    })

    // 绘制悬停提示
    if (hoveredElement) {
      const { x, y, name, type, status } = hoveredElement
      const tipX = x + offset.x + 15
      const tipY = y + offset.y - 10

      ctx.save()
      ctx.fillStyle = 'rgba(15, 23, 42, 0.95)'
      ctx.fillRect(tipX, tipY, 120, 50)
      ctx.strokeStyle = '#3b82f6'
      ctx.lineWidth = 1
      ctx.strokeRect(tipX, tipY, 120, 50)

      ctx.fillStyle = '#e2e8f0'
      ctx.font = 'bold 11px Arial'
      ctx.textAlign = 'left'
      ctx.fillText(name, tipX + 8, tipY + 18)

      ctx.fillStyle = '#94a3b8'
      ctx.font = '10px Arial'
      ctx.fillText(`类型: ${type}`, tipX + 8, tipY + 34)
      ctx.fillText(`状态: ${status}`, tipX + 8, tipY + 46)
      ctx.restore()
    }
  }, [trackSections, switches, signals, lockedSections, drawSection, drawSwitch, drawSignal, drawBackground, drawLegend, offset, hoveredElement])

  // 获取点击位置对应的元素
  const getElementAtPosition = useCallback((clientX, clientY) => {
    const canvas = canvasRef.current
    if (!canvas) return null

    const rect = canvas.getBoundingClientRect()
    const x = (clientX - rect.left - offset.x) / scale
    const y = (clientY - rect.top - offset.y) / scale

    // 检查信号机（优先级最高）
    for (const signal of signals) {
      const dx = Math.abs(x - signal.x)
      const dy = Math.abs(y - signal.y)
      if (dx < 15 && dy < 15) {
        return { type: 'signal', element: signal }
      }
    }

    // 检查道岔
    for (const sw of switches) {
      const dx = Math.abs(x - sw.x)
      const dy = Math.abs(y - sw.y)
      if (dx < 15 && dy < 15) {
        return { type: 'switch', element: sw }
      }
    }

    // 检查区段
    for (const section of trackSections) {
      const angle = section.angle || 0
      const rad = (angle * Math.PI) / 180
      const cos = Math.cos(-rad)
      const sin = Math.sin(-rad)
      const localX = (x - section.x) * cos - (y - section.y) * sin
      const localY = (x - section.x) * sin + (y - section.y) * cos

      if (localX >= 0 && localX <= section.length && Math.abs(localY) < 8) {
        return { type: 'section', element: section }
      }
    }

    return null
  }, [signals, switches, trackSections, offset, scale])

  // 处理鼠标点击
  const handleClick = useCallback((e) => {
    const hit = getElementAtPosition(e.clientX, e.clientY)
    if (hit && hit.type === 'signal') {
      onSignalClick && onSignalClick(hit.element)
    }
  }, [getElementAtPosition, onSignalClick])

  // 处理鼠标移动
  const handleMouseMove = useCallback((e) => {
    if (isDragging) {
      const dx = e.clientX - dragStart.x
      const dy = e.clientY - dragStart.y
      setOffset(prev => ({
        x: prev.x + dx,
        y: prev.y + dy
      }))
      setDragStart({ x: e.clientX, y: e.clientY })
    } else {
      const hit = getElementAtPosition(e.clientX, e.clientY)
      if (hit) {
        setHoveredElement(hit.element)
        canvasRef.current.style.cursor = hit.type === 'signal' ? 'pointer' : 'default'
      } else {
        setHoveredElement(null)
        canvasRef.current.style.cursor = 'grab'
      }
    }
  }, [isDragging, dragStart, getElementAtPosition])

  // 处理鼠标按下
  const handleMouseDown = useCallback((e) => {
    const hit = getElementAtPosition(e.clientX, e.clientY)
    if (!hit) {
      setIsDragging(true)
      setDragStart({ x: e.clientX, y: e.clientY })
      canvasRef.current.style.cursor = 'grabbing'
    }
  }, [getElementAtPosition])

  // 处理鼠标释放
  const handleMouseUp = useCallback(() => {
    setIsDragging(false)
    canvasRef.current.style.cursor = 'grab'
  }, [])

  // 处理滚轮缩放
  const handleWheel = useCallback((e) => {
    e.preventDefault()
    const delta = e.deltaY > 0 ? 0.9 : 1.1
    setScale(prev => Math.max(0.3, Math.min(2, prev * delta)))
  }, [])

  // 初始化Canvas尺寸
  useEffect(() => {
    const updateCanvasSize = () => {
      const canvas = canvasRef.current
      const container = containerRef.current
      if (!canvas || !container) return

      const dpr = window.devicePixelRatio || 1
      const rect = container.getBoundingClientRect()

      canvas.width = rect.width * dpr
      canvas.height = rect.height * dpr
      canvas.style.width = `${rect.width}px`
      canvas.style.height = `${rect.height}px`

      const ctx = canvas.getContext('2d')
      ctx.scale(dpr, dpr)
    }

    updateCanvasSize()
    window.addEventListener('resize', updateCanvasSize)

    return () => {
      window.removeEventListener('resize', updateCanvasSize)
    }
  }, [])

  // 监听数据变化重绘
  useEffect(() => {
    draw()
  }, [draw])

  return (
    <div ref={containerRef} className="station-canvas-container">
      <canvas
        ref={canvasRef}
        onClick={handleClick}
        onMouseMove={handleMouseMove}
        onMouseDown={handleMouseDown}
        onMouseUp={handleMouseUp}
        onMouseLeave={handleMouseUp}
        onWheel={handleWheel}
        style={{
          width: '100%',
          height: '100%',
          display: 'block',
          cursor: 'grab'
        }}
      />
      {/* 缩放控制 */}
      <div className="canvas-controls">
        <button
          onClick={() => setScale(prev => Math.min(2, prev * 1.2))}
          className="control-btn"
          title="放大"
        >
          +
        </button>
        <button
          onClick={() => setScale(prev => Math.max(0.3, prev * 0.8))}
          className="control-btn"
          title="缩小"
        >
          −
        </button>
        <button
          onClick={() => { setScale(0.8); setOffset({ x: 20, y: 20 }) }}
          className="control-btn"
          title="重置视图"
        >
          ⟲
        </button>
        <span className="scale-indicator">{Math.round(scale * 100)}%</span>
      </div>
    </div>
  )
}

export default StationCanvas
