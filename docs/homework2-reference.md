# 第二次作业填写参考

## 1. 软件架构

本项目采用前后端分离的三层架构：

- 表示层：`custom` 用户客户端、`admin` 管理员端，均使用 Vue 组织页面状态和用户操作。
- 业务层：`backend/src/main/java/edu/bupt/charging/service`，负责账号、充电请求、调度、计费、充电桩运行控制。
- 持久层：`backend/src/main/java/edu/bupt/charging/repository`，使用 Spring JDBC 访问 SQLite。
- 数据库：`backend/src/main/resources/schema.sql`，定义用户、充电桩、充电请求、账单、计费规则。

## 2. 界面设计

用户客户端：`custom/index.html`

- 注册车辆账号：对应 `createNewAccount(car_Id, userName, car_Capacity)`。
- 提交充电请求：对应 `E_chargingRequest(car_Id, Request_Amount, Request_Mode)`。
- 修改电量和模式：对应 `Modify_Amount`、`Modify_Mode`。
- 查询车辆状态：对应 `Query_Car_State`。
- 结束充电与账单查询：对应 `End_Charging`、`Request_Bill`、`Request_DetailedList`。

管理员端：`admin/index.html`

- 充电桩监控：展示每个桩的工作状态、当前充电车辆、队列车辆。
- 运行控制：对应 `powerOn`、`powerOff`、故障上报、故障恢复。
- 参数设置：对应 `setParameters`，维护峰/平/谷电价和服务费。
- 报表展示：对应充电详单、总充电次数、总充电时长、总充电量。

## 3. 动态结构设计可选用例

### UC_01 注册

- 接收对象：`AccountController`
- 创建对象：`AccountService`
- 持久化对象：`UserAccountRepository`
- 实体对象：`UserAccount`
- 接口：`POST /api/accounts`

### UC_02 提交充电请求

- 接收对象：`ChargingController`
- 业务对象：`ChargingRequestService`
- 调度对象：`SchedulingService`
- 持久化对象：`ChargingRequestRepository`、`ChargingPileRepository`
- 实体对象：`ChargingRequest`、`ChargingPile`
- 接口：`POST /api/charging/requests`

### UC_03 修改充电请求

- 接收对象：`ChargingController`
- 业务对象：`ChargingRequestService`
- 调度对象：`SchedulingService`
- 接口：
  - `PUT /api/charging/requests/{carId}/amount`
  - `PUT /api/charging/requests/{carId}/mode`

### UC_04 查询队列/充电状态

- 接收对象：`ChargingController`、`AdminController`
- 查询对象：`ChargingRequestRepository`
- 快照对象：`StationSnapshot`、`PileStateView`
- 接口：
  - `GET /api/charging/requests/{carId}/state`
  - `GET /api/admin/snapshot`

### UC_05 结束充电并生成账单

- 接收对象：`ChargingController`
- 业务对象：`ChargingRequestService`
- 计费对象：`BillingService`
- 持久化对象：`BillRepository`
- 实体对象：`Bill`、`PriceRule`
- 接口：`POST /api/charging/requests/{carId}/end`

### UC_06 充电桩故障重调度与恢复

- 接收对象：`AdminController`
- 运行对象：`PileService`
- 调度对象：`SchedulingService`
- 持久化对象：`ChargingPileRepository`、`ChargingRequestRepository`
- 接口：
  - `POST /api/admin/piles/{pileId}/fault`
  - `POST /api/admin/piles/{pileId}/recover`

## 4. 静态结构设计类图素材

边界类：

- `AccountController`
- `ChargingController`
- `AdminController`

控制类：

- `AccountService`
- `ChargingRequestService`
- `SchedulingService`
- `BillingService`
- `PileService`

实体类：

- `UserAccount`
- `ChargingRequest`
- `ChargingPile`
- `Bill`
- `PriceRule`

持久化类：

- `UserAccountRepository`
- `ChargingRequestRepository`
- `ChargingPileRepository`
- `BillRepository`
- `PriceRuleRepository`

关系建议：

- Controller 依赖 Service。
- Service 依赖 Repository。
- Repository 创建/查询 Domain 实体。
- `ChargingRequestService` 依赖 `SchedulingService` 和 `BillingService`。
- `PileService` 在故障/恢复时依赖 `SchedulingService` 完成再调度。

## 5. 操作契约示例

`E_chargingRequest(car_Id, Request_Amount, Request_Mode)`

- 前置条件：车辆账号存在；该车辆无未完成请求；请求电量大于 0。
- 后置条件：创建 `ChargingRequest`，状态初始为 `WAITING_AREA`；系统调用 `SchedulingService.schedule()` 尝试进入对应快/慢充队列。
- 返回：车辆位置、车辆状态、队列编号、请求时间。

`End_Charging(car_id, ChargingPileNum)`

- 前置条件：车辆存在处于 `CHARGING` 或已分配充电桩的请求。
- 后置条件：请求状态变为 `FINISHED`；生成 `Bill`；更新充电桩累计充电次数、时长、容量；触发下一轮调度。
- 返回：账单对象。

`fault(pile_Id)`

- 前置条件：充电桩存在。
- 后置条件：充电桩状态变为 `FAULT`；该桩上的充电中/排队车辆释放回等待区；`SchedulingService` 按模式重新分配到其他可运行桩。
- 返回：0/1。
