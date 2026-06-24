import unittest

from scripts.qwen_image_generate import (
    GenerationConfig,
    PRESETS,
    Preset,
    build_api_url,
    build_dashscope_api_url,
    build_payload,
    load_manifest,
    sanitize_filename,
)


class BuildApiUrlTest(unittest.TestCase):
    def test_builds_cn_beijing_generation_url_from_workspace(self):
        self.assertEqual(
            build_api_url("ws-123", "cn-beijing"),
            "https://ws-123.cn-beijing.maas.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation",
        )

    def test_builds_public_dashscope_generation_url_without_workspace(self):
        self.assertEqual(
            build_dashscope_api_url("cn-beijing"),
            "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation",
        )


class BuildPayloadTest(unittest.TestCase):
    def test_builds_qwen_image_payload_with_expected_defaults(self):
        preset = Preset(
            name="achievement_badge",
            system_suffix="游戏UI成就徽章图标，正视图，居中构图",
            negative_prompt="文字，水印，实物拍摄",
            size="2048*2048",
        )
        config = GenerationConfig(
            model="qwen-image-2.0-pro",
            prompt="珐琅金属材质，深青绿色与金色配色",
            negative_prompt="",
            size="",
            seed=123456,
            watermark=False,
            prompt_extend=False,
            n=1,
        )

        payload = build_payload(config, preset)

        self.assertEqual(payload["model"], "qwen-image-2.0-pro")
        self.assertEqual(payload["parameters"]["size"], "2048*2048")
        self.assertEqual(payload["parameters"]["seed"], 123456)
        self.assertFalse(payload["parameters"]["watermark"])
        self.assertFalse(payload["parameters"]["prompt_extend"])
        self.assertEqual(payload["parameters"]["n"], 1)
        self.assertIn("珐琅金属材质", payload["input"]["messages"][0]["content"][0]["text"])
        self.assertIn("游戏UI成就徽章图标", payload["input"]["messages"][0]["content"][0]["text"])
        self.assertEqual(
            payload["parameters"]["negative_prompt"],
            "文字，水印，实物拍摄",
        )

    def test_appends_custom_negative_prompt_to_preset_negative_prompt(self):
        preset = Preset(
            name="achievement_badge",
            system_suffix="精致徽章",
            negative_prompt="文字，水印",
            size="2048*2048",
        )
        config = GenerationConfig(
            model="qwen-image-2.0-pro",
            prompt="金色月桂叶",
            negative_prompt="模糊，塑料感",
            size="1024*1024",
            seed=None,
            watermark=False,
            prompt_extend=False,
            n=2,
        )

        payload = build_payload(config, preset)

        self.assertEqual(payload["parameters"]["size"], "1024*1024")
        self.assertEqual(
            payload["parameters"]["negative_prompt"],
            "文字，水印，模糊，塑料感",
        )
        self.assertNotIn("seed", payload["parameters"])
        self.assertEqual(payload["parameters"]["n"], 2)

    def test_build_payload_keeps_manifest_prompt_and_preset_suffix(self):
        preset = PRESETS["pet_character"]
        config = GenerationConfig(
            model="qwen-image-2.0-pro",
            prompt="温柔的月滴精灵，正视图",
            negative_prompt="廉价卡通感",
            size="2048*2048",
            seed=42,
            watermark=False,
            prompt_extend=False,
            n=1,
        )

        payload = build_payload(config, preset)

        self.assertIn("温柔的月滴精灵", payload["input"]["messages"][0]["content"][0]["text"])
        self.assertIn("治愈系游戏宠物角色立绘", payload["input"]["messages"][0]["content"][0]["text"])
        self.assertIn("廉价卡通感", payload["parameters"]["negative_prompt"])


class LoadManifestTest(unittest.TestCase):
    def test_load_manifest_returns_named_jobs(self):
        manifest = {
            "jobs": [
                {"name": "pet-main", "preset": "pet_character", "prompt": "温柔的月滴精灵"},
                {"name": "badge-rare", "preset": "achievement_badge", "prompt": "稀有成就徽章"},
            ]
        }

        jobs = load_manifest(manifest)

        self.assertEqual([job.name for job in jobs], ["pet-main", "badge-rare"])
        self.assertEqual(jobs[0].preset, "pet_character")
        self.assertEqual(jobs[1].count, 1)


class SanitizeFilenameTest(unittest.TestCase):
    def test_replaces_spaces_and_reserved_characters(self):
        self.assertEqual(
            sanitize_filename("rare badge: writing/star"),
            "rare-badge-writing-star",
        )


if __name__ == "__main__":
    unittest.main()
