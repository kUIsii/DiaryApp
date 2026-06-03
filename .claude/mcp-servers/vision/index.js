import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import OpenAI from "openai";
import { readFileSync, existsSync } from "fs";
import { z } from "zod";

const API_KEY = process.env.QWEN_API_KEY;
const BASE_URL = process.env.QWEN_BASE_URL || "https://dashscope.aliyuncs.com/compatible-mode/v1";
const MODEL = process.env.QWEN_MODEL || "qwen-vl-max";

if (!API_KEY) {
  console.error("Error: QWEN_API_KEY environment variable is required");
  process.exit(1);
}

const client = new OpenAI({
  apiKey: API_KEY,
  baseURL: BASE_URL,
});

const server = new McpServer({
  name: "qwen-vision",
  version: "1.0.0",
});

server.tool(
  "analyze_image",
  "Analyze an image using Qwen vision model. Can describe UI designs, screenshots, photos, diagrams, etc.",
  {
    image_path: z.string().describe("Absolute path to the image file"),
    prompt: z
      .string()
      .optional()
      .describe("Optional custom prompt for analysis. Default: describe the image in detail"),
  },
  async ({ image_path, prompt }) => {
    try {
      if (!existsSync(image_path)) {
        return {
          content: [{ type: "text", text: `Error: File not found: ${image_path}` }],
          isError: true,
        };
      }

      const imageData = readFileSync(image_path);
      const base64 = imageData.toString("base64");
      const ext = image_path.toLowerCase().split(".").pop();
      const mimeType =
        ext === "png"
          ? "image/png"
          : ext === "gif"
            ? "image/gif"
            : ext === "webp"
              ? "image/webp"
              : "image/jpeg";

      const response = await client.chat.completions.create({
        model: MODEL,
        messages: [
          {
            role: "user",
            content: [
              {
                type: "image_url",
                image_url: { url: `data:${mimeType};base64,${base64}` },
              },
              {
                type: "text",
                text: prompt || "请详细描述这张图片的内容。如果是UI设计稿，请分析布局、配色、组件等。",
              },
            ],
          },
        ],
        max_tokens: 2000,
      });

      const text = response.choices[0]?.message?.content || "No response from model";
      return { content: [{ type: "text", text }] };
    } catch (err) {
      return {
        content: [{ type: "text", text: `Error: ${err.message}` }],
        isError: true,
      };
    }
  }
);

const transport = new StdioServerTransport();
await server.connect(transport);
