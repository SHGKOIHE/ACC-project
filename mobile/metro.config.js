const { getDefaultConfig } = require('expo/metro-config');

const config = getDefaultConfig(__dirname);

// Exclude .omx (OMX state dir) from Metro's file watcher to prevent race
// conditions with lock files that are created/deleted by OMX hooks.
const existing = config.resolver.blockList;
config.resolver.blockList = [
  /[/\\]\.omx([/\\]|$)/,
  ...(Array.isArray(existing) ? existing : existing ? [existing] : []),
];

module.exports = config;
