# Bloom for Desktop

Blocks every way into Instagram Reels except a reel a friend sent you in a chat.
Works on instagram.com in Chrome, Edge and Firefox. Free, no network, open source.

## Install
**Chrome / Edge:** unzip `Bloom-extension.zip` (or use this folder), open `chrome://extensions` (or `edge://extensions`), turn on **Developer mode**, click **Load unpacked**, choose the folder.

**Firefox:** unzip `Bloom-extension-firefox.zip`, open `about:debugging` -> **This Firefox** -> **Load Temporary Add-on** -> pick `manifest.json`. Then open `about:addons` -> Bloom -> **Permissions** and allow access to instagram.com. (Temporary add-ons reset when Firefox restarts; submit to AMO for a permanent install.)

## How it behaves
- Reel opened from a chat: plays; like and reply work. Next-video wheel, swipe, arrow keys, PageUp/Down, Space/J/K and next arrows are blocked with a small toast. A second try within 5 s sends you back to your chats.
- `/reels/` feed: a black overlay. In **Strict mode** (default) any reel not opened from a chat is blocked too. In **Relaxed mode** only the feed and reels opened from Explore are blocked.
- Reels links are hidden (toggle in the popup). Typing, Stories, DMs and profiles are never touched.
- Unsure? It does nothing. Redirects are limited to 2 per 10 seconds.

## If Instagram changes
Fix `src/content/selectors.js`. It holds every URL pattern and selector.

## QA checklist
1. Open a reel from a DM: plays, like/reply work.
2. ArrowDown, PageDown, J, wheel, trackpad swipe: same reel stays, toast appears.
3. Second attempt within 5 s: you land in the inbox.
4. `/reels/`: overlay. Explore reel in Strict: overlay. In Relaxed: Explore reel overlay, profile reel plays.
5. Type in a DM or search using arrows, space, J/K: unaffected.
6. Popup OFF: Instagram returns to normal instantly.
7. Network tab: no requests except instagram.com.

## Store submission checklist
Short and long description, 1280x800 screenshots (popup, overlay, toast), privacy statement (see PRIVACY.md: no data collected), single purpose: "Block Instagram Reels scrolling".
