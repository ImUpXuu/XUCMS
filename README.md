# XUCMS

极简的云端记事客户端。手机上随手写，一键同步到自建 CMS。

编辑器是本项目自己写的——没有 WebView，没有第三方富文本库。段落、标题、列表、引用、代码块直接以最终样式渲染在可编辑区域里，`**` `##` `- ` 这些标记不会留在屏幕上。

> 配套后端：[blog-admin-workers](https://github.com/ImUpXuu/blog-admin-workers)，部署在 Cloudflare Workers 上。也可以在登录页填写任何兼容该 API 的自建地址。

---

## 设计原则

极简、易用、UI 逻辑正常。具体来说：

- **一屏一件事**。列表页只有列表，编辑页只有正文。元数据、图库这类次要内容放在 sheet 里，需要时才出现。
- **单一强调色**。配色取自 [upxuu.com](https://upxuu.com) 的天蓝 + 石板灰 + 少量琥珀，但去掉了博客的粗野主义边框与位移阴影：这里全是平面表面加一根发丝线。
- **没有装饰性动效**。转场只有 180ms 的淡入和轻微位移。
- **可预测的返回栈**。返回永远回到上一页，不会中途跳到别处；退出编辑器会自动落一份本地草稿。

## 编辑器

`app/src/main/java/com/upxuu/xucms/editor/` 下是完整实现，分三层：

| 层 | 文件 | 职责 |
| --- | --- | --- |
| 模型 | `model/Block.kt`、`model/MarkSpan.kt` | 块类型 + 行内样式区间。文本里不含 Markdown 标记 |
| 编解码 | `markdown/InlineMarkdown.kt`、`markdown/BlockMarkdown.kt`、`markdown/FrontmatterCodec.kt` | Markdown ⇄ 块列表的双向转换 |
| 状态与视图 | `EditorState.kt`、`ui/MarkdownEditor.kt`、`ui/EditorToolbar.kt` | 光标、撤销栈、自动格式化、渲染 |

工作方式：文档是一串 `Block`，每块一个 `BasicTextField`。块类型（H1、列表、引用……）决定这一块用什么字号、什么装饰；行内加粗/斜体/删除线/行内代码/链接存成 `MarkSpan` 区间，通过 `VisualTransformation` 画上去——偏移量不变，所以光标和选区不会漂。

支持的能力：

- 标题 H1–H3、正文、无序/有序/任务列表（含多级缩进）、引用、围栏代码块、分割线、图片块
- 行内：**加粗**、*斜体*、~~删除线~~、`行内代码`、链接
- 输入即转换：行首输入 `# `、`## `、`- `、`1. `、`> `、`[] ` 立刻变成对应块，标记不会留下
- 回车在列表里续行、在空列表项上退出列表；退格在块首降级或与上一块合并
- 撤销/重做（80 步）

Frontmatter 里应用不认识的键会原样保留，所以手写的 `license`、`series` 之类不会因为在手机上保存一次就丢。

## 其他功能

- **草稿**：编辑停止约 8 秒后写入应用私有目录（间隔可在设置里改，也可关闭）。发布成功才清除。列表页会把未同步的草稿单独列出来。
- **离线可用**：拉取失败时回落到本地草稿，仍然可以继续写。
- **图片**：选图后先在本地压到最长边 1920、JPEG 质量 86 再上传，避免手机原图撑爆请求体。图库支持多选插入、复制链接/Markdown、删除。
- **主题**：跟随系统 / 浅色 / 深色。

## 构建

需要 JDK 17。仓库里不含 Gradle wrapper，CI 会自动生成；本地可以用已安装的 Gradle 9.3.1+：

```bash
gradle wrapper --gradle-version 9.3.1   # 首次
./gradlew testDebugUnitTest             # 单元测试
./gradlew assembleDebug                 # 可安装的 debug 包
./gradlew assembleRelease               # 发布包
```

`local.properties` 里需要有 `sdk.dir` 指向 Android SDK。

推送到 `main` 会触发 [`.github/workflows/release.yml`](.github/workflows/release.yml)：跑单元测试 → 构建 debug 与 release APK → 上传 artifact 并发一个 Release。

## 版本与更新

`app/build.gradle.kts` 里的 `versionCode` / `versionName` 是唯一版本来源，任何用户可见的改动都要在同一个提交里一起改——应用内的更新检查只比较 `versionCode`。

构建成功后 CI 会执行 [`scripts/generate-version-json.sh`](scripts/generate-version-json.sh)，把版本号、commit、构建时间以及"自上次版本号变更以来的所有提交"写进根目录的 [`version.json`](version.json)，并提交回主分支。之所以放在分支上而不是 Release 附件里，是因为应用是通过 GitHub raw 镜像去读它的，而 Release 附件的地址没法走 raw 镜像。

`设置 › 更新 › 检查更新` 会拉这个文件并展示更新日志。更新源默认用 `raw.gh.1s.fan` 加速，可以在同一处切换到官方 `raw.githubusercontent.com`；配置的源失败时会自动回退到另一个，避免镜像挂掉被误判成"已是最新"。

## 技术栈

Kotlin · Jetpack Compose · Material 3 · Navigation Compose · OkHttp · kotlinx.serialization · Coil。没有 Room、没有 Hilt、没有富文本库——依赖表刻意保持得很短。

## 隐私

管理密钥与服务地址只存在应用私有 `SharedPreferences`，只发往你自己填写的服务地址。草稿存在应用私有目录，卸载即删除。不含统计、广告与崩溃上报 SDK。
