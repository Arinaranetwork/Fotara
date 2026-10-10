# Phase 48 - AcademicEvaluationAndBundles
## Goal
Equip Fotara with university-grade academic study evaluation and multimodal capture capabilities: automated course syllabus grade weighting projections, interactive active recall occlusion tape for diagram/vocabulary self-quizzing, optical perspective dewarping with whiteboard contrast enhancement, and standardized coursework archive exchange (.fotara bundle format).

## Scope
- Pure syllabus grade evaluator engine (`SyllabusEvaluator`) calculating required final exam scores to achieve target letter grades (A, B, C, D).
- Composable interactive syllabus evaluator sheet (`SyllabusEvaluatorSheet.kt`) displaying weighted component cards, letter grade projection badges, and live target percentage slider.
- Pure active recall occlusion masking engine (`OcclusionTapeEngine`) supporting add, remove, toggle reveal, reveal all, and hide all operations.
- Composable interactive occlusion tape overlay (`OcclusionTapeOverlay.kt`) rendering study masks with tap-to-reveal animations and quiz study mode.
- Pure zip coursework archive exchange engine (`FotaraBundleEngine`, `FotaraBundleExporter`, `FotaraBundleImporter`) packaging and safely extracting `.fotara` containers with checksum-verified `manifest.json`.
- Pure geometric 3x3 projective homography transformation matrix (`PerspectiveDewarpMatrix`) computing mapping from 4 quad corners to rectangular bounds without external CV libraries.
- Pure optical whiteboard contrast and shadow removal filter (`WhiteboardContrastFilter`) for document image clarity.
- Comprehensive unit tests under `app/src/test/java/com/arinara/fotara/feature/academic/` verifying 100% test pass.

## Out Of Scope
- External cloud synchronization or server-side grading algorithms.
- Proprietary OpenCV or native C++ camera binaries (relying strictly on pure Kotlin mathematical transformations).
- Cloud AI syllabus extraction (parsing is deterministic and local-first).

## Features
### Course Syllabus Weight & Target Grade Evaluator
- Models syllabus grading components (`SyllabusComponent`) with name, percentage weight, max score, and optional current score.
- Computes exact required final exam percentages across standard academic letter grades (A: >= 90%, B: >= 80%, C: >= 70%, D: >= 60%).
- Gracefully handles zero remaining weight, already secured targets, and mathematically unachievable goals (`isAchievable = false`).
- Interactive bottom sheet allows dynamic score tweaking and real-time projection updates.

### Active Recall Occlusion Tape
- Mask rectangular regions (`OcclusionTape`) on anatomical diagrams, chemical reactions, mathematical derivations, or vocabulary lists.
- Provides test-taking study mode where taped elements are concealed with high-contrast accent tape and revealed on single-tap with dashed outlines.
- Fast bulk actions: "Reveal All" and "Hide All" for quick pre-exam self-assessment.

### Coursework Archive & Interchange Bundle (.fotara)
- Standardized ZIP archive packaging containing `manifest.json` metadata, coursework notes, photos, and structure.
- Safe extraction preventing directory traversal (Zip Slip attack mitigation).
- Checksum validation ensures payload integrity upon peer import.

### Document Perspective Dewarping & Whiteboard Enhancement
- Calculates 3x3 homography matrix from 4 arbitrary quadrilateral corner points to an orthogonal destination rectangle using Gaussian elimination with partial pivoting.
- Applies adaptive contrast stretching and shadow suppression filter to convert dimly lit classroom whiteboard captures into crisp, readable study notes.

## UI Mockup
```
+-------------------------------------------------------+
|  Syllabus Grade Projection                    [ X ]   |
+-------------------------------------------------------+
|  Target Grade: [ A (90%) ]  Required on Final: 88.5%  |
|  [==========================O-------------]           |
+-------------------------------------------------------+
|  Components:                                          |
|  +-------------------------------------------------+  |
|  | Midterm 1 (20%)                      85 / 100   |  |
|  +-------------------------------------------------+  |
|  | Homework & Quizzes (30%)             94 / 100   |  |
|  +-------------------------------------------------+  |
|  | Final Exam (50%)                     Unscored   |  |
|  +-------------------------------------------------+  |
+-------------------------------------------------------+
```

```
+-------------------------------------------------------+
|  Active Recall Quiz Mode             [Hide All] [Show]|
+-------------------------------------------------------+
|  [====================== Diagram ===================] |
|  |               +--------------------+             | |
|  |  Mitochondria | [// TAPE: HIDDEN //|             | |
|  |               +--------------------+             | |
|  |  Ribosome     | - - - Protein  - - | (Revealed)  | |
|  |               + - - - - - - - - - -+             | |
+-------------------------------------------------------+
```

## Logic Notes
- `SyllabusEvaluator`: Earned weighted points = sum of (score / maxScore * weight). Remaining weight = total weight minus scored weight. Target required score = (targetPercent - earnedWeighted) / remainingWeight * 100.
- `OcclusionTapeEngine`: Immutable state updates via unidirectional state flow, tracking revealed tape count and quiz completion progress.
- `PerspectiveDewarpMatrix`: Linear system $A \cdot h = b$ solved for 8 homography coefficients with normalized $h_{22} = 1.0$.
- `WhiteboardContrastFilter`: Normalizes luminance distribution and boosts stroke contrast while suppressing uneven illumination shadows.
- `FotaraBundleEngine`: Secure zip extraction enforcing `File.canonicalPath` prefix validation against destination folder.

## Risks
- Matrix singularity in homography computation -> Detect collinear or degenerate quad corners and provide identity/fallback matrix with graceful handling.
- Large images during whiteboard contrast filtering causing memory churn -> Efficient single-pass integer pixel buffer processing.

## Dependencies
- Phase 46 Space super-hierarchy & Phase 45 Note ecosystem.
- Jetpack Compose & Android.md design system tokens.

## Acceptance Criteria
- `SyllabusEvaluator` correctly calculates required final exam score across diverse syllabus configurations and target thresholds.
- `SyllabusEvaluatorSheet` displays components with ElmsSans typography and MidnightNavy dark palette.
- `OcclusionTapeEngine` and `OcclusionTapeOverlay` support adding, toggling, revealing all, and quiz tracking.
- `FotaraBundleExporter` and `FotaraBundleImporter` successfully round-trip export and import `.fotara` zip bundles with manifest integrity verification.
- `PerspectiveDewarpMatrix` solves 8-parameter homography mapping corners accurately.
- `WhiteboardContrastFilter` suppresses background shadows and heightens stroke contrast.
- 100% unit tests pass under `com.arinara.fotara.feature.academic.*`.
