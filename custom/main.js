const API = '/api';

Vue.createApp({
  data() {
    return {
      page: 'login',
      tab: 'submit',
      refreshTimer: null,
      carId: '',
      message: '',
      login: { account: 'V1', password: '123456' },
      register: { carId: 'V1', userName: '用户1', password: '123456', carCapacity: 100 },
      request: { carId: '', requestAmount: 40, requestMode: 'SLOW' },
      newAmount: 30,
      newMode: 'FAST',
      state: null,
      detail: null,
      bills: [],
      payments: [],
      abnormalEvents: [],
      abnormalLoaded: false,
      autoSettling: false
    };
  },
  computed: {
    tabTitle() {
      return {
        submit: '提交申请',
        modify: '修改请求',
        control: '充电控制',
        state: '队列状态',
        detail: '充电详单',
        bill: '账单支付',
        payment: '支付记录',
        abnormal: '异常与通知'
      }[this.tab];
    }
  },
  methods: {
    async call(url, options = {}, silent = false) {
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
        if (!silent) this.message = message;
        throw new Error(message);
      }
      if (!silent) this.message = body.success ? '' : body.message;
      if (!body.success) throw new Error(body.message);
      return body.data;
    },
    enterClient(account) {
      this.carId = account.carId;
      this.request.carId = account.carId;
      this.page = 'app';
      this.tab = 'submit';
      this.queryAbnormalEvents(true).catch(() => {});
      this.refreshBillingData().catch(() => {});
      this.startAutoRefresh();
    },
    async loginClient() {
      const account = await this.call('/accounts/login', {
        method: 'POST',
        body: JSON.stringify(this.login)
      });
      this.enterClient(account);
    },
    async createAccount() {
      const account = await this.call('/accounts', { method: 'POST', body: JSON.stringify(this.register) });
      this.login.account = this.register.carId;
      this.login.password = this.register.password;
      this.enterClient(account);
    },
    logout() {
      this.page = 'login';
      this.tab = 'submit';
      this.state = null;
      this.detail = null;
      this.bills = [];
      this.payments = [];
      this.abnormalEvents = [];
      this.abnormalLoaded = false;
      this.stopAutoRefresh();
    },
    modeText(value) {
      if (value === 'FAST') return '快充';
      if (value === 'SLOW') return '慢充';
      return value || '-';
    },
    formatNumber(value) {
      if (value === undefined || value === null || value === '') return '-';
      const number = Number(value);
      return Number.isFinite(number) ? number.toFixed(2) : value;
    },
    async submitRequest() {
      const data = await this.call('/charging/requests', { method: 'POST', body: JSON.stringify(this.request) });
      this.state = data;
      this.tab = 'state';
    },
    async modifyAmount() {
      this.state = await this.call(`/charging/requests/${this.carId}/amount`, { method: 'PUT', body: JSON.stringify({ amount: this.newAmount }) });
      this.tab = 'state';
    },
    async modifyMode() {
      this.state = await this.call(`/charging/requests/${this.carId}/mode`, { method: 'PUT', body: JSON.stringify({ mode: this.newMode }) });
      this.tab = 'state';
    },
    async cancelRequest() {
      await this.call(`/charging/requests/${this.carId}`, { method: 'DELETE' });
      this.state = null;
      this.detail = null;
    },
    async queryState() {
      this.state = await this.call(`/charging/requests/${this.carId}/state`);
    },
    async queryDetail() {
      this.detail = await this.call(`/charging/details/${this.carId}`);
    },
    async startCharging() {
      const latestState = await this.call(
        `/charging/requests/${this.carId}/state`,
        {},
        true
      );
      this.state = latestState;
      if (!latestState.pileId) {
        this.message = '车辆尚未分配到充电桩，请等待系统调度';
        return;
      }
      this.state = await this.call(`/charging/requests/${this.carId}/start`, {
        method: 'POST',
        body: JSON.stringify({ pileId: latestState.pileId })
      });
      this.tab = 'state';
    },
    async endCharging() {
      await this.call(`/charging/requests/${this.carId}/end`, { method: 'POST' });
      await this.refreshBillingData();
      this.state = null;
      this.detail = null;
      this.tab = 'bill';
    },
    async refreshBillingData() {
      const [bills, payments] = await Promise.all([
        this.call(`/charging/bills/${this.carId}`, {}, true),
        this.call(`/charging/payments/${this.carId}`, {}, true)
      ]);
      this.bills = bills || [];
      this.payments = payments || [];
    },
    async queryBills() {
      await this.refreshBillingData();
    },
    async payBill(bill) {
      const payment = await this.call('/charging/bills/pay', {
        method: 'POST',
        body: JSON.stringify({ billNo: bill.billNo, carId: bill.carId, amount: bill.totalFee })
      });
      await this.refreshBillingData();
      this.message = `账单 ${payment.billNo} 已支付`;
      this.tab = 'payment';
    },
    async queryPayments() {
      await this.refreshBillingData();
    },
    async queryAbnormalEvents(silent = false) {
      const events = await this.call(`/charging/abnormal-events/${this.carId}`, {}, silent);
      const oldIds = new Set((this.abnormalEvents || []).map(event => event.id));
      const newEvents = (events || []).filter(event => !oldIds.has(event.id));
      this.abnormalEvents = events || [];
      if (this.abnormalLoaded && newEvents.length > 0) {
        this.message = `收到 ${newEvents.length} 条新的异常处理通知`;
        await this.refreshBillingData();
      }
      this.abnormalLoaded = true;
    },
    abnormalTypeText(value) {
      return {
        NO_SHOW: '过号未到',
        OCCUPY_WITHOUT_CHARGE: '霸占充电桩不充电',
        OVERSTAY: '充完不走',
        QUEUE_JUMP: '恶意插队'
      }[value] || value;
    },
    startAutoRefresh() {
      this.stopAutoRefresh();
      this.refreshTimer = window.setInterval(async () => {
        if (this.page !== 'app') return;
        try {
          await this.queryAbnormalEvents(true);
          if (this.tab === 'state' && this.state) {
            this.state = await this.call(
              `/charging/requests/${this.carId}/state`,
              {},
              true
            );
          } else if (this.tab === 'detail' && this.detail) {
            this.detail = await this.call(
              `/charging/details/${this.carId}`,
              {},
              true
            );
          } else if (this.tab === 'bill' || this.tab === 'payment') {
            await this.refreshBillingData();
          }
        } catch (error) {
          // Background refresh should not interrupt manual operation.
        }
      }, 3000);
    },
    stopAutoRefresh() {
      if (this.refreshTimer !== null) {
        window.clearInterval(this.refreshTimer);
        this.refreshTimer = null;
      }
    }
  },
  beforeUnmount() {
    this.stopAutoRefresh();
  }
}).mount('#app');


