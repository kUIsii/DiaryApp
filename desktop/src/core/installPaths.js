import fs from "node:fs";
import path from "node:path";

const WINDOWS_DEFAULT_ROOT = "D:\\DiaryApp\\Desktop";

export function resolveDesktopInstallRoot({
  platform = process.platform,
  env = process.env,
  existsSync = fs.existsSync
} = {}) {
  const override = cleanInstallRoot(env.DIARYAPP_DESKTOP_HOME);
  if (override) return override;
  if (platform === "win32" && existsSync("D:\\")) return WINDOWS_DEFAULT_ROOT;
  return null;
}

export function resolveDesktopUserDataPath(options = {}) {
  const root = resolveDesktopInstallRoot(options);
  if (!root) return null;
  const platform = options.platform ?? process.platform;
  return platform === "win32"
    ? path.win32.join(root, "UserData")
    : path.join(root, "UserData");
}

function cleanInstallRoot(value) {
  const trimmed = String(value ?? "").trim();
  if (!trimmed) return "";
  if (/^[A-Za-z]:[\\/]?$/.test(trimmed)) {
    return `${trimmed[0].toUpperCase()}:\\`;
  }
  return trimmed.replace(/[\\/]+$/, "");
}
