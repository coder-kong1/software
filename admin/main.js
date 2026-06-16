import { createApp } from 'vue';

const API = '/api/admin';

createApp({
  data() {
    return {
      loggedIn: false,
      refreshTimer: null,
      page: 'dashboard',
      login: { account: 'admin', password: 'admin123' },
      message: '',
      snapshot: { piles: [], waitingArea: [], fastQueue: [], slowQueue: [] },
      rule: {
        peakPrice: 1,
        normalPrice: 0.7,
        valleyPrice: 0.4,
        fastServicePrice: 1.0,
        slowServicePrice: 0.8
      },
      schedulingStrategy: 'TIME_ORDER',
      pricePreviewAmount: 40,
      selectedPileId: '',
      queueSearchId: '',
      queueDetail: null,
      bills: [],
      report: {
        billCount: 0,
        paidBillCount: 0,
        unpaidBillCount: 0,
        totalChargeAmount: 0,
        totalChargeDuration: 0,
        totalRevenue: 0,
        pendingAbnormalCount: 0,
        resolvedAbnormalCount: 0
      },
      abnormal: { carId: 'V1', eventType: 'QUEUE_JUMP', description: '', penaltyFee: 80 },
      abnormalEvents: []
    };
  },
  computed: {
    fastCount() { return this.snapshot.piles.filter(p => p.pile.mode === 'FAST').length; },
    slowCount() { return this.snapshot.piles.filter(p => p.pile.mode === 'SLOW').length; },
    pileQueuedCars() {
      return this.snapshot.piles.flatMap(item =>
        (item.queue || []).map(car => ({ ...car, pileId: item.pile.id }))
      );
    },
    waitingCount() {
      return this.snapshot.waitingArea.length + this.pileQueuedCars.length;
    },
    fastWaitingCars() {
      return this.pileQueuedCars.filter(car => car.requestMode === 'FAST');
    },
    slowWaitingCars() {
      return this.pileQueuedCars.filter(car => car.requestMode === 'SLOW');
    },
    selectedPile() {
      const target = (this.queueSearchId || '').trim().toUpperCase();
      if (!target) return null;
      if (this.queueDetail && this.queueDetail.pile.id.toUpperCase() === target) {
        return { pile: this.queueDetail.pile };
      }
      return this.snapshot.piles.find(item => item.pile.id.toUpperCase() === target) || null;
    },
    selectedPileCars() {
      if (this.queueDetail && this.selectedPile
          && this.queueDetail.pile.id === this.selectedPile.pile.id) {
        return this.queueDetail.cars;
      }
      return this.selectedPile ? this.pileCars(this.selectedPile) : [];
    },
    selectedChargingCar() {
      return this.selectedPileCars.find(car => car.state === 'CHARGING') || null;
    },
    selectedWaitingQueue() {
      return this.selectedPileCars
        .filter(car => car.state !== 'CHARGING')
        .map((car, index) => ({ ...car, waitPosition: `第 ${index + 1} 位` }));
    },
    selectedWaitingCars() {
      return this.selectedWaitingQueue.length;
    },
    selectedQueueUsage() {
      if (!this.selectedPile) return '0 / 0';
      return `${this.selectedPileCars.length} / ${this.selectedPile.pile.queueLimit + 1}`;
    },
    selectedQueuePercent() {
      if (!this.selectedPile) return 0;
      const total = this.selectedPile.pile.queueLimit + 1;
      return Math.min(100, Math.round((this.selectedPileCars.length / total) * 100));
    },
    pageTitle() {
      return {
        dashboard: '运行总览',
        piles: '充电桩管理',
        queues: '队列状态',
        pricing: '计费参数',
        exceptions: '异常处理',
        reports: '运营报表'
      }[this.page];
    }
  },
  watch: {
    async page() {
      if (this.loggedIn) await this.refreshCurrent();
    }
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
        body = JSON.parse(text);
      } catch (error) {
        const message = text || `HTTP ${res.status}`;
        this.message = message;
        throw new Error(message);
      }
      this.message = body.success ? '' : body.message;
      if (!body.success) throw new Error(body.message);
      return body.data;
    },
    async loginAdmin() {
      if (this.login.account !== 'admin' || this.login.password !== 'admin123') {
        this.message = '管理员账号或密码错误';
        return;
      }
      this.loggedIn = true;
      this.page = 'dashboard';
      await Promise.all([this.refresh(), this.loadReport(), this.loadSchedulingStrategy()]);
      this.startAutoRefresh();
    },
    logout() {
      this.loggedIn = false;
      this.page = 'dashboard';
      this.snapshot = { piles: [], waitingArea: [], fastQueue: [], slowQueue: [] };
      this.selectedPileId = '';
      this.queueSearchId = '';
      this.queueDetail = null;
      this.bills = [];
      this.report = {
        billCount: 0,
        paidBillCount: 0,
        unpaidBillCount: 0,
        totalChargeAmount: 0,
        totalChargeDuration: 0,
        totalRevenue: 0,
        pendingAbnormalCount: 0,
        resolvedAbnormalCount: 0
      };
      this.abnormalEvents = [];
      this.stopAutoRefresh();
    },
    async refreshCurrent() {
      if (this.page === 'dashboard') await Promise.all([this.refresh(), this.loadReport(), this.loadSchedulingStrategy()]);
      if (this.page === 'piles' || this.page === 'queues') await this.refresh();
      if (this.page === 'pricing') await this.loadRule();
      if (this.page === 'exceptions') await this.loadAbnormalEvents();
      if (this.page === 'reports') await Promise.all([this.loadBills(), this.loadReport()]);
    },
    async refresh() {
      try {
        const data = await this.call('/snapshot');
        this.snapshot = data || { piles: [], waitingArea: [], fastQueue: [], slowQueue: [] };
      } catch (error) {
        this.message = '加载充电桩状态失败：' + error.message;
      }
    },
    async loadSchedulingStrategy() {
      this.schedulingStrategy = await this.call('/scheduling-strategy');
    },
    async saveSchedulingStrategy() {
      this.schedulingStrategy = await this.call('/scheduling-strategy', {
        method: 'PUT',
        body: JSON.stringify({ strategy: this.schedulingStrategy })
      });
      await this.refresh();
    },
    strategyText(value) {
      if (value === 'TIME_ORDER') return '时间顺序调度';
      if (value === 'PRIORITY') return '优先级调度';
      return value || '-';
    },
    startAutoRefresh() {
      this.stopAutoRefresh();
      this.refreshTimer = window.setInterval(() => {
        if (!this.loggedIn) return;
        if (this.page === 'dashboard' || this.page === 'piles') {
          this.refresh().catch(() => {});
        } else if (this.page === 'queues' && this.queueSearchId) {
          this.queryPileQueue(this.queueSearchId).catch(() => {});
        }
      }, 3000);
    },
    stopAutoRefresh() {
      if (this.refreshTimer !== null) {
        window.clearInterval(this.refreshTimer);
        this.refreshTimer = null;
      }
    },
    async queryPileQueue(id = this.selectedPileId) {
      const target = (id || '').trim().toUpperCase();
      if (!target) {
        this.message = '请输入充电桩编号';
        return;
      }
      this.selectedPileId = target;
      await this.refresh();
      this.queueSearchId = target;
      try {
        this.queueDetail = await this.call(`/queues/${target}`);
      } catch (error) {
        this.queueDetail = null;
        this.message = `未找到充电桩 ${target}`;
      }
    },
    async refreshPileQueue() {
      if (this.queueSearchId || this.selectedPileId) {
        await this.queryPileQueue(this.queueSearchId || this.selectedPileId);
        return;
      }
      await this.refresh();
    },
    async pileAction(id, action) {
      const actionText = {
        'power-on': '开机',
        start: '运行',
        'power-off': '关机',
        fault: '标记故障',
        recover: '恢复'
      }[action] || '操作';
      try {
        await this.call(`/piles/${id}/${action}`, { method: 'POST' });
        await this.refresh();
        if (this.queueSearchId && this.queueSearchId === id) {
          await this.queryPileQueue(id);
        }
        this.message = `${id} 已${actionText}`;
      } catch (error) {
        this.message = `${id} ${actionText}失败：${error.message}`;
      }
    },
    async loadRule() {
      const rule = await this.call('/price-rule');
      this.rule = {
        peakPrice: Number(rule?.peakPrice) || 0,
        normalPrice: Number(rule?.normalPrice) || 0,
        valleyPrice: Number(rule?.valleyPrice) || 0,
        fastServicePrice: Number(rule?.fastServicePrice ?? rule?.servicePrice) || 0,
        slowServicePrice: Number(rule?.slowServicePrice ?? rule?.servicePrice) || 0
      };
    },
    async saveRule() {
      await this.call('/price-rule', { method: 'PUT', body: JSON.stringify(this.rule) });
    },
    previewFee(price, mode) {
      const servicePrice = mode === 'FAST'
        ? Number(this.rule.fastServicePrice)
        : Number(this.rule.slowServicePrice);
      return ((Number(this.pricePreviewAmount) || 0) * (Number(price) + servicePrice)).toFixed(2);
    },
    formatNumber(value) {
      if (value === undefined || value === null || value === '') return '-';
      const number = Number(value);
      return Number.isFinite(number) ? number.toFixed(2) : value;
    },
    modeText(value) {
      if (value === 'FAST') return '快充';
      if (value === 'SLOW') return '慢充';
      return value || '-';
    },
    stateText(value) {
      return {
        WAITING_AREA: '等待区',
        QUEUING: '充电桩等待队列',
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
    pileCars(item) {
      const charging = item.chargingCar ? [{ ...item.chargingCar, position: '正在充电' }] : [];
      const queue = (item.queue || []).map((car, index) => ({ ...car, position: `等待第 ${index + 1} 位` }));
      return charging.concat(queue);
    },
    powerOffDisabled(item) {
      return (item.pile.status !== 'POWER_ON' && item.pile.status !== 'RUNNING') || !!item.chargingCar;
    },
    powerOffHint(item) {
      if (item.chargingCar) {
        return '有车正在充电，不能关闭充电桩';
      }
      return '';
    },
    statusClass(value) {
      return {
        POWER_ON: 'power-on',
        RUNNING: 'running',
        STOPPED: 'stopped',
        FAULT: 'fault',
        CHARGING: 'charging',
        QUEUING: 'queuing',
        WAITING_AREA: 'waiting',
        FINISHED: 'finished',
        CANCELED: 'canceled'
      }[value] || 'default';
    },
    async loadBills() {
      this.bills = await this.call('/reports/bills');
    },
    async loadReport() {
      this.report = await this.call('/reports/summary');
    },
    async reportAbnormalEvent() {
      const carId = (this.abnormal.carId || '').trim().toUpperCase();
      if (!carId) {
        this.message = '请先填写异常车辆车号';
        return;
      }
      this.message = '正在登记 ' + carId + ' 的异常事件...';
      try {
        const payload = {
          ...this.abnormal,
          carId,
          description: (this.abnormal.description || '').trim()
        };
        const event = await this.call('/abnormal-events', {
          method: 'POST',
          body: JSON.stringify(payload)
        });
        this.abnormal.description = '';
        await Promise.all([this.loadAbnormalEvents(), this.loadReport()]);
        this.message = '已登记 ' + event.carId + ' 的异常事件；该车辆客户端会收到通知，罚款会进入账单支付';
      } catch (error) {
        this.message = '登记异常失败：' + (error.message || '请检查后端服务和车号是否存在');
      }
    },
    async loadAbnormalEvents() {
      const events = await this.call('/abnormal-events');
      this.abnormalEvents = this.sortAbnormalEvents(events || []);
    },
    async resolveAbnormalEvent(id) {
      this.message = '正在处理异常事件 #' + id + '...';
      try {
        const event = await this.call(`/abnormal-events/${id}/resolve`, { method: 'POST' });
        this.abnormalEvents = this.abnormalEvents.map(item => item.id === id ? event : item);
        await Promise.all([this.loadAbnormalEvents(), this.loadReport()]);
        this.message = '异常事件 #' + id + ' 已处理完成';
      } catch (error) {
        this.message = '处理异常失败：' + (error.message || '请刷新后重试');
      }
    },
    sortAbnormalEvents(events) {
      return [...events].sort((a, b) => {
        if (a.status !== b.status) return a.status === 'PENDING' ? -1 : 1;
        return String(b.createdAt || '').localeCompare(String(a.createdAt || ''));
      });
    },    abnormalTypeText(value) {
      return {
        NO_SHOW: '过号未到',
        OCCUPY_WITHOUT_CHARGE: '霸占充电桩不充电',
        OVERSTAY: '充完不走',
        QUEUE_JUMP: '恶意插队'
      }[value] || value;
    }
  },
  beforeUnmount() {
    this.stopAutoRefresh();
  }
}).mount('#app');


