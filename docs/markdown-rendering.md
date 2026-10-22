# memo-m 的 Markdown 渲染

本文记录 memo-m 里 Markdown 从「服务器返回的一串文本」到「屏幕上的一张卡片」的完整链路、每一处样式的来源，以及改动时的入口和已知限制。渲染器的行为以本文为准；样式数值若与代码不一致，以代码为准并顺手更新本文。

---

## 1. 概览

| | |
| --- | --- |
| 解析器 | `org.intellij.markdown`，GFM 方言（`GFMFlavourDescriptor`） |
| 渲染方式 | 纯 Compose 递归渲染 AST，**不使用** WebView / HTML |
| 唯一入口 | `NativeComposeMarkdown(...)`（`ui/component/item/markdown/NativeComposeMarkdown.kt`） |
| 样式来源 | 编辑器语法高亮与正文渲染共用一份 Markdown 语义；外观来自 `docs/` 下的 `BorderTitle.css`、`段落间距优化.css` 与本项目自己的设计决定（`多彩标签.css` 评估后未采用） |
| 使用位置 | 首页卡片、详情页、地图卡片的 memo 正文；表格单元格与链接预览卡片在其内部生成 |

渲染分两条独立链路，互不影响：

- **读**：`NativeComposeMarkdown` → AST → Compose 树（本文第 2–7 节）。
- **写**：`MemoInput` 里的 `BasicTextField` + `MarkdownLanguageHandler`，只做**语法高亮**和列表续行，不做排版（第 8 节）。

---

## 2. 数据流

```
Memo.content (String, Markdown)
      │
      ├─ MarkdownCache.cached()  ── 命中 ──┐
      │                                    │
      └─ 未命中：Dispatchers.Default       │
         MarkdownParser.buildMarkdownTree  │
         → ParsedMarkdown(tree, refs)      │
         → LruCache(1,000,000 字符)        │
                                           ▼
                    produceState(parsed)  ── 解析未完成时先渲染纯文本
                                           │
                                           ▼
             CompositionLocalProvider(LocalToken / LocalHostUrl /
                                      LocalMarkdownReferences)
                                           │
                                           ▼
             NativeMarkdownNode(node = tree, content, headerScale, ...)
                                           │
                                           ▼
             NativeMarkdownNodeRecursive(node)   ← 「块」递归
                                           │
                                           ▼
             appendInlineChildren / visitInlineChild  ← 「行内」递归
                                           │
                                           ▼
             MarkdownText (Material3 Text) + drawBehind 背景装饰
```

要点：

- **解析在后台线程**，`produceState` 先在主线程渲染原始文本占位，解析完成后替换。列表里几百条 memo 首次组合时不会卡住滚动。
- **缓存按源码字符串为键**，容量按字符数计（不是条数），因此长文和短文占用同一维度。同一个 memo 滚动出屏再回来不会重复解析。
- `ParsedMarkdown` 里除语法树外还有一份 **链接引用表**（`[label]: url`），供 `![x][label]`、`[x][label]` 这类引用式链接解析。

---

## 3. 文件地图

| 文件 | 职责 |
| --- | --- |
| `NativeComposeMarkdown.kt` | 唯一入口；解析调度、CompositionLocal 注入、可选中包装 |
| `MarkdownCache.kt` | AST + 引用表的 LRU 缓存 |
| `NativeMarkdownNode.kt` | **块级渲染器**（段落、标题、列表、引用、分隔线）+ 行内构建器 + 正文节奏常量 |
| `MarkdownCodeBlock.kt` | 代码面板：语言标签、复制、横向滚动、Tab 展开、超长折叠、主题化配色与阴影 |
| `CodeHighlighter.kt` | 代码语法高亮（`dev.snipme:highlights`），深浅两套配色，带缓存 |
| `NativeMarkdownTable.kt` | GFM 表格：两趟测量、横向滚动、对齐解析 |
| `NativeMarkdownAttachmentImage.kt` | 图片块，转交 `AttachmentCard` |
| `NativeMarkdownLatex.kt` | 行内/块级公式，`MTMathView` 原生渲染 |
| `NativeLinkPreview.kt` / `StandalonePreviewLink.kt` | 「整段只有一个 URL」→ 链接预览卡片 |
| `MDUtils.kt` | `getTextInNode` / 颜色转十六进制等小工具 |
| `CheckboxUtils.kt` | 任务项勾选后改写源文本 |
| `ui/component/composer/MarkdownLanguageHandler.kt` | **编辑器**语法高亮 + 回车续行 |
| `ui/component/composer/MarkdownListInputTransformation.kt` | 编辑器里回车自动续写列表前缀 |

---

## 4. 块级渲染

入口是 `NativeMarkdownNodeRecursive`，一个 `when (node.type)` 分派。除特别说明外，块之间通过 `Modifier.padding` 产生间距，**不叠加**（见第 6 节）。

| 节点 | 渲染 |
| --- | --- |
| `MARKDOWN_FILE` | 顺序渲染子节点，同时开启链接预览探测 |
| `PARAGRAPH` | 见 4.1 |
| `ATX_1…ATX_6` | 见 4.2 |
| `UNORDERED_LIST` / `ORDERED_LIST` | 见 4.3 |
| `CODE_BLOCK` / `CODE_FENCE` | 见 4.4 |
| `BLOCK_QUOTE` | 4dp 圆角竖条 + 12dp 内缩，内容用 `onSurfaceVariant`；竖条在 RTL 下镜像到右侧 |
| `GFMElementTypes.TABLE` | 见 4.5 |
| `IMAGE` | `AttachmentCard`，按图片真实比例回填 `aspectRatio`，上下各 8dp |
| `BLOCK_MATH` | `$$…$$` → `MTMathView`，上下各 8dp |
| `HORIZONTAL_RULE` | `AppDivider`，上下各 8dp |
| `LINK_DEFINITION` | 不渲染（只进引用表） |

### 4.1 段落与换行

段落被拆成**逐行**渲染：`MarkdownParagraphText` 把整段的 `AnnotatedString` 按 `\n` 切片，用 `Column(verticalArrangement = spacedBy(0.5em))` 排开。

- 为什么拆：`段落间距优化.css` 的 `p > br { margin-top: 0.5em }` 只对**显式换行**生效，自动折行的长句不受影响。单行兜底走原路径（不拆）。
- 切片用 `AnnotatedString.subSequence`，样式与注解按行裁剪，链接、行内代码、公式占位符都能跨行保留。
- 段落末尾的 `EOL` 会留下空行，切分时丢掉。

### 4.2 标题

来源 `docs/BorderTitle.css`：左侧 3dp 彩色竖条 + 标题下方一条**右浓左淡**的 2dp 渐变横线。

- 颜色按级别取色板（日/夜两套），字号倍率 1.3 → 1.0，字重 700 → 575，行高比 1.2 → 1.5，多数情况下最终字号会被 `headerScale`（设置里的「标题大小」）再缩放一次，并保证不小于正文字号。
- 竖条高度按行高推算，避免和文字基线打架。

### 4.3 列表

- 无序：`•`；有序：`1.` `2.` …；任务项：Material3 `Checkbox` 缩到 0.8 倍，勾选后调用 `toggleCheckbox(content, start, end, checked)` 改写源码并回传 `onContentChange`（乐观更新，失败由上层回滚）。
- 每个 item 内部是 `Column`，条目首段的上内边距被 `LocalForceNoTopPadding` 清零，让项目符号与文字首行顶端对齐。
- 条目间距 = 上一项下内边距 0.1em + 列表容器 `spacedBy(0.1em)` = **0.2em**。

### 4.4 代码块

见 `MarkdownCodeBlock.kt`，渲染规则：

| 项 | 值 |
| --- | --- |
| 圆角 / 内边距 | 12dp / 18dp（左内边距额外让出 4dp 主色条） |
| 字号 / 行高 | 14sp / 22.4sp（CSS `line-height: 1.6`） |
| 左侧条 | 4dp，`colorScheme.primary`，RTL 时在右侧 |
| 面板（夜间） | `#0B1120`，文字 `#E2E8F0`，深色语法盘 |
| 面板（日间） | `surfaceContainerHighest` + 1dp `outlineVariant` 描边，文字 `onSurface`，浅色语法盘 |
| 阴影 | 夜间 10% / 扩散 18dp / 下移 6dp；日间 5% / 12dp / 3dp；用堆叠圆角矩形模拟（`Modifier.shadow` 无法表达 CSS 的偏移与模糊量） |
| 头部 | 左：语言标签（`kt`→Kotlin 等规范化；未知语言原样；**没写语言时显示 `txt`**）；右：复制按钮（复制纯代码，对勾反馈 1.2s + 振动） |
| 长行 | 默认 `softWrap = false` + `horizontalScroll`，不折行；`settings_editor_wrap_lines` 打开后改为自动换行（编辑器设置） |
| 行号 | `settings_editor_line_numbers` 打开后左侧显示 1 基行号；自动换行时按「每个源码行一组」排版，行号仍与首行对齐（见 6.1） |
| 长按 | 长按某一行复制该行源码到剪贴板，并提示「已复制第 N 行」 |
| Tab | 显示与复制均展开为 4 空格 |
| 折叠 | 超过 18 行默认折叠，底部「展开剩余 N 行 / 收起」；复制始终是全量代码 |

主题判定用 `colorScheme.background.luminance() < 0.5f`，而不是 `isSystemInDarkTheme()`，因此跟随 app 内的「外观」设置。

### 4.5 表格

`NativeMarkdownTable` 用 `SubcomposeLayout` 两趟测量：先量每个单元格的自然宽度（上限两倍视口，避免超长单元格把宽度撑爆），再按列宽组合。整表横向滚动，表头用 `secondaryContainer` 底色，行/列分隔线 1dp。对齐由分隔行（`:---:`）解析出来。

---

## 5. 行内渲染

行内构建器是 `appendInlineChildren` / `visitInlineChild`，直接往 `AnnotatedString` 里追加，不产生 Composable（公式与图片占位符除外）。

| 语法 | 表现 |
| --- | --- |
| `**粗**` / `*斜*` | `FontWeight.Bold` / `FontStyle.Italic` |
| `~~删除线~~` | `TextDecoration.LineThrough` |
| `` `行内代码` `` | 等宽字体；**反引号保留**（弱化色）让人认得出这是行内代码，底色只覆盖中间的代码 |
| `[文字](url)`、`<url>`、裸 URL | `LinkAnnotation.Url`，主色 + 下划线（下划线由渲染器显式添加，见 5.2） |
| `#标签` | 主色系**胶囊**：主题色低透明度底 + 主题色文字，左右各两个不换行空格作为内边距，整颗胶囊可点击并回调 `onHashtagClick` |
| `==高亮==` | 去掉两侧 `==`，用主色 16% 作为背景（`MarkdownStyles.highlightBackground`），文字保持正文色 |
| `@提及` | `@名字` 用主色 + Medium 字重标出；电子邮箱里的 `@` 不算（正则带前置断言）。memos 目前没有单用户主页，所以**不可点击** |
| `$公式$` | 用 `MTMathView` 实测宽高，结果按 `(latex, fontSize)` 缓存（`LatexMetrics`），换算成 em 交给 `InlineTextContent` 占位 |
| `![图](url)` | 段落内的图片会打断文本流，单独作为块渲染 |
| 引用式链接/图片 | 通过 `ParsedMarkdown.references` 解析 |

**整段只有一个 URL** 的段落会被 `standalonePreviewUrl` 识别成链接预览卡片，取代纯文字链接（与 Web 端一致）。

### 5.1 行内代码与标签胶囊的绘制

两者都靠 `MarkdownText` 的 `drawBehind` 在文字后面画形状：

- **行内代码**：`ROUNDED_BG_COLOR` 注解标出范围，从 `getPathForRange` 取包围盒画 8px 圆角矩形。该包围盒是整行高度，所以底色是一条与行同高的色带；反引号本身保留、用弱化色，底色只覆盖中间的代码。
- **标签胶囊**：`ROUNDED_TAG_CAPSULE` 注解携带胶囊颜色，包围盒同样来自 `getPathForRange`（其中已包含两侧的不换行空格），纵向上下各内缩 2dp 后以「高度一半」为圆角，得到胶囊。因为水平内边距是真实字符，胶囊不会压到相邻文字。

### 5.2 链接样式归渲染器

`MarkdownText` 用的是 foundation 的 `BasicText`，不是 Material3 的 `Text`。后者会用 `createTextWithLinkStyles` 把字符串里**每个** `LinkAnnotation` 区间重刷成"主题主色 + 下划线"，覆盖区间自己的 `SpanStyle`——链接和标签都是 link，颜色就再也管不住了。

换成 `BasicText` 之后：URL 链接由渲染器显式加下划线，`#标签` 显式不加，两者都能用 `SpanStyle` 自由配色。代价是框架不再兜底任何链接外观，新增链接类型时要自己写全。

---

## 6. 正文节奏（间距）

全部以正文 `em` 为单位（`fontSizePx / density`），随系统字号缩放。来源 `docs/段落间距优化.css`，但该 CSS 给的是 CodeMirror 每行的 padding，渲染器没有"行内边距"，因此换算成块间距：

| 常量 | 值 | 作用 |
| --- | --- | --- |
| `ParagraphSpacingEm` | 0.5 | 段落上下内边距（两段之间合计 1em） |
| `HeadingSpacingEm` | 0.75 | 标题上下，保证比段落间隔更大 |
| `ListSpacingEm` | 0.1 | 列表内每行内边距；条目之间另加 0.1em 排布 → 0.2em |
| `LineBreakSpacingEm` | 0.5 | 段内显式换行的额外留白（`p > br`） |
| `CodeBlockSpacingEm` | 1 | 代码面板上下外边距，给阴影留出淡出空间 |

规则：相邻两个块的间距 = **上一个块的下内边距 + 下一个块的上内边距**，容器一律不加额外 padding，避免出现"双重间距"。

### 6.1 编辑器设置（`MarkdownFormat`）

「设置 → 编辑器」（`SettingsSection.EDITOR`）里有四个开关，通过 `MarkdownFormat` 交给渲染器：

| 开关 | 字段 | 效果 |
| --- | --- | --- |
| 代码行号 | `codeLineNumbers` | 代码块左侧显示 1 基行号；自动换行时按源码行对齐（见 4.4） |
| 代码自动换行 | `codeWrapLines` | 长行折行；关闭时横向滚动 |
| 首行缩进 | `paragraphIndent` | 段落 `text-indent: 2em`，只作用于每段第一行；**纯标签行、纯 `@提及` 行与列表行不缩进** |
| 中英文加空格 | `cjkSpacing` | 中文与字母/数字之间插入 U+2009 细空格，仅在渲染时插入，不写回内容 |

数据流：`DataStoreManager`（4 个布尔键，已加入备份白名单）→ `AppSettingsDelegate.loadEditorSettings()` → `MemosUiState.appSettings` → `MainScreen` / `ProfileActivity` 组装 `MarkdownFormat` 并 `LocalMarkdownFormat provides` → 渲染器读取。

没有提供该 CompositionLocal 的调用点（例如 androidTest 直接渲染 `NativeComposeMarkdown`）拿到 `MarkdownFormat.Default`，即四个开关全关，行为与旧版本一致。

---

## 7. 交互

| 交互 | 行为 |
| --- | --- |
| 点链接 | `LocalUriHandler` 打开 |
| 点 `#标签` | `LocalOnHashtagClick` → ViewModel 切换标签筛选 |
| `@提及` | 仅着色，无可点击目标（memos 没有单用户主页） |
| 勾选任务项 | 改写源码并通过 `LocalOnContentChange` 回传（列表页与详情页都支持） |
| 图片 | 点击进全屏查看器（`AttachmentCard` 内部处理） |
| 代码 | 复制全部（纯代码）、长按复制单行、展开/收起、横向滚动或自动换行 |
| 文本选择 | 详情页 `selectable = true`，外层包 `SelectionContainer` |

无障碍：链接与标签区间由 foundation 的 `TextLinkScope` 生成为可聚焦的点击节点；代码块的复制/折叠按钮显式带 `Role.Button`。

---

## 8. 编辑器的语法高亮（与渲染无关）

`MemoInput` 的 `BasicTextField` 接两个变换器：

1. `MarkdownLanguageHandler : OutputTransformation`
   - `transformOutput()` 里解析当前文本、把 `SpanStyle` 逐段套上去（标题分级配色、粗斜体、行内代码、链接、标签、引用等），**只影响显示，不改数据**。
   - 末尾追加 3 个空行（`ComposerTrailingBlankLines`）纯属视觉，让最后一行能滚出悬浮操作栏；这些换行永远不会写进存储的文本。
   - `processInput()`（`TextFieldValue` 旧路径）负责回车续写列表/引用前缀。
2. `MarkdownListInputTransformation : InputTransformation`
   - 新 API 路径下的同类逻辑：回车时按上一行前缀续写 `- `、`- [ ] `、`1. `、`> `、`[] `、`() `。

编辑页**不做排版**：不换字体、不加卡片、不做段落间距——写作面是朴素的全屏纸面。

---

## 9. 已知限制与坑

1. **链接外观是自己画出来的**：`MarkdownText` 用 `BasicText`（见 5.2），框架不会为 `LinkAnnotation` 补任何颜色或下划线。新增的链接类型若想要下划线/颜色，必须在 `visitInlineChild` 里显式写 `SpanStyle`。反过来，同一区间若被外部（例如 Material3 `Text`）接管，`SpanStyle` 会失效——这也是 `MarkdownText` 不能用 `Text` 的原因。
2. **表格的宽度测量不看行内占位符**：宽度趟用 `TextMeasurer` 量字符串（每格只组合一次），单元格里的 `$公式$` 按替换字符估算宽度，可能比实际略窄；正文右侧会自动换行，不会被裁掉。
3. **`@提及` 不可点击**：memos 客户端没有单用户主页，等有了再挂 `onMentionClick`。
4. **自动换行模式逐行排版**：为了行号对齐，折行模式下每个源码行是一个独立 `Text`（文本按行切片，高亮结果跨行仍然正确）；代价是跨行文本选择不连续。
5. **解析在后台，但组合在主线程**：超长笔记（数千行）首次渲染仍可能掉帧。
6. **不支持**：脚注、emoji 短代码、图片说明（caption）、表格单元格内换行、`~~` 之外的 GFM 扩展。
7. **标签是主题色胶囊，不按标签名区分颜色**：曾尝试按 CSS 做七彩胶囊，因亮度/对比度问题回退；现在统一用主题色的低透明度底，同一篇里所有标签长得一样。
8. 编译开了 `-Werror`，废弃 API（例如 `LocalClipboardManager`）会直接让构建失败，新增交互时优先用 Android 原生 API。

---

## 10. 改动指南

| 想改什么 | 改哪里 |
| --- | --- |
| 段落/标题/列表/换行间距 | `NativeMarkdownNode.kt` 顶部 `*SpacingEm` 常量 |
| 代码块配色、阴影、折叠行数、Tab 宽度 | `MarkdownCodeBlock.kt` 顶部常量 |
| 代码块行号 / 自动换行 / 首行缩进 / 中英空格 | `MarkdownFormat` 四个字段 + 设置页「编辑器」卡片 |
| 行内标记（`#标签`、`==高亮==`、`@提及`）的识别规则 | `MarkdownFormat.kt` 里的 `tokenizeInline` 与三个正则 |
| 中英文间距规则 | `addCjkLatinSpacing`（`MarkdownFormat.kt`） |
| 行内代码胶囊尺寸 | `NativeMarkdownNode.kt` 的 `drawInlineCodeBackground` |
| 公式尺寸缓存 | `LatexMetrics.kt` |
| 表格列宽策略 | `NativeMarkdownTable.kt`（`TextMeasurer` 测量趟 + `SubcomposeLayout` 组合趟） |
| 代码高亮配色 | `CodeHighlighter.kt`（`SyntaxThemes.default(isDarkMode)`） |
| 标题色板与字号阶梯 | `NativeMarkdownNode.kt` 的 `headingDecoration(level)` |
| 新增一种**行内**语法 | `visitInlineChild` 的 `when` 分支 |
| 新增一种**块级**元素 | `NativeMarkdownNodeRecursive` 的 `when` 分支，必要时加 CompositionLocal |
| 新增标题/正文样式来源 CSS | 丢进 `docs/`，在上面两处常量旁注明出处，保持"一处数值一处注释" |
| 编辑器高亮 | `MarkdownLanguageHandler.applyMarkdownStyles` |
| 编辑器回车行为 | `MarkdownListInputTransformation` / `MarkdownLanguageHandler.processInput` |

### 测试

| 测试 | 覆盖 |
| --- | --- |
| `MarkdownInlineStyleTest` | 行内样式契约：标签胶囊（主题色底、内边距字符、整颗可点）、`==高亮==`（背景色）、`@提及`（主色 + 字重、邮箱不算）、链接与自动链接（主色 + 下划线）、行内代码（反引号保留 + 底色只覆盖代码）、粗斜体删除线、首行缩进的适用判断 |
| `MarkdownInlineSyntaxTest` | 行内标记分词顺序、未闭合标记与邮箱的排除、中英文细空格边界 |
| `MarkdownStructureTest` | 段落按换行切片、间距表、标题字号/字重/行高阶梯与日夜色板 |
| `CodeBlockRenderingTest` | Tab 展开、行切分、折叠阈值 |
| `MarkdownCacheTest` | 解析缓存、引用表、代码高亮缓存 |
| `CodeLanguageLabelTest` | 围栏语言别名规范化 |
| `StandalonePreviewLinkTest` | 链接预览识别边界 |
| `MarkdownLanguageHandlerTest` | 编辑器回车续写与高亮基本行为 |
| `MarkdownListInputTransformationTest` | 新 API 下的续写前缀 |

行内/块级样式测试跑在 JVM 上：用 `ContextWrapper(null)` 充当 Context（只有行内公式分支会用到它，测试路径不经过），直接调用真实的 `visitInlineChild` 与 `splitLines`，断言渲染器最终交给文本组件的 `AnnotatedString`。样式一改，测试先红。

渲染层仍没有 Compose UI 测试（`androidTest` 里只有地图相关用例），纯绘制（圆角、阴影、间距像素）靠构建后人工验收。
