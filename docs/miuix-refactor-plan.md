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
| **P3 弹层与浮层** | 对话框 / 底部弹层 / 下拉菜单 / Snackbar / ActionSheet / 权限与冲突弹窗 | 🔄 第四轮完成（17 处弹窗 + 2 个弹层 + 3 处菜单 + Snackbar） |
| **P4 设置页与偏好组件** | 设置子页、偏好行、输入框、滑条、备份恢复长表单、账号与审计 | ✅ 完成（三批） |
| **P5 内容与媒体** | Memo 卡片细节、Markdown、附件与媒体播放器、编辑器 | 🔄 第一轮完成（详情页、编辑器容器、草稿卡） |
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

**进度：两轮已完成**（首页搜索栏、筛选胶囊、下拉刷新、加载指示、错误重试、附件/通知下拉刷新、通知卡片、个人页信息行、主容器）

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
- 第二轮补齐：`ErrorView` 的重试按钮改 `AppButton`；`AttachmentsScreen`、`NotificationsScreen`
  的 `PullToRefreshBox` 改 `AppPullToRefresh`；通知卡片改 `AppCard`；个人页「实例信息」行改
  `AppListItem`；`MainScreen` 最外层 `Surface` 改 `AppSurface`。
- overscroll / 按压反馈：`MiuixTheme` 已提供 `LocalOverscrollFactory` 与 `LocalIndication`，
  确认外层 `ComposeView` 未被覆盖。

**验收**

- [x] 首页：搜索栏、筛选胶囊、下拉刷新、加载中已为 Miuix 样式。
- [x] 错误态/空态的重试按钮、附件页与通知页的下拉刷新、通知卡片、个人页信息行已切换。
- [ ] 首页同步状态条仍是纯图标+文字的 Material 排版（颜色已随主题，暂不改结构）。
- [ ] 地图页的操作栏与地图内浮层未动（地图本体由 MapLibre 渲染）。
- [x] 四个 tab 首屏视觉一致；切换动画无卡顿（平板侧栏细节留待 P6）。
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

**进度：第四轮已完成**（权限弹窗、备份弹窗、Snackbar 桥接）

本轮新增：
- `LocalNetworkPermission` 的拒绝提示、`RecoveryCard` 的删除确认与消息提示 → `AppDialog`；
  `AppDialog` 的 `title` 改为可选（无标题弹窗给 `WindowDialog` 传 null）。
- **Snackbar 桥接**：新增 `AppSnackbarHost(hostState)` —— Miuix 的 `SnackbarHost` 只接受自己的
  `SnackbarHostState`，无法直接喂 Material 的 state；做法是 MIUIX 下用 Material state 的
  `currentSnackbarData` 驱动一个 Miuix 胶囊，并按 `SnackbarDuration` 自行 `dismiss()`，
  保证调用方 `showSnackbar` 的协程能正常返回。登录页已接入。
- 真机验证：应用启动、首页渲染正常（本轮未逐屏验证每个弹窗）。

本轮新增：
- 表情选择 `ReactionPickerDialog` → `ActionSheet`（`WindowBottomSheet`）。
- 内容类弹窗 → `AppDialog`：`MemoItem` 原文查看（`heightIn(max = 420.dp)`）、
  `AttachmentActionsButton` 下载确认、`AccountsList` 移除账号确认、`SyncStatusWidget` 队列删除确认。
- 菜单 → `AppDropdownMenu`：地图视图切换、视频倍速。
- 真机验证：账号切换弹层（`WindowBottomSheet` + Miuix 行）、排序菜单。

本轮新增：
- 表单弹窗：`ShortcutEditDialog`、`WebhookEditDialog` → `AppDialog` + `AppTextField`
  + `AppLinearProgressIndicator`（保存中状态、错误文案、帮助链接图标都保留）。
- 账号切换：`ProfileScreen` 的 `ModalBottomSheet` 改走 `ActionSheet`（即 `WindowBottomSheet`），
  账号列表与逻辑不变；该页自己的下拉刷新也换成 `AppPullToRefresh`。
- 下拉菜单：新增 `AppDropdownMenu`（`WindowListPopup` + `ListPopupColumn`）与
  `AppDropdownMenuItem`（支持 leadingIcon / destructive），首个迁移点是搜索排序菜单。
  ⚠️ 菜单其余 5 个文件（写作工具栏、地图、离线设置、搜索页其余菜单、视频控制）仍是
  `DropdownMenu` / `ExposedDropdownMenu`，下一轮逐个替换。

已迁移到 `AppDialog`（`WindowDialog`）：
- 设置：语言/可见性选择（`SettingsCard`）、分页大小（`AppSettingsCard`，含 `AppTextField`）、
  快捷方式删除（`ShortcutsCard`）、Webhook 删除（`WebhookCard`）、外观三项（`AppearancePreferenceDialog`）
- 通用：`CacheCleanupDialog`（含 `AppCheckbox`）、`DeleteConfirmationDialog`（笔记删除）
- 草稿：`DraftsScreen` 单条删除、`DraftsCard` 全部删除 / 全部发布
- `ActionSheet` → `WindowBottomSheet`，`ActionSheetItem` → Miuix 行

仍在用 Material 的（下一轮）：`ConflictDialog`（三态切换 + diff + 合并编辑）、`MemoComposer`；
`RecoveryCard` 的密码输入与恢复确认弹窗（需要 `AppSecureTextField`，随 P4 一起做）；
写作工具栏的两个菜单（`MemoComposerBottomBar`，依赖 `PopupProperties(focusable = false)`，
`WindowListPopup` 没有对应参数，需要给适配层加配置）、离线设置的缓存上限可编辑下拉
（`menuAnchor(PrimaryEditable)`）。

**范围（按数量）**

> 下表次数为 `grep` 命中数（可能包含 `import` 行），用于排期参考，具体以逐文件迁移时为准。

| 组件 | 数量 | 文件 |
| --- | --- | --- |
| `AlertDialog` | 18 次 / 18 文件 | `CacheCleanupDialog`、`MemoEditDeleteDialog`、`ConflictDialog`、`AttachmentActionsButton`、`DraftsCard`、`MemoItem`、`LocalNetworkPermission`、`AccountsList`、`AppearanceSettingsContent`、`AppSettingsCard`、`OfflineSettingsCard`、`RecoveryCard`、`SettingsCard`、`ShortcutsCard`、`WebhookCard`、`SyncStatusWidget`、`DraftsScreen`、`MemosScreen` |
| `ModalBottomSheet` | 8 次 / 4 文件 | `ActionSheet`、`ReactionPickerDialog`、`SyncStatusWidget`、`ProfileScreen` |
| `DropdownMenu` | 42 次 / 6 文件 | `MemoComposerBottomBar`、`MemoInput`、`VideoPlaybackControls`、`SearchScreen`、`OfflineSettingsCard`、`MapScreen` |
| `SnackbarHost` | 4 处 / 4 文件 | 各 `Scaffold` |

**做法**

1. `AppDialog(show, title, summary, onDismissRequest, buttons, content)`：
   - MIUIX → `WindowDialog`（方案 B）；MD → `AlertDialog`。按钮统一用 `AppTextButton`（支持
     `destructive`），在 `buttons` 槽里用 `Modifier.weight(1f)` 排成等宽胶囊。
   - ⚠️ `WindowDialog` 的高度由内容决定：长列表（语言选择）必须给 `heightIn(max = …)`，
     否则弹窗会撑满整屏。
2. `AppBottomSheet`：`ActionSheet` 已切 `WindowBottomSheet`；`ReactionPicker`、账号切换随后。
   ⚠️ 调用方只在可见时组合，因此传 `show = true`，关闭动画由调用方移除组合承担（会少一段退场动画，
   需要的话改成「父级持有 show 状态」）。
3. `AppDropdownMenu`：MIUIX → Miuix `Dropdown`（或引入 `miuix-preference` 的菜单组件）。
   注意视频播放器里的菜单位置计算（`VideoPlaybackControls`）不做替换，仅换样式。
4. `AppSnackbarHost`：MIUIX → Miuix `SnackbarHost` 并接到 `Scaffold`。
5. 特殊弹窗：`ConflictDialog`、`LocalNetworkPermission` 需要单独评估
   （内容较长、按钮语义特殊），允许保留 Material 结构、只换底色与圆角。

**验收**

- [x] 第一轮 9 处对话框在 MIUIX 下视觉一致（28dp 圆角、标题居中、等宽胶囊按钮、遮罩）。
- [x] 删除确认、外观设置、缓存清理流程无回归（真机验证语言选择弹窗）。
- [x] 底部弹层 `ActionSheet` 走 `WindowBottomSheet`（代码已切，真机待跟随具体入口复验）。
- [ ] 其余弹窗、下拉菜单与 Snackbar 仍为 Material。
- [ ] 弹层内 `Scaffold`/insets 正确（尤其手势导航条区域）。

---

### P4 设置页与偏好组件

**目标**：设置体系全部 HyperOS 化，并抽象出可复用的偏好行。

**进度：第一轮已完成**（行原语 + 文本输入）

已完成：
- `AboutAppCard`（版本/Git Hash/仓库/问题反馈/许可证）、`ShortcutsCard`、`WebhookCard`、
  `ProfileScreen` 实例信息、`RecoveryCard` 备份选择行 → `AppListItem` / `AppSwitch`。
  `AppListItem` 支持 `leading` / `trailing` / `containerColor` / `onClick`，需要长按的行
  （版本号彩蛋）仍用 `combinedClickable` 挂在 `modifier` 上。
- `LoginScreen`（服务器地址/Token/用户名）与 `EditProfileScreen`（5 个字段）→ `AppTextField`；
  `AppTextField` 新增 `minLines`/`maxLines` 透传（Miuix 与 M3 都支持）。
- 真机检查：设置总览、常规页、个人页渲染正常。

**进度：已完成（第三轮收尾）**

第三轮已完成：
- 审计日志行改为「卡片 + 明细列」（一行里有时间、操作、账号、目标，`AppListItem` 的单行摘要装不下）。
- 账号行改为自定义 Row（头像 + 名称 + 主机地址），保留当前账号加粗；外层可滑动卡片保持 Material。
- 离线缓存的用量行、`SettingsSurface`、备份归档/待恢复编辑行、备份密码与恢复确认弹窗全部走适配组件。
- **配色修正**：Material 的 `primaryContainer` 之前映射自 Miuix `tertiaryContainer`（紫色系容器），
  所以设置分组图标是 Material You 的淡紫。现在改为 `primary` 12% 叠在 surface 上，
  图标底圈与选中行变成 HyperOS 的浅蓝。真机确认。

**已知例外（有意保留）**
- 离线缓存的「自定义上限」是可编辑 `ExposedDropdownMenu`（`menuAnchor(PrimaryEditable)`），
  Miuix 无等价控件，保留 Material 结构。
- 账号卡片外层 `Card` 承载横向拖拽删除手势，保留 Material。

**已知待处理**

第二轮已完成：
- `AppSecureTextField(state: TextFieldState, label, …)`：MD 走 `OutlinedSecureTextField`，MIUIX 走
  Miuix `TextField(value = …, visualTransformation = PasswordVisualTransformation())`。
  ⚠️ 该构造在本项目 Compose 版本已 deprecated（官方建议 `BasicSecureTextField`），但 Miuix 只提供
  `VisualTransformation` 这条路径，因此在函数上做了带注释的 `@Suppress("DEPRECATION")`；
  日后若要彻底去掉，需自己用 `BasicSecureTextField` + Miuix 容器重绘。
  登录密码、修改密码、备份密码三处已切换。
- `AppSlider(state: SliderState, onValueChange, valueRange)` 重载：复用已有的 value 版本。
  ⚠️ Material 的 `SliderState` 不暴露 `trackRange`/`onValueChangeFinished`，所以范围要由调用方传；
  外观「标题大小」「色相」两处已切换，真机确认是 Miuix 分段轨道样式。

**已知待处理**
- 设置总览的分组图标底圈是淡紫色（`primaryContainer` 取色），和 HyperOS 的浅蓝灰不一致，
  属于配色映射调优，放到 P6 统一定稿。
- 密码框已解决（见上）；`AccountsList` / `AuditLogCard` / `OfflineSettingsCard` 的行与开关、
  备份恢复长表单、账号编辑页仍是 Material。

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

**进度：第六轮已完成**（详情页、编辑器、Markdown 容器、媒体滑条、reaction 胶囊、附件卡）

- 新增 `AppReactionChip`：MIUIX 下「我点过」= 主色胶囊 + onPrimary 文字，否则灰胶囊；
  文字颜色由胶囊统一提供，调用方不再各自算对比色。
- `AttachmentCard` 外层 → `AppCard`（画廊模式 `cornerRadius = 0.dp` 保持方角，
  点击/长按仍挂在 modifier 上）；`AttachmentInfoSheet` 早前已走 `ActionSheet`。
- LaTeX 错误卡、音频播放器容器 → `AppCard`。
- `AppExtendedFloatingActionButton` 改用 `buttonColorsPrimary()`——Miuix 默认的次级灰
  让「添加评论」看起来像禁用态，真机已确认改成主色胶囊。
- Markdown 链接预览卡 → `AppCard` + `clickable`；代码块/表格分隔线 → `AppDivider`；
  音频/视频进度条 → `AppSlider`（补 `enabled` 与 `onValueChangeFinished`）。

**进度：第一轮已完成**（详情页、编辑器容器、草稿卡）

- 详情页：外层 `Surface` → `AppSurface`（圆角裁剪用 modifier），离线提示块同改；
  「添加评论」的 `ExtendedFloatingActionButton` → `AppExtendedFloatingActionButton`
  （Miuix 没有 extended FAB，MIUIX 下渲染为胶囊按钮，MD 保留展开动画）。
- 编辑器：底部工具栏的 `Card` → `AppCard`（有内容时 16dp 圆角、空时 50dp 胶囊），
  发布按钮的两个 FAB → `AppExtendedFloatingActionButton` / `AppFloatingActionButton`；
  位置编辑弹窗 → `AppDialog` + 三个 `AppTextField`。
- 草稿卡 `DraftsCard` → `AppCard`。
- 已知待办：详情页顶栏仍是自定义 `TopAppBar`（标题居中逻辑特殊），
  笔记卡片内部的操作行、reaction chip、Markdown 渲染与媒体查看器仍是 Material。

**进度：第二轮已完成**（Markdown 容器、分隔线、媒体滑条）

- `AppDivider` 支持 `thickness` / `color` 透传，代码块与表格的分隔线已切换。
- Markdown 链接预览卡 → `AppCard` + `clickable`（去掉 Material 描边，更接近 HyperOS 卡片）。
- 音频/视频进度条 → `AppSlider` 的 `SliderState` 重载，并补上 `enabled` /
  `onValueChangeFinished`，拖动结束提交 seek 的行为与原来一致。
- 真机：安装成功、首页与卡片渲染正常；详情页因盲目点击未命中，下轮用 UI dump 精确定位后再验。

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

**进度：`miuix-blur` 基础设施已接入（第一轮）**

- 依赖：`top.yukonga.miuix.kmp:miuix-blur:0.9.4`（其 `minSdk = 33`）。
- 清单：`<uses-sdk tools:overrideLibrary="top.yukonga.miuix.kmp.blur" />` 放行低版本设备安装；
  运行时由 `ui/theme/MiuixBlur.kt` 的 `miuixBlurSupported()` 统一把关
  （MIUIX + API 33），`drawBackdrop` 自身还会在无 RuntimeShader 时自动降级。
- 首页：列表包进 `Modifier.layerBackdrop()`，浮层 header 用 `drawBackdrop(shape = Rectangle, effects = { blur(24f) })` 取景。
- **底部导航栏（可见效果）**：内容列包进 backdrop 层，`MiuixNavigationBar` 传
  `color = surface.copy(alpha = 0.72f)` + `drawBackdrop { blur(24f) }`，滚到导航栏下方的笔记
  会被磨砂；真机已确认。MD 分支保持不透明纯色。
- **实测限制**：首页 header 在向下滚动时会整体隐藏（滚动策略），所以顶栏毛玻璃只在
  「header 半隐半现」的过渡态可见，静止时看不出来。若要让顶栏也明显，需要改成「header 常驻 +
  scrollBehavior 收缩」，属于交互改动，留待产品确认。
- MD 分支完全不启用（`LocalThemeStyle == MD` 时直接走原路径），保证 MD 零变化。

- **深色模式（已核对）**：真机切「外观 → 主题模式 → 深色」后确认
  页面纯黑 `#000000`、卡片 `#242424`、文字与图标对比正常、底栏磨砂在深色下同样生效。
  浅色下页面 `#F7F7F7`、卡片纯白，与 HyperOS 一致。
- **配色定稿**：`primaryContainer` 改为「主色 12% 叠页面色」，并补了浅色/深色两套单测
  （`MiuixColorMappingTest`），防止再次退回紫色系容器。

- **动效对齐（第一轮）**：新增 `ui/theme/AppMotion.kt`
  - `appFadeSpec(miuix, durationMillis, delayMillis)` / `appSlideSpec(miuix, durationMillis)`：
    MIUIX 用 Miuix 风格的短弹簧（`spring(0.9f, 700f)` / `spring(0.9f, 550f)`），
    MD 保持原有 `tween(220/300, delay 90)`——只换 spec、不换转场形态，MD 零变化。
  - ⚠️ `transitionSpec` lambda 不是 `@Composable`，所以 spec 函数接收 `miuix: Boolean` 而不是读
    `LocalThemeStyle`；调用点先 `val miuixMotion = isMiuixMotion(LocalThemeStyle.current)` 捕获。
  - 已应用：底部 tab 切换（`MainScreen` 的 `AnimatedContent`）、列表/详情转场（`MemosCommon`）。
  - 搜索栏展开/收起在 MIUIX 下走 Miuix `SearchBar` 自带动画，无需另调。
  - 已补齐：`FullScreenMediaDialog` 的入场/退场/返回（`appEnterSpec` / `appExitSpec` / `appSlideSpec`；
  背景色渐隐仍用 tween，spring 对 Color 不自然）、`AccountsList` 的拖拽 fling
  （`AnchoredDraggableDefaults.flingBehavior` 的 `animationSpec`）、`AudioPlayer` 的播放/暂停图标切换、
  `VideoPlaybackControls` 控制条淡入淡出。
- 有意保留：`VideoSeekRipple` 的 750/80/170ms 波纹时序（弹簧会破坏波纹观感）。
- 进度指示器彻底统一：`AppCircularProgressIndicator` / `AppLinearProgressIndicator` 新增 `color`，
  MIUIX 映射为 `progressIndicatorColors(foregroundColor = …)`，于是「跟随按钮内容色的 spinner」
  （编辑器、登录、视频加载圈）也能走适配层。

- **一致性扫描（第一轮）**：用下式统计 `ui/` 下仍直接构造 Material 组件的位置（排除适配层）：

  ```bash
  rg -n "^\s*(Card|Surface|AlertDialog|DropdownMenu|ExposedDropdownMenu|ModalBottomSheet|OutlinedTextField|CircularProgressIndicator|LinearProgressIndicator|TopAppBar|NavigationBar|NavigationRail|TabRow|SnackbarHost|PullToRefreshBox|FilterChip|ListItem|Switch|Checkbox|RadioButton|Slider|OutlinedSecureTextField|ExtendedFloatingActionButton|FloatingActionButton|HorizontalDivider)\(" \
     app/src/main/java --glob '!**/ui/theme/AppComponents.kt'
  ```

  当前 **53 处**，分三类：

  | 类别 | 数量 | 处理 |
  | --- | --- | --- |
  | A. Activity / 页面根容器（`MainActivity`、`ProfileActivity`、`SsoLoginActivity`、`SetupScreen`、`OpenSourceLicensesScreen`、`EditAccountCredentialsActivity`、widget 配置页） | 12 | 只是 `Surface` 当根，颜色已由 `MaterialTheme` 映射；可择机换 `AppSurface`，无视觉差异 |
  | B. **有意保留的 Material**（见下方清单） | 33 | 记录在案，不迁 |
  | C. 待办 | 8 | `SyncStatusWidget` 同步面板（4）、`MemoComposerBottomBar` 的两个菜单（2）、`LoginScreen` 的根容器与按钮内 spinner（2） |

  **有意保留清单（B）**：`OfflineSettingsCard`（可编辑下拉 + 缓存编辑弹窗）、`ConflictDialog`（三态
  对话框）、`AccountsList`（拖拽手势卡）、`MemoCatchDialog`（彩蛋）、`MemoInput` 的建议气泡、
  `NativeMarkdownNode` 代码块底色、`ViewerHeader` 圆形按钮、`AttachmentCard`/`MemoItem` 的小角标、
  `FullScreenMediaDialog` 的全屏容器、`SearchScreen`/`NotificationsScreen`/`MapScreen` 的内联提示条、
  `SettingsOverview` 的分组图标圆底、`AppearanceSettingsContent` 的色点、`ReactionPickerDialog` 的
  表情圆底、`VideoPlaybackControls` 的加载圈、`MemosScreen` 的草稿提示条。

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

### 6.0 后续功能与修复（2026-10-21）

- **主题风格默认 MIUIX**：`AppearancePreferences.themeStyle` 默认值与 DataStore 解析兜底同步改成
  `MIUIX`，老用户已保存的值不受影响。
- **导航控制（设置 → 导航栏）**：新增 `NavBarScrollMode`（滚动隐藏 / 始终显示）、
  `TopBarScrollMode`（不隐藏 / 隐藏搜索框 / 隐藏搜索框和标签）与底栏项目自定义
  （开关 + 上下移动）。底栏项目以 `MainDestination` 枚举名逗号拼接存储，空列表 = 全部按默认顺序，
  并已加入备份白名单与校验。
  ⚠️ `settingsItem {}` 的内容是放在 `Box` 里的，一个设置页里要放多个分组必须自己包一层 `Column`，
  否则会互相重叠（本次踩到：前两组被项目列表盖住）。
- **日历星期全显示「星」**：`TextStyle.NARROW` 在中文下取的是「星期三」首字，七天都是「星」；
  改为取 `SHORT`（周一…周日）再去掉「周/星期」前缀。
- **深色按钮上的黑色图标**：Miuix `FloatingActionButton` 不向下传递内容色，未指定 `tint` 的图标会
  落到 Material 的 `onSurface`（黑）；适配层统一提供 `onPrimary`。
- **搜索图标贴边**：Miuix 输入框的 `leadingIcon` 无内边距，搜索栏在 MIUIX 分支加了 12dp 起点缩进。

### 6.1 首页顶栏减重（A+B+C，2026-10-21）

首页顶部原本是「42dp 全宽分段控件 + 16dp 空隙 + 48dp 灰搜索框」三块 chrome 叠在一起，且左右边距
（0dp 与 28dp）互相错位。改造结论：**顶栏只保留一行**。

- **A 文字标签**：`MemoFeedTabs` 的 MIUIX 分支不再用 `MiuixTabRow`，改为 18dp 间距的文字标签 +
  16×3dp 主色下划线（未选中透明、选中淡入），左对齐并与搜索框左缘对齐，高度 40dp（含 12dp 隐形热区）。
  MD 分支仍是 `PrimaryTabRow`，未改一行。
- **B 搜索框瘦身**：折叠态不再有输入框，展开面板里的输入框 42dp、左右 12dp 内边距。
- **C 搜索折叠为图标**：折叠态只有右上角一个搜索图标，点开才是整块面板；展开态由
  `MemoSearchBar(expandedOverride = …)` 受控渲染（`MemosScaffold` 持有 `isSearchExpanded`，
  `listHeader` 回调新增第三个参数 `onSearchExpandedChange`）。
  - 面板内 Miuix `InputField` 在 `expanded = true` 时不画 label，空查询会没有任何提示，因此自己叠了
    一层 `onSurfaceVariantSummary` 色的占位文字。
  - 没有顶栏动作可点的页面（地图）仍走 `expandedOverride == null` 的旧路径：保留 Miuix 自带的可点击
    折叠胶囊，避免搜索入口消失。
- **同步状态图标只在非 IDLE 时出现**：正常状态不再常驻一个蓝色对勾；点击面板随图标迁到
  `MemosScreen` 持有（`SyncStatusPanel` 在页面级渲染）。
- **滚动隐藏三档语义不变**：`全部` = 整行滚走；`搜索框` = 标签滚走、图标留下；`不隐藏` = 常驻。
- **列表顶部留白**：MIUIX 固定 `contentPadding.top = 64dp`（一行 chrome + 16dp 间距），
  MD 仍是 `88dp + 实测标签高度`。

### 6.2 四条修复（2026-10-21 下午）

- **顶栏/底栏滚动行为改成开关**：`TopBarScrollMode` 从三值（`NEVER` / `SEARCH_ONLY` / `ALL`）收敛为
  两值（`ALWAYS` / `ON_SCROLL`），设置页两行都用 `SettingToggleRow` 开关，一次点击即可切换。
  `DataStoreManager.topBarScrollModeOf` 保留了旧值映射（`NEVER` → `ALWAYS`，`ALL` / `SEARCH_ONLY` →
  `ON_SCROLL`），备份校验也用同一函数，老备份不会因为字符串变了而被判非法；配对单测在
  `TopBarScrollModeTest`。
- **下拉刷新指示器被顶栏毛玻璃糊住**：Miuix `PullToRefresh` 的 `contentPadding.top` 只作用在
  `RefreshHeader` 的 `offset` 上（读的是字节码里的 `calculateTopPadding()`），所以
  `AppPullToRefresh` 新增 `indicatorTopPadding`，首页 MIUIX 传 52dp，把转圈推到顶栏下方；内容布局
  不受影响。MD 分支忽略该参数。
- **详情页「添加评论」被底栏盖住**：底栏是浮层（`Box(align = BottomCenter)`）而不是布局占位，所以
  `MemosScaffold` 新增 `navBarInset`（`MemosScreen` 按 `onToggleNavBar != null && isNavBarVisible`
  算成 80dp 并做动画），透传给 `MemoDetailView`：FAB 加同高底部内边距，列表底部内边距同样加上。
- **首页 FAB 改毛玻璃**：`MemosScaffold` 通过 `LocalListBackdrop` 把列表录制的那层背景暴露出来
  （未开启模糊时为 null），FAB 用它做 `drawBackdrop`，容器色 `primary.copy(alpha = 0.72f)`，
  另加 6dp 阴影。两个坑：
  1. Miuix `FloatingActionButton` **不能半透明**——它内部还画了一层 squircle，容器一透明就会浮出
     一个八边形环。所以毛玻璃版自绘圆钮（`Box` + `clip` + `drawBackdrop` + `background` + `clickable`），
     纯色版才回落到 Miuix 组件。
  2. `drawBackdrop` 采样的那层**不能包含自己**。试着复用它放在 `MainScreen` 内容层（底栏用的
     `navBackdrop`）会让录制 与 采样 互相递归，直接在 `libhwui RenderNode::prepareTreeImpl` 里
     栈溢出（native crash）。只能用 `MemosScaffold` 里只包住列表的那层。

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
