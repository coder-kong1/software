# 智能充电桩调度计费系统

技术栈：Spring Boot + Vue + SQLite。

目录：

- `backend`：Spring Boot 后端，分层为 Controller / Service / Repository / Domain。
- `custom`：用户客户端 Vue 页面。
- `admin`：管理员端 Vue 页面。

覆盖的作业二核心内容：

- 软件架构：前后端分离，客户端/管理员端通过 REST API 访问 Spring Boot 服务，服务层负责调度、计费、故障恢复，Repository 层负责 SQLite 持久化。
- 动态结构：注册、提交充电请求、修改电量、修改模式、查询车辆状态、开始充电、查询充电状态、结束充电、查看账单/详单、启动/关闭充电桩、设置参数、故障重调度/恢复。
- 静态结构：`ChargingRequestService`、`SchedulingService`、`BillingService`、`PileService`、`AccountService`、`ChargingPileRepository`、`ChargingRequestRepository`、`BillRepository`、`UserAccountRepository` 等类可直接用于绘制类图。
- 前端界面：`custom` 为客户端，`admin` 为管理员端，包含作业模板第二部分所需的界面设计原型。

运行：

```powershell
cd backend
mvn spring-boot:run
```

浏览器打开：

- 客户端：`custom/index.html`
- 管理员端：`admin/index.html`

默认 API 地址为 `http://localhost:8080/api`。

前端分端口启动：

```powershell
cd custom
npm install
npm run dev
```

客户端地址：`http://localhost:5173`

```powershell
cd admin
npm install
npm run dev
```

管理员端地址：`http://localhost:5174`


## 完整需求补充

根据 `1205第一次作业.pdf`，项目已补充 UC01-UC07 对应功能：

- UC01 提交充电请求：创建订单、进入快/慢充等待队列、返回队列和状态。
- UC02 修改充电请求：支持修改充电模式和目标电量，并重新排队/调度。
- UC03 取消充电请求：取消未完成请求，释放队列或停车位资源。
- UC04 查看充电信息：查看排队进度、充电状态、账单明细。
- UC05 支付充电费用：生成支付记录并展示支付状态。
- UC06 管理充电桩状态：管理员查看、启动、关闭、故障、恢复充电桩。
- UC07 处理异常事件：登记和处理过号、霸占充电桩不充电、充完不走、恶意插队等异常，并记录罚款。
