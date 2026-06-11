const API = 'http://localhost:8080/api/admin';

Vue.createApp({
  data() {
    return {
      loggedIn: false,
      page: 'dashboard',
      login: { account: 'admin', password: 'admin123' },
      message: '',
      snapshot: { piles: [], waitingArea: [], fastQueue: [], slowQueue: [] },
      rule: { peakPrice: 1, normalPrice: 0.7, valleyPrice: 0.4, servicePrice: 0.8 },
      pricePreviewAmount: 40,
      selectedPileId: '',
      queueSearchId: '',
      bills: [],
      abnormal: { carId: 'V21', eventType: 'QUEUE_JUMP', description: '', penaltyFee: 80 },
      abnormalEvents: []
    };
  },
  computed: {
    fastCount() { return this.snapshot.piles.filter(p => p.pile.mode === 'FAST').length; },
    slowCount() { return this.snapshot.piles.filter(p => p.pile.mode === 'SLOW').length; },
    waitingCount() { return this.snapshot.waitingArea.length; },
    selectedPile() {
      const target = (this.queueSearchId || '').trim().toUpperCase();
      if (!target) return null;
      return this.snapshot.piles.find(item => item.pile.id.toUpperCase() === target) || null;
    },
    selectedPileCars() {
      return this.selectedPile ? this.pileCars(this.selectedPile) : [];
    },
    selectedWaitingCars() {
      return this.selectedPileCars.filter(car => car.state !== 'CHARGING').length;
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
  methods: {
    async call(url, options = {}) {
      const res = await fetch(API + url, {
        headers: { 'Content-Type': 'application/json' },
        ...options
      });
      const body = await res.json();
      this.message = body.success ? '操作成功' : body.message;
      if (!body.success) throw new Error(body.message);
      return body.data;
    },
    async loginAdmin() {
      this.loggedIn = true;
      this.page = 'dashboard';
    },
    logout() {
      this.loggedIn = false;
      this.page = 'dashboard';
      this.snapshot = { piles: [], waitingArea: [], fastQueue: [], slowQueue: [] };
      this.selectedPileId = '';
      this.queueSearchId = '';
      this.bills = [];
      this.abnormalEvents = [];
    },
    async refreshCurrent() {
      if (this.page === 'dashboard' || this.page === 'piles' || this.page === 'queues') await this.refresh();
      if (this.page === 'pricing') await this.loadRule();
      if (this.page === 'exceptions') await this.loadAbnormalEvents();
      if (this.page === 'reports') await this.loadBills();
    },
    async refresh() {
      this.snapshot = await this.call('/snapshot');
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
      if (!this.selectedPile) {
        this.message = `未找到充电桩 ${target}`;
      }
    },
    async pileAction(id, action) {
      await this.call(`/piles/${id}/${action}`, { method: 'POST' });
      await this.refresh();
    },
    async loadRule() {
      this.rule = await this.call('/price-rule');
    },
    async saveRule() {
      await this.call('/price-rule', { method: 'PUT', body: JSON.stringify(this.rule) });
    },
    previewFee(price) {
      return ((Number(this.pricePreviewAmount) || 0) * (Number(price) + Number(this.rule.servicePrice))).toFixed(2);
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
    pileCars(item) {
      const charging = item.chargingCar ? [{ ...item.chargingCar, position: '正在充电' }] : [];
      const queue = (item.queue || []).map((car, index) => ({ ...car, position: `等待第 ${index + 1} 位` }));
      return charging.concat(queue);
    },
    statusClass(value) {
      return {
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
    async reportAbnormalEvent() {
      const event = await this.call('/abnormal-events', { method: 'POST', body: JSON.stringify(this.abnormal) });
      this.abnormalEvents.unshift(event);
    },
    async loadAbnormalEvents() {
      this.abnormalEvents = await this.call('/abnormal-events');
    },
    async resolveAbnormalEvent(id) {
      await this.call(`/abnormal-events/${id}/resolve`, { method: 'POST' });
      await this.loadAbnormalEvents();
    }
  }
}).mount('#app');
