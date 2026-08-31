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

Restrained but present: 150–260ms, `FastOutSlowInEasing` or a light spring. Colour changes on selected controls animate (`animateColorAsState`), list changes use `Modifier.animateItem()`, and screens enter with a small slide plus scale while the outgoing screen only fades. Nothing bounces, spins, or draws attention to itself.

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
