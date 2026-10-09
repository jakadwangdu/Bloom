var api = globalThis.chrome || globalThis.browser;
var act = api.action || api.browserAction;
function badge() {
  api.storage.local.get({ on: true, day: 0, count: 0 }, function (v) {
    var n = v.day === Math.floor(Date.now() / 86400000) ? v.count : 0;
    act.setBadgeText({ text: !v.on ? 'off' : (n ? String(n) : '') });
    act.setBadgeBackgroundColor({ color: '#000000' });
    if (act.setBadgeTextColor) act.setBadgeTextColor({ color: '#ffffff' });
  });
}
api.storage.onChanged.addListener(badge);
api.runtime.onStartup.addListener(badge);
api.runtime.onInstalled.addListener(badge);
