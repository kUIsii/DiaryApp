import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";
import { writeFileSync, mkdirSync, existsSync } from "fs";
import { join } from "path";
import { homedir } from "os";

const API_KEY = process.env.QWEN_API_KEY;
const MODEL = process.env.IMAGE_MODEL || "wan2.6-t2i";
const OUTPUT_DIR = process.env.OUTPUT_DIR || join(homedir(), "Desktop", "generated-images");

if (!API_KEY) {
  console.error("Error: QWEN_API_KEY environment variable is required");
  process.exit(1);
}

if (!existsSync(OUTPUT_DIR)) {
  mkdirSync(OUTPUT_DIR, { recursive: true });
}

async function generateImage(prompt, size, n) {
  // 阿里云 DashScope 图片生成 API (同步模式 - wan2.6)
  const response = await fetch(
    "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${API_KEY}`,
      },
      body: JSON.stringify({
        model: MODEL,
        input: {
          messages: [
            {
              role: "user",
              content: [{ text: prompt }],
            },
          ],
        },
        parameters: {
          size: size || "1024*1024",
          n: n || 1,
          prompt_extend: true,
          watermark: false,
        },
      }),
    }
  );

  if (!response.ok) {
    const errText = await response.text();
    throw new Error(`API error ${response.status}: ${errText}`);
  }

  const data = await response.json();

  // 提取图片 URL
  const images = [];
  if (data.output?.choices) {
    for (const choice of data.output.choices) {
      if (choice.message?.content) {
        for (const item of choice.message.content) {
          if (item.image) {
            images.push({ url: item.image });
          }
        }
      }
    }
  }

  return images;
}

const server = new McpServer({
  name: "qwen-image-gen",
  version: "1.0.0",
});

server.tool(
  "generate_image",
  "Generate images using Qwen image model. Returns saved file paths.",
  {
    prompt: z.string().describe("Description of the image to generate, in English or Chinese"),
    size: z
      .enum(["1024*1024", "720*1280", "1280*720"])
      .optional()
      .describe("Image size. Default: 1024*1024 (square), 720*1280 (portrait), 1280*720 (landscape)"),
    filename: z
      .string()
      .optional()
      .describe("Custom filename without extension. Default: auto-generated from timestamp"),
  },
  async ({ prompt, size, filename }) => {
    try {
      const results = await generateImage(prompt, size, 1);

      if (!results.length) {
        return {
          content: [{ type: "text", text: "Error: No image was generated" }],
          isError: true,
        };
      }

      const savedPaths = [];
      for (const item of results) {
        let imagePath;
        if (item.url) {
          // Download from URL
          const resp = await fetch(item.url);
          const buffer = Buffer.from(await resp.arrayBuffer());
          const name = filename || `img_${Date.now()}`;
          imagePath = join(OUTPUT_DIR, `${name}.png`);
          writeFileSync(imagePath, buffer);
        } else if (item.b64_json) {
          const buffer = Buffer.from(item.b64_json, "base64");
          const name = filename || `img_${Date.now()}`;
          imagePath = join(OUTPUT_DIR, `${name}.png`);
          writeFileSync(imagePath, buffer);
        }
        if (imagePath) savedPaths.push(imagePath);
      }

      return {
        content: [
          {
            type: "text",
            text: `Image saved to: ${savedPaths.join(", ")}`,
          },
        ],
      };
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
