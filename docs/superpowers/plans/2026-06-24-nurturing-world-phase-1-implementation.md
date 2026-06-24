# Nurturing World Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver the first production-quality slice of the nurturing world by shipping a reliable Qwen asset workflow, a redesigned nurturing-world entry, and a visually reworked pet screen that establishes the new art direction.

**Architecture:** Phase 1 introduces a small `ui/nurturing` presentation layer for shared preview state, keeps asset generation isolated in `scripts/`, and reworks the pet screen around a scene-first layout without rewriting the full data model. The plan deliberately produces a complete user-visible improvement before touching the island and achievement screens in follow-up phases.

**Tech Stack:** Kotlin + Jetpack Compose, existing Room/ViewModel stack, Python 3.12 local tooling for Qwen asset generation, JUnit 4 for JVM tests.

---

## File Structure

### New files

- `app/src/main/java/com/diary/app/ui/nurturing/NurturingWorldPreviewState.kt`
  - Shared preview models and mapping helpers for the nurturing-world entry card.
- `app/src/main/java/com/diary/app/ui/nurturing/NurturingWorldEntryCard.kt`
  - New rich entry card shown inside `ToolsScreen`.
- `app/src/main/java/com/diary/app/ui/pet/PetSceneCard.kt`
  - Scene-first pet hero section used by `PetScreen`.
- `app/src/main/java/com/diary/app/ui/pet/PetMoodCopy.kt`
  - Copy helpers for concise emotional messaging on the pet page.
- `app/src/test/java/com/diary/app/ui/nurturing/NurturingWorldPreviewStateTest.kt`
  - JVM tests for preview-state mapping.
- `scripts/qwen_asset_batch.py`
  - Batch wrapper over `qwen_image_generate.py` for generating prompt sets from manifests.
- `scripts/assets/nurturing_world_phase1.json`
  - Prompt manifest for pet, badge, and entry concept assets.

### Modified files

- `app/src/main/java/com/diary/app/ui/tools/ToolsScreen.kt`
  - Replace the current three simple rows under “养成世界” with a rich world entry card plus secondary shortcuts.
- `app/src/main/java/com/diary/app/ui/pet/PetScreen.kt`
  - Convert the page from a stacked status panel into a scene-first companion page.
- `app/src/main/java/com/diary/app/ui/pet/PetViewModel.kt`
  - Expose any small derived values needed by the new layout without changing persistence behavior.
- `scripts/qwen_image_generate.py`
  - Add manifest-friendly output metadata and preset polish needed for production asset work.
- `scripts/tests/test_qwen_image_generate.py`
  - Extend tests for manifest/batch-related behavior.

### Files intentionally left for later phases

- `app/src/main/java/com/diary/app/ui/island/IslandScreen.kt`
- `app/src/main/java/com/diary/app/ui/achievement/AchievementScreen.kt`

Those are explicitly deferred so Phase 1 stays shippable and reviewable.

---

## Task 1: Harden The Local Qwen Asset Workflow

**Files:**
- Modify: `scripts/qwen_image_generate.py`
- Create: `scripts/qwen_asset_batch.py`
- Create: `scripts/assets/nurturing_world_phase1.json`
- Test: `scripts/tests/test_qwen_image_generate.py`

- [ ] **Step 1: Write the failing tests for manifest-driven batch generation**

```python
def test_load_manifest_returns_named_jobs(self):
    manifest = {
        "jobs": [
            {"name": "pet-main", "preset": "pet_character", "prompt": "温柔的月滴精灵"},
            {"name": "badge-rare", "preset": "achievement_badge", "prompt": "稀有成就徽章"}
        ]
    }

    jobs = load_manifest(manifest)

    self.assertEqual([job.name for job in jobs], ["pet-main", "badge-rare"])
    self.assertEqual(jobs[0].preset, "pet_character")


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
```

- [ ] **Step 2: Run the Python tests to verify they fail for the right reason**

Run:

```powershell
@'
import unittest
loader = unittest.TestLoader()
suite = loader.discover("scripts/tests")
runner = unittest.TextTestRunner()
result = runner.run(suite)
raise SystemExit(0 if result.wasSuccessful() else 1)
'@ | python -
```

Expected:

- FAIL in `test_load_manifest_returns_named_jobs`
- Error mentions missing `load_manifest` or equivalent batch helper

- [ ] **Step 3: Implement manifest loading and batch CLI support**

Add these data helpers to `scripts/qwen_image_generate.py`:

```python
@dataclass(frozen=True)
class ManifestJob:
    name: str
    preset: str
    prompt: str
    negative_prompt: str = ""
    size: str = ""
    seed: int | None = None
    count: int = 1


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
```

Create `scripts/qwen_asset_batch.py` with a minimal wrapper:

```python
import json
from pathlib import Path

from scripts.qwen_image_generate import PRESETS, GenerationConfig, build_payload, invoke_generation, extract_image_urls, download_image, load_manifest, require_env


def main() -> int:
    manifest_path = Path(args.manifest)
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    jobs = load_manifest(manifest)
    api_key = require_env("DASHSCOPE_API_KEY")
    workspace_id = require_env("BAILIAN_WORKSPACE_ID")
    region = os.environ.get("BAILIAN_REGION", "cn-beijing")
    ...
```

Create `scripts/assets/nurturing_world_phase1.json`:

```json
{
  "jobs": [
    {
      "name": "pet-main-moon-drop",
      "preset": "pet_character",
      "prompt": "温柔的月滴精灵，夜间陪伴感，正视图，圆中带挺，柔光半透明",
      "negative_prompt": "史莱姆感，实验品感，低幼，诡异表情",
      "seed": 424242
    },
    {
      "name": "badge-rare-observer",
      "preset": "achievement_badge",
      "prompt": "高级游戏成就徽章，观察者主题，植物纹样与月相边饰，深墨绿与暗月金",
      "negative_prompt": "实物摄影，桌面摆拍，文字标签，AI角标",
      "seed": 515151
    }
  ]
}
```

- [ ] **Step 4: Run tests again and verify they pass**

Run:

```powershell
@'
import unittest
loader = unittest.TestLoader()
suite = loader.discover("scripts/tests")
runner = unittest.TextTestRunner()
result = runner.run(suite)
raise SystemExit(0 if result.wasSuccessful() else 1)
'@ | python -
```

Expected:

- PASS
- All script tests green

- [ ] **Step 5: Smoke-test one real Qwen generation**

Run:

```powershell
python scripts/qwen_image_generate.py "高级游戏UI成就徽章，观察者主题，墨松绿与暗月金，正视图，单主体" --preset achievement_badge --name smoke-badge --output-dir output/qwen-smoke --seed 515151
```

Expected:

- One `.png` file appears under `output/qwen-smoke`
- No API or workspace-id error

- [ ] **Step 6: Commit the asset-workflow slice**

```bash
git add scripts/qwen_image_generate.py scripts/qwen_asset_batch.py scripts/assets/nurturing_world_phase1.json scripts/tests/test_qwen_image_generate.py
git commit -m "feat: add nurturing world qwen asset workflow"
```

---

## Task 2: Build A Shared Nurturing World Preview Model

**Files:**
- Create: `app/src/main/java/com/diary/app/ui/nurturing/NurturingWorldPreviewState.kt`
- Test: `app/src/test/java/com/diary/app/ui/nurturing/NurturingWorldPreviewStateTest.kt`

- [ ] **Step 1: Write the failing JVM tests for preview-state mapping**

```kotlin
@Test
fun builds_pet_focused_preview_when_pet_has_feedback_and_active_title() {
    val state = buildNurturingWorldPreview(
        petName = "小记",
        petStateLabel = "平静",
        petMessage = "今晚也辛苦了，我在这里。",
        islandLevel = 7,
        islandMoodLabel = "夜色宁静",
        recentTitle = "凌晨诗人"
    )

    assertEquals("小记正在等你", state.headline)
    assertEquals("今晚也辛苦了，我在这里。", state.petSnippet)
    assertEquals("夜色宁静 · Lv.7", state.islandSnippet)
    assertEquals("最近珍藏：凌晨诗人", state.collectionSnippet)
}


@Test
fun falls_back_to_generic_copy_when_optional_values_are_missing() {
    val state = buildNurturingWorldPreview(
        petName = null,
        petStateLabel = null,
        petMessage = null,
        islandLevel = 1,
        islandMoodLabel = null,
        recentTitle = null
    )

    assertEquals("养成世界正在慢慢生长", state.headline)
    assertEquals("去看看你的陪伴精灵今天状态如何", state.petSnippet)
}
```

- [ ] **Step 2: Run the specific JVM test to confirm failure**

Run:

```powershell
.\gradlew.bat testExperimentalDebugUnitTest --tests "com.diary.app.ui.nurturing.NurturingWorldPreviewStateTest"
```

Expected:

- FAIL because `buildNurturingWorldPreview` and the model file do not exist yet

- [ ] **Step 3: Implement a small, pure preview-state model**

Create `NurturingWorldPreviewState.kt`:

```kotlin
package com.diary.app.ui.nurturing

data class NurturingWorldPreviewState(
    val headline: String,
    val petSnippet: String,
    val islandSnippet: String,
    val collectionSnippet: String
)

fun buildNurturingWorldPreview(
    petName: String?,
    petStateLabel: String?,
    petMessage: String?,
    islandLevel: Int,
    islandMoodLabel: String?,
    recentTitle: String?
): NurturingWorldPreviewState {
    val safePetName = petName?.takeIf { it.isNotBlank() }
    val headline = if (safePetName != null) "$safePetName正在等你" else "养成世界正在慢慢生长"
    val petSnippet = petMessage?.takeIf { it.isNotBlank() } ?: "去看看你的陪伴精灵今天状态如何"
    val islandSnippet = "${islandMoodLabel ?: "夜色浮动"} · Lv.$islandLevel"
    val collectionSnippet = if (recentTitle != null) "最近珍藏：$recentTitle" else "今晚也许会有新的珍藏出现"
    return NurturingWorldPreviewState(headline, petSnippet, islandSnippet, collectionSnippet)
}
```

- [ ] **Step 4: Run the targeted JVM test and verify it passes**

Run:

```powershell
.\gradlew.bat testExperimentalDebugUnitTest --tests "com.diary.app.ui.nurturing.NurturingWorldPreviewStateTest"
```

Expected:

- PASS

- [ ] **Step 5: Commit the shared preview-state slice**

```bash
git add app/src/main/java/com/diary/app/ui/nurturing/NurturingWorldPreviewState.kt app/src/test/java/com/diary/app/ui/nurturing/NurturingWorldPreviewStateTest.kt
git commit -m "feat: add nurturing world preview state"
```

---

## Task 3: Replace The Simple Tools Entry With A Rich Nurturing World Card

**Files:**
- Create: `app/src/main/java/com/diary/app/ui/nurturing/NurturingWorldEntryCard.kt`
- Modify: `app/src/main/java/com/diary/app/ui/tools/ToolsScreen.kt`
- Test: `app/src/test/java/com/diary/app/ui/nurturing/NurturingWorldPreviewStateTest.kt`

- [ ] **Step 1: Extend the preview-state test to cover collection fallback copy used by the card**

```kotlin
@Test
fun uses_default_collection_copy_when_recent_title_missing() {
    val state = buildNurturingWorldPreview(
        petName = "小记",
        petStateLabel = "开心",
        petMessage = "今天看起来很亮堂。",
        islandLevel = 4,
        islandMoodLabel = "微风晴朗",
        recentTitle = null
    )

    assertEquals("今晚也许会有新的珍藏出现", state.collectionSnippet)
}
```

- [ ] **Step 2: Run the targeted JVM test to verify the new assertion goes red if needed**

Run:

```powershell
.\gradlew.bat testExperimentalDebugUnitTest --tests "com.diary.app.ui.nurturing.NurturingWorldPreviewStateTest"
```

Expected:

- PASS if helper already covers it
- If FAIL, fix helper before moving on

- [ ] **Step 3: Implement the rich entry card UI**

Create `NurturingWorldEntryCard.kt` with a focused API:

```kotlin
@Composable
fun NurturingWorldEntryCard(
    state: NurturingWorldPreviewState,
    onOpenPet: () -> Unit,
    onOpenIsland: () -> Unit,
    onOpenCollection: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        gradientColors = listOf(Color(0xFF18352F), Color(0xFF24334A))
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(state.headline, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF3EFE6))
            Text(state.petSnippet, fontSize = 14.sp, color = Color(0xFFE3DDD2))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PreviewChip("宠物", state.petSnippet, onOpenPet)
                PreviewChip("小岛", state.islandSnippet, onOpenIsland)
                PreviewChip("珍藏", state.collectionSnippet, onOpenCollection)
            }
        }
    }
}
```

Use a small private `PreviewChip` inside the file instead of leaking another component.

- [ ] **Step 4: Wire the card into `ToolsScreen.kt` and remove the old three-row nurturing entry**

Replace the current nurturing section body with:

```kotlin
val previewState = buildNurturingWorldPreview(
    petName = "小记",
    petStateLabel = "平静",
    petMessage = "今晚也辛苦了，我在这里。",
    islandLevel = 7,
    islandMoodLabel = "夜色宁静",
    recentTitle = "凌晨诗人"
)

NurturingWorldEntryCard(
    state = previewState,
    onOpenPet = { onNavigateToPet?.invoke() },
    onOpenIsland = { onNavigateToIsland?.invoke() },
    onOpenCollection = { onNavigateToTitleWall?.invoke() ?: onNavigateToAchievements() }
)
```

Keep secondary quick rows below the card only if they still add navigation value. Do not keep the current three plain rows as the primary content.

- [ ] **Step 5: Run unit tests and a debug build**

Run:

```powershell
.\gradlew.bat testExperimentalDebugUnitTest --tests "com.diary.app.ui.nurturing.NurturingWorldPreviewStateTest"
.\gradlew.bat assembleExperimentalDebug
```

Expected:

- Preview-state tests PASS
- Experimental debug build succeeds

- [ ] **Step 6: Commit the tools-entry redesign**

```bash
git add app/src/main/java/com/diary/app/ui/nurturing/NurturingWorldEntryCard.kt app/src/main/java/com/diary/app/ui/tools/ToolsScreen.kt
git commit -m "feat: redesign nurturing world tools entry"
```

---

## Task 4: Rebuild The Pet Screen Around A Scene-First Layout

**Files:**
- Create: `app/src/main/java/com/diary/app/ui/pet/PetSceneCard.kt`
- Create: `app/src/main/java/com/diary/app/ui/pet/PetMoodCopy.kt`
- Modify: `app/src/main/java/com/diary/app/ui/pet/PetScreen.kt`
- Modify: `app/src/main/java/com/diary/app/ui/pet/PetViewModel.kt`

- [ ] **Step 1: Add a failing unit test for the concise pet copy helper**

Create a new JVM test class or add to an existing pet test file:

```kotlin
@Test
fun returns_warm_default_copy_for_calm_state() {
    assertEquals(
        "今晚也辛苦了，我会陪你慢慢安静下来。",
        buildPetMoodCopy(stateLabel = "平静", feedbackText = "")
    )
}


@Test
fun prefers_live_feedback_when_present() {
    assertEquals(
        "今天看起来很亮堂。",
        buildPetMoodCopy(stateLabel = "开心", feedbackText = "今天看起来很亮堂。")
    )
}
```

- [ ] **Step 2: Run the targeted test and verify it fails**

Run:

```powershell
.\gradlew.bat testExperimentalDebugUnitTest --tests "com.diary.app.ui.pet.PetMoodCopyTest"
```

Expected:

- FAIL because `buildPetMoodCopy` does not exist yet

- [ ] **Step 3: Implement the copy helper and any tiny derived ViewModel fields**

Create `PetMoodCopy.kt`:

```kotlin
package com.diary.app.ui.pet

fun buildPetMoodCopy(stateLabel: String?, feedbackText: String): String {
    if (feedbackText.isNotBlank()) return feedbackText
    return when (stateLabel) {
        "开心" -> "你一来，空气都亮起来了。"
        "困倦" -> "夜深了，我们都慢一点。"
        "担心" -> "如果今天很累，也没关系，我在这里。"
        else -> "今晚也辛苦了，我会陪你慢慢安静下来。"
    }
}
```

If `PetViewModel` needs a `stateLabel` flow or helper getter, add the smallest derived property necessary rather than new persisted state.

- [ ] **Step 4: Replace the current top-of-page panel stack with a scene card**

Create `PetSceneCard.kt`:

```kotlin
@Composable
fun PetSceneCard(
    petName: String,
    stateLabel: String,
    moodCopy: String,
    growthLabel: String,
    onTapPet: () -> Unit,
    onFeedPet: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 28.dp,
        gradientColors = listOf(Color(0xFF1F3040), Color(0xFF18352F))
    ) {
        Column {
            Text("陪伴角落", color = Color(0xFFE9E4D8), fontSize = 13.sp)
            ...
            PetComposable(...)
            ...
            Text(moodCopy, color = Color(0xFFF3EFE6), fontSize = 16.sp)
        }
    }
}
```

Update `PetScreen.kt` so the order becomes:

1. Scene card
2. Name/title/growth identity row
3. Interaction shortcuts
4. Memory and charts
5. Secondary stats

Do not keep the old “300dp box then lots of cards” layout as-is.

- [ ] **Step 5: Verify the helper tests, pet-related tests, and debug build**

Run:

```powershell
.\gradlew.bat testExperimentalDebugUnitTest --tests "com.diary.app.ui.pet.PetMoodCopyTest"
.\gradlew.bat testExperimentalDebugUnitTest --tests "com.diary.app.ui.todo.HabitUiStateBuilderTest"
.\gradlew.bat assembleExperimentalDebug
```

Expected:

- `PetMoodCopyTest` PASS
- Existing unrelated JVM tests still PASS
- Build succeeds without Compose compile errors

- [ ] **Step 6: Commit the pet-screen redesign slice**

```bash
git add app/src/main/java/com/diary/app/ui/pet/PetSceneCard.kt app/src/main/java/com/diary/app/ui/pet/PetMoodCopy.kt app/src/main/java/com/diary/app/ui/pet/PetScreen.kt app/src/main/java/com/diary/app/ui/pet/PetViewModel.kt
git commit -m "feat: redesign pet screen around companion scene"
```

---

## Task 5: Generate And Stage The First Production Asset Set

**Files:**
- Modify: `scripts/assets/nurturing_world_phase1.json`
- Create: `output/qwen-assets/` (generated artifacts, not committed unless explicitly desired)
- Optionally Modify: `app/src/main/res/` or `app/src/main/assets/` only after visual review

- [ ] **Step 1: Expand the asset manifest to cover the first reviewable set**

Append these jobs to `scripts/assets/nurturing_world_phase1.json`:

```json
{
  "name": "pet-main-night-guardian",
  "preset": "pet_character",
  "prompt": "月夜绘本风格的陪伴精灵，温柔眼神，柔光半透明，圆中带挺，深夜蓝与雾金点缀",
  "negative_prompt": "史莱姆感，诡异，恐怖，低幼，塑料感",
  "seed": 626262
},
{
  "name": "badge-legend-observer",
  "preset": "achievement_badge",
  "prompt": "传说级成就徽章，私人博物馆藏品风格，植物纹样，月相结构，暗月金与墨松绿",
  "negative_prompt": "实物摄影，背景杂乱，文字，AI角标",
  "seed": 737373
},
{
  "name": "island-lantern-treehouse",
  "preset": "island_asset",
  "prompt": "心情小岛装饰素材，小木屋与树灯组合，绘本纸片生态风格，夜间柔和发光",
  "negative_prompt": "复杂背景，多角色，写实照片，文字",
  "seed": 848484
}
```

- [ ] **Step 2: Run batch generation for the first asset pass**

Run:

```powershell
python scripts/qwen_asset_batch.py --manifest scripts/assets/nurturing_world_phase1.json --output-dir output/qwen-assets/phase1
```

Expected:

- A generated image file for each manifest job
- No missing-environment-variable errors

- [ ] **Step 3: Review the generated set and keep only the strongest candidates**

Run:

```powershell
Get-ChildItem output/qwen-assets/phase1 | Select-Object Name, Length, LastWriteTime
```

Expected:

- Confirm all planned assets exist
- Manually inspect the generated files before integrating any into app resources

- [ ] **Step 4: Commit only the manifest changes, not the generated binaries**

```bash
git add scripts/assets/nurturing_world_phase1.json
git commit -m "chore: expand nurturing world phase 1 asset manifest"
```

---

## Task 6: Final Verification For The Phase 1 Slice

**Files:**
- Verify: all files touched in Tasks 1-5

- [ ] **Step 1: Run the Python tooling tests**

```powershell
@'
import unittest
loader = unittest.TestLoader()
suite = loader.discover("scripts/tests")
runner = unittest.TextTestRunner()
result = runner.run(suite)
raise SystemExit(0 if result.wasSuccessful() else 1)
'@ | python -
```

- [ ] **Step 2: Run the targeted JVM tests**

```powershell
.\gradlew.bat testExperimentalDebugUnitTest --tests "com.diary.app.ui.nurturing.NurturingWorldPreviewStateTest"
.\gradlew.bat testExperimentalDebugUnitTest --tests "com.diary.app.ui.pet.PetMoodCopyTest"
```

- [ ] **Step 3: Run a full experimental debug assemble**

```powershell
.\gradlew.bat assembleExperimentalDebug
```

- [ ] **Step 4: Sanity-check the Git diff**

```powershell
git status --short
git diff -- app/src/main/java/com/diary/app/ui/tools/ToolsScreen.kt app/src/main/java/com/diary/app/ui/pet/PetScreen.kt app/src/main/java/com/diary/app/ui/nurturing/NurturingWorldPreviewState.kt scripts/qwen_image_generate.py
```

Expected:

- Only intended files are modified
- No accidental edits in unrelated nurturing-world systems

- [ ] **Step 5: Create the phase checkpoint commit**

```bash
git add app/src/main/java/com/diary/app/ui/nurturing/NurturingWorldPreviewState.kt app/src/main/java/com/diary/app/ui/nurturing/NurturingWorldEntryCard.kt app/src/main/java/com/diary/app/ui/pet/PetSceneCard.kt app/src/main/java/com/diary/app/ui/pet/PetMoodCopy.kt app/src/main/java/com/diary/app/ui/pet/PetScreen.kt app/src/main/java/com/diary/app/ui/pet/PetViewModel.kt app/src/test/java/com/diary/app/ui/nurturing/NurturingWorldPreviewStateTest.kt scripts/qwen_image_generate.py scripts/qwen_asset_batch.py scripts/tests/test_qwen_image_generate.py scripts/assets/nurturing_world_phase1.json docs/superpowers/specs/2026-06-24-nurturing-world-design.md
git commit -m "feat: ship nurturing world phase 1 foundation"
```

---

## Spec Coverage Check

This phase intentionally covers the first shippable slice of the spec:

- Qwen local asset workflow: covered in Task 1 and Task 5
- Entry card redesign: covered in Task 2 and Task 3
- Pet companion-first redesign: covered in Task 4
- Asset production strategy: covered in Task 5

Deferred to follow-up plans:

- Full island screen redesign
- Full achievement hall redesign
- Deep cross-system reward linkage and rare-event content

Those are not omitted; they are deferred so this phase remains deliverable and reviewable.
