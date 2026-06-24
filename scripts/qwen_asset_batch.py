#!/usr/bin/env python3
"""
Batch wrapper for local Qwen image generation manifests.
"""

from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path

if __package__ in (None, ""):
    sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from scripts.qwen_image_generate import (
    DEFAULT_MODEL,
    DEFAULT_REGION,
    DEFAULT_OUTPUT_DIR,
    PRESETS,
    GenerationConfig,
    download_image,
    extract_image_urls,
    invoke_generation,
    load_manifest,
    require_env,
    sanitize_filename,
    build_payload,
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate Qwen assets from a manifest file.")
    parser.add_argument("--manifest", required=True, help="Path to the JSON manifest.")
    parser.add_argument("--output-dir", default=str(DEFAULT_OUTPUT_DIR), help="Directory to save generated images.")
    parser.add_argument("--model", default=DEFAULT_MODEL, help="Override model for all jobs.")
    parser.add_argument("--print-payload", action="store_true", help="Print request payloads and exit.")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    manifest_path = Path(args.manifest)
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    jobs = load_manifest(manifest)
    output_dir = Path(args.output_dir)

    if args.print_payload:
        for job in jobs:
            preset = PRESETS[job.preset]
            config = GenerationConfig(
                model=args.model,
                prompt=job.prompt,
                negative_prompt=job.negative_prompt,
                size=job.size,
                seed=job.seed,
                watermark=False,
                prompt_extend=False,
                n=job.count,
            )
            payload = build_payload(config, preset)
            print(json.dumps({"job": job.name, "payload": payload}, ensure_ascii=False, indent=2))
        return 0

    api_key = require_env("DASHSCOPE_API_KEY")
    workspace_id = os.environ.get("BAILIAN_WORKSPACE_ID", "").strip() or None
    region = os.environ.get("BAILIAN_REGION", DEFAULT_REGION).strip() or DEFAULT_REGION

    saved_paths: list[Path] = []
    for job in jobs:
        preset = PRESETS[job.preset]
        config = GenerationConfig(
            model=args.model,
            prompt=job.prompt,
            negative_prompt=job.negative_prompt,
            size=job.size,
            seed=job.seed,
            watermark=False,
            prompt_extend=False,
            n=job.count,
        )
        payload = build_payload(config, preset)
        result = invoke_generation(api_key, workspace_id, region, payload)
        urls = extract_image_urls(result)
        if not urls:
            print(json.dumps(result, ensure_ascii=False, indent=2))
            raise RuntimeError(f"No image URLs returned by the generation API for job: {job.name}")

        base_name = sanitize_filename(job.name)
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
