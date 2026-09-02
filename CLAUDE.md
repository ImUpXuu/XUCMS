# CLAUDE.md

Guidance for AI coding agents working in this repository.

## What this is

XUCMS is an Android client for a self-hosted CMS ([blog-admin-workers](https://github.com/ImUpXuu/blog-admin-workers)). Two content kinds: **文章** (posts, with full frontmatter) and **说说** (talks, short timeline notes). Both are markdown files with YAML frontmatter, fetched and written over a small REST API.

Single Gradle module (`:app`), Kotlin + Jetpack Compose, package `com.upxuu.xucms`.

## Hard constraints

**No third-party rich-text or markdown-rendering library.** The editor under `editor/` is hand-written on purpose — a previous version used `richeditor-compose` and was replaced because of it. Do not reintroduce that dependency, a WebView-based editor, or a markdown rendering library. If the editor needs a feature, extend `editor/`.

**Keep the dependency list short.** No Room, no Hilt, no Retrofit/Moshi. Storage is `SharedPreferences` plus JSON files; HTTP is raw OkHttp; JSON is kotlinx.serialization. Adding a dependency needs a real justification.

**No Gradle wrapper is committed.** CI generates it (`gradle wrapper --gradle-version 9.3.1`). AGP 9.1.1 requires Gradle ≥ 9.3.1.

## Layout

```
app/src/main/java/com/upxuu/xucms/
  AppContainer.kt          hand-rolled DI: settings, drafts, api
  MainActivity.kt          single activity, sets up theme + nav
  navigation/XucmsApp.kt   NavHost and all routes
  data/                    Api client, models, settings, drafts, image pipeline, frontmatter mapping
  editor/                  the markdown editor (see below)
  feature/<screen>/        one package per screen: Screen.kt + ViewModel.kt
  ui/theme/                colors, type, shapes
  ui/components/           shared building blocks
```

## The editor

Three layers, in `editor/`:

| Layer | Files | Role |
| --- | --- | --- |
| Model | `model/Block.kt`, `model/MarkSpan.kt` | Block list; inline emphasis as ranges |
| Codec | `markdown/InlineMarkdown.kt`, `markdown/BlockMarkdown.kt`, `markdown/FrontmatterCodec.kt` | Markdown ⇄ blocks |
| State + view | `EditorState.kt`, `ui/MarkdownEditor.kt`, `ui/EditorToolbar.kt` | Caret, undo, auto-format, rendering |

The invariant that makes it WYSIWYG: **markdown markers are never present in a block's text.** `##` lives in `Block.type`, `**` lives in `Block.marks`, `- ` is drawn in the gutter. Inline styling is applied with a `VisualTransformation` using `OffsetMapping.Identity`, so caret and selection offsets are never remapped.

If you touch marks, remember `MarkSpans.remap` must be called for every text edit or spans will drift out of sync with the text. `EditorState.onTextChange` computes a single-range diff and does this.

## Conventions

- Two-space indent, trailing commas, `MaterialTheme` for every colour and text style — no hardcoded `Color(0x...)` outside `ui/theme/`.
- Screens are stateless composables driven by a `StateFlow<XUiState>` from a ViewModel; build ViewModels with `ui/rememberViewModel`.
- All user-facing strings are Simplified Chinese, written inline (there is no localisation).
- API calls return `Result<T>`; `ApiException.code == 401 || 403` means the session is gone — clear the token and route to login.
- Comments explain constraints the code cannot show. Do not narrate what the next line does.

## Visual design

Minimal. Palette derives from [upxuu.com](https://upxuu.com) — sky blue accent, slate neutrals, amber used sparingly — but **not** the blog's brutalist style: no thick borders, no offset shadows, no elevation. Flat surfaces with a 1dp hairline outline. One accent colour. Transitions are ~180ms fades; nothing decorative.

One screen does one thing. Secondary concerns (metadata, gallery, draft management) go in bottom sheets.

## Drafts

Two distinct situations, and the UI must keep them apart:

- **Unpublished** (`Draft.filename == null`): nothing exists in the cloud. Listed as its own section at the top of the home list.
- **Local changes to a published note** (`filename != null`): the cloud copy exists. **Not** listed separately; the note's row gets a 有本地改动 pill, and the drafts are managed from 草稿管理 inside the editor.

Each note may hold one `AUTO` draft (overwritten by the autosave timer) plus any number of `MANUAL` snapshots the user creates.

A draft is only written when the content actually differs from the baseline loaded from the server, and the comparison must be **normalised**: run the fetched markdown through `applyMarkdown` then `composeMarkdown` before diffing, because the server's byte-for-byte text differs from what this app writes for identical content (frontmatter key order, quoting, `1.` numbering). Comparing raw text marks every note as changed the moment it is opened. `EditorViewModel.load` also deletes an `AUTO` draft that turns out to equal the server copy, so a stale one from an older build stops lying about local changes. Only `AUTO` drafts drive the 有本地改动 pill — manual snapshots are kept on purpose and say nothing about sync state.

`DraftStore` migrates drafts written by older releases on construction — a schema change must never silently drop the user's unpublished writing. Same reasoning behind the explicit `<include>` entries in `backup_rules.xml` and `data_extraction_rules.xml`: drafts and settings survive an update or a device transfer; the admin key is absent from cloud backup on purpose (note that with `<include>` present an `<exclude>` outside an included path is a lint error).

## Destructive actions

Every delete follows the same three steps, and none may be skipped:

1. A gesture or button only **requests** the delete.
2. A `ConfirmDeleteDialog` asks.
3. The row disappears and a snackbar offers 撤销 for five seconds. Only when that window closes does anything actually get removed.

Swipe gestures are never destructive by themselves. `SwipeActionRow` is hand-rolled rather than built on `SwipeToDismissBox`, because that component latches once its threshold is crossed and the user cannot pull the row back. The row follows the finger and always springs home on release.

Two properties are load-bearing and easy to regress: the gesture must wait for `awaitHorizontalTouchSlopOrCancellation` before claiming the pointer, or it eats the list's vertical scroll; and the action threshold is deliberately long (168dp) so brushing a row while scrolling cannot fire it. The hint behind the row reads 继续滑动 until the pull is far enough to actually do something.

## Motion

All durations and curves live in `ui/theme/Motion.kt`; do not write a bare `tween(200)` at a call site. Springs are preferred for anything driven by a finger or a caret because they preserve velocity when a second gesture interrupts the first; tweens are for discrete flips like a colour swap or a panel appearing. Nothing bounces, spins, or draws attention to itself.

The editor keeps the caret clear of the keyboard by measuring the cursor rectangle from `onTextLayout` plus `boundsInWindow`, then calling `animateScrollBy` with exactly the overlap. That is why `BlockField` takes an `onCaretRect` callback — the scroll amount has to come from real layout, not from a guess about line height. Two guards are load-bearing: the caret rectangle is cleared when focus leaves the editor (a stale one made opening the keyboard jump), and the effect waits a frame to confirm the viewport has stopped resizing (the keyboard animates over several frames, and acting on an intermediate height scrolls by the wrong amount).

## Editor performance

The typing path runs on every keystroke, so it is kept allocation-light on purpose. If you change any of this, keep the fast paths:

- `InlineMarkdown.parse` returns the input untouched when it contains no marker characters at all, which is most lines of prose.
- `MarkSpans.normalize/remap/slice/shift` return early for empty and single-element inputs instead of running the group-and-sort path.
- `blockTextStyle` and `rememberMarkTransformation` are memoised; the transformation resolves each mark's `SpanStyle` once per mark set rather than per character run, and returns `VisualTransformation.None` when a block has no marks.
- `EditorState.pushHistory` takes a tag and coalesces consecutive same-tag edits inside a short window. A snapshot copies the whole block list, so pushing per keystroke would allocate a document copy per character *and* make undo step one letter at a time. Structural changes clear the tag so they stay separate undo steps.
- `EditorToolbar` reads selection state through `derivedStateOf`, and `MarkdownEditor` passes a remembered `onCaretRect` lambda plus a `contentType` to `items`, so a keystroke in one block does not recompose the others.

## Toolbar configuration

`editor/ToolbarAction.kt` enumerates every control the toolbar can show; `ToolbarLayout` holds the user's ordered selection and a row count of 1 or 2, persisted in `SettingsStore` by stable string ids. `EditorToolbar` renders whatever the layout says and dispatches by action, so adding a control means adding an enum entry, an icon mapping and a `dispatch` branch — nothing in the editor screen changes.

`feature/settings/ToolbarSettingsScreen` previews the toolbar using the same `ToolbarControl` composable the editor uses. That sharing is the point: the arrangement the user drags is literally the thing they will get, so the preview cannot drift from reality.

## Versioning and releases

`versionCode` and `versionName` in `app/build.gradle.kts` are the single source of truth, and **both must be bumped in the same commit as any user-visible change**. Do not push a feature or a fix without touching them: the in-app update check compares `versionCode` only, so a build that ships changes under an unchanged code is invisible to everyone who already has the app.

- `versionName` is semver — patch for a fix, minor for a feature, major for a rewrite.
- `versionCode` is a monotonic integer, +1 per release. Never reuse or decrease it.
- `scripts/generate-version-json.sh` greps those two lines with a literal pattern, so keep the `versionCode = 3` / `versionName = "2.1.0"` formatting intact.

CI runs that script after a successful build and commits the resulting `version.json` back to the default branch. It has to live on the branch rather than as a release asset because the app fetches it through a **GitHub raw mirror**, and release asset URLs are not raw-mirrorable. Its `changes` array is every non-merge commit since the previous `versionCode` bump, which is why the workflow checks out with `fetch-depth: 0`.

`UpdateSource` offers `raw.gh.1s.fan` (default, reachable from the mainland) and `raw.githubusercontent.com`. `UpdateChecker` retries the other host once when the configured one fails, so a mirror outage does not read as "no update available".

Commit messages use conventional prefixes (`feat`, `fix`, `refactor`, `chore`, `perf`) with an imperative subject under about 70 characters, and a body that states the cause when fixing something rather than just the symptom. Those subjects are what users read in the changelog dialog, so write them for someone who does not know the codebase.

## Testing

`app/src/test/` holds JVM unit tests for the editor codecs, `MarkSpans`, `EditorState`, and frontmatter mapping. Anything touching `Context` (DraftStore, SettingsStore) has no test — keep the pure logic separable so it stays testable.

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

## Where things commonly go wrong

- Multiplying an `Int` by a `Dp` does not compile. Use `IntrinsicSize`/`fillMaxHeight` for content-driven sizing.
- `EditorState.setMarkdown` bumps `revision`, so a `snapshotFlow { revision }` collector fires once on load. Gate autosave on a content comparison, not on the revision counter alone.
- Ordered-list numbering is recomputed at render time and normalised to `1.` on write; do not store the number.
