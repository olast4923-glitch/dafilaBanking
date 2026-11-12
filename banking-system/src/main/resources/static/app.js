const $ = (id) => document.getElementById(id);
const moneyFormat = new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

let customer = null;
let accounts = [];
let selectedAccount = null;
let activeView = 'overview';
let modalMode = 'deposit';
let historyFilter = 'ALL';

const TYPE_HINTS = {
  SAVINGS: 'A savings account pays 0.05% monthly interest. Deposits are allowed, withdrawals are not.',
  INVESTMENT: 'An investment account pays 5% monthly and requires an opening deposit of at least BWP 500.00.',
  CHEQUE: 'A cheque account is for salary credits. It requires your employer name and company address.'
};

function esc(value) {
  return String(value == null ? '' : value)
    .replaceAll('&', '&amp;').replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;').replaceAll('"', '&quot;');
}

function bwp(value) {
  const number = Number(value);
  return 'BWP ' + moneyFormat.format(Number.isFinite(number) ? number : 0);
}

function mask(number) {
  return '•••• ' + String(number || '').slice(-4);
}

function initials(name) {
  return String(name || '').trim().split(/\s+/).slice(0, 2).map((p) => p.charAt(0).toUpperCase()).join('') || 'MB';
}

function fmtDate(iso) {
  const date = new Date(String(iso).replace(' ', 'T'));
  const day = Number.isNaN(date.getTime())
    ? String(iso).slice(0, 10)
    : date.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
  return day + ' · ' + String(iso).slice(11, 16);
}

function greeting() {
  const hour = new Date().getHours();
  if (hour < 12) return 'Good morning';
  if (hour < 18) return 'Good afternoon';
  return 'Good evening';
}

function toast(message, kind = 'ok') {
  const el = $('toast');
  el.textContent = message;
  el.className = 'toast ' + kind;
  el.classList.remove('hidden');
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => el.classList.add('hidden'), 5500);
}

async function api(path, body) {
  const response = await fetch(path, {
    method: body === undefined ? 'GET' : 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  let payload;
  try {
    payload = await response.json();
  } catch (err) {
    throw new Error('The server returned an unreadable response.');
  }
  if (!payload.ok) throw new Error(payload.message || 'Request failed.');
  return payload;
}

async function loadHealth() {
  try {
    const result = await api('/api/health');
    $('db-label').textContent = result.data.database;
  } catch (err) {
    $('db-label').textContent = 'Connection unavailable';
  }
}

function accountCardHtml(account) {
  const type = account.type.toLowerCase();
  return `
    <div class="account-card ${type} ${account.accountNumber === selectedAccount ? 'selected' : ''}"
         data-account="${esc(account.accountNumber)}">
      <div class="card-top">
        <span class="type">${esc(account.displayName)}</span>
        <span class="badge ${type}">${esc(account.type)}</span>
      </div>
      <div class="number">${esc(mask(account.accountNumber))}</div>
      <div class="balance">${bwp(account.balance)}</div>
      <div class="balance-label">Available balance</div>
      <div class="card-foot"><span>${esc(account.branch)}</span><span>${esc(account.interestLabel)}</span></div>
    </div>`;
}

function renderAccounts() {
  const html = accounts.length
    ? accounts.map(accountCardHtml).join('')
    : '<div class="panel muted">You have no accounts yet. Open one to get started.</div>';
  $('overview-accounts').innerHTML = html;
  $('all-accounts').innerHTML = html;

  document.querySelectorAll('.account-card').forEach((card) => {
    card.addEventListener('click', () => {
      selectedAccount = card.dataset.account;
      historyFilter = selectedAccount;
      renderAccounts();
      renderAccountDetail();
      loadActivity();
      if (activeView === 'overview') setView('accounts');
    });
  });

  const total = accounts.reduce((sum, a) => sum + Number(a.balance), 0);
  $('total-balance').textContent = bwp(total);
  $('hero-sub').textContent = 'Across ' + accounts.length + ' account' + (accounts.length === 1 ? '' : 's');
  $('hero-accounts').textContent = accounts.length;
  $('hero-interest').textContent = accounts.filter((a) => a.earnsInterest).length;

  populateSelects();
  renderAccountDetail();
}

function populateSelects() {
  const options = accounts.map((a) =>
    `<option value="${esc(a.accountNumber)}">${esc(mask(a.accountNumber))} · ${esc(a.displayName)}</option>`
  ).join('');

  const action = $('action-account');
  action.innerHTML = options;
  if (selectedAccount) action.value = selectedAccount;

  const stillValid = historyFilter === 'ALL'
    || accounts.some((a) => a.accountNumber === historyFilter);
  if (!stillValid) historyFilter = 'ALL';
  const filter = $('history-filter');
  filter.innerHTML = '<option value="ALL">All accounts</option>' + options;
  filter.value = historyFilter;
}

function renderAccountDetail() {
  const target = $('account-detail');
  const account = accounts.find((a) => a.accountNumber === selectedAccount);
  if (!account) {
    target.innerHTML = '<span class="muted">Select an account above to inspect it.</span>';
    return;
  }
  const items = [
    ['Account number', account.accountNumber],
    ['Account type', account.displayName],
    ['Branch', account.branch],
    ['Available balance', bwp(account.balance)],
    ['Interest rate', account.interestLabel],
    ['Withdrawals', account.allowsWithdrawals ? 'Allowed' : 'Not permitted'],
    ['Employer', account.employer || 'Not applicable']
  ];
  target.innerHTML = '<div class="detail-grid">' + items.map(([label, value]) =>
    `<div class="detail-item"><small>${esc(label)}</small><strong>${esc(value)}</strong></div>`
  ).join('') + '</div>';
}

function renderHistoryTable(elementId, rows) {
  const body = $(elementId);
  if (!rows.length) {
    body.innerHTML = '<tr><td class="empty" colspan="6">No transactions to display.</td></tr>';
    return;
  }
  body.innerHTML = rows.map((t) => {
    const incoming = ['DEPOSIT', 'OPENING', 'INTEREST'].includes(t.type);
    return `
      <tr>
        <td class="date">${esc(fmtDate(t.occurredAt))}</td>
        <td class="desc">${esc(t.note || (incoming ? 'Funds received' : 'Funds withdrawn'))}</td>
        <td>${esc(mask(t.accountNumber))}</td>
        <td><span class="tag ${esc(t.type)}">${esc(t.type)}</span></td>
        <td class="num ${incoming ? 'amount-in' : 'amount-out'}">${incoming ? '+' : '−'}${bwp(t.amount)}</td>
        <td class="num">${bwp(t.balanceAfter)}</td>
      </tr>`;
  }).join('');
}

function renderInterestList(rows) {
  const target = $('interest-list');
  if (!rows.length) {
    target.innerHTML = '<span class="muted">No interest payments recorded yet.</span>';
    return;
  }
  target.innerHTML = rows.map((t) => `
    <div class="interest-row">
      <span>${esc(t.accountNumber)}<br><small class="muted">${esc(fmtDate(t.occurredAt))}</small></span>
      <strong>+${bwp(t.amount)}</strong>
    </div>`).join('');
}

async function loadAccounts() {
  if (!customer) return;
  try {
    const result = await api('/api/accounts?customerId=' + encodeURIComponent(customer.id));
    accounts = result.data || [];
    if (!accounts.some((a) => a.accountNumber === selectedAccount)) {
      selectedAccount = accounts.length ? accounts[0].accountNumber : null;
    }
    renderAccounts();
  } catch (err) {
    toast(err.message, 'error');
  }
}

async function loadActivity() {
  try {
    const path = historyFilter === 'ALL'
      ? '/api/transactions/all'
      : '/api/transactions?accountNumber=' + encodeURIComponent(historyFilter);
    const result = await api(path);
    const rows = result.data || [];
    renderHistoryTable('history-body', rows);
    renderHistoryTable('recent-body', rows.slice(0, 6));
    $('history-count').textContent = rows.length + ' transaction' + (rows.length === 1 ? '' : 's');
    renderInterestList(rows.filter((t) => t.type === 'INTEREST').slice(0, 8));
  } catch (err) {
    toast(err.message, 'error');
  }
}

function setView(name) {
  activeView = name;
  document.querySelectorAll('.view').forEach((view) => view.classList.add('hidden'));
  $('view-' + name).classList.remove('hidden');
  document.querySelectorAll('.nav-item[data-view]').forEach((button) =>
    button.classList.toggle('active', button.dataset.view === name));
  window.scrollTo({ top: 0, behavior: 'smooth' });
  if (name === 'activity' || name === 'interest') loadActivity();
  if (name === 'accounts') renderAccountDetail();
}

function afterAuth(data) {
  customer = data;
  accounts = data.accounts || [];
  selectedAccount = accounts.length ? accounts[0].accountNumber : null;
  historyFilter = 'ALL';

  $('auth-screen').classList.add('hidden');
  $('app-shell').classList.remove('hidden');
  $('user-name').textContent = data.fullName;
  $('user-email').textContent = data.email;
  $('avatar').textContent = initials(data.fullName);
  $('greeting').textContent = greeting() + ', ' + data.firstName;
  $('today').textContent = new Date().toLocaleDateString('en-GB',
    { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });

  renderAccounts();
  setView('overview');
  loadActivity();
}

function openModal(mode) {
  modalMode = mode;
  $('modal-title').textContent = mode === 'deposit' ? 'Deposit funds' : 'Withdraw funds';
  $('mode-deposit').classList.toggle('active', mode === 'deposit');
  $('mode-withdraw').classList.toggle('active', mode === 'withdraw');
  $('modal-submit').textContent = mode === 'deposit' ? 'Confirm deposit' : 'Confirm withdrawal';
  $('modal-hint').textContent = mode === 'deposit'
    ? 'Funds are credited to the selected account as soon as you confirm.'
    : 'Savings accounts do not permit withdrawals, and balances cannot go below zero.';
  if (selectedAccount) $('action-account').value = selectedAccount;
  $('modal').classList.remove('hidden');
}

function closeModal() {
  $('modal').classList.add('hidden');
}

async function submitTransaction() {
  const payload = {
    accountNumber: $('action-account').value,
    amount: $('action-amount').value,
    note: $('action-note').value
  };
  const path = modalMode === 'deposit'
    ? '/api/transactions/deposit' : '/api/transactions/withdraw';
  try {
    const result = await api(path, payload);
    selectedAccount = result.data.accountNumber;
    closeModal();
    $('action-amount').value = '';
    $('action-note').value = '';
    await loadAccounts();
    await loadActivity();
    toast(result.message);
  } catch (err) {
    toast(err.message, 'error');
  }
}

function selectedType() {
  const checked = document.querySelector('input[name="acct-type"]:checked');
  return checked ? checked.value : 'SAVINGS';
}

function syncTypeSelection() {
  const type = selectedType();
  document.querySelectorAll('.type-option').forEach((option) =>
    option.classList.toggle('active', option.querySelector('input').value === type));
  $('employer-fields').classList.toggle('hidden', type !== 'CHEQUE');
  $('open-hint').textContent = TYPE_HINTS[type];
  if (type === 'INVESTMENT') $('open-balance').value = '500.00';
  if (type === 'CHEQUE') $('open-balance').value = '0.00';
  if (type === 'SAVINGS') $('open-balance').value = '100.00';
}

/* ---------- events ---------- */

$('tab-login').addEventListener('click', () => {
  $('tab-login').classList.add('active');
  $('tab-register').classList.remove('active');
  $('login-form').classList.remove('hidden');
  $('register-form').classList.add('hidden');
  $('auth-title').textContent = 'Sign in to your account';
  $('auth-subtitle').textContent = 'Enter your details to continue to online banking.';
});

$('tab-register').addEventListener('click', () => {
  $('tab-register').classList.add('active');
  $('tab-login').classList.remove('active');
  $('register-form').classList.remove('hidden');
  $('login-form').classList.add('hidden');
  $('auth-title').textContent = 'Create your profile';
  $('auth-subtitle').textContent = 'Register as a customer before opening an account.';
});

$('fill-demo').addEventListener('click', () => {
  $('tab-login').click();
  $('login-email').value = 'dafila.philimon@example.com';
  $('login-password').value = 'Passw0rd!';
});

$('login-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  try {
    const result = await api('/api/login', {
      email: $('login-email').value,
      password: $('login-password').value
    });
    afterAuth(result.data);
    toast(result.message);
  } catch (err) {
    toast(err.message, 'error');
  }
});

$('register-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  try {
    const result = await api('/api/register', {
      firstName: $('reg-first').value,
      surname: $('reg-last').value,
      address: $('reg-address').value,
      email: $('reg-email').value,
      password: $('reg-password').value,
      employed: $('reg-employed').checked,
      companyName: $('reg-company').value,
      companyAddress: $('reg-company-address').value
    });
    afterAuth(result.data);
    toast(result.message);
  } catch (err) {
    toast(err.message, 'error');
  }
});

document.querySelectorAll('.nav-item[data-view]').forEach((button) => {
  button.addEventListener('click', () => setView(button.dataset.view));
});

document.querySelectorAll('[data-goto]').forEach((button) => {
  button.addEventListener('click', () => setView(button.dataset.goto));
});

$('qa-deposit').addEventListener('click', () => openModal('deposit'));
$('qa-withdraw').addEventListener('click', () => openModal('withdraw'));
$('mode-deposit').addEventListener('click', () => openModal('deposit'));
$('mode-withdraw').addEventListener('click', () => openModal('withdraw'));
$('modal-close').addEventListener('click', closeModal);
$('modal-submit').addEventListener('click', submitTransaction);
$('modal').addEventListener('click', (event) => {
  if (event.target === $('modal')) closeModal();
});
document.addEventListener('keydown', (event) => {
  if (event.key === 'Escape') closeModal();
});

$('history-filter').addEventListener('change', (event) => {
  historyFilter = event.target.value;
  loadActivity();
});

$('refresh-btn').addEventListener('click', async () => {
  await loadAccounts();
  await loadActivity();
  toast('Accounts and transactions are up to date.');
});

document.querySelectorAll('input[name="acct-type"]').forEach((input) => {
  input.addEventListener('change', syncTypeSelection);
});

$('open-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  try {
    const result = await api('/api/accounts/open', {
      customerId: customer.id,
      type: selectedType(),
      openingBalance: $('open-balance').value,
      branch: $('open-branch').value,
      companyName: $('open-company').value,
      companyAddress: $('open-company-address').value
    });
    selectedAccount = result.data.accountNumber;
    historyFilter = 'ALL';
    await loadAccounts();
    await loadActivity();
    toast(result.message);
    setView('accounts');
  } catch (err) {
    toast(err.message, 'error');
  }
});

$('interest-btn').addEventListener('click', async () => {
  try {
    const result = await api('/api/interest/run', {});
    historyFilter = 'ALL';
    $('history-filter').value = 'ALL';
    await loadAccounts();
    await loadActivity();
    toast(result.message);
  } catch (err) {
    toast(err.message, 'error');
  }
});

$('logout-btn').addEventListener('click', () => {
  customer = null;
  accounts = [];
  selectedAccount = null;
  historyFilter = 'ALL';
  closeModal();
  $('app-shell').classList.add('hidden');
  $('auth-screen').classList.remove('hidden');
  $('toast').classList.add('hidden');
});

syncTypeSelection();
loadHealth();
