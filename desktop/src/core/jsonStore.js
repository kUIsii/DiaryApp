import fs from "node:fs";
import path from "node:path";

export class JsonStore {
  constructor(filePath) {
    this.filePath = filePath;
  }

  load(fallback = null) {
    if (!fs.existsSync(this.filePath)) return fallback;
    const raw = fs.readFileSync(this.filePath, "utf8");
    if (!raw.trim()) return fallback;
    return JSON.parse(raw);
  }

  save(state) {
    fs.mkdirSync(path.dirname(this.filePath), { recursive: true });
    const tempPath = `${this.filePath}.tmp`;
    fs.writeFileSync(tempPath, JSON.stringify(state, null, 2), "utf8");
    fs.renameSync(tempPath, this.filePath);
    return state;
  }
}
