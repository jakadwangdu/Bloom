var api = globalThis.chrome || globalThis.browser;
var D = { on: true, strict: true, hide: true, day: 0, count: 0 };
var S = Object.assign({}, D);
var $ = function (id) { return document.getElementById(id); };
var rm = matchMedia('(prefers-reduced-motion:reduce)').matches;
function today() { return Math.floor(Date.now() / 86400000); }

function render() {
  var n = S.day === today() ? S.count : 0;
  $('cnt').textContent = n + ' stopped today';
  $('st').textContent = S.on ? 'On' : 'Off';
  $('sub').textContent = S.on ? "Reels can't pull you in." : 'Click the bloom to turn on.';
  $('vs').textContent = S.strict ? 'On' : 'Off';
  $('vh').textContent = S.hide ? 'On' : 'Off';
}
function save(k, v) { S[k] = v; var o = {}; o[k] = v; api.storage.local.set(o); render(); }

api.storage.local.get(D, function (v) { S = v; render(); });
api.storage.onChanged.addListener(function (c) {
  for (var k in c) S[k] = c[k].newValue;
  render();
});

var cv = $('c'), c = cv.getContext('2d'), d2 = Math.min(devicePixelRatio || 1, 2);
cv.width = cv.height = 200 * d2;
var open = 0, ripple = 0;
function toggle() { save('on', !S.on); ripple = 1; }
cv.onclick = toggle;
cv.onkeydown = function (e) { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); toggle(); } };
$('rs').onclick = function () { save('strict', !S.strict); };
$('rh').onclick = function () { save('hide', !S.hide); };

function draw(ts) {
  open += ((S.on ? 1 : 0.04) - open) * (rm ? 1 : 0.07);
  ripple = Math.max(0, ripple - 0.02);
  var w = cv.width, x = w / 2, R = w * 0.42, len = R * (0.14 + 0.86 * open), rot = rm ? 0 : ts / 1000 * 0.07;
  c.clearRect(0, 0, w, w);
  c.strokeStyle = '#fff'; c.fillStyle = '#fff'; c.lineWidth = 1.2 * d2;
  for (var i = 0; i < 18; i++) {
    c.save(); c.translate(x, x); c.rotate(rot + i * Math.PI * 2 / 18);
    c.globalAlpha = 0.12 + 0.5 * open;
    c.beginPath(); c.ellipse(0, -len / 2, R * 0.1, len / 2, 0, 0, 7); c.stroke(); c.restore();
  }
  c.globalAlpha = 1; c.beginPath(); c.arc(x, x, (3 + 5 * open) * d2, 0, 7); c.fill();
  if (ripple > 0) { c.globalAlpha = ripple * 0.5; c.beginPath(); c.arc(x, x, R * (1.15 - ripple * 0.8), 0, 7); c.stroke(); c.globalAlpha = 1; }
  requestAnimationFrame(draw);
}
requestAnimationFrame(draw);
