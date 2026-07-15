const REMEMBER_KEY = "tennisly.remember_session";
const BROWSER_ALIVE_KEY = "tennisly.browser_alive";
const REMEMBER_PENDING_KEY = "tennisly.remember_pending";

export function setRememberPreference(remember: boolean) {
  if (typeof window === "undefined") return;
  if (remember) {
    window.localStorage.setItem(REMEMBER_KEY, "1");
  } else {
    window.localStorage.removeItem(REMEMBER_KEY);
  }
  window.sessionStorage.removeItem(REMEMBER_PENDING_KEY);
}

export function stashRememberBeforeRedirect(remember: boolean) {
  if (typeof window === "undefined") return;
  window.sessionStorage.setItem(REMEMBER_PENDING_KEY, remember ? "1" : "0");
}

export function consumeRememberPending() {
  if (typeof window === "undefined") return;
  const pending = window.sessionStorage.getItem(REMEMBER_PENDING_KEY);
  if (pending === null) return;
  window.sessionStorage.removeItem(REMEMBER_PENDING_KEY);
  setRememberPreference(pending === "1");
}

export function shouldPersistSession() {
  if (typeof window === "undefined") return true;
  return window.localStorage.getItem(REMEMBER_KEY) === "1";
}

/** True once per browser process; false on the first paint after a full browser quit. */
export function claimBrowserSession() {
  if (typeof window === "undefined") return true;
  const alive = window.sessionStorage.getItem(BROWSER_ALIVE_KEY);
  if (alive) return true;
  window.sessionStorage.setItem(BROWSER_ALIVE_KEY, "1");
  return false;
}
