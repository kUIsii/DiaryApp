import test from "node:test";
import assert from "node:assert/strict";
import {
  resolveDesktopInstallRoot,
  resolveDesktopUserDataPath
} from "./installPaths.js";

test("resolveDesktopInstallRoot prefers D drive on Windows when available", () => {
  const result = resolveDesktopInstallRoot({
    platform: "win32",
    env: {},
    existsSync: (target) => target === "D:\\"
  });

  assert.equal(result, "D:\\DiaryApp\\Desktop");
});

test("resolveDesktopInstallRoot respects explicit environment override", () => {
  const result = resolveDesktopInstallRoot({
    platform: "win32",
    env: { DIARYAPP_DESKTOP_HOME: "E:\\Portable\\DiaryApp" },
    existsSync: () => true
  });

  assert.equal(result, "E:\\Portable\\DiaryApp");
});

test("resolveDesktopUserDataPath stores runtime data under the preferred install root", () => {
  const result = resolveDesktopUserDataPath({
    platform: "win32",
    env: {},
    existsSync: (target) => target === "D:\\"
  });

  assert.equal(result, "D:\\DiaryApp\\Desktop\\UserData");
});

test("resolveDesktopUserDataPath returns null when no preferred root is available", () => {
  const result = resolveDesktopUserDataPath({
    platform: "linux",
    env: {},
    existsSync: () => false
  });

  assert.equal(result, null);
});
