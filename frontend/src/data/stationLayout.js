// 铁路站场布局数据
// 包含100+轨道区段、道岔、信号机的完整站场配置

// 区段状态枚举
export const SectionStatus = {
  IDLE: 'IDLE',           // 空闲
  OCCUPIED: 'OCCUPIED',   // 占用
  LOCKED: 'LOCKED',       // 锁闭
  FAULT: 'FAULT'          // 故障
}

// 信号机状态枚举
export const SignalStatus = {
  RED: 'RED',         // 红灯-停车
  YELLOW: 'YELLOW',   // 黄灯-注意
  GREEN: 'GREEN',     // 绿灯-通行
  OFF: 'OFF'          // 灭灯
}

// 道岔位置枚举
export const SwitchPosition = {
  STRAIGHT: 'STRAIGHT',   // 定位
  CURVED: 'CURVED',       // 反位
  UNKNOWN: 'UNKNOWN'      // 未知
}

// 轨道区段数据 - 共120个区段
export const trackSections = [
  // ========== 上行正线（I道） ==========
  { id: 'S1', name: 'S1', type: 'SECTION', x: 50, y: 80, length: 100, status: 'IDLE', line: '上行正线' },
  { id: 'S2', name: 'S2', type: 'SECTION', x: 150, y: 80, length: 100, status: 'IDLE', line: '上行正线' },
  { id: 'S3', name: 'S3', type: 'SECTION', x: 250, y: 80, length: 100, status: 'IDLE', line: '上行正线' },
  { id: 'S4', name: 'S4', type: 'SECTION', x: 350, y: 80, length: 100, status: 'IDLE', line: '上行正线' },
  { id: 'S5', name: 'S5', type: 'SECTION', x: 450, y: 80, length: 100, status: 'IDLE', line: '上行正线' },
  { id: 'S6', name: 'S6', type: 'SECTION', x: 550, y: 80, length: 100, status: 'IDLE', line: '上行正线' },
  { id: 'S7', name: 'S7', type: 'SECTION', x: 650, y: 80, length: 100, status: 'IDLE', line: '上行正线' },
  { id: 'S8', name: 'S8', type: 'SECTION', x: 750, y: 80, length: 100, status: 'IDLE', line: '上行正线' },
  { id: 'S9', name: 'S9', type: 'SECTION', x: 850, y: 80, length: 100, status: 'IDLE', line: '上行正线' },
  { id: 'S10', name: 'S10', type: 'SECTION', x: 950, y: 80, length: 100, status: 'IDLE', line: '上行正线' },
  { id: 'S11', name: 'S11', type: 'SECTION', x: 1050, y: 80, length: 100, status: 'IDLE', line: '上行正线' },
  { id: 'S12', name: 'S12', type: 'SECTION', x: 1150, y: 80, length: 100, status: 'IDLE', line: '上行正线' },
  { id: 'S13', name: 'S13', type: 'SECTION', x: 1250, y: 80, length: 100, status: 'IDLE', line: '上行正线' },
  { id: 'S14', name: 'S14', type: 'SECTION', x: 1350, y: 80, length: 100, status: 'IDLE', line: '上行正线' },
  { id: 'S15', name: 'S15', type: 'SECTION', x: 1450, y: 80, length: 100, status: 'IDLE', line: '上行正线' },

  // ========== 下行正线（II道） ==========
  { id: 'X1', name: 'X1', type: 'SECTION', x: 50, y: 130, length: 100, status: 'IDLE', line: '下行正线' },
  { id: 'X2', name: 'X2', type: 'SECTION', x: 150, y: 130, length: 100, status: 'IDLE', line: '下行正线' },
  { id: 'X3', name: 'X3', type: 'SECTION', x: 250, y: 130, length: 100, status: 'IDLE', line: '下行正线' },
  { id: 'X4', name: 'X4', type: 'SECTION', x: 350, y: 130, length: 100, status: 'IDLE', line: '下行正线' },
  { id: 'X5', name: 'X5', type: 'SECTION', x: 450, y: 130, length: 100, status: 'IDLE', line: '下行正线' },
  { id: 'X6', name: 'X6', type: 'SECTION', x: 550, y: 130, length: 100, status: 'IDLE', line: '下行正线' },
  { id: 'X7', name: 'X7', type: 'SECTION', x: 650, y: 130, length: 100, status: 'IDLE', line: '下行正线' },
  { id: 'X8', name: 'X8', type: 'SECTION', x: 750, y: 130, length: 100, status: 'IDLE', line: '下行正线' },
  { id: 'X9', name: 'X9', type: 'SECTION', x: 850, y: 130, length: 100, status: 'IDLE', line: '下行正线' },
  { id: 'X10', name: 'X10', type: 'SECTION', x: 950, y: 130, length: 100, status: 'IDLE', line: '下行正线' },
  { id: 'X11', name: 'X11', type: 'SECTION', x: 1050, y: 130, length: 100, status: 'IDLE', line: '下行正线' },
  { id: 'X12', name: 'X12', type: 'SECTION', x: 1150, y: 130, length: 100, status: 'IDLE', line: '下行正线' },
  { id: 'X13', name: 'X13', type: 'SECTION', x: 1250, y: 130, length: 100, status: 'IDLE', line: '下行正线' },
  { id: 'X14', name: 'X14', type: 'SECTION', x: 1350, y: 130, length: 100, status: 'IDLE', line: '下行正线' },
  { id: 'X15', name: 'X15', type: 'SECTION', x: 1450, y: 130, length: 100, status: 'IDLE', line: '下行正线' },

  // ========== 3道（到发线） ==========
  { id: 'D3-1', name: '3G-1', type: 'SECTION', x: 300, y: 180, length: 80, status: 'IDLE', line: '3道' },
  { id: 'D3-2', name: '3G-2', type: 'SECTION', x: 380, y: 180, length: 80, status: 'IDLE', line: '3道' },
  { id: 'D3-3', name: '3G-3', type: 'SECTION', x: 460, y: 180, length: 80, status: 'IDLE', line: '3道' },
  { id: 'D3-4', name: '3G-4', type: 'SECTION', x: 540, y: 180, length: 80, status: 'IDLE', line: '3道' },
  { id: 'D3-5', name: '3G-5', type: 'SECTION', x: 620, y: 180, length: 80, status: 'IDLE', line: '3道' },
  { id: 'D3-6', name: '3G-6', type: 'SECTION', x: 700, y: 180, length: 80, status: 'IDLE', line: '3道' },
  { id: 'D3-7', name: '3G-7', type: 'SECTION', x: 780, y: 180, length: 80, status: 'IDLE', line: '3道' },
  { id: 'D3-8', name: '3G-8', type: 'SECTION', x: 860, y: 180, length: 80, status: 'IDLE', line: '3道' },
  { id: 'D3-9', name: '3G-9', type: 'SECTION', x: 940, y: 180, length: 80, status: 'IDLE', line: '3道' },
  { id: 'D3-10', name: '3G-10', type: 'SECTION', x: 1020, y: 180, length: 80, status: 'IDLE', line: '3道' },

  // ========== 4道（到发线） ==========
  { id: 'D4-1', name: '4G-1', type: 'SECTION', x: 300, y: 230, length: 80, status: 'IDLE', line: '4道' },
  { id: 'D4-2', name: '4G-2', type: 'SECTION', x: 380, y: 230, length: 80, status: 'IDLE', line: '4道' },
  { id: 'D4-3', name: '4G-3', type: 'SECTION', x: 460, y: 230, length: 80, status: 'IDLE', line: '4道' },
  { id: 'D4-4', name: '4G-4', type: 'SECTION', x: 540, y: 230, length: 80, status: 'IDLE', line: '4道' },
  { id: 'D4-5', name: '4G-5', type: 'SECTION', x: 620, y: 230, length: 80, status: 'IDLE', line: '4道' },
  { id: 'D4-6', name: '4G-6', type: 'SECTION', x: 700, y: 230, length: 80, status: 'IDLE', line: '4道' },
  { id: 'D4-7', name: '4G-7', type: 'SECTION', x: 780, y: 230, length: 80, status: 'IDLE', line: '4道' },
  { id: 'D4-8', name: '4G-8', type: 'SECTION', x: 860, y: 230, length: 80, status: 'IDLE', line: '4道' },
  { id: 'D4-9', name: '4G-9', type: 'SECTION', x: 940, y: 230, length: 80, status: 'IDLE', line: '4道' },
  { id: 'D4-10', name: '4G-10', type: 'SECTION', x: 1020, y: 230, length: 80, status: 'IDLE', line: '4道' },

  // ========== 5道（到发线） ==========
  { id: 'D5-1', name: '5G-1', type: 'SECTION', x: 300, y: 280, length: 80, status: 'IDLE', line: '5道' },
  { id: 'D5-2', name: '5G-2', type: 'SECTION', x: 380, y: 280, length: 80, status: 'IDLE', line: '5道' },
  { id: 'D5-3', name: '5G-3', type: 'SECTION', x: 460, y: 280, length: 80, status: 'IDLE', line: '5道' },
  { id: 'D5-4', name: '5G-4', type: 'SECTION', x: 540, y: 280, length: 80, status: 'IDLE', line: '5道' },
  { id: 'D5-5', name: '5G-5', type: 'SECTION', x: 620, y: 280, length: 80, status: 'IDLE', line: '5道' },
  { id: 'D5-6', name: '5G-6', type: 'SECTION', x: 700, y: 280, length: 80, status: 'IDLE', line: '5道' },
  { id: 'D5-7', name: '5G-7', type: 'SECTION', x: 780, y: 280, length: 80, status: 'IDLE', line: '5道' },
  { id: 'D5-8', name: '5G-8', type: 'SECTION', x: 860, y: 280, length: 80, status: 'IDLE', line: '5道' },
  { id: 'D5-9', name: '5G-9', type: 'SECTION', x: 940, y: 280, length: 80, status: 'IDLE', line: '5道' },
  { id: 'D5-10', name: '5G-10', type: 'SECTION', x: 1020, y: 280, length: 80, status: 'IDLE', line: '5道' },

  // ========== 6道（到发线） ==========
  { id: 'D6-1', name: '6G-1', type: 'SECTION', x: 300, y: 330, length: 80, status: 'IDLE', line: '6道' },
  { id: 'D6-2', name: '6G-2', type: 'SECTION', x: 380, y: 330, length: 80, status: 'IDLE', line: '6道' },
  { id: 'D6-3', name: '6G-3', type: 'SECTION', x: 460, y: 330, length: 80, status: 'IDLE', line: '6道' },
  { id: 'D6-4', name: '6G-4', type: 'SECTION', x: 540, y: 330, length: 80, status: 'IDLE', line: '6道' },
  { id: 'D6-5', name: '6G-5', type: 'SECTION', x: 620, y: 330, length: 80, status: 'IDLE', line: '6道' },
  { id: 'D6-6', name: '6G-6', type: 'SECTION', x: 700, y: 330, length: 80, status: 'IDLE', line: '6道' },
  { id: 'D6-7', name: '6G-7', type: 'SECTION', x: 780, y: 330, length: 80, status: 'IDLE', line: '6道' },
  { id: 'D6-8', name: '6G-8', type: 'SECTION', x: 860, y: 330, length: 80, status: 'IDLE', line: '6道' },
  { id: 'D6-9', name: '6G-9', type: 'SECTION', x: 940, y: 330, length: 80, status: 'IDLE', line: '6道' },
  { id: 'D6-10', name: '6G-10', type: 'SECTION', x: 1020, y: 330, length: 80, status: 'IDLE', line: '6道' },

  // ========== 牵出线 ==========
  { id: 'Q1', name: '牵1', type: 'SECTION', x: 1050, y: 380, length: 100, status: 'IDLE', line: '牵出线' },
  { id: 'Q2', name: '牵2', type: 'SECTION', x: 1150, y: 380, length: 100, status: 'IDLE', line: '牵出线' },
  { id: 'Q3', name: '牵3', type: 'SECTION', x: 1250, y: 380, length: 100, status: 'IDLE', line: '牵出线' },
  { id: 'Q4', name: '牵4', type: 'SECTION', x: 1350, y: 380, length: 100, status: 'IDLE', line: '牵出线' },
  { id: 'Q5', name: '牵5', type: 'SECTION', x: 1450, y: 380, length: 100, status: 'IDLE', line: '牵出线' },

  // ========== 机走线 ==========
  { id: 'J1', name: '机1', type: 'SECTION', x: 200, y: 380, length: 80, status: 'IDLE', line: '机走线' },
  { id: 'J2', name: '机2', type: 'SECTION', x: 280, y: 380, length: 80, status: 'IDLE', line: '机走线' },
  { id: 'J3', name: '机3', type: 'SECTION', x: 360, y: 380, length: 80, status: 'IDLE', line: '机走线' },
  { id: 'J4', name: '机4', type: 'SECTION', x: 440, y: 380, length: 80, status: 'IDLE', line: '机走线' },
  { id: 'J5', name: '机5', type: 'SECTION', x: 520, y: 380, length: 80, status: 'IDLE', line: '机走线' },

  // ========== 连接渡线段 ==========
  { id: 'LD1', name: '渡1', type: 'SECTION', x: 200, y: 105, length: 25, angle: 45, status: 'IDLE', line: '渡线' },
  { id: 'LD2', name: '渡2', type: 'SECTION', x: 200, y: 155, length: 25, angle: -45, status: 'IDLE', line: '渡线' },
  { id: 'LD3', name: '渡3', type: 'SECTION', x: 1200, y: 105, length: 25, angle: 45, status: 'IDLE', line: '渡线' },
  { id: 'LD4', name: '渡4', type: 'SECTION', x: 1200, y: 155, length: 25, angle: -45, status: 'IDLE', line: '渡线' },
  { id: 'LD5', name: '渡5', type: 'SECTION', x: 280, y: 205, length: 25, angle: 45, status: 'IDLE', line: '渡线' },
  { id: 'LD6', name: '渡6', type: 'SECTION', x: 280, y: 255, length: 25, angle: -45, status: 'IDLE', line: '渡线' },
  { id: 'LD7', name: '渡7', type: 'SECTION', x: 1050, y: 205, length: 25, angle: 45, status: 'IDLE', line: '渡线' },
  { id: 'LD8', name: '渡8', type: 'SECTION', x: 1050, y: 255, length: 25, angle: -45, status: 'IDLE', line: '渡线' },
  { id: 'LD9', name: '渡9', type: 'SECTION', x: 280, y: 305, length: 25, angle: 45, status: 'IDLE', line: '渡线' },
  { id: 'LD10', name: '渡10', type: 'SECTION', x: 280, y: 355, length: 25, angle: -45, status: 'IDLE', line: '渡线' }
]

// 道岔数据 - 共20组道岔
export const switches = [
  { id: 'SW1', name: '1号', x: 200, y: 80, position: 'STRAIGHT', status: 'NORMAL', connects: ['S2', 'S3', 'LD1'] },
  { id: 'SW2', name: '2号', x: 200, y: 130, position: 'STRAIGHT', status: 'NORMAL', connects: ['X2', 'X3', 'LD2'] },
  { id: 'SW3', name: '3号', x: 1200, y: 80, position: 'STRAIGHT', status: 'NORMAL', connects: ['S12', 'S13', 'LD3'] },
  { id: 'SW4', name: '4号', x: 1200, y: 130, position: 'STRAIGHT', status: 'NORMAL', connects: ['X12', 'X13', 'LD4'] },
  { id: 'SW5', name: '5号', x: 280, y: 180, position: 'STRAIGHT', status: 'NORMAL', connects: ['D3-1', 'D3-2', 'LD5'] },
  { id: 'SW6', name: '6号', x: 280, y: 230, position: 'STRAIGHT', status: 'NORMAL', connects: ['D4-1', 'D4-2', 'LD6'] },
  { id: 'SW7', name: '7号', x: 1050, y: 180, position: 'STRAIGHT', status: 'NORMAL', connects: ['D3-9', 'D3-10', 'LD7'] },
  { id: 'SW8', name: '8号', x: 1050, y: 230, position: 'STRAIGHT', status: 'NORMAL', connects: ['D4-9', 'D4-10', 'LD8'] },
  { id: 'SW9', name: '9号', x: 280, y: 280, position: 'STRAIGHT', status: 'NORMAL', connects: ['D5-1', 'D5-2', 'LD9'] },
  { id: 'SW10', name: '10号', x: 280, y: 330, position: 'STRAIGHT', status: 'NORMAL', connects: ['D6-1', 'D6-2', 'LD10'] },
  { id: 'SW11', name: '11号', x: 1050, y: 280, position: 'STRAIGHT', status: 'NORMAL', connects: ['D5-9', 'D5-10', 'Q1'] },
  { id: 'SW12', name: '12号', x: 1050, y: 330, position: 'STRAIGHT', status: 'NORMAL', connects: ['D6-9', 'D6-10', 'Q1'] },
  { id: 'SW13', name: '13号', x: 600, y: 380, position: 'STRAIGHT', status: 'NORMAL', connects: ['J3', 'J4', 'Q1'] },
  { id: 'SW14', name: '14号', x: 200, y: 380, position: 'STRAIGHT', status: 'NORMAL', connects: ['J1', 'J2', 'LD10'] },
  { id: 'SW15', name: '15号', x: 400, y: 80, position: 'STRAIGHT', status: 'NORMAL', connects: ['S4', 'S5', 'X4'] },
  { id: 'SW16', name: '16号', x: 800, y: 80, position: 'STRAIGHT', status: 'NORMAL', connects: ['S8', 'S9', 'X8'] },
  { id: 'SW17', name: '17号', x: 400, y: 130, position: 'STRAIGHT', status: 'NORMAL', connects: ['X4', 'X5', 'S4'] },
  { id: 'SW18', name: '18号', x: 800, y: 130, position: 'STRAIGHT', status: 'NORMAL', connects: ['X8', 'X9', 'S8'] },
  { id: 'SW19', name: '19号', x: 600, y: 180, position: 'STRAIGHT', status: 'NORMAL', connects: ['D3-4', 'D3-5', 'D4-4'] },
  { id: 'SW20', name: '20号', x: 600, y: 280, position: 'STRAIGHT', status: 'NORMAL', connects: ['D5-4', 'D5-5', 'D6-4'] }
]

// 信号机数据 - 共24架信号机
export const signals = [
  // 进站信号机
  { id: 'SN', name: 'S', type: 'ENTRY', x: 30, y: 80, status: 'RED', direction: 'RIGHT', line: '上行' },
  { id: 'XN', name: 'X', type: 'ENTRY', x: 1570, y: 130, status: 'RED', direction: 'LEFT', line: '下行' },
  
  // 出站信号机
  { id: 'S1', name: 'S1', type: 'EXIT', x: 1280, y: 65, status: 'RED', direction: 'RIGHT', line: 'I道' },
  { id: 'X1', name: 'X1', type: 'EXIT', x: 270, y: 145, status: 'RED', direction: 'LEFT', line: 'I道' },
  { id: 'S2', name: 'S2', type: 'EXIT', x: 1280, y: 115, status: 'RED', direction: 'RIGHT', line: 'II道' },
  { id: 'X2', name: 'X2', type: 'EXIT', x: 270, y: 95, status: 'RED', direction: 'LEFT', line: 'II道' },
  { id: 'S3', name: 'S3', type: 'EXIT', x: 1100, y: 165, status: 'RED', direction: 'RIGHT', line: '3道' },
  { id: 'X3', name: 'X3', type: 'EXIT', x: 320, y: 195, status: 'RED', direction: 'LEFT', line: '3道' },
  { id: 'S4', name: 'S4', type: 'EXIT', x: 1100, y: 215, status: 'RED', direction: 'RIGHT', line: '4道' },
  { id: 'X4', name: 'X4', type: 'EXIT', x: 320, y: 245, status: 'RED', direction: 'LEFT', line: '4道' },
  { id: 'S5', name: 'S5', type: 'EXIT', x: 1100, y: 265, status: 'RED', direction: 'RIGHT', line: '5道' },
  { id: 'X5', name: 'X5', type: 'EXIT', x: 320, y: 295, status: 'RED', direction: 'LEFT', line: '5道' },
  { id: 'S6', name: 'S6', type: 'EXIT', x: 1100, y: 315, status: 'RED', direction: 'RIGHT', line: '6道' },
  { id: 'X6', name: 'X6', type: 'EXIT', x: 320, y: 345, status: 'RED', direction: 'LEFT', line: '6道' },

  // 调车信号机
  { id: 'D1', name: 'D1', type: 'SHUNT', x: 220, y: 60, status: 'OFF', direction: 'RIGHT', line: '上行' },
  { id: 'D2', name: 'D2', type: 'SHUNT', x: 1180, y: 60, status: 'OFF', direction: 'LEFT', line: '上行' },
  { id: 'D3', name: 'D3', type: 'SHUNT', x: 220, y: 150, status: 'OFF', direction: 'RIGHT', line: '下行' },
  { id: 'D4', name: 'D4', type: 'SHUNT', x: 1180, y: 150, status: 'OFF', direction: 'LEFT', line: '下行' },
  { id: 'D5', name: 'D5', type: 'SHUNT', x: 300, y: 200, status: 'OFF', direction: 'RIGHT', line: '3道' },
  { id: 'D6', name: 'D6', type: 'SHUNT', x: 1030, y: 200, status: 'OFF', direction: 'LEFT', line: '3道' },
  { id: 'D7', name: 'D7', type: 'SHUNT', x: 300, y: 300, status: 'OFF', direction: 'RIGHT', line: '5道' },
  { id: 'D8', name: 'D8', type: 'SHUNT', x: 1030, y: 300, status: 'OFF', direction: 'LEFT', line: '5道' },
  { id: 'D9', name: 'D9', type: 'SHUNT', x: 180, y: 400, status: 'OFF', direction: 'RIGHT', line: '机走线' },
  { id: 'D10', name: 'D10', type: 'SHUNT', x: 1070, y: 400, status: 'OFF', direction: 'LEFT', line: '牵出线' }
]

// 进路数据 - 预设常用进路
export const routes = [
  // 上行正线接车进路
  {
    id: 'R-S-1',
    name: 'S-I道接车',
    startSignal: 'SN',
    endSignal: 'S1',
    sections: ['S1', 'S2', 'S3', 'S4', 'S5', 'S6', 'S7', 'S8', 'S9', 'S10', 'S11', 'S12'],
    switches: [{ id: 'SW1', position: 'STRAIGHT' }, { id: 'SW3', position: 'STRAIGHT' }],
    type: 'RECEPTION'
  },
  {
    id: 'R-S-2',
    name: 'S-II道接车',
    startSignal: 'SN',
    endSignal: 'S2',
    sections: ['S1', 'S2', 'LD1', 'X2', 'X3', 'X4', 'X5', 'X6', 'X7', 'X8', 'X9', 'X10', 'X11', 'X12'],
    switches: [{ id: 'SW1', position: 'CURVED' }, { id: 'SW2', position: 'CURVED' }, { id: 'SW4', position: 'STRAIGHT' }],
    type: 'RECEPTION'
  },
  // 上行发车进路
  {
    id: 'R-X-1',
    name: 'I道-X发车',
    startSignal: 'X1',
    endSignal: 'XN',
    sections: ['S13', 'S14', 'S15'],
    switches: [{ id: 'SW3', position: 'STRAIGHT' }],
    type: 'DEPARTURE'
  },
  {
    id: 'R-X-2',
    name: 'II道-X发车',
    startSignal: 'X2',
    endSignal: 'XN',
    sections: ['X13', 'X14', 'X15'],
    switches: [{ id: 'SW4', position: 'STRAIGHT' }],
    type: 'DEPARTURE'
  },
  // 3道接发车进路
  {
    id: 'R-S-3',
    name: 'S-3道接车',
    startSignal: 'SN',
    endSignal: 'S3',
    sections: ['S1', 'S2', 'LD1', 'X2', 'LD5', 'D3-1', 'D3-2', 'D3-3', 'D3-4', 'D3-5', 'D3-6', 'D3-7', 'D3-8', 'D3-9'],
    switches: [{ id: 'SW1', position: 'CURVED' }, { id: 'SW2', position: 'CURVED' }, { id: 'SW5', position: 'CURVED' }],
    type: 'RECEPTION'
  },
  {
    id: 'R-X-3',
    name: '3道-X发车',
    startSignal: 'X3',
    endSignal: 'XN',
    sections: ['D3-10', 'LD7', 'X10', 'X11', 'X12', 'X13', 'X14', 'X15'],
    switches: [{ id: 'SW7', position: 'CURVED' }, { id: 'SW8', position: 'CURVED' }, { id: 'SW4', position: 'STRAIGHT' }],
    type: 'DEPARTURE'
  },
  // 4道接发车进路
  {
    id: 'R-S-4',
    name: 'S-4道接车',
    startSignal: 'SN',
    endSignal: 'S4',
    sections: ['S1', 'S2', 'LD1', 'X2', 'LD5', 'LD6', 'D4-1', 'D4-2', 'D4-3', 'D4-4', 'D4-5', 'D4-6', 'D4-7', 'D4-8', 'D4-9'],
    switches: [{ id: 'SW1', position: 'CURVED' }, { id: 'SW2', position: 'CURVED' }, { id: 'SW5', position: 'CURVED' }, { id: 'SW6', position: 'CURVED' }],
    type: 'RECEPTION'
  },
  {
    id: 'R-X-4',
    name: '4道-X发车',
    startSignal: 'X4',
    endSignal: 'XN',
    sections: ['D4-10', 'LD8', 'LD7', 'X10', 'X11', 'X12', 'X13', 'X14', 'X15'],
    switches: [{ id: 'SW8', position: 'CURVED' }, { id: 'SW7', position: 'CURVED' }, { id: 'SW4', position: 'STRAIGHT' }],
    type: 'DEPARTURE'
  },
  // 5道接发车进路
  {
    id: 'R-S-5',
    name: 'S-5道接车',
    startSignal: 'SN',
    endSignal: 'S5',
    sections: ['S1', 'S2', 'LD1', 'X2', 'LD5', 'LD6', 'LD9', 'D5-1', 'D5-2', 'D5-3', 'D5-4', 'D5-5', 'D5-6', 'D5-7', 'D5-8', 'D5-9'],
    switches: [{ id: 'SW1', position: 'CURVED' }, { id: 'SW2', position: 'CURVED' }, { id: 'SW5', position: 'CURVED' }, { id: 'SW6', position: 'CURVED' }, { id: 'SW9', position: 'CURVED' }],
    type: 'RECEPTION'
  },
  {
    id: 'R-X-5',
    name: '5道-X发车',
    startSignal: 'X5',
    endSignal: 'XN',
    sections: ['D5-10', 'Q1', 'LD8', 'LD7', 'X10', 'X11', 'X12', 'X13', 'X14', 'X15'],
    switches: [{ id: 'SW11', position: 'CURVED' }, { id: 'SW8', position: 'CURVED' }, { id: 'SW7', position: 'CURVED' }, { id: 'SW4', position: 'STRAIGHT' }],
    type: 'DEPARTURE'
  },
  // 6道接发车进路
  {
    id: 'R-S-6',
    name: 'S-6道接车',
    startSignal: 'SN',
    endSignal: 'S6',
    sections: ['S1', 'S2', 'LD1', 'X2', 'LD5', 'LD6', 'LD9', 'LD10', 'D6-1', 'D6-2', 'D6-3', 'D6-4', 'D6-5', 'D6-6', 'D6-7', 'D6-8', 'D6-9'],
    switches: [{ id: 'SW1', position: 'CURVED' }, { id: 'SW2', position: 'CURVED' }, { id: 'SW5', position: 'CURVED' }, { id: 'SW6', position: 'CURVED' }, { id: 'SW9', position: 'CURVED' }, { id: 'SW10', position: 'CURVED' }],
    type: 'RECEPTION'
  },
  {
    id: 'R-X-6',
    name: '6道-X发车',
    startSignal: 'X6',
    endSignal: 'XN',
    sections: ['D6-10', 'Q1', 'LD8', 'LD7', 'X10', 'X11', 'X12', 'X13', 'X14', 'X15'],
    switches: [{ id: 'SW12', position: 'CURVED' }, { id: 'SW8', position: 'CURVED' }, { id: 'SW7', position: 'CURVED' }, { id: 'SW4', position: 'STRAIGHT' }],
    type: 'DEPARTURE'
  }
]

// 车站名称
export const stationInfo = {
  name: '南站',
  code: 'NAN',
  type: '中间站',
  tracks: 6,
  platforms: 4
}

// 导出完整站场布局
export const stationLayout = {
  stationInfo,
  trackSections,
  switches,
  signals,
  routes
}

export default stationLayout
