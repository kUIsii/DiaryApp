import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { JsonStore } from "./jsonStore.js";

test("JsonStore saves and loads desktop state from disk", () => {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), "diary-desktop-store-"));
  const store = new JsonStore(path.join(dir, "state.json"));
  const state = { tasks: [{ id: "a", title: "本地任务" }], settings: { theme: "focus" } };

  store.save(state);
  const loaded = store.load();

  assert.deepEqual(loaded, state);
});

test("JsonStore returns fallback when the file does not exist", () => {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), "diary-desktop-store-"));
  const store = new JsonStore(path.join(dir, "missing.json"));

  assert.deepEqual(store.load({ tasks: [] }), { tasks: [] });
});
