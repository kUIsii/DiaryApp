#!/usr/bin/env python3
"""
Generate achievement images using AGNES AI image generation API.
Usage: python generate_achievement_images.py --api-key YOUR_KEY
"""
import argparse
import json
import os
import sys
import time
import urllib.request
import urllib.error

API_URL = "https://apihub.agnes-ai.com/v1/images/generations"
MODEL = "agnes-image-2.1-flash"
SIZE = "768x768"
OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "drawable-nodpi")

# Achievement key -> English prompt for image generation
# Style: soft watercolor illustration, pastel colors, gentle, dreamy, no text
PROMPTS = {
    "first_entry": "a single glowing quill pen writing on blank paper, soft watercolor, pastel blue and gold, gentle light, dreamy atmosphere",
    "entries_10": "a small stack of handwritten letters tied with ribbon, soft watercolor, warm pastel tones, cozy gentle mood",
    "entries_50": "an open journal filled with beautiful handwriting, surrounded by dried flowers, soft watercolor, warm pastel colors",
    "entries_100": "a tall stack of colorful journals with a golden bookmark, soft watercolor illustration, warm dreamy tones",
    "words_10000": "flowing calligraphy ink forming beautiful swirls on paper, soft watercolor, indigo and gold pastel tones",
    "words_100000": "an open book with words floating up like butterflies, soft watercolor, warm amber and cream tones, magical gentle mood",
    "tags_5": "colorful ribbon bookmarks of different pastel colors arranged neatly, soft watercolor illustration, gentle and warm",
    "images_10": "a photo album open with pressed flowers and small photographs, soft watercolor, pastel pink and green tones",
    "streak_7": "seven small candles lit in a row on a wooden windowsill, soft watercolor, warm golden light, gentle cozy atmosphere",
    "streak_30": "a glowing campfire with 30 tiny flames forming a circle, soft watercolor, warm orange and amber pastel tones",
    "daily_writer": "a bright lightning bolt made of golden ink above an open notebook, soft watercolor, warm yellow and white tones",
    "hundred_days": "a majestic golden laurel wreath made of delicate leaves, soft watercolor, warm gold and green pastel tones, elegant",
    "night_writer": "a crescent moon glowing softly over a desk with an open journal, soft watercolor, deep blue and silver pastel tones",
    "early_bird": "a golden rooster silhouette on top of a hill at dawn, soft watercolor, warm peach and gold pastel tones, peaceful morning mood",
    "night_poet": "stars forming a constellation above a glowing quill pen and candle, soft watercolor, deep purple and silver",
    "dawn_recorder": "misty dawn landscape with soft pink sky, a quill pen resting on a windowsill, soft watercolor, pastel rose and lavender",
    "morning_writer": "golden morning light streaming through curtains onto an open journal, soft watercolor, warm amber and cream tones",
    "weekday_killer": "seven small colorful cards arranged in a circle like a clock, soft watercolor, rainbow pastel tones, gentle",
    "time_capsule_master": "a beautiful glass jar filled with 12 tiny glowing scrolls, soft watercolor, warm golden and amber tones, magical",
    "moods_5": "five overlapping watercolor circles in different pastel emotions colors, soft gentle abstract art, dreamy mood",
    "mood_palette": "an artist palette with six beautiful pastel paint drops, soft watercolor illustration, colorful gentle tones",
    "optimist": "a bright sun shining through soft clouds with tiny flowers blooming, soft watercolor, warm yellow and pink pastel",
    "deep_thinker": "a calm deep ocean with gentle waves under moonlight, soft watercolor, deep blue and silver pastel tones, serene",
    "calm_sea": "a serene calm sea horizon with soft gentle waves, pastel blue and turquoise watercolor, peaceful tranquil mood",
    "mood_rollercoaster": "gentle rolling hills with ups and downs covered in flowers, soft watercolor, colorful pastel tones, whimsical",
    "all_weather": "four gentle weather symbols sun rain wind snow arranged in harmony, soft watercolor, pastel tones, balanced",
    "rain_collector": "raindrops falling into a beautiful glass jar creating tiny ripples, soft watercolor, blue and silver pastel tones",
    "snow_writer": "gentle snowflakes falling on an open journal on a windowsill, soft watercolor, white and ice blue pastel tones",
    "storm_writer": "a cozy room with rain on the window and a candle next to an open journal, soft watercolor, warm amber tones",
    "sunny_recorder": "a bright sunny day with flowers blooming and a journal on grass, soft watercolor, warm yellow and green pastel",
    "fearless_recorder": "a brave umbrella standing in gentle rain with a rainbow emerging, soft watercolor, pastel rainbow tones, hopeful",
    "thousand_words": "a flowing river of golden ink words on parchment, soft watercolor, warm amber and cream pastel tones",
    "brief_master": "a minimalist haiku written on a small beautiful card, soft watercolor, clean white and soft gray tones, elegant",
    "photo_diary": "a collage of small beautiful photos with pressed flowers, soft watercolor, warm pastel pink and green tones",
    "collector": "a beautiful wooden bookshelf with carefully arranged journals, soft watercolor, warm brown and gold pastel tones",
    "fifty_thousand_words": "a tall tower made of stacked books with a glowing light on top, soft watercolor, warm amber and cream tones",
    "favorite_1": "a single heart-shaped bookmark on an open journal page, soft watercolor, warm pink and cream pastel tones",
    "favorites_10": "ten small glowing hearts floating above an open journal, soft watercolor, warm pink and gold pastel tones",
    "returnee": "a door opening to a beautiful garden path, soft watercolor, warm green and gold pastel tones, hopeful atmosphere",
    "flash_writer": "a lightning bolt striking a quill pen creating a spark, soft watercolor, bright yellow and white pastel tones",
    "deep_writer": "an hourglass with golden sand flowing slowly next to an open journal, soft watercolor, warm amber and brown tones",
    "twin_stars": "two bright stars side by side in a gentle night sky, soft watercolor, deep blue and silver pastel tones, magical",
    "time_traveler": "a beautiful vintage clock with gears and flowers, soft watercolor, warm gold and pastel tones, nostalgic",
    "new_year_eve": "twelve glowing candles arranged in a circle with a golden clock, soft watercolor, deep blue and golden pastel tones, festive",
    "midnight_bell": "a glowing bell ringing at midnight with stars around it, soft watercolor, deep purple and gold pastel tones",
    "full_moon": "a luminous full moon over a quiet landscape, soft watercolor, silver and lavender pastel tones, dreamy serene",
    "first_echo": "a gentle echo ripple in a quiet pond reflecting a journal, soft watercolor, blue and silver pastel tones, peaceful",
    "legendary_entries_500": "a magnificent golden scroll unfurling with 500 tiny stars, soft watercolor, warm gold and cream tones, majestic",
    "legendary_streak_365": "a glowing calendar with 365 days marked in golden light forming a circle, soft watercolor, warm amber tones",
    "legendary_words_million": "a massive open book with a million golden words flowing like a river, soft watercolor, warm gold tones, epic",
    "legendary_all_categories": "eight beautiful gemstones of different colors arranged in a circle, soft watercolor, rainbow pastel tones, magnificent",
}


def generate_image(api_key: str, prompt: str, output_path: str, retries: int = 3) -> bool:
    """Generate a single image using AGNES AI API."""
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {api_key}",
    }
    body = json.dumps({
        "model": MODEL,
        "prompt": prompt,
        "n": 1,
        "size": SIZE,
    }).encode("utf-8")

    for attempt in range(retries):
        try:
            req = urllib.request.Request(API_URL, data=body, headers=headers, method="POST")
            with urllib.request.urlopen(req, timeout=120) as resp:
                data = json.loads(resp.read().decode("utf-8"))

            image_url = data.get("data", [{}])[0].get("url")
            if not image_url:
                print(f"  [WARN] No URL in response, retrying...")
                time.sleep(5)
                continue

            # Download the image
            img_req = urllib.request.Request(image_url)
            with urllib.request.urlopen(img_req, timeout=60) as img_resp:
                img_data = img_resp.read()

            with open(output_path, "wb") as f:
                f.write(img_data)

            size_kb = len(img_data) / 1024
            print(f"  [OK] Saved ({size_kb:.0f} KB)")
            return True

        except urllib.error.HTTPError as e:
            error_body = e.read().decode("utf-8", errors="replace")
            print(f"  [ERROR] HTTP {e.code}: {error_body[:200]}")
            if e.code == 429:
                wait = 30 * (attempt + 1)
                print(f"  Rate limited, waiting {wait}s...")
                time.sleep(wait)
            elif attempt < retries - 1:
                time.sleep(10)
        except Exception as e:
            print(f"  [ERROR] {e}")
            if attempt < retries - 1:
                time.sleep(10)

    return False


def main():
    parser = argparse.ArgumentParser(description="Generate achievement images via AGNES AI")
    parser.add_argument("--api-key", required=True, help="AGNES AI API key")
    parser.add_argument("--only", nargs="*", help="Only generate for these achievement keys")
    parser.add_argument("--dry-run", action="store_true", help="Print prompts without generating")
    args = parser.parse_args()

    os.makedirs(OUTPUT_DIR, exist_ok=True)

    achievements = PROMPTS if not args.only else {k: v for k, v in PROMPTS.items() if k in args.only}

    print(f"Generating {len(achievements)} achievement images...")
    print(f"Output: {OUTPUT_DIR}\n")

    success = 0
    failed = []

    for i, (key, prompt) in enumerate(achievements.items(), 1):
        output_path = os.path.join(OUTPUT_DIR, f"achievement_{key}.webp")
        # Check if already exists
        if os.path.exists(output_path) and not args.dry_run:
            print(f"[{i}/{len(achievements)}] {key} - SKIP (already exists)")
            success += 1
            continue

        print(f"[{i}/{len(achievements)}] {key}")
        print(f"  Prompt: {prompt[:80]}...")

        if args.dry_run:
            print(f"  [DRY RUN] Would save to: {output_path}")
            success += 1
            continue

        if generate_image(args.api_key, prompt, output_path):
            success += 1
        else:
            failed.append(key)

        # Small delay between requests to be polite
        if i < len(achievements):
            time.sleep(2)

    print(f"\n{'='*50}")
    print(f"Done: {success}/{len(achievements)} succeeded")
    if failed:
        print(f"Failed: {', '.join(failed)}")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
