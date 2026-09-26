/* Nori Android wrapper performance/AI bridge patch.
 * The V10 artifact remains untouched. This only fixes the WebView-to-backend
 * transport used by the packaged Android build.
 */
(function (g) {
  'use strict';
  if (g.__NORI_ANDROID_FIX_V1__) return;
  g.__NORI_ANDROID_FIX_V1__ = true;

  const EDGE = 'https://qrlvkxewyoceykaquapv.supabase.co/functions/v1/nori-brain';
  const nativeFetch = g.fetch.bind(g);

  function urlOf(input) {
    try { return typeof input === 'string' ? input : String(input && input.url || ''); }
    catch (_) { return ''; }
  }

  function isNoriBackendPath(url) {
    try {
      const u = new URL(url, g.location && g.location.href || 'file:///android_asset/www/index.html');
      return /^\/api\/(nori|brain|brain\.js)$/.test(u.pathname);
    } catch (_) {
      return /\/api\/(nori|brain|brain\.js)(?:\?|$)/.test(String(url));
    }
  }

  g.fetch = function (input, init) {
    const url = urlOf(input);
    const method = String((init && init.method) || (input && input.method) || 'GET').toUpperCase();

    if (method !== 'POST' || !isNoriBackendPath(url)) {
      return nativeFetch(input, init);
    }

    const headers = new Headers((init && init.headers) || (input && input.headers) || {});
    try {
      const token = typeof g.noriSyncGetAccessToken === 'function' ? g.noriSyncGetAccessToken() : '';
      if (token) headers.set('Authorization', 'Bearer ' + token);
    } catch (_) {}
    headers.set('Content-Type', 'application/json');
    headers.set('X-Nori-Android', '1');

    const next = Object.assign({}, init || {}, {
      method: 'POST',
      headers,
      cache: 'no-store'
    });

    return nativeFetch(EDGE, next);
  };
})(window);
