/* Bloom for Desktop: fail-open. If anything is unsure, do nothing. */
(function () {
  'use strict';
  var api = globalThis.chrome || globalThis.browser;
  var D = { on: true, strict: true, hide: true };
  var S = Object.assign({}, D);
  var prev = '', fromDm = false, fromExplore = false, lockedId = null, firstLoad = true;
  var strikes = [], redirects = [], lastEvt = 0, lastCountPath = null, toastEl, toastT;

  function today() { return Math.floor(Date.now() / 86400000); }
  function hit() {
    try {
      api.storage.local.get({ day: 0, count: 0 }, function (v) {
        var d = today();
        api.storage.local.set(v.day === d ? { day: d, count: v.count + 1 } : { day: d, count: 1 });
      });
    } catch (e) {}
  }

  function toast(msg) {
    try {
      if (!toastEl) {
        toastEl = document.createElement('div');
        toastEl.id = 'bloom-toast';
        (document.body || document.documentElement).appendChild(toastEl);
      }
      toastEl.textContent = msg;
      toastEl.classList.add('show');
      clearTimeout(toastT);
      toastT = setTimeout(function () { toastEl.classList.remove('show'); }, 1800);
    } catch (e) {}
  }

  function showOverlay() {
    if (document.getElementById('bloom-ov')) return;
    var o = document.createElement('div');
    o.id = 'bloom-ov';
    o.innerHTML = '<div><h1>Reels are off.</h1><p>Open one from a message.</p><a href="/direct/inbox/">Messages</a><a href="/">Home</a></div>';
    (document.body || document.documentElement).appendChild(o);
  }
  function hideOverlay() { var o = document.getElementById('bloom-ov'); if (o) o.remove(); }
  function mute() {
    document.querySelectorAll('video').forEach(function (v) { try { v.pause(); v.muted = true; } catch (e) {} });
  }

  function go(url) {                       // never more than 2 redirects per 10 s
    var now = Date.now();
    redirects = redirects.filter(function (t) { return now - t < 10000; });
    if (redirects.length >= 2) return;
    redirects.push(now);
    location.assign(url);
  }

  function violation(nav) {                // one count per gesture, not per wheel tick
    var now = Date.now(), cont = now - lastEvt < 500;
    lastEvt = now;
    if (cont) return;
    strikes = strikes.filter(function (t) { return now - t < 5000; });
    strikes.push(now);
    hit();
    if (strikes.length >= 2) { strikes = []; toast('Back to your chat.'); go('/direct/inbox/'); }
    else { toast("That's the only video."); if (nav) history.back(); }
  }

  function onPath(p) {
    var wasReel = !!bloomReelId(prev), nowReel = !!bloomReelId(p);
    if (nowReel && !wasReel) {
      fromDm = BLOOM_SEL.dmPath.test(prev) || (firstLoad && document.referrer.indexOf('/direct/') > -1);
      fromExplore = BLOOM_SEL.explorePath.test(prev);
      lockedId = bloomReelId(p);
    } else if (nowReel && wasReel && bloomReelId(p) !== lockedId && fromDm && S.on) {
      violation(true);                     // tried to skip to another reel
    } else if (!nowReel) {
      fromDm = false; fromExplore = false; lockedId = null;
    }
    prev = p; firstLoad = false;
  }

  function check() {
    try {
      var h = document.documentElement;
      if (!S.on) { h.classList.remove('bloom-hide', 'bloom-dm'); hideOverlay(); return; }
      var p = location.pathname;
      if (p !== prev) onPath(p);
      h.classList.toggle('bloom-hide', !!S.hide);
      h.classList.toggle('bloom-dm', BLOOM_SEL.dmPath.test(p));
      var id = bloomReelId(p), blocked = false;
      if (BLOOM_SEL.feedPath.test(p)) blocked = true;
      else if (id) blocked = S.strict ? !fromDm : fromExplore;
      if (blocked) {
        showOverlay(); mute();
        if (lastCountPath !== p) { lastCountPath = p; hit(); }
      } else { hideOverlay(); lastCountPath = null; }
    } catch (e) {}
  }

  function active() {
    return S.on && !!bloomReelId(location.pathname) && (S.strict ? fromDm : !fromExplore);
  }
  function editable(el) {
    if (!el || !el.tagName) return false;
    var t = el.tagName;
    return t === 'INPUT' || t === 'TEXTAREA' || t === 'SELECT' || el.isContentEditable ||
      el.getAttribute('role') === 'textbox';
  }
  function bigScroll(el) {                 // small scrollers (comments) are fine; the feed is not
    for (var n = el; n && n !== document.documentElement; n = n.parentElement) {
      var cs = getComputedStyle(n);
      if (/(auto|scroll)/.test(cs.overflowY)) {
        return n.scrollHeight > n.clientHeight * BLOOM_SEL.bigScrollRatio;
      }
    }
    return true;
  }

  function swipe(e) {
    try {
      if (!active() || !bigScroll(e.target)) return;
      e.preventDefault(); e.stopPropagation(); violation(false);
    } catch (_) {}
  }
  addEventListener('wheel', swipe, { capture: true, passive: false });
  addEventListener('touchmove', swipe, { capture: true, passive: false });

  addEventListener('keydown', function (e) {
    try {
      if (!active() || e.ctrlKey || e.metaKey || e.altKey || editable(e.target)) return;
      var k = e.key;
      var nav = k === 'ArrowUp' || k === 'ArrowDown' || k === 'PageUp' || k === 'PageDown';
      var loose = (k === ' ' || k === 'j' || k === 'k' || k === 'J' || k === 'K') &&
        (e.target === document.body || e.target.tagName === 'VIDEO');
      if (nav || loose) { e.preventDefault(); e.stopPropagation(); violation(false); }
    } catch (_) {}
  }, true);

  addEventListener('click', function (e) {
    try {
      if (active()) {
        var t = e.target.closest && e.target.closest(BLOOM_SEL.nextPrev);
        if (t) { e.preventDefault(); e.stopPropagation(); violation(false); return; }
      }
      setTimeout(check, 50);
    } catch (_) {}
  }, true);
  addEventListener('popstate', function () { setTimeout(check, 0); });

  onPath(location.pathname);
  api.storage.local.get(D, function (v) { S = Object.assign({}, D, v); check(); });
  api.storage.onChanged.addListener(function (c) {
    ['on', 'strict', 'hide'].forEach(function (k) { if (c[k]) S[k] = c[k].newValue; });
    check();
  });
  setInterval(check, 500);
})();
