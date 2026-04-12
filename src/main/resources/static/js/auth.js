/**
 * auth.js — shared authentication state helper
 *
 * Include this script on any page. Call:
 *   LALAuth.init(options)
 *
 * options = {
 *   loginSelector:   CSS selector for the "Login" link/button (will be hidden when logged in)
 *   userSelector:    CSS selector for the user-info container  (will be shown when logged in)
 *   usernameSelector:CSS selector for the <span> that shows username
 *   logoutSelector:  CSS selector for the logout button/link
 *   onReady:         callback(status) – called after status is fetched
 * }
 */

const LALAuth = (() => {

  let _status = null; // cached result

  async function fetchStatus() {
    try {
      const res = await fetch('/api/auth/status', { credentials: 'same-origin' });
      if (!res.ok) return { loggedIn: false, username: null, isAdmin: false };
      return await res.json();
    } catch {
      return { loggedIn: false, username: null, isAdmin: false };
    }
  }

  function applyToDOM(status, opts = {}) {
    const { loggedIn, username, isAdmin } = status;

    // ── elements injected by init() ──────────────────────────────────────
    if (opts.loginSelector) {
      document.querySelectorAll(opts.loginSelector).forEach(el => {
        el.style.display = loggedIn ? 'none' : '';
      });
    }

    if (opts.userSelector) {
      document.querySelectorAll(opts.userSelector).forEach(el => {
        el.style.display = loggedIn ? '' : 'none';
      });
    }

    if (opts.usernameSelector && username) {
      document.querySelectorAll(opts.usernameSelector).forEach(el => {
        el.textContent = username;
      });
    }

    if (opts.logoutSelector) {
      document.querySelectorAll(opts.logoutSelector).forEach(el => {
        el.addEventListener('click', logout);
      });
    }

    // ── generic data-auth attributes ─────────────────────────────────────
    // data-auth="show-logged-in"  → visible only when logged in
    // data-auth="show-logged-out" → visible only when logged out
    // data-auth="username"        → replaced with username
    document.querySelectorAll('[data-auth]').forEach(el => {
      const rule = el.getAttribute('data-auth');
      if (rule === 'show-logged-in')  el.style.display = loggedIn ? '' : 'none';
      if (rule === 'show-logged-out') el.style.display = loggedIn ? 'none' : '';
      if (rule === 'username')        el.textContent   = username || 'Guest';
    });
  }

  async function init(opts = {}) {
    _status = await fetchStatus();
    applyToDOM(_status, opts);
    if (typeof opts.onReady === 'function') opts.onReady(_status);
    return _status;
  }

  function logout() {
    window.location.href = '/logout';
  }

  function getStatus() { return _status; }

  return { init, fetchStatus, logout, getStatus };
})();

// ── Guest login helper (used by login.html) ───────────────────────────────────
async function guestLoginAndRedirect(redirectTo) {
  const btn = document.getElementById('guest-btn');
  if (btn) { btn.textContent = '⏳ Setting up…'; btn.disabled = true; }

  try {
    const res = await fetch('/api/guest/login', {
      method: 'POST',
      credentials: 'same-origin'
    });

    if (res.ok) {
      window.location.href = redirectTo || '/index.html';
    } else {
      alert('Could not start guest session. Please try again.');
      if (btn) { btn.textContent = '👤 Continue as Guest'; btn.disabled = false; }
    }
  } catch (err) {
    alert('Network error — please check your connection.');
    if (btn) { btn.textContent = '👤 Continue as Guest'; btn.disabled = false; }
  }
}
