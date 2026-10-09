/* Every fragile, Instagram-specific bit lives in this file.
   If Instagram changes something and Bloom stops working, fix it here. */
var BLOOM_SEL = {
  // /reel/{id}/ , /reels/{id}/ , /{user}/reel/{id}/
  reelPath: [/^\/reels?\/([A-Za-z0-9_-]+)\/?$/, /^\/[^/]+\/reels?\/([A-Za-z0-9_-]+)\/?$/],
  feedPath: /^\/reels\/?$/,          // the endless Reels feed
  dmPath: /^\/direct\//,             // chats
  explorePath: /^\/explore/,
  // Next / previous arrows inside the reel viewer
  nextPrev: '[aria-label*="next" i],[aria-label*="previous" i]',
  // A scroll container taller than this many screens is "the feed", not a comment list
  bigScrollRatio: 1.5
};
function bloomReelId(p) {
  for (var i = 0; i < BLOOM_SEL.reelPath.length; i++) {
    var m = p.match(BLOOM_SEL.reelPath[i]);
    if (m) return m[1];
  }
  return null;
}
