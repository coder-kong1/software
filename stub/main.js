import { createApp } from 'vue/dist/vue.esm-bundler.js';

const API = '/api';

const EXCEL_EVENTS = [
  { time: '06:00:00', raw: '(A,V1,T,40)', type: 'A', target: 'V1', mode: 'T', value: 40 },
  { time: '06:05:00', raw: '(A,V2,T,30)', type: 'A', target: 'V2', mode: 'T', value: 30 },
  { time: '06:10:00', raw: '(A,V3,F,100)', type: 'A', target: 'V3', mode: 'F', value: 100 },
  { time: '06:15:00', raw: '(A,V4,F,120)', type: 'A', target: 'V4', mode: 'F', value: 120 },
  { time: '06:20:00', raw: '(A,V2,O,0)', type: 'A', target: 'V2', mode: 'O', value: 0 },
  { time: '06:25:00', raw: '(A,V5,T,20)', type: 'A', target: 'V5', mode: 'T', value: 20 },
  { time: '06:30:00', raw: '(A,V6,T,20)', type: 'A', target: 'V6', mode: 'T', value: 20 },
  { time: '06:35:00', raw: '(A,V7,F,110)', type: 'A', target: 'V7', mode: 'F', value: 110 },
  { time: '06:40:00', raw: '(A,V8,T,20)', type: 'A', target: 'V8', mode: 'T', value: 20 },
  { time: '06:45:00', raw: '(A,V9,F,105)', type: 'A', target: 'V9', mode: 'F', value: 105 },
  { time: '06:50:00', raw: '(A,V10,T,10)', type: 'A', target: 'V10', mode: 'T', value: 10 },
  { time: '06:55:00', raw: '(A,V11,F,110)', type: 'A', target: 'V11', mode: 'F', value: 110 },
  { time: '07:00:00', raw: '(A,V12,F,90)', type: 'A', target: 'V12', mode: 'F', value: 90 },
  { time: '07:05:00', raw: '(A,V13,F,110)', type: 'A', target: 'V13', mode: 'F', value: 110 },
  { time: '07:10:00', raw: '(A,V14,F,95)', type: 'A', target: 'V14', mode: 'F', value: 95 },
  { time: '07:15:00', raw: '(A,V15,T,10)', type: 'A', target: 'V15', mode: 'T', value: 10 },
  { time: '07:20:00', raw: '(A,V16,F,60)', type: 'A', target: 'V16', mode: 'F', value: 60 },
  { time: '07:25:00', raw: '(A,V17,T,10)', type: 'A', target: 'V17', mode: 'T', value: 10 },
  { time: '07:30:00', raw: '(A,V18,T,7.5)', type: 'A', target: 'V18', mode: 'T', value: 7.5 },
  { time: '07:35:00', raw: '(A,V19,F,75)', type: 'A', target: 'V19', mode: 'F', value: 75 },
  { time: '07:40:00', raw: '(A,V20,F,95)', type: 'A', target: 'V20', mode: 'F', value: 95 },
  { time: '07:45:00', raw: '(A,V21,F,95)', type: 'A', target: 'V21', mode: 'F', value: 95 },
  { time: '07:50:00', raw: '(A,V22,F,70)', type: 'A', target: 'V22', mode: 'F', value: 70 },
  { time: '07:55:00', raw: '(A,V23,F,80)', type: 'A', target: 'V23', mode: 'F', value: 80 },
  { time: '08:00:00', raw: '(A,V24,T,5)', type: 'A', target: 'V24', mode: 'T', value: 5 },
  { time: '08:20:00', raw: '(A,V25,T,15)', type: 'A', target: 'V25', mode: 'T', value: 15 },
  { time: '08:25:00', raw: '(B,T1,O,0)', type: 'B', target: 'T1', mode: 'O', value: 0 },
  { time: '08:30:00', raw: '(A,V26,T,20)', type: 'A', target: 'V26', mode: 'T', value: 20 },
  { time: '08:35:00', raw: '(A,V27,T,25)', type: 'A', target: 'V27', mode: 'T', value: 25 },
  { time: '08:50:00', raw: '(B,F1,O,0)', type: 'B', target: 'F1', mode: 'O', value: 0 },
  { time: '09:00:00', raw: '(A,V28,F,30)', type: 'A', target: 'V28', mode: 'F', value: 30 },
  { time: '09:10:00', raw: '(A,V1,O,0)', type: 'A', target: 'V1', mode: 'O', value: 0 },
  { time: '09:15:00', raw: '(B,T1,O,1)', type: 'B', target: 'T1', mode: 'O', value: 1 },
  { time: '09:20:00', raw: '(A,V27,O,0)', type: 'A', target: 'V27', mode: 'O', value: 0 },
  { time: '09:25:00', raw: '(C,V21,O,35)', type: 'C', target: 'V21', mode: 'O', value: 35 },
  { time: '09:30:00', raw: '(A,V19,O,0)', type: 'A', target: 'V19', mode: 'O', value: 0 },
  { time: '09:35:00', raw: '(A,V28,O,0)', type: 'A', target: 'V28', mode: 'O', value: 0 },
  { time: '09:40:00', raw: '(C,V23,O,40)', type: 'C', target: 'V23', mode: 'O', value: 40 },
  { time: '09:50:00', raw: '(A,V29,T,30)', type: 'A', target: 'V29', mode: 'T', value: 30 },
  { time: '09:55:00', raw: '(C,V14,O,30)', type: 'C', target: 'V14', mode: 'O', value: 30 },
  { time: '10:00:00', raw: '(A,V30,T,10)', type: 'A', target: 'V30', mode: 'T', value: 10 },
  { time: '10:50:00', raw: '(B,F1,O,1)', type: 'B', target: 'F1', mode: 'O', value: 1 }
];

createApp({
  data() {
    return {
      backendOk: false,
      running: false,
      prefix: 'V',
      delayMs: 350,
      excelDelayMs: 350,
      strategy: 'TIME_ORDER',
      queuePileId: 'F1',
      selectedQueueCars: [],
      snapshot: { piles: [], waitingArea: [], fastQueue: [], slowQueue: [] },
      report: {},
      recentBills: [],
      schedulingLogs: [],
      lastRefresh: '',
      logSeq: 0,
      logs: [],
      manual: {
        carId: 'T1',
        userName: 'test_user_1',
        carCapacity: 100,
        requestAmount: 40,
        requestMode: 'SLOW',
        controlCarId: 'T1',
        pileId: '',
        newAmount: 30,
        newMode: 'FAST',
        adminPileId: 'F1',
        abnormalCarId: 'T1',
        abnormalType: 'QUEUE_JUMP',
        penaltyFee: 80
      }
    };
  },
  computed: {
    pileQueuedCars() {
      return this.snapshot.piles.flatMap(item =>
        this.pileCars(item).map(car => ({ ...car, pileId: item.pile.id }))
      );
    },
    activeQueueCarIds() {
      const ids = new Set();
      const add = car => {
        if (car && car.carId) ids.add(String(car.carId).toUpperCase());
      };
      (this.snapshot.waitingArea || []).forEach(add);
      (this.snapshot.fastQueue || []).forEach(add);
      (this.snapshot.slowQueue || []).forEach(add);
      (this.snapshot.piles || []).forEach(item => {
        add(item.chargingCar);
        (item.queue || []).forEach(add);
      });
      return Array.from(ids);
    }
  },
  async mounted() {
    await this.refreshAll();
    window.setInterval(() => this.refreshAll(true), 4000);
  },
  methods: {
    async call(url, options = {}) {
      const res = await fetch(API + url, {
        headers: { 'Content-Type': 'application/json' },
        ...options
      });
      const text = await res.text();
      let body;
      try {
        body = text ? JSON.parse(text) : { success: true, data: null };
      } catch (error) {
        const preview = text.slice(0, 160).replace(/\s+/g, ' ');
        throw new Error(`${url} returned non-json, HTTP ${res.status}: ${preview}`);
      }
      if (!res.ok || !body.success) {
        throw new Error(`${url} failed, HTTP ${res.status}: ${body.message || 'unknown error'}`);
      }
      return body.data;
    },
    delay(delayMs = this.delayMs) {
      const normalizedDelay = Math.max(0, Number(delayMs) || 0);
      return new Promise(resolve => window.setTimeout(resolve, normalizedDelay));
    },
    log(text, type = 'info', extra = {}) {
      this.logs.push({
        id: ++this.logSeq,
        time: new Date().toLocaleTimeString(),
        text,
        type,
        ...extra
      });
      this.logs = this.logs.slice(-160);
      this.$nextTick(() => {
        const box = document.querySelector('.log-box');
        if (box) box.scrollTop = box.scrollHeight;
      });
    },
    carNo(index) {
      return `${(this.prefix || 'T').toUpperCase()}${index}`;
    },
    async refreshAll(silent = false) {
      try {
        const [snapshot, report, bills, strategy] = await Promise.all([
          this.call('/admin/snapshot'),
          this.call('/admin/reports/summary'),
          this.call('/admin/reports/bills'),
          this.call('/admin/scheduling-strategy')
        ]);
        this.snapshot = snapshot;
        this.report = report || {};
        this.recentBills = (bills || []).slice(0, 8);
        try {
          this.schedulingLogs = await this.call('/admin/scheduling-logs');
        } catch (error) {
          this.schedulingLogs = [];
        }
        this.strategy = strategy || this.strategy;
        this.backendOk = true;
        this.lastRefresh = new Date().toLocaleTimeString();
        if (!silent) this.log('已刷新系统状态', 'success');
      } catch (error) {
        this.backendOk = false;
        if (!silent) this.log(`Backend connection failed: ${error.message}`, 'error');
      }
    },
    async runScenario(name, steps) {
      if (this.running) return;
      this.running = true;
      const displayName = this.scenarioName(name);
      this.log(`开始演示：${displayName}`, 'start');
      try {
        for (const step of steps) {
          await step();
          await this.refreshAll(true);
          await this.delay();
        }
        await this.refreshAll(true);
        this.logQueueSummary('最终结果');
        this.log(`演示完成：${displayName}`, 'success');
      } catch (error) {
        this.log(`演示中止：${error.message}`, 'error');
      } finally {
        this.running = false;
      }
    },
    scenarioName(name) {
      return {
        'excel acceptance input': 'Excel 验收输入',
        'clear current queues': '清空当前队列',
        'time order scheduling': '时间顺序调度',
        'priority scheduling': '优先级调度',
        'fault recovery': '故障恢复调度',
        'billing and payment': '计费支付',
        'abnormal event': '异常处理'
      }[name] || name;
    },
    logQueueSummary(label = '当前结果') {
      const chargingCount = (this.snapshot.piles || []).filter(item => item.chargingCar).length;
      const queueCount = this.pileQueuedCars.length;
      const waitingCount = (this.snapshot.waitingArea || []).length;
      const bills = this.report.billCount || 0;
      this.log(`${label}：等候区 ${waitingCount} 辆，桩内队列 ${queueCount} 辆，充电中 ${chargingCount} 辆，账单 ${bills} 条`, 'summary');
    },
    async ensureCar(carId, userName = null) {
      const id = carId.toUpperCase();
      const accountName = userName || `test_user_${id}`;
      try {
        await this.call(`/accounts/${id}`);
        await this.call(`/accounts/${id}`, {
          method: 'PUT',
          body: JSON.stringify({
            userName: accountName,
            password: '123456',
            carCapacity: 150
          })
        });
        this.log(`Reuse car ${id}, capacity set to 150 kWh`);
        return id;
      } catch (error) {
        await this.call('/accounts', {
          method: 'POST',
          body: JSON.stringify({
            carId: id,
            userName: accountName,
            password: '123456',
            carCapacity: 150
          })
        });
        this.log(`Create car ${id}`, 'success');
        return id;
      }
    },
    async ensureCars(count = 21) {
      for (let i = 1; i <= count; i += 1) {
        await this.ensureCar(this.carNo(i), `test_user_${i}`);
      }
    },
    async requestCharge(carId, mode, amount) {
      const id = carId.toUpperCase();
      await this.ensureCar(id);
      try {
        const state = await this.createChargeRequest(id, mode, amount);
        this.log(`系统动作：${id} 提交${this.modeText(mode)}申请，电量 ${amount} kWh，进入${this.stateText(state.state)}${state.pileId ? `，分配到 ${state.pileId}` : ''}`, 'success');
      } catch (error) {
        if (error.message.includes('409')) {
          this.log(`检测到 ${id} 有未完成申请，先释放旧请求后重新提交`, 'warn');
          await this.finishOrCancelCar(id);
          const state = await this.createChargeRequest(id, mode, amount);
          this.log(`系统动作：${id} 重新提交${this.modeText(mode)}申请，电量 ${amount} kWh，当前为${this.stateText(state.state)}`, 'success');
          return;
        }
        this.log(`${id} 申请失败：${error.message}`, 'warn');
      }
    },
    async createChargeRequest(id, mode, amount) {
      return this.call('/charging/requests', {
        method: 'POST',
        body: JSON.stringify({ carId: id, requestMode: mode, requestAmount: amount })
      });
    },
    async setStrategyValue(value) {
      this.strategy = await this.call('/admin/scheduling-strategy', {
        method: 'PUT',
        body: JSON.stringify({ strategy: value })
      });
      this.log(`Strategy set to ${this.strategyText(this.strategy)}`, 'success');
    },
    resetLog() {
      this.logs = [];
      this.logSeq = 0;
    },
    resetManualDefaults() {
      this.prefix = 'V';
      this.delayMs = 350;
      this.queuePileId = 'F1';
      this.strategy = 'TIME_ORDER';
      this.selectedQueueCars = [];
      this.manual.carId = 'T1';
      this.manual.userName = 'test_user_1';
      this.manual.carCapacity = 100;
      this.manual.requestAmount = 40;
      this.manual.requestMode = 'SLOW';
      this.manual.controlCarId = 'T1';
      this.manual.pileId = '';
      this.manual.newAmount = 30;
      this.manual.newMode = 'FAST';
      this.manual.adminPileId = 'F1';
      this.manual.abnormalCarId = 'T1';
      this.manual.abnormalType = 'QUEUE_JUMP';
      this.manual.penaltyFee = 80;
    },
    async initializeStub() {
      if (this.running) return;
      this.running = true;
      this.resetLog();
      this.resetManualDefaults();
      this.log('初始化测试桩：开始清理队列、恢复充电桩并重置演示参数', 'start');
      try {
        await this.clearActiveRequests();
        await this.recoverAllPiles();
        await this.setStrategyValue('TIME_ORDER');
        await this.refreshAll(true);
        await this.queryPileQueue('F1');
        this.logQueueSummary('初始化结果');
        this.log('初始化完成：可以开始运行 Excel 验收输入', 'success');
      } catch (error) {
        this.log(`初始化失败：${error.message}`, 'error');
      } finally {
        this.running = false;
      }
    },
    async clearCurrentQueues() {
      await this.runScenario('clear current queues', [
        () => this.clearActiveRequests(),
        () => this.recoverAllPiles()
      ]);
    },
    async clearActiveRequests() {
      await this.refreshAll(true);
      const ids = this.activeQueueCarIds;
      if (!ids.length) {
        this.log('清理结果：当前没有未完成的排队或充电车辆', 'success');
        return;
      }
      this.log(`Clearing ${ids.length} active queue cars`, 'start');
      for (const id of ids) {
        await this.finishOrCancelCar(id);
      }
      await this.refreshAll(true);
    },
    async recoverAllPiles() {
      await this.refreshAll(true);
      for (const pileId of ['F1', 'F2', 'S1', 'S2', 'S3']) {
        await this.ensurePileRunning(pileId);
      }
    },
    async ensurePileRunning(pileId) {
      const pile = (this.snapshot.piles || []).find(item => item.pile.id === pileId)?.pile;
      if (!pile) return;
      if (pile.status === 'RUNNING') return;
      if (pile.status === 'FAULT') {
        await this.safePileAction(pileId, 'recover', `${pileId} 恢复运行`);
        return;
      }
      if (pile.status === 'STOPPED') {
        await this.safePileAction(pileId, 'power-on', `${pileId} 开机`);
        await this.safePileAction(pileId, 'start', `${pileId} 开始运行`);
        return;
      }
      if (pile.status === 'POWER_ON') {
        await this.safePileAction(pileId, 'start', `${pileId} 开始运行`);
      }
    },
    async runAcceptanceExcelScenario() {
      if (this.running) return;
      this.running = true;
      const eventDelayMs = Math.max(0, Number(this.excelDelayMs) || 0);
      const displayName = `Excel 验收输入（${EXCEL_EVENTS.length} 条事件）`;
      this.log(`开始演示：${displayName}`, 'start');
      this.log(`Excel 事件输入间隔：${eventDelayMs} ms`, 'event');
      try {
        await this.prepareExcelScenario();
        await this.setStrategyValue('TIME_ORDER');
        await this.refreshAll(true);
        for (let index = 0; index < EXCEL_EVENTS.length; index += 1) {
          await this.executeExcelEvent(EXCEL_EVENTS[index], index + 1);
          await this.refreshAll(true);
          if (index < EXCEL_EVENTS.length - 1) await this.delay(eventDelayMs);
        }
        await this.refreshAll(true);
        this.logQueueSummary('最终结果');
        this.log(`演示完成：${displayName}`, 'success');
      } catch (error) {
        this.log(`演示中止：${error.message}`, 'error');
      } finally {
        this.running = false;
      }
    },
    async prepareExcelScenario() {
      await this.clearActiveRequests();
      this.log('准备阶段：创建/更新 V1-V30 测试车辆，并恢复所有充电桩', 'start');
      for (let i = 1; i <= 30; i += 1) {
        const id = `V${i}`;
        await this.ensureCar(id, `excel_user_${i}`);
        await this.finishOrCancelCar(id);
      }
      await this.recoverAllPiles();
    },
    async executeExcelEvent(event, index) {
      const excelNo = `${String(index).padStart(2, '0')}/${EXCEL_EVENTS.length}`;
      this.log(`输入事件 ${event.time}：${this.describeExcelEvent(event)} ${event.raw}`, 'event', { excelNo });
      if (event.type === 'A') {
        if (event.mode === 'F' || event.mode === 'T') {
          const chargeMode = event.mode === 'F' ? 'FAST' : 'SLOW';
          await this.requestCharge(event.target, chargeMode, event.value);
          this.logQueueSummary('调度结果');
          return;
        }
        if (event.mode === 'O') {
          await this.finishOrCancelCar(event.target);
          this.logQueueSummary('释放后结果');
          return;
        }
      }
      if (event.type === 'B') {
        if (event.target === 'T1') {
          await this.setStrategyValue(event.value === 1 ? 'PRIORITY' : 'TIME_ORDER');
          this.logQueueSummary('策略切换后结果');
          return;
        }
        if (event.target.startsWith('F') || event.target.startsWith('S')) {
          const action = event.value === 1 ? 'recover' : 'fault';
          await this.safePileAction(event.target, action, `充电桩 ${event.target} ${action === 'recover' ? '恢复运行，系统触发恢复调度' : '发生故障，系统迁移该桩队列车辆'}`);
          this.logQueueSummary(action === 'recover' ? '恢复调度结果' : '故障调度结果');
          return;
        }
      }
      if (event.type === 'C') {
        await this.modifyRequestAmount(event.target, event.value);
        this.logQueueSummary('修改后结果');
        return;
      }
      this.log(`不支持的 Excel 输入：${event.raw}`, 'warn');
    },
    describeExcelEvent(event) {
      if (event.type === 'A' && (event.mode === 'F' || event.mode === 'T')) {
        return `${event.target} 提交${event.mode === 'F' ? '快充' : '慢充'}申请，电量 ${event.value} kWh`;
      }
      if (event.type === 'A' && event.mode === 'O') return `${event.target} 结束/取消当前申请`;
      if (event.type === 'B' && event.target === 'T1') return `切换调度策略为${event.value === 1 ? '优先级调度' : '时间顺序调度'}`;
      if (event.type === 'B' && (event.target.startsWith('F') || event.target.startsWith('S'))) return `${event.target} 充电桩${event.value === 1 ? '恢复' : '故障'}`;
      if (event.type === 'C') return `${event.target} 修改请求电量为 ${event.value} kWh`;
      return '未知输入';
    },
    async finishOrCancelCar(carId) {
      const id = carId.toUpperCase();
      try {
        const state = await this.getState(id);
        if (state.state === 'CHARGING') {
          const bill = await this.call(`/charging/requests/${id}/end`, { method: 'POST' });
          this.log(`系统动作：${id} 正在充电，结束充电并生成账单 ${bill.billNo}`, 'success');
          return;
        }
        if (state.state === 'QUEUING' || state.state === 'WAITING_AREA') {
          await this.call(`/charging/requests/${id}`, { method: 'DELETE' });
          this.log(`系统动作：${id} 位于${this.stateText(state.state)}，已取消申请并释放队列位置`, 'success');
          return;
        }
        this.log(`${id} 当前状态为${this.stateText(state.state)}，无需释放`, 'warn');
      } catch (error) {
        this.log(`${id} 无未完成申请，跳过释放`, 'warn');
      }
    },
    async modifyRequestAmount(carId, amount) {
      const id = carId.toUpperCase();
      await this.call(`/charging/requests/${id}/amount`, {
        method: 'PUT',
        body: JSON.stringify({ requestAmount: amount })
      });
      this.log(`${id} amount changed to ${amount} from Excel input`, 'success');
    },
    async runPrepareCars() {
      await this.runAcceptanceExcelScenario(); return;
      await this.runScenario('prepare cars', [
        () => this.ensureCars(21)
      ]);
    },
    async runTimeOrderScenario() {
      await this.runScenario('time order scheduling', [
        () => this.setStrategyValue('TIME_ORDER'),
        () => this.ensureCars(8),
        () => this.requestCharge(this.carNo(1), 'SLOW', 40),
        () => this.requestCharge(this.carNo(2), 'SLOW', 30),
        () => this.requestCharge(this.carNo(3), 'FAST', 100),
        () => this.requestCharge(this.carNo(4), 'FAST', 120),
        () => this.requestCharge(this.carNo(5), 'SLOW', 20),
        () => this.requestCharge(this.carNo(6), 'FAST', 60),
        () => this.queryPileQueue()
      ]);
    },
    async runPriorityScenario() {
      await this.runScenario('priority scheduling', [
        () => this.setStrategyValue('PRIORITY'),
        () => this.ensureCars(18),
        () => this.requestCharge(this.carNo(11), 'FAST', 30),
        () => this.requestCharge(this.carNo(12), 'FAST', 30),
        () => this.requestCharge(this.carNo(13), 'FAST', 30),
        () => this.requestCharge(this.carNo(14), 'FAST', 30),
        () => this.safePileAction('F1', 'fault', 'F1 fault simulated'),
        () => this.safePileAction('F1', 'recover', 'F1 recovered and rescheduled'),
        () => this.queryPileQueue('F1')
      ]);
    },
    async runFaultRecoveryScenario() {
      await this.runScenario('fault recovery', [
        () => this.setStrategyValue('TIME_ORDER'),
        () => this.ensureCars(10),
        () => this.requestCharge(this.carNo(7), 'SLOW', 35),
        () => this.requestCharge(this.carNo(8), 'SLOW', 25),
        () => this.safePileAction('S1', 'fault', 'S1 fault simulated'),
        () => this.safePileAction('S1', 'recover', 'S1 recovered and rescheduled'),
        () => this.queryPileQueue('S1')
      ]);
    },
    async runBillingScenario() {
      const carId = this.carNo(21);
      await this.runScenario('billing and payment', [
        () => this.ensureCar(carId, 'billing_test_car'),
        () => this.requestCharge(carId, 'FAST', 8),
        () => this.startByCurrentPile(carId),
        () => this.waitAndLog('simulate charging for 1 second', 1000),
        () => this.endAndPay(carId)
      ]);
    },
    async runAbnormalScenario() {
      const carId = this.carNo(20);
      await this.runScenario('abnormal event', [
        () => this.ensureCar(carId, 'abnormal_test_car'),
        () => this.requestCharge(carId, 'SLOW', 20),
        () => this.createAbnormalFor(carId, 'QUEUE_JUMP', 80),
        () => this.resolveLatestAbnormal(carId)
      ]);
    },
    async waitAndLog(text, ms) {
      this.log(text, 'event');
      await new Promise(resolve => window.setTimeout(resolve, ms));
    },
    async getState(carId) {
      return this.call(`/charging/requests/${carId.toUpperCase()}/state`);
    },
    async startByCurrentPile(carId) {
      const id = carId.toUpperCase();
      const state = await this.getState(id);
      if (!state.pileId) {
        throw new Error(`${id} has no assigned pile`);
      }
      await this.call(`/charging/requests/${id}/start`, {
        method: 'POST',
        body: JSON.stringify({ pileId: state.pileId })
      });
      this.log(`${id} started charging on ${state.pileId}`, 'success');
    },
    async endAndPay(carId) {
      const id = carId.toUpperCase();
      const bill = await this.call(`/charging/requests/${id}/end`, { method: 'POST' });
      this.log(`${id} finished charging, bill ${bill.billNo}`, 'success');
      await this.call('/charging/bills/pay', {
        method: 'POST',
        body: JSON.stringify({ billNo: bill.billNo, carId: id, amount: bill.totalFee })
      });
      this.log(`${id} paid bill ${bill.billNo}`, 'success');
    },
    async createAbnormalFor(carId, type, fee) {
      const event = await this.call('/admin/abnormal-events', {
        method: 'POST',
        body: JSON.stringify({
          carId: carId.toUpperCase(),
          eventType: type,
          description: 'test stub abnormal event',
          penaltyFee: fee
        })
      });
      this.log(`Abnormal event #${event.id} created for ${carId}`, 'success');
      return event;
    },
    async resolveLatestAbnormal(carId) {
      const events = await this.call('/admin/abnormal-events');
      const target = events.find(item => item.carId === carId.toUpperCase() && item.status === 'PENDING');
      if (!target) {
        this.log(`${carId} has no pending abnormal event`, 'warn');
        return;
      }
      await this.call(`/admin/abnormal-events/${target.id}/resolve`, { method: 'POST' });
      this.log(`Abnormal event #${target.id} resolved`, 'success');
    },
    async safePileAction(pileId, action, message) {
      const actionText = this.pileActionText(action);
      try {
        await this.call(`/admin/piles/${pileId}/${action}`, { method: 'POST' });
        this.log(message || `${pileId} ${actionText}`, 'success');
      } catch (error) {
        this.log(`${pileId} ${actionText}失败：${error.message}`, 'warn');
      }
    },
    async refreshQueueStatus() {
      await this.refreshAll(true);
      if (this.queuePileId) {
        await this.queryPileQueue(this.queuePileId);
      }
      this.log('队列状态已刷新', 'summary');
    },
    async queryPileQueue(id = this.queuePileId) {
      const pileId = (id || '').trim().toUpperCase();
      if (!pileId) {
        this.log('请先输入充电桩编号再查询队列', 'warn');
        return;
      }
      this.queuePileId = pileId;
      try {
        const data = await this.call(`/admin/queues/${pileId}`);
        const charging = data.chargingCar ? [data.chargingCar] : [];
        this.selectedQueueCars = charging.concat(data.cars || []);
        this.log(`${pileId} queue loaded, ${this.selectedQueueCars.length} cars`, 'success');
      } catch (error) {
        this.selectedQueueCars = [];
        this.log(`${pileId} queue query failed: ${error.message}`, 'error');
      }
    },
    async ensureManualCar() {
      await this.ensureCar(this.manual.carId, this.manual.userName);
      await this.refreshAll(true);
    },
    async submitManualRequest() {
      await this.requestCharge(this.manual.carId, this.manual.requestMode, this.manual.requestAmount);
      this.manual.controlCarId = this.manual.carId;
      await this.refreshAll(true);
    },
    async queryManualState() {
      const state = await this.getState(this.manual.controlCarId);
      this.manual.pileId = state.pileId || this.manual.pileId;
      this.log(`${this.manual.controlCarId} state ${this.stateText(state.state)}, pile ${state.pileId || '-'}`, 'success');
    },
    async modifyManualAmount() {
      await this.call(`/charging/requests/${this.manual.controlCarId.toUpperCase()}/amount`, {
        method: 'PUT',
        body: JSON.stringify({ requestAmount: this.manual.newAmount })
      });
      this.log(`${this.manual.controlCarId} amount changed to ${this.manual.newAmount}`, 'success');
      await this.refreshAll(true);
    },
    async modifyManualMode() {
      await this.call(`/charging/requests/${this.manual.controlCarId.toUpperCase()}/mode`, {
        method: 'PUT',
        body: JSON.stringify({ mode: this.manual.newMode })
      });
      this.log(`${this.manual.controlCarId} mode changed to ${this.modeText(this.manual.newMode)}`, 'success');
      await this.refreshAll(true);
    },
    async startManualCharging() {
      const carId = this.manual.controlCarId.toUpperCase();
      const pileId = this.manual.pileId || (await this.getState(carId)).pileId;
      if (!pileId) throw new Error('车辆尚未分配充电桩');
      await this.call(`/charging/requests/${carId}/start`, {
        method: 'POST',
        body: JSON.stringify({ pileId })
      });
      this.log(`${carId} started charging`, 'success');
      await this.refreshAll(true);
    },
    async endManualCharging() {
      const carId = this.manual.controlCarId.toUpperCase();
      const bill = await this.call(`/charging/requests/${carId}/end`, { method: 'POST' });
      this.log(`${carId} ended charging, bill ${bill.billNo}`, 'success');
      await this.refreshAll(true);
    },
    async setStrategy() {
      await this.setStrategyValue(this.strategy);
      await this.refreshAll(true);
    },
    async pileAction(action) {
      const pileId = (this.manual.adminPileId || '').trim().toUpperCase();
      if (!pileId) {
        this.log('请先输入充电桩编号', 'warn');
        return;
      }
      await this.safePileAction(pileId, action);
      await this.refreshAll(true);
    },
    async createAbnormal() {
      await this.createAbnormalFor(this.manual.abnormalCarId, this.manual.abnormalType, this.manual.penaltyFee);
      await this.refreshAll(true);
    },
    async payBill(bill) {
      await this.call('/charging/bills/pay', {
        method: 'POST',
        body: JSON.stringify({ billNo: bill.billNo, carId: bill.carId, amount: bill.totalFee })
      });
      this.log(`Bill ${bill.billNo} paid`, 'success');
      await this.refreshAll(true);
    },
    pileCars(item) {
      const charging = item.chargingCar ? [{ ...item.chargingCar, state: 'CHARGING' }] : [];
      return charging.concat(item.queue || []);
    },
    logModeText(value) {
      return { FAST: '快充', SLOW: '慢充' }[value] || value || '-';
    },
    logStateText(value) {
      return {
        WAITING_AREA: '等候区',
        QUEUING: '桩内等待',
        CHARGING: '充电中',
        FINISHED: '已完成',
        CANCELED: '已取消'
      }[value] || value || '-';
    },
    logStrategyText(value) {
      return { TIME_ORDER: '时间顺序', PRIORITY: '优先级' }[value] || value || '-';
    },
    formatNumber(value) {
      const number = Number(value);
      return Number.isFinite(number) ? number.toFixed(2) : '-';
    },
    modeText(value) {
      return { FAST: '快充', SLOW: '慢充' }[value] || value || '-';
    },
    stateText(value) {
      return {
        WAITING_AREA: '等候区',
        QUEUING: '桩内等待',
        CHARGING: '充电中',
        FINISHED: '已完成',
        CANCELED: '已取消'
      }[value] || value || '-';
    },
    pileStatusText(value) {
      return {
        POWER_ON: '已开机',
        RUNNING: '运行中',
        STOPPED: '已关机',
        FAULT: '故障'
      }[value] || value || '-';
    },
    pileActionText(action) {
      return {
        fault: '故障',
        recover: '恢复',
        start: '运行',
        'power-on': '开机',
        'power-off': '关机'
      }[action] || action;
    },
    strategyText(value) {
      return { TIME_ORDER: '时间顺序调度', PRIORITY: '优先级调度' }[value] || value || '-';
    },
    statusClass(value) {
      return {
        POWER_ON: 'stopped',
        RUNNING: 'running',
        STOPPED: 'stopped',
        FAULT: 'fault',
        CHARGING: 'charging',
        QUEUING: 'queuing',
        WAITING_AREA: 'waiting'
      }[value] || 'default';
    }
  }
}).mount('#app');



















