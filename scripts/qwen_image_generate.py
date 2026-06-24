#!/usr/bin/env python3
"""
Local Qwen image generation CLI for Alibaba Bailian.

Environment variables:
  DASHSCOPE_API_KEY      Required. Bailian / DashScope API key.
  BAILIAN_WORKSPACE_ID   Optional. If present, uses workspace-specific MaaS endpoint.
  BAILIAN_REGION         Optional. Defaults to cn-beijing.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.request
from dataclasses import dataclass
from pathlib import Path
from typing import Any


DEFAULT_MODEL = "qwen-image-2.0-pro"
DEFAULT_REGION = "cn-beijing"
DEFAULT_SIZE = "2048*2048"
DEFAULT_OUTPUT_DIR = Path("output") / "qwen-images"
DEFAULT_DASHSCOPE_BASE_URL = "https://dashscope.aliyuncs.com"


@dataclass(frozen=True)
class Preset:
    name: str
    system_suffix: str
    negative_prompt: str
    size: str = DEFAULT_SIZE


@dataclass(frozen=True)
class GenerationConfig:
    model: str
    prompt: str
    negative_prompt: str
    size: str
    seed: int | None
    watermark: bool
    prompt_extend: bool
    n: int


@dataclass(frozen=True)
class ManifestJob:
    name: str
    preset: str
    prompt: str
    negative_prompt: str = ""
    size: str = ""
    seed: int | None = None
    count: int = 1


PRESETS: dict[str, Preset] = {
    "achievement_badge": Preset(
        name="achievement_badge",
        system_suffix=(
            "游戏UI成就徽章图标，正视图，居中构图，单主体，精致收藏品风格，"
            "非实物商品摆拍，边缘锐利，细节丰富，适合作为APP内高品质成就徽章素材"
        ),
        negative_prompt=(
            "文字，水印，AI生成角标，实物拍摄，桌面，背景杂物，透视角度，廉价塑料感，"
            "模糊，边缘发虚，构图混乱，多余装饰"
        ),
    ),
    "pet_character": Preset(
        name="pet_character",
        system_suffix=(
            "治愈系游戏宠物角色立绘，正视图，居中构图，柔和高级绘本风，单角色主体，"
            "情绪陪伴感强，适合作为APP养成宠物主视觉"
        ),
        negative_prompt=(
            "文字，水印，复杂背景，写实摄影，廉价卡通感，低幼感，诡异表情，"
            "多角色，边缘发虚，肢体错乱"
        ),
    ),
    "island_asset": Preset(
        name="island_asset",
        system_suffix=(
            "游戏场景装饰素材，单个物件，正视或轻微等距视角，绘本质感，"
            "高细节，适合作为心情小岛装饰资产"
        ),
        negative_prompt=(
            "文字，水印，复杂场景背景，人物，照片质感，边缘模糊，廉价素材感"
        ),
    ),
}


def sanitize_filename(value: str) -> str:
    value = value.strip().lower()
    value = re.sub(r"[^\w\-]+", "-", value, flags=re.UNICODE)
    value = re.sub(r"-{2,}", "-", value).strip("-")
    return value or "image"


def build_api_url(workspace_id: str, region: str) -> str:
    return (
        f"https://{workspace_id}.{region}.maas.aliyuncs.com/"
        "api/v1/services/aigc/multimodal-generation/generation"
    )


def build_dashscope_api_url(region: str) -> str:
    if region == "cn-beijing":
        base_url = DEFAULT_DASHSCOPE_BASE_URL
    elif region == "ap-southeast-1":
        base_url = "https://dashscope-intl.aliyuncs.com"
    elif region == "us-east-1":
        base_url = "https://dashscope-us.aliyuncs.com"
    elif region == "cn-hongkong":
        base_url = "https://cn-hongkong.dashscope.aliyuncs.com"
    else:
        base_url = DEFAULT_DASHSCOPE_BASE_URL

    return f"{base_url}/api/v1/services/aigc/multimodal-generation/generation"


def merge_negative_prompt(preset_negative: str, custom_negative: str) -> str:
    parts = [part.strip() for part in [preset_negative, custom_negative] if part.strip()]
    return "，".join(parts)


def build_prompt(prompt: str, preset: Preset) -> str:
    return f"{prompt}，{preset.system_suffix}"


def build_payload(config: GenerationConfig, preset: Preset) -> dict[str, Any]:
    parameters: dict[str, Any] = {
        "size": config.size or preset.size,
        "watermark": config.watermark,
        "prompt_extend": config.prompt_extend,
        "n": config.n,
        "negative_prompt": merge_negative_prompt(preset.negative_prompt, config.negative_prompt),
    }
    if config.seed is not None:
        parameters["seed"] = config.seed

    return {
        "model": config.model,
        "input": {
            "messages": [
                {
                    "role": "user",
                    "content": [{"text": build_prompt(config.prompt, preset)}],
                }
            ]
        },
        "parameters": parameters,
    }


def load_manifest(data: dict[str, Any]) -> list[ManifestJob]:
    jobs: list[ManifestJob] = []
    for raw in data.get("jobs", []):
        jobs.append(
            ManifestJob(
                name=raw["name"],
                preset=raw["preset"],
                prompt=raw["prompt"],
                negative_prompt=raw.get("negative_prompt", ""),
                size=raw.get("size", ""),
                seed=raw.get("seed"),
                count=raw.get("count", 1),
            )
        )
    return jobs


def read_json_response(response: urllib.request.addinfourl) -> dict[str, Any]:
    charset = response.headers.get_content_charset() or "utf-8"
    return json.loads(response.read().decode(charset))


def invoke_generation(
    api_key: str,
    workspace_id: str | None,
    region: str,
    payload: dict[str, Any]
) -> dict[str, Any]:
    endpoint = build_api_url(workspace_id, region) if workspace_id else build_dashscope_api_url(region)
    request = urllib.request.Request(
        endpoint,
        data=json.dumps(payload).encode("utf-8"),
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=120) as response:
            return read_json_response(response)
    except urllib.error.HTTPError as exc:
        body = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"Generation request failed with HTTP {exc.code}: {body}") from exc


def extract_image_urls(result: dict[str, Any]) -> list[str]:
    urls: list[str] = []
    for choice in result.get("output", {}).get("choices", []):
        message = choice.get("message", {})
        for item in message.get("content", []):
            image_url = item.get("image")
            if image_url:
                urls.append(image_url)
    return urls


def download_image(url: str, output_path: Path) -> None:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with urllib.request.urlopen(url, timeout=120) as response:
        output_path.write_bytes(response.read())


def require_env(name: str) -> str:
    value = os.environ.get(name, "").strip()
    if not value:
        raise RuntimeError(f"Missing required environment variable: {name}")
    return value


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate images with Alibaba Bailian Qwen image models.")
    parser.add_argument("prompt", help="Main image prompt.")
    parser.add_argument("--preset", choices=sorted(PRESETS.keys()), default="achievement_badge")
    parser.add_argument("--model", default=DEFAULT_MODEL)
    parser.add_argument("--negative-prompt", default="")
    parser.add_argument("--size", default="")
    parser.add_argument("--seed", type=int, default=None)
    parser.add_argument("--count", type=int, default=1)
    parser.add_argument("--output-dir", default=str(DEFAULT_OUTPUT_DIR))
    parser.add_argument("--name", default="")
    parser.add_argument("--watermark", action="store_true", help="Enable watermark. Disabled by default.")
    parser.add_argument("--prompt-extend", action="store_true", help="Enable server-side prompt extension.")
    parser.add_argument("--print-payload", action="store_true", help="Print request payload and exit.")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    preset = PRESETS[args.preset]
    config = GenerationConfig(
        model=args.model,
        prompt=args.prompt,
        negative_prompt=args.negative_prompt,
        size=args.size,
        seed=args.seed,
        watermark=args.watermark,
        prompt_extend=args.prompt_extend,
        n=args.count,
    )
    payload = build_payload(config, preset)

    if args.print_payload:
        print(json.dumps(payload, ensure_ascii=False, indent=2))
        return 0

    api_key = require_env("DASHSCOPE_API_KEY")
    workspace_id = os.environ.get("BAILIAN_WORKSPACE_ID", "").strip() or None
    region = os.environ.get("BAILIAN_REGION", DEFAULT_REGION).strip() or DEFAULT_REGION

    result = invoke_generation(api_key, workspace_id, region, payload)
    urls = extract_image_urls(result)
    if not urls:
        print(json.dumps(result, ensure_ascii=False, indent=2))
        raise RuntimeError("No image URLs returned by the generation API.")

    output_dir = Path(args.output_dir)
    base_name = sanitize_filename(args.name or args.prompt[:48])
    saved_paths: list[Path] = []
    for index, url in enumerate(urls, start=1):
        suffix = f"-{index}" if len(urls) > 1 else ""
        output_path = output_dir / f"{base_name}{suffix}.png"
        download_image(url, output_path)
        saved_paths.append(output_path)

    for path in saved_paths:
        print(path.resolve())
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except RuntimeError as exc:
        print(str(exc), file=sys.stderr)
        raise SystemExit(1)
