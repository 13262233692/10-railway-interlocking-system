# 铁路联锁系统 API 接口文档

## 基础信息
- 服务地址: `http://localhost:8080/api`
- WebSocket: `ws://localhost:8080/api/ws/interlocking`

## REST API 接口

### 1. 健康检查
```
GET /interlocking/health
```
**响应示例:**
```json
{
    "code": 200,
    "message": "铁路联锁系统运行正常",
    "data": "OK",
    "timestamp": "2024-01-01T12:00:00"
}
```

---

### 2. 获取系统完整状态
```
GET /interlocking/status
```
**响应:** 返回所有轨道区段、道岔、信号机、进路的完整状态

---

### 3. 办理进路
```
POST /interlocking/route/establish
Content-Type: application/json

{
    "routeId": "ROUTE_R001",
    "operator": "张三",
    "guidanceRoute": false,
    "remark": "G1234次列车接车"
}
```
**请求参数:**
- `routeId` (必填): 进路ID
- `operator`: 办理人
- `guidanceRoute`: 是否为引导进路
- `remark`: 备注

---

### 4. 取消进路
```
POST /interlocking/route/cancel
Content-Type: application/json

{
    "routeId": "ROUTE_R001",
    "operator": "张三",
    "reason": "列车晚点",
    "forceCancel": false
}
```
**请求参数:**
- `routeId` (必填): 进路ID
- `operator`: 办理人
- `reason`: 取消原因
- `forceCancel`: 是否强制取消（解锁接近锁闭）

---

### 5. 操作道岔
```
POST /interlocking/switch/operate
Content-Type: application/json

{
    "switchId": "SW1",
    "targetPosition": "REVERSE",
    "operator": "张三",
    "forceOperate": false
}
```
**请求参数:**
- `switchId` (必填): 道岔ID
- `targetPosition` (必填): 目标位置 `NORMAL`(定位) / `REVERSE`(反位)
- `operator`: 办理人
- `forceOperate`: 是否强制操作

---

### 6. 控制信号机
```
POST /interlocking/signal/control
Content-Type: application/json

{
    "signalId": "S",
    "targetAspect": "GREEN",
    "operator": "张三",
    "forceControl": false
}
```
**请求参数:**
- `signalId` (必填): 信号机ID
- `targetAspect` (必填): 目标显示
  - `RED` - 红灯（禁止）
  - `YELLOW` - 黄灯（注意）
  - `GREEN` - 绿灯（允许通过）
  - `DOUBLE_YELLOW` - 双黄灯（引导）
  - `OFF` - 灭灯
- `operator`: 办理人
- `forceControl`: 是否强制控制

---

### 7. 占用轨道区段（模拟列车）
```
POST /interlocking/section/occupy
Content-Type: application/json

{
    "sectionId": "01G01",
    "trainId": "G1234",
    "operator": "系统"
}
```
**请求参数:**
- `sectionId` (必填): 轨道区段ID
- `trainId` (必填): 列车ID
- `operator`: 办理人

---

### 8. 释放轨道区段（模拟列车出清）
```
POST /interlocking/section/release/{sectionId}?operator=系统
```
**路径参数:**
- `sectionId` (必填): 轨道区段ID

**查询参数:**
- `operator`: 办理人

---

### 9. 查询所有轨道区段
```
GET /interlocking/sections
```

---

### 10. 查询单个轨道区段
```
GET /interlocking/sections/{id}
```

---

### 11. 查询所有道岔
```
GET /interlocking/switches
```

---

### 12. 查询单个道岔
```
GET /interlocking/switches/{id}
```

---

### 13. 查询所有信号机
```
GET /interlocking/signals
```

---

### 14. 查询单个信号机
```
GET /interlocking/signals/{id}
```

---

### 15. 查询所有进路
```
GET /interlocking/routes
```

---

### 16. 查询单个进路
```
GET /interlocking/routes/{id}
```

---

## WebSocket 接口

### 连接地址
```
ws://localhost:8080/api/ws/interlocking
```

### 支持的消息类型

#### 1. 心跳检测
**发送:**
```
PING
```
**响应:**
```json
{
    "type": "PONG",
    "title": "心跳响应",
    "content": "PONG",
    "level": "INFO",
    "timestamp": "2024-01-01T12:00:00"
}
```

#### 2. 获取状态
**发送:**
```
GET_STATUS
```
**响应:** 返回完整的系统状态

#### 3. 自动推送消息
系统会在以下情况自动推送消息:
- `STATUS_UPDATE`: 系统状态更新
- `ROUTE_UPDATE`: 进路状态变更
- `SECTION_UPDATE`: 轨道区段状态变更
- `SWITCH_UPDATE`: 道岔状态变更
- `SIGNAL_UPDATE`: 信号机状态变更
- `ALERT`: 告警信息

---

## 数据说明

### 轨道区段状态
- `IDLE` - 空闲
- `OCCUPIED` - 占用
- `LOCKED` - 锁闭
- `FAULT` - 故障

### 道岔位置
- `NORMAL` - 定位
- `REVERSE` - 反位
- `FOUR_WAY` - 四开（危险）
- `FAULT` - 故障
- `MOVING` - 转换中

### 信号机显示
- `RED` - 红灯（禁止）
- `YELLOW` - 黄灯（注意）
- `GREEN` - 绿灯（允许通过）
- `DOUBLE_YELLOW` - 双黄灯（引导）
- `OFF` - 灭灯

### 进路状态
- `NOT_ESTABLISHED` - 未建立
- `LOCKING` - 锁闭中
- `LOCKED` - 已锁闭
- `CLEARED` - 开放
- `OCCUPIED` - 占用中
- `UNLOCKING` - 解锁中
- `CANCELLING` - 取消中
- `FAULT` - 故障

### 进路类型
- `RECEPTION` - 接车进路
- `DEPARTURE` - 发车进路
- `THROUGH` - 通过进路
- `SHUNTING` - 调车进路
- `GUIDANCE` - 引导进路

---

## 站场数据说明

系统初始化包含以下数据:
- **轨道区段**: 108个（正线、到发线、调车线、渡线、联络线等）
- **道岔**: 32个（单开道岔、渡线道岔）
- **信号机**: 42个（进站、出站、进路、调车、预告、通过信号机）
- **进路**: 56条（接车进路24条、发车进路24条、通过进路8条）

### 主要进路ID说明
- `ROUTE_R001` ~ `ROUTE_R024`: 接车进路
- `ROUTE_D025` ~ `ROUTE_D048`: 发车进路
- `ROUTE_T049` ~ `ROUTE_T056`: 通过进路

### 测试示例

1. **办理上行接1道进路:**
```bash
curl -X POST http://localhost:8080/api/interlocking/route/establish \
  -H "Content-Type: application/json" \
  -d '{"routeId":"ROUTE_R001","operator":"测试员"}'
```

2. **模拟列车G1234占用区段:**
```bash
curl -X POST http://localhost:8080/api/interlocking/section/occupy \
  -H "Content-Type: application/json" \
  -d '{"sectionId":"JG1G","trainId":"G1234"}'
```

3. **取消进路:**
```bash
curl -X POST http://localhost:8080/api/interlocking/route/cancel \
  -H "Content-Type: application/json" \
  -d '{"routeId":"ROUTE_R001","operator":"测试员"}'
```
