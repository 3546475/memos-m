# Miuix 主题分阶段改造计划

> Phased plan for the Miuix (HyperOS) design language. The MD path must stay byte-for-byte
> unchanged; every stage is shippable on its own.

本文档记录「设置 → 常规 → 主题风格」开关（`ThemeStyle.MD` / `ThemeStyle.MIUIX`）的改造路线。
目标是让 MIUIX 成为与 MD 平级的一套完整设计语言，而不是换一层配色。

---

## 1. 背景结论

第一版只做了 **token 级换肤**（把 Miuix 的 `Colors` 映射进 `MaterialTheme`）。因为「配色主题 =
跟随系统」走的是 Material You Monet，两套调色板同源，视觉差异极小，用户感知等于「没变」。

第二版（提交 `4b883ce`）开始做 **组件级替换**：新增自适应组件层，MIUIX 下顶栏、底部导航、
标签页、FAB、卡片、设置行等改用 Miuix 组件；同时把中性色改成 HyperOS 自己的层级
（浅色：`#F7F7F7` 页面 + 白卡片；深色：纯黑页面 + `#242424` 卡片），强调色才跟随配色来源。

结论：**必须逐组件替换**，无法通过主题令牌一次性解决。因此需要分阶段推进，每阶段可独立交付。

---

## 2. 架构约定（所有阶段通用）

### 2.1 主题状态

| 位置 | 作用 |
| --- | --- |
| `model/AppearancePreferences.kt` → `ThemeStyle { MD, MIUIX }` | 持久化的主题风格偏好（默认 `MD`） |
| `ui/theme/Theme.kt` → `MemosMTheme` | 按 `themeStyle` 分派：MD 走原 Material 3 分支，MIUIX 走 `MiuixMemosMTheme` |
| `ui/theme/MiuixTheme.kt` | Miuix 调色板、`LocalThemeStyle`、`LocalMiuixColors`、色板 → `ColorScheme` 映射、字阶映射 |
| `ui/theme/AppComponents.kt` | 自适应组件层：`AppTopBar` / `AppCard` / `AppSurface` / `AppListItem` / `AppSwitch` / `AppCheckbox` / `AppRadioButton` / `AppFloatingActionButton` / `AppButton` / `AppDivider` |

### 2.2 迁移规则

1. **只新增 `App*` 组件，不在界面里直接 `if (MIUIX)` 分叉。** 例外：结构性差异（如
   `MainScreen` 的导航条、`MemoFeedTabs` 的标签行）允许在调用点分叉，但要保持两侧语义一致。
2. **MD 分支必须零回归。** 迁移一个组件前后，MD 模式的像素与交互不能变化；用真机截图对比。
3. **Miuix 组件必须运行在 `MiuixTheme` 之下。** 自适应层负责补一层
   `MiuixTheme(colors = LocalMiuixColors.current)`，避免组件回退到库默认色。
4. **不引入第二套业务逻辑。** 适配层只做渲染分派；状态、点击、语义（`Role`、`toggleable`、
   a11y）沿用调用方。
5. **组件命名 `AppXxx`，参数保持最小公共子集**（只暴露调用方真正需要的参数），
   不照搬 Material 的全部参数。
6. **`miuix-preference` 等新模块按需引入**，且只在 `MIUIX` 分支使用，避免默认包体膨胀讨论
   反复（当前只依赖 `miuix-ui`）。

### 2.3 组件对照表

| Material 3（当前） | Miuix（`miuix-ui`） | 状态 |
| --- | --- | --- |
| `TopAppBar` / `LargeTopAppBar` | `SmallTopAppBar` / `TopAppBar` | 已接入（`AppTopBar`） |
| `NavigationBar` | `NavigationBar` + `NavigationBarItem` | 已接入 |
| `NavigationRail` | `NavigationRail` + `NavigationRailItem` | 已接入 |
| `PrimaryTabRow` | `TabRow` | 已接入（Feed 标签） |
| `FloatingActionButton` | `FloatingActionButton` | 已接入（`AppFloatingActionButton`） |
| `Card` | `Card` | 已接入（`AppCard`） |
| `ListItem` | `BasicComponent` | 已接入（`AppListItem`），仍有剩余调用点 |
| `Switch` | `Switch` | 已接入（`AppSwitch`） |
| `Checkbox` | `Checkbox` | 已接入（`AppCheckbox`），待推广 |
| `RadioButton` | `RadioButton` | 已接入（`AppRadioButton`），待推广 |
| `HorizontalDivider` | `HorizontalDivider` | 已接入（`AppDivider`） |
| `Button` | `Button` | 已接入（`AppButton`） |
| `TextButton` | `TextButton(text:)`（无 slot 重载） | 已接入（`AppTextButton(text)`）；调用点需传入纯文本 |
| `OutlinedTextField` | `TextField`（`TextFieldState` / `TextFieldValue` / `String` 三种 API） | 已接入（`AppTextField`，String 版）；密码框见 §6 |
| `AlertDialog` | `OverlayDialog`（需 popup host）/ `WindowDialog`（独立窗口） | 已接入（`AppDialog` → `WindowDialog`），待推广（P3） |
| `ModalBottomSheet` | `OverlayBottomSheet` / `WindowBottomSheet` | 待迁移（P3） |
| `DropdownMenu` | `Dropdown` / `ListPopup` / `miuix-preference` 的 `*DropdownPreference` | 待迁移（P3） |
| `SnackbarHost` | `SnackbarHost`（自有 `SnackbarHostState` 类型，需要桥接） | 待迁移（P3，见 §6） |
| `SearchBar` | `SearchBar`（`InputField` 组合） | 待迁移（P2） |
| `FilterChip` | 无 chip 组件 | 已接入（`AppFilterChip`，自绘胶囊） |
| `Slider`（含 `SliderState`） | `Slider(value, onValueChange, …)`（连续，无 steps） | 已接入（`AppSlider`，按步长吸附模拟 steps） |
| `LinearProgressIndicator` / `CircularProgressIndicator` | `LinearProgressIndicator` / `CircularProgressIndicator` | 已接入（`AppLinearProgressIndicator` / `AppCircularProgressIndicator`） |
| `PullToRefreshBox` | `PullToRefresh` | 待迁移（P2） |
| `Badge` | `Badge` | 待迁移（P2） |
| `MaterialExpressiveTheme`（`ProfileTheme`） | 无直接对应 | 已决策：MIUIX 下保留但只用于动效（P1） |
| 顶栏/底栏毛玻璃 | `miuix-blur`：`rememberLayerBackdrop` + `Modifier.drawBackdrop { blur(…) }` | 可选增强（P2 备注，minSdk 33） |

---

## 3. 进度总览

| 阶段 | 内容 | 状态 |
| --- | --- | --- |
| **P0 已完成** | 主题开关 + 偏好持久化 + 备份；调色板；适配层雏形；顶栏 / 导航 / 标签 / FAB / 卡片 / 设置行 / 开关 | ✅ `329d7bf`、`4b883ce` |
| **P1 主题底座** | 令牌与映射收口、适配层补齐、字体/圆角/密度、主题测试与截图基线 | ✅ `4b883ce` 之后（见下节） |
| **P2 主界面骨架** | Scaffold / 搜索栏 / 筛选 / 下拉刷新 / 进度与空态 / 滚动与 overscroll / 四个主 tab | 🔄 第一轮完成（搜索栏、筛选、下拉刷新、进度） |
| **P3 弹层与浮层** | 对话框 / 底部弹层 / 下拉菜单 / Snackbar / ActionSheet / 权限与冲突弹窗 | ⬜ |
| **P4 设置页与偏好组件** | 设置子页、偏好行、输入框、滑条、备份恢复长表单、账号与审计 | ⬜ |
| **P5 内容与媒体** | Memo 卡片细节、Markdown、附件与媒体播放器、编辑器 | ⬜ |
| **P6 收尾验收** | 深色/字体/动画一致性、a11y、性能、回归清单、文档与截图 | ⬜ |

> 每个阶段独立提交、独立可装机验证；P2 → P3 → P4 有依赖（弹层宿主由主 Scaffold 提供），
> 建议按顺序推进。P5 可与 P4 并行。

---

## 4. 阶段详解

### P0 已完成（基线）

**已交付**

- 「设置 → 常规」的 `主题风格` 开关（`ThemeStyleRow`），偏好写入 DataStore，包含在设置备份中。
- MIUIX 调色板：HyperOS 中性色阶固定；强调色来自所选配色来源（系统 / 黑白 / Memos / 自定义）。
  色板 → `ColorScheme` 映射见 `MiuixTheme.toMaterialColorScheme`。
- 字阶：Miuix `TextStyles` 映射到 Material `Typography`（保留字体设置）。
- 适配层与首批迁移：
  - `AppTopBar`：设置页、通知、草稿、归档、编辑资料、凭据、登录、小组件配置等 12 处顶栏。
  - `AppListItem`：设置总览、常规设置、外观设置、开关行、导航行。
  - `MainScreen`：底部 `NavigationBar` / 侧边 `NavigationRail`。
  - `MemoFeedTabs`：Miuix 分段标签。
  - `MemosScreen`：Miuix 圆形 FAB。
  - `MemoItem`：Miuix 卡片。
  - `AppSwitch` / `AppCheckbox` / `AppRadioButton`。

**已知遗留问题**

1. `ProfileTheme`（`MaterialExpressiveTheme`）会把 `surface`/`background` 覆盖成
   `surfaceContainerLow`；映射已把 `surfaceContainerLow` 对齐页面色，但表达式的动效（motion）
   仍来自 Material，需在 P1 决策。
2. `NavigationBarItem` 只接受 `ImageVector`：个人页 tab 的**头像**退化成 `Person` 图标
   （头像仍显示在个人主页顶部）。P2 决定是接受、还是自绘一个支持 slot 的 Miuix 风格导航项。
3. `AppListItem` 的 `BasicComponent` 分支用了 `Modifier.background` 上色，按下态高亮由
   `BasicComponent` 自己处理；需要在 P4 统一「分组卡片 + 行内分隔」的视觉。

---

### P1 主题底座（地基，先做能少返工）

**状态：已完成**（`4b883ce` 之后的未提交改动合并进来后单独提交）

**已交付**

- `ui/theme/MiuixTokens.kt`：页边距、分组/行间距、圆角（行 4 / 卡片 16 / 弹层 28 / 胶囊 50）、
  行内边距、图标尺寸、分隔线粗细，以及 MIUIX 下使用的 Material `Shapes`。
- 适配层新增：`AppDialog`（`WindowDialog` 方案）、`AppTextButton`、`AppTextField`（String 版）、
  `AppSlider`（按步长吸附模拟 steps）、`AppLinearProgressIndicator`、`AppCircularProgressIndicator`、
  `AppFilterChip`（自绘胶囊）；`AppCard` / `AppListItem` 改用令牌。
- 弹层宿主定案：**方案 B（`WindowDialog`）**，不改主 `Scaffold`。`AppDialog` 已按此实现，
  并在首页「草稿提示」弹窗上做了首个冒烟迁移（真机验证：底部圆角卡片 + 双胶囊按钮）。
- 单测：`MiuixColorMappingTest` 断言 MIUIX 的中性色结构（白卡片/灰页面、深色黑页面、
  强调色与文本角色映射）。
- 截图基线：`app/build/screens/`（本地，不提交）：`miuix-*.png` / `md-*.png`。

**范围**

1. **令牌收口** `ui/theme/`：
   - `MiuixTokens.kt`：间距（行高、页边距、分组间距）、圆角（8/12/16/24/28）、
     分隔线粗细、图标尺寸、按下态。
   - `MiuixShapes` / 字阶映射改为引用令牌，不再散落 `dp` 字面量。
   - 明确「哪些 Material 角色由 Miuix 决定、哪些继承配色来源」，写进映射函数注释。
2. **适配层补齐**（本阶段只加组件，不改调用点；已完成的见上）：
   - ✅ `AppTextField`（String 版；`TextFieldState` 版留给密码/富文本场景）
   - ✅ `AppSlider`（`value/onValueChange/range/steps`）
   - ✅ `AppProgressIndicator`（linear / circular）
   - ✅ `AppDialog`（宿主方案已定）
   - ✅ `AppChip`（筛选/标签胶囊）
   - ⏸ `AppSnackbarHost`：Miuix 的 `SnackbarHostState` 是自有类型，无法直接兼容调用方的
     `showSnackbar` 协程；桥接放到 P3（见 §6）。
   - ➡ `AppSearchBar`：Miuix `SearchBar` 需要 `InputField` 组合且交互模型不同，与调用点一起改
     更省事，移到 P2。
3. **弹层宿主决策**（阻塞 P3，必须在 P1 定案）：
   - 方案 A：主页面 `Scaffold` 换成 Miuix `Scaffold`，提供 `MiuixPopupHost`，弹层用 `OverlayDialog` /
     `OverlayBottomSheet`（与 HyperOS 视觉最接近，但 Scaffold/insets 需要整体回归）。
   - 方案 B：弹层用 `WindowDialog` / `WindowBottomSheet`（独立窗口，不动主 Scaffold，改动最小）。
   - **决策：B 先行**，需要弹层内嵌套 `Scaffold` 的场景再评估 A。
4. **字体与排版**：确认 Miuix 字阶在中文、日文、韩文下的行长与断行；`AppFont` 四种字体逐一截图。
5. **深色与动态色**：深色（黑页面 + `#242424` 卡片）、跟随系统取色、黑白、Memos、自定义色相
   五种组合 × 深浅色各一张基线截图。
6. **测试**：
   - 单测：`MiuixTheme.toMaterialColorScheme` 映射的关键角色断言（纯函数，无需 Robolectric）✅
   - 截图基线：真机 `adb exec-out screencap`，保存在 `app/build/screens/`（构建产物目录，不提交）。

**验收**

- [x] 组件对照表中「已接入」的组件在 MIUIX 下无 Material 默认色泄漏（残留在待迁移组件里）。
- [x] MD 模式关键页面（首页、设置）与修改前截图一致（`md-*.png` 基线）。
- [ ] 五种配色 × 深浅色截图齐全，无明显对比度问题（当前只覆盖浅色 + 跟随系统）。
- [x] 新增的 `App*` 组件均有 KDoc，说明两侧分支行为。

**风险**：字阶差异会让部分页面行高变化（尤其设置页），需要逐页看；`MaterialExpressiveTheme`
可能与 Miuix 的字阶/动效冲突。

---

### P2 主界面骨架

**目标**：四个主 tab 的「骨架」全部 HyperOS 化：顶栏、搜索、筛选、下拉刷新、空态、进度、
滚动反馈、悬浮元素。

**范围（按文件）**

| 文件 | 内容 |
| --- | --- |
| `ui/MainScreen.kt` | 已完成导航条/侧栏；剩余：外层 `Surface`/背景、`AnimatedContent` 转场时长与 Miuix 动效对齐 |
| `ui/nav/MemosScreen.kt` | `SearchBar`、`FilterChip`（13 处/4 文件）、`PullToRefreshBox`、同步状态条、空态、`LinearProgressIndicator` |
| `ui/nav/AttachmentsScreen.kt` | 分组头、列表容器、空态 |
| `ui/nav/MapScreen.kt` | 外围栏与搜索、筛选 chip（地图本体由 MapLibre 渲染，不在范围内） |
| `ui/nav/ProfileScreen.kt` | `ModalBottomSheet`（账号切换）、行样式、日历/统计卡片 |
| `ui/nav/MemoFeedTabs.kt` | ✅ 已完成 |
| `ui/component/MemosCommon.kt` | 共用 `SearchBar` 封装 |
| 全局 | `SnackbarHost`（4 处/4 文件）、`CircularProgressIndicator`（约 26 文件）、`PullToRefresh`、滚动条 |

**进度：第一轮已完成**（首页搜索栏、筛选胶囊、下拉刷新、加载指示已切到 Miuix）

**做法**

- 搜索栏：`MemoSearchBar` 内部按 `LocalThemeStyle` 分叉 —— MIUIX 用 Miuix
  `SearchBar` + `InputField`，MD 保留原有 M3 `SearchBar`/`SearchBarDefaults.InputField`。
  ⚠️ Miuix `SearchBar` 的 `content` **不画背景**，展开时必须在内容外包一层不透明容器
  （当前用 `Box(Modifier.fillMaxSize().background(colorScheme.surface))`），否则列表会透出来。
- `FilterChip`：Miuix 无 chip 组件，`AppFilterChip` 用自绘胶囊（`Surface` + 圆角 + 选中态）实现，
  已替换首页标签/快捷方式筛选、地图 scope、搜索标签、审计日志分类。
- 下拉刷新：`AppPullToRefresh` 封装 Miuix `PullToRefresh` / M3 `PullToRefreshBox`，
  落在共享的 `GenericMemosListPane` 上，四个主 tab 与搜索/归档列表同时受益。
  ⚠️ 指示器当前从列表顶部（列表头下方）出现，若与自定义 header 冲突，需要用
  `contentPadding` 或 `topAppBarScrollBehavior` 调位置。
- 进度指示：`AppCircularProgressIndicator` / `AppLinearProgressIndicator` 已替换
  `MemosCommon`、`MemosScreen`、`AttachmentsScreen`、`ProfileScreen`、`SearchScreen`。
  带自定义尺寸/描边的场景（地图草稿指示、同步面板）暂留 Material。
- overscroll / 按压反馈：`MiuixTheme` 已提供 `LocalOverscrollFactory` 与 `LocalIndication`，
  确认外层 `ComposeView` 未被覆盖。

**验收**

- [x] 首页：搜索栏、筛选胶囊、下拉刷新、加载中已为 Miuix 样式。
- [ ] 首页空态与同步状态条仍为 Material，留待下一轮。
- [ ] 附件页、个人页、地图页首屏视觉一致；四个 tab 切换动画无卡顿（首屏已看，地图未动）。
- [x] Android 手势返回 / 滚动 / 下拉刷新交互无回归（真机验证）。
- [x] MD 模式首页截图与改造前一致（`md-*.png` 基线）。

#### 可选增强：顶栏 / 底栏毛玻璃（`miuix-blur`）

HyperOS 的顶栏、底栏和悬浮工具栏在滚动内容上方是半透明模糊的。Miuix 把这项能力拆在
`miuix-blur` 模块里，本阶段只做设计与评估，实现放在 P6（或独立小 PR）。

**API 形态**

```kotlin
val backdrop = rememberLayerBackdrop()
// 内容层：把需要被模糊的内容记录进 backdrop
Box(Modifier.layerBackdrop(backdrop)) { content() }
// 顶栏：绘制模糊后的背景
AppTopBar(
    modifier = Modifier.drawBackdrop(backdrop) {
        blur(radiusX = 24f)
        // 可选：progressiveBlur(...) 让顶部更模糊
    }
)
```

**接入代价与约束**

1. 依赖 `top.yukonga.miuix.kmp:miuix-blur:0.9.4`，其 Android 变体声明 **minSdk 33**，而本 App 的
   minSdk 是 24 —— 直接加依赖会在 manifest 合并阶段失败。方案：
   - 在 `AndroidManifest.xml` 的 `<application>` 上加
     `tools:overrideLibrary="top.yukonga.miuix.kmp.blur,top.yukonga.miuix.kmp.shader"`；
   - 所有调用点用 `Build.VERSION.SDK_INT >= 33 && isRuntimeShaderSupported()` 双重判断，
     低版本退化成现在的纯色顶栏（不透明）。
   - 需要单独验证：minSdk 24 真机启动不崩、类加载不提前触发。
2. `renderEffect` / `RuntimeShader` 在部分机型（尤其 MIUI 的省电模式、分屏）可能不可用，
   必须提供无模糊的降级路径。
3. 性能：`LayerBackdrop` 会额外渲染一层内容层。首页列表滚动时要测帧率，必要时只在
   `LazyColumn` 静止时采样或降低模糊半径。
4. 视觉：模糊层要配合 `surface` 的 alpha（HyperOS 顶栏背景约 0.7 透明度 + 0.5dp 分隔线），
   需要与 `MiuixTokens` 里的颜色一起调。
5. 该能力**仅 MIUIX** 使用，MD 分支不引入任何模糊代码路径（避免 MD 回归与性能负担）。

**验收**

- [ ] API 33+ 真机：首页滚动时顶栏/底栏呈毛玻璃，内容从下方穿过。
- [ ] API 24–32 真机或模拟器：启动正常，顶栏为纯色，无崩溃、无类加载异常。
- [ ] 帧率与内存对比基线无明显回退。
- [ ] MD 主题下完全不启用（代码路径与视觉都不变）。

---

### P3 弹层与浮层

**目标**：所有「浮起来的东西」统一为 HyperOS 弹层：对话框、底部弹层、下拉菜单、Snackbar。

**范围（按数量）**

> 下表次数为 `grep` 命中数（可能包含 `import` 行），用于排期参考，具体以逐文件迁移时为准。

| 组件 | 数量 | 文件 |
| --- | --- | --- |
| `AlertDialog` | 18 次 / 18 文件 | `CacheCleanupDialog`、`MemoEditDeleteDialog`、`ConflictDialog`、`AttachmentActionsButton`、`DraftsCard`、`MemoItem`、`LocalNetworkPermission`、`AccountsList`、`AppearanceSettingsContent`、`AppSettingsCard`、`OfflineSettingsCard`、`RecoveryCard`、`SettingsCard`、`ShortcutsCard`、`WebhookCard`、`SyncStatusWidget`、`DraftsScreen`、`MemosScreen` |
| `ModalBottomSheet` | 8 次 / 4 文件 | `ActionSheet`、`ReactionPickerDialog`、`SyncStatusWidget`、`ProfileScreen` |
| `DropdownMenu` | 42 次 / 6 文件 | `MemoComposerBottomBar`、`MemoInput`、`VideoPlaybackControls`、`SearchScreen`、`OfflineSettingsCard`、`MapScreen` |
| `SnackbarHost` | 4 处 / 4 文件 | 各 `Scaffold` |

**做法**

1. 先实现 `AppDialog(title, content, confirm, dismiss, onDismissRequest)`：
   - MIUIX → `WindowDialog`（方案 B）；MD → `AlertDialog`。
   - 保留 `TextButton` 语义，但按钮用 Miuix `TextButton(text:)`，需要把调用点的
     `confirmButton = { TextButton(...) { Text(...) } }` 收敛成 `confirm = { onConfirm -> … }` 形态，
     逐文件改（18 处，机械但需要人工确认文案来源）。
2. `AppBottomSheet`：MIUIX → `WindowBottomSheet`；先迁移 `ActionSheet`（它是通用壳），
   `ReactionPicker`、账号切换随之受益。
3. `AppDropdownMenu`：MIUIX → Miuix `Dropdown`（或引入 `miuix-preference` 的菜单组件）。
   注意视频播放器里的菜单位置计算（`VideoPlaybackControls`）不做替换，仅换样式。
4. `AppSnackbarHost`：MIUIX → Miuix `SnackbarHost` 并接到 `Scaffold`。
5. 特殊弹窗：`ConflictDialog`、`LocalNetworkPermission`、`CacheCleanupDialog` 需要单独评估
   （内容较长、按钮语义特殊），允许保留 Material 结构、只换底色与圆角。

**验收**

- [ ] 18 处对话框在 MIUIX 下视觉一致（圆角、标题、按钮排布、遮罩透明度）。
- [ ] 删除确认、草稿提示、冲突解决、权限请求、备份恢复确认流程无回归。
- [ ] 底部弹层出现/消失动画为 Miuix spring；MD 仍为 Material 动效。
- [ ] 弹层内 `Scaffold`/insets 正确（尤其手势导航条区域）。

---

### P4 设置页与偏好组件

**目标**：设置体系全部 HyperOS 化，并抽象出可复用的偏好行。

**范围**

> 下表次数为 `grep` 命中数（可能包含 `import` 行），用于排期参考。

| 组件 | 数量 | 说明 |
| --- | --- | --- |
| `ListItem` 剩余 | 19 次 / 12 文件 | `AccountsList`、`AuditLogCard`、`OfflineSettingsCard`、`RecoveryCard`、`ShortcutsCard`、`WebhookCard`、`AboutAppCard`、`ProfileScreen` 等 |
| `OutlinedTextField` | 24 次 / 7 文件 | `LoginScreen`、`EditProfileScreen`、`ShortcutsCard`、`WebhookCard`、`AppSettingsCard`（分页大小）、`OfflineSettingsCard`、`MemoComposer` |
| `Slider` | 8 次 / 4 文件 | 外观「标题大小」、离线缓存上限、音频/视频进度（后两者保留自定义控件） |
| `Switch` / `Checkbox` / `RadioButton` 残留 | 约 18 处 | 设置与备份选择列表 |
| `SettingsGroup` / `SettingsLabel` / `SettingsSurface` | 3 个容器 | 分组圆角、间距、分隔线 |
| 备份恢复长表单 | `RecoveryCard`（356 行）、`BackupTransferActivity` | 分类勾选、密码输入、账号选择 |

**做法**

1. 在 `ui/component/setting/` 收敛出行原语：`SettingsRow`（导航/开关/滑条/输入）、
   `SettingsSection`（标题 + 分组卡片 + 行）。之后所有设置页只用这些原语。
2. 评估引入 `miuix-preference`（`SwitchPreference`、`ArrowPreference`、`RadioButtonPreference`、
   `SliderPreference`）。引入后需在 `gradle/libs.versions.toml` 增加一个库，且只在 MIUIX 分支使用；
   好处是交互（按压缩放、行内切换动画）完全原生。
3. `AppTextField` 落地：
   - `LoginScreen` / `EditProfileScreen` / `WebhookCard` / `ShortcutsCard` 用 `TextFieldValue` 版本。
   - `MemoComposer` 的自定义编辑器不做替换（富文本 + 附件 + 位置），只调色与圆角。
   - 密码框：Miuix 无 `OutlinedSecureTextField` 对应，先用 `TextField` + `visualTransformation`。
4. 分组视觉统一：`BasicComponent` 行默认透明，靠容器上色；需要重做 `SettingsGroup`
   的「整卡裁剪 + 行间 2dp」为「整卡 + 行内分隔线」两种方案各截一版，选定一种。
5. `AuditLogCard` / `AccountsList` 的复杂行（多行副文本、尾部按钮）先做「容器 + 文本层级」统一，
   不追求 1:1 组件替换。

**验收**

- [ ] 设置总览、常规、外观、离线、备份恢复、关于、账号列表全部为 Miuix 视觉。
- [ ] 所有开关/单选/滑条在 MIUIX 下为 Miuix 组件，状态联动与 a11y 语义不变。
- [ ] 输入框在键盘弹起、IMM 切换、`imePadding` 下布局正确。
- [ ] 备份导入/导出全流程可走通（这是最容易回归的区域）。

---

### P5 内容与媒体组件

**目标**：内容区（笔记本身）观感统一；媒体控件保持自有实现但配色/圆角跟随主题。

**范围**

- `MemoItem`（卡片已换，剩余：操作按钮、reaction chip、时间与可见性图标、展开/收起动画）
- Markdown 渲染：段落、引用、代码块高亮、表格、链接预览卡（`NativeMarkdown*`）
- 附件卡与选择器：`AttachmentCard`、`AttachmentInfoSheet`、`FileThumbnail`
- 媒体查看器：`FullScreenImageViewer`、`VideoPlayer`、`AudioPlayer`（只换色/圆角/进度样式）
- 编辑器：`MemoComposer*`、`MemoInput`、底部工具栏、位置/录音/附件入口
- 搜索页 `SearchScreen`、通知页、草稿卡、归档页

**做法**：优先统一「容器（卡片/弹层）+ 文本层级 + 图标色」，媒体控件仅替换可见 Material 组件
（`Slider`、`IconButton`、`Menu`），不能替换的（ExoPlayer 控件、Coil 图片）只做 tint/Shape 适配。

**验收**：写笔记、看详情、看图片/视频/音频、搜索、通知、草稿全流程视觉一致，性能无明显下降。

---

### P6 收尾与验收

- **一致性检查**：圆角、间距、分隔线、图标尺寸、按压反馈四项全 App 扫描（可写 grep 脚本，
  禁止在 MIUIX 分支新增 `dp` 字面量、禁止直接使用 `androidx.compose.material3.*`）。
- **lint 规则（可选）**：自定义 lint 或 CI grep，限制 `ui/` 下直接 import Material 组件
  （白名单 `ui/theme/AppComponents.kt`）。
- **深色与纯黑**：HyperOS 深色为 #242424 卡片 + 黑页面，已实现；可选「纯黑模式」后续再说。
- **动画**：Miuix 组件自带 spring；自有转场（`AnimatedContent`、`AnimatedVisibility`）统一时长曲线。
- **a11y**：`BasicComponent` 行需要显式 `role`/`onClickLabel`；导航项选中态可读；对比度检查。
- **性能**：`MiuixTheme` 包裹范围（目前每个 `AppSwitch` 都套一层）可考虑提到页面级，避免重复组合。
- **文档与截图**：`docs/screens/miuix/` 保存 MD/MIUIX 双份基线截图；更新本计划状态表与 README。
- **回归清单**（每次发版前）：
  1. 冷启动 → 登录 → 首页加载 → 刷新 → 下拉刷新
  2. 新建/编辑/删除笔记，附件上传，位置与录音
  3. 搜索、筛选、地图定位
  4. 设置：切换主题风格 → 切换配色 → 切字体 → 切深浅色
  5. 备份导出 → 恢复 → 冲突解决
  6. 小组件（不属于 Miuix，但确认未被破坏）

---

## 5. 阶段通用完成标准（DoD）

1. `./gradlew :app:compileDebugKotlin` 通过（项目开启 `allWarningsAsErrors`）。
2. `./gradlew :app:assembleDebug` 通过，并 `adb install -r` 到真机。
3. MD / MIUIX 双主题关键页面截图对比，差异只在预期处。
4. 该阶段涉及的功能手工走查一遍（见各阶段验收）。
5. 一个阶段一次提交，提交信息说明「迁移了哪些组件、哪些没动、为什么」。
6. 本文件状态表同步更新。

---

## 6. 已知问题与决策记录

| 问题 | 现状 / 决策 |
| --- | --- |
| 个人页 tab 头像 | Miuix `NavigationBarItem` 只接受 `ImageVector`；当前退化为 `Person` 图标，头像保留在个人主页。P2 决策：接受，或自绘支持 slot 的导航项。 |
| `ProfileTheme` 使用 `MaterialExpressiveTheme` | 会把 `surface`/`background` 覆盖为 `surfaceContainerLow`（已对齐页面色）。**已决策**：MIUIX 下保留（只影响动效），不绕开。 |
| 弹层宿主 | **已决策（P1）：方案 B** —— `AppDialog` 用 `WindowDialog`，不回填 popup host，不动主 `Scaffold`。若后续需要 `OverlayBottomSheet` 的遮罩/layer 行为再评估方案 A。 |
| `TextButton` 无 slot 重载 | **已解决（P1）**：新增 `AppTextButton(text: String, …)`；需要自定义内容的按钮继续用 Material（并在 P3 逐个收敛）。 |
| `VisualTransformation` 已废弃 | 该 Compose 版本要求改用 `OutputTransformation`；`AppTextField` 因此不暴露密码变换参数。密码框现状是 `OutlinedSecureTextField`，P4 再设计 `AppSecureTextField`。 |
| `SnackbarHostState` 类型不兼容 | Miuix `SnackbarHostState` 与 Material 的不是同一类型，而调用方用 `showSnackbar` 协程等待显示完成；直接替换会让协程挂起。桥接方案（双 State 或适配器）放 P3。 |
| Miuix `SearchBar` 内容无背景 | 展开时 `content` 是透明的，必须自己铺不透明底（否则下层列表透出）。已在 `MemoSearchBar` 内用 `Box(fillMaxSize().background(surface))` 处理；其它复用点要注意同样问题。 |
| Miuix `PullToRefresh` 指示器位置 | 指示器从滚动内容顶部开始，若页面有自定义 header，需要 `contentPadding` 或把 header 移出滚动区，否则会被 header 遮住。首页当前可接受，附件/通知页接入时再调。 |
| Glance 小组件 | 使用 `glance-material3`，**不在改造范围**；仅确认主题切换不影响其渲染。 |
| 第三方视图（MapLibre、ExoPlayer 控件、Coil） | 不做组件替换，只做颜色/形状适配。 |
| `miuix-blur` | 顶栏/底栏毛玻璃，依赖声明 **minSdk 33**（本 App minSdk 24）：需 `tools:overrideLibrary` + 运行时双重降级判断。设计见 P2「可选增强」，实现放 P6 或独立 PR。 |
| 截图基线 | 保存在 `app/build/screens/`（构建产物目录，**不提交**）；需要长期比对时再挑关键页放进 `docs/screens/` 并允许二进制入库。 |
| 本地验证环境 | 本机 `gradle/gradle-daemon-jvm.properties` 要求 JetBrains JDK 21（会自动下载并卡住）；离线验证时临时移开该文件，用 `JAVA_HOME=<JDK21>` + 本地 Gradle 9.7.1 构建，验证完必须恢复，不要提交。 |
| 单测环境 | Windows 上 DataStore 二次写入会因文件重命名失败（`SetupSettingsTest` 同因失败），涉及 DataStore 的用例请在 Linux/CI 跑。 |

---

## 7. 附：新增自适应组件模板

```kotlin
/** One-line: what this renders in each design language. */
@Composable
fun AppFoo(
    required: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    if (usesMiuix) {
        MiuixTheme(colors = LocalMiuixColors.current) {
            MiuixFoo(value = required, modifier = modifier) { content() }
        }
    } else {
        MaterialFoo(value = required, modifier = modifier) { content() }
    }
}
```

要点：

- `usesMiuix`（`AppComponents.kt` 内私有属性）判断当前风格。
- Miuix 组件一律包在 `MiuixTheme(colors = LocalMiuixColors.current)` 里。
- 适配层只做渲染分派，状态与语义留在调用方。
- 新增组件后在 §2.3 对照表登记，并补一条 KDoc。
