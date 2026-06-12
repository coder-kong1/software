const API = 'http://localhost:8080/api';

Vue.createApp({
  data() {
    return {
      page: 'login',
      tab: 'submit',
      carId: '',
      message: '',
      login: { account: 'V1', password: '123456' },
      register: { carId: 'V1', userName: '用户1', password: '123456', carCapacity: 100 },
      request: { carId: '', requestAmount: 40, requestMode: 'SLOW' },
      newAmount: 30,
      newMode: 'FAST',
      startPile: 'F1',
      state: null,
      detail: null,
      bills: [],
      payments: []
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
        payment: '支付记录'
      }[this.tab];
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
    enterClient(account) {
      this.carId = account.carId;
      this.request.carId = account.carId;
      this.page = 'app';
      this.tab = 'submit';
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
      this.state = await this.call(`/charging/requests/${this.carId}/start`, { method: 'POST', body: JSON.stringify({ pileId: this.startPile }) });
      this.tab = 'state';
    },
    async endCharging() {
      const bill = await this.call(`/charging/requests/${this.carId}/end`, { method: 'POST' });
      this.bills.unshift(bill);
      this.tab = 'bill';
    },
    async queryBills() {
      this.bills = await this.call(`/charging/bills/${this.carId}`);
    },
    async payBill(bill) {
      const payment = await this.call('/charging/bills/pay', {
        method: 'POST',
        body: JSON.stringify({ billNo: bill.billNo, carId: bill.carId, amount: bill.totalFee })
      });
      this.payments.unshift(payment);
      this.tab = 'payment';
    },
    async queryPayments() {
      this.payments = await this.call(`/charging/payments/${this.carId}`);
    }
  }
}).mount('#app');
