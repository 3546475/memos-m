# 筛选系统重构方案（Feed Filter v2）

> 讨论稿。目标是把「快捷方式胶囊」从一个不透明的 CEL 开关，变成一套**可读、可组合、离线可用、
> 三个入口共用**的筛选系统。每个阶段都能独立交付，P0 等于「只改位置和样式」，随时可以停。

本文讨论的是**筛选**（喂给列表的取数条件），不涉及搜索结果的排序算法，也不做 AND/OR 嵌套
查询构建器。涉及的界面与数据层：时间线 feed、搜索覆盖层、地图「已保存视图」。

---

## 1. 现状：问题出在哪

### 1.1 入口位置与样式

| 现象 | 代码位置 |
| --- | --- |
| 胶囊是 `LazyColumn` 的 header item，会随内容滚走；滚出屏幕后「正在筛选」没有任何痕迹 | `ui/nav/MemosScreen.kt:692-816` |
| 有草稿卡 / 无快捷方式 / 有标签时这一行反复出现消失，header 高度变化会推移整列 | `MemosScreen.kt:701-724`；`ui/component/ScrollUtils.kt:54` 已为这种抖动做过滚动补偿 |
| 每个胶囊都用同一个 `Icons.AutoMirrored.Outlined.Shortcut` 图标 + 标题，看不出它筛的是什么 | `MemosScreen.kt:795-808` |
| 选中态 = 填充胶囊 + 多一个 X；「点 X」和「点胶囊」是同一个点击目标，语义含糊 | `MemosScreen.kt:787-809` |
| 没有「清除全部」、没有条件计数、没有排序入口；筛选结果为空时只有通用的「加载失败/空」样式 | `ui/component/MemosCommon.kt:589-606` |
| 无障碍：Miuix 分支的胶囊用 `Modifier.clickable`，没有 `Role.Checkbox` / 选中语义；X 图标 `contentDescription = null`，读屏只念标题 | `ui/theme/AppComponents.kt:718-745` |

### 1.2 筛选语义与数据流

| 现象 | 代码位置 |
| --- | --- |
| 唯一的筛选状态是一个不透明 CEL 字符串；用户必须在「设置 → 快捷方式」里手写，占位符是 `"work" in tags && has_task_list`，帮助链接跳官网 | `ui/component/setting/ShortcutsCard.kt:213-234` |
| 写错了没有校验：feed 上只表现为一条通用的加载失败 | `MemosViewModel.kt:118-140` |
| 标签筛选与快捷方式**互斥**：两者写进同一个 `extraFilter`，开一个就清掉另一个 | `MemosViewModel.kt:125-135`、`delegate/ShortcutDelegate.kt:89-119` |
| 点击即 `fetch(refresh = true)` 整页重拉，没有本地预筛，所以是「转圈 → 结果」；离线/弱网时这个 fetch 直接失败，胶囊是选中的但列表仍是旧的未筛选内容 | `MemosViewModel.kt:393-405` |
| 冷启动时缓存 prefill 是**未过滤**的，持久化的筛选会与首屏数据短暂冲突 | `manager/ListManager.kt:294-315` |
| 筛选状态只在内存里（`MemosUiState`），进程被杀或切账号即丢 | `viewmodel/UiState.kt:48-53` |
| 排序写死 `PINNED_DESC`，feed 上不可改 | `manager/MemoListManager.kt:73` |
| 拼 CEL 时没有转义：标签里出现 `"` 会把表达式拼坏 | `MemosViewModel.kt:131` |
| 同样的语义有三套 UI：feed 胶囊、搜索页（多标签 + 日期 + 排序，`component/SearchScreen.kt:180-271`）、地图「已保存视图」下拉（`ui/nav/MapScreen.kt:239-255`） | — |
| 标签树（带计数、可折叠）只有个人页能进，选一个标签后又落回同一个不透明筛选 | `ui/component/TagTreeDialog.kt` |
| 类型筛选（链接/代码/待办）只能本地过滤，入口在个人页，feed 里没有 | `ui/component/MemoTypeMemosScreen.kt` |

**一句话**：现在不是一个「筛选系统」，而是一个「CEL 输入框的远程开关」。位置、样式只是它最表层
的症状；真正的问题是**筛选不可读、不可组合、不可离线、不可复用**。

---

## 2. 目标与非目标

**目标**

1. **入口常驻**：筛选入口固定在 chrome，不随内容滚动；有条件时在下方常驻一条筛选摘要。
2. **状态可读**：任何时候都能看到「现在按什么筛」，每个条件可单独删除，一键清除，空态直接给出
   修改/清除的出口。
3. **结构化**：筛选是一个可分面构造的模型，CEL 只是它的编译产物；手写 CEL 仍保留为「高级」。
4. **即时**：先本地筛已加载数据，再补服务器；不再整屏转圈；离线仍能正确筛选。
5. **一致**：feed / 搜索 / 地图共用同一模型和同一批组件。
6. **双主题**：新 UI 全部走 `App*` 适配层，MD 与 MIUIX 语义一致（见 `docs/miuix-refactor-plan.md` §2.2）。

**非目标（本轮不做）**

- NOT/OR 嵌套的查询构建器、正则、脚本化筛选。
- 结果总数统计（需要额外查询或后端支持，放 P3 之后再说）。
- 跨账号共享筛选；筛选状态仍然按账号隔离。

---

## 3. 三种方案与推荐

| 方案 | 内容 | 成本 | 风险 | 适用 |
| --- | --- | --- | --- | --- |
| **A 表面修复** | 胶囊行移到 chrome；统一图标/选中态；补「清除」、空态、a11y | 1–2 天 | 低 | 只想马上不那么难受 |
| **B 结构化筛选（推荐）** | 引入 `MemoFilter` + CEL 编译 + 本地谓词；筛选面板 + 摘要条 + 持久化；三处统一 | 4 阶段，约 1.5–2 周 | 中 | 解决 1.2 的全部结构问题 |
| **C 查询构建器** | 分组 AND/OR、无限嵌套 | 数周 | 高 | 移动端收益低，不建议 |

推荐 **B**，并且把 **A 作为 B 的 P0** 先交付：位置/样式先落地，数据模型随后替换，任何阶段都能停。
C 的唯一合理形态是 B 之后的「高级」抽屉（直接编辑 CEL），而不是主界面。

---

## 4. 目标交互

### 4.1 无筛选

```
┌──────────────────────────────────────────┐
│  笔记   探索              ⊘(筛选)  ⌕ 搜索 │  ← chrome 常驻，不参与滚动
├──────────────────────────────────────────┤
│  ┌────────────────────────────────────┐  │
│  │ 卡片…                              │  │
```

- 筛选入口是 chrome 右侧的图标按钮（MD 下放在浮动搜索栏右侧），**不占额外一行**。
- 没有筛选时不显示摘要条，也不显示标签胶囊行。

### 4.2 有筛选

```
┌──────────────────────────────────────────┐
│  笔记   探索              ⊘·2     ⌕      │  ← 图标带条件数角标
├──────────────────────────────────────────┤
│ [工作 ×] [#type/book ×] [本月 ×]   清除  │  ← 摘要条：常驻、横向滚动、末尾固定「清除」
├──────────────────────────────────────────┤
│  ┌────────────────────────────────────┐  │
```

- 摘要条是**唯一**的筛选状态展示：视图名 → 标签 → 时间 → 类型，点 token 打开对应分面，
  点 × 立即删除该条件（一次请求），「清除」清空全部（带撤销 snackbar）。
- 视图 token 被用户改动后显示「工作 · 已修改」，保存时可覆盖视图或另存新视图。

### 4.3 筛选面板

```
┌─ 筛选 ──────────────────────────  重置 ─┐
│ 保存的视图                                 │
│  ( ) 全部笔记  (•) 工作  ( ) 本月待办      │
│                                            │
│ 标签                            [AND|OR]   │
│  [× #work] [× #type/book]  [+ 选择标签]    │
│                                            │
│ 时间                                       │
│  [不限] [今天] [本周] [本月] [自定义…]     │
│                                            │
│ 内容类型                                   │
│  [链接] [代码] [待办] [有附件]             │
│                                            │
│ 排序          最新 ↓ / 最早 / 置顶优先     │
├────────────────────────────────────────────┤
│              [ 应用（3） ]                 │
└────────────────────────────────────────────┘
```

- 面板里编辑的是 **draft**，点「应用」才提交：避免每勾一下就打一次请求。
- 标签区复用 `buildTagTree`（`TagTreeDialog.kt:48`）与 `userStats.tagCount`，带计数、可搜索、多选。
- 面板形态：**P1 用 `AppDialog` 承载可滚动内容**（两种主题今天就能用）；P2 再换成自适应的
  `AppBottomSheet`（MD `ModalBottomSheet` / MIUIX `OverlayBottomSheet`），避免为了形态先啃 Miuix
  的 popup host。
- 关闭面板不修改筛选项；「重置」只清空 draft，方便对比。

### 4.4 关键状态机

```
applied ──打开面板──▶ draft = applied.copy()
draft ──勾选/输入──▶ draft′（按钮计数 = draft′.activeCount）
draft ──应用──▶ applied = draft′ ──▶ 本地先筛 ──▶ 需要服务器时 fetch(refresh=true)
摘要条 ──×──▶ applied = applied - token ──▶ 立即提交
卡片标签 ──点击──▶ applied = applied + tag ──▶ 立即提交（snackbar 可撤销）
选视图 ──▶ applied = 视图预设（整体替换，不是叠加）
```

**视图的语义**（建议）：视图 = **整套筛选的预设**，选中即整体替换；之后改分面视为在预设上编辑。
这比「视图作为一个可叠加的 scope」更好解释，也延续了现在「标签和快捷方式互斥」的用户预期。
视图对应的 `MemoFilter` 在本地缓存，跨设备仍然靠服务器上的 CEL。

---

## 5. 数据模型

```kotlin
// model/filter/MemoFilter.kt
data class MemoFilter(
    val viewName: String? = null,          // 服务器 shortcut 的 name
    val legacyCel: List<String> = emptyList(), // 无法可视化的原始表达式，原样透传
    val tags: Set<String> = emptySet(),
    val tagMatch: TagMatch = TagMatch.ALL, // ALL -> &&，ANY -> ||
    val date: DateRange? = null,           // created_ts 区间
    val types: Set<MemoContentType> = emptySet(), // 链接/代码/待办
    val sort: MemoSort = MemoSort.PINNED_DESC
) {
    val activeCount: Int
    fun describe(views: List<SavedView>): List<FilterToken>
    fun matches(memo: Memo): Boolean                 // 本地谓词
}

// model/filter/MemoFilterCel.kt
internal fun MemoFilter.toCel(creator: String?): String? // 服务器表达式
internal fun MemoFilter.matches(memo: Memo): Boolean     // 与 toCel 等价

// model/filter/SavedView.kt
data class SavedView(
    val name: String?,   // users/{u}/shortcuts/{id}
    val title: String,
    val cel: String,
    val facets: MemoFilter? // 本地缓存的解析结果，可空
)
```

要点：

1. **CEL 编译规则**（与搜索页现有拼法保持一致，见 `SearchScreen.kt:238-257`）：
   - 标签 ALL：`"work" in tags && "home" in tags`；ANY：`("work" in tags || "home" in tags)`
   - 时间：`created_ts >= {start} && created_ts < {endExclusive}`
   - 类型：链接 → `has_link`；待办 → `has_task_list`；代码 → 无可靠服务端表达式，留本地
   - 创作者：始终 AND 现有 `buildMemoCreatorFilter`（`api/MemosApiImpl.kt:84-98`）
   - 字符串字面量必须转义 `\` 与 `"`（现有拼标签没做，顺手修）
2. **能力降级**：按 `api.constants` / 实例版本决定哪些字段可用；未知字段不拼进 CEL，并退化为本地
   过滤（`has_link` 在旧实例上可能不存在）。
3. **legacy CEL**：服务器快捷方式里的写法无法解析时，保留为一个 token（显示标题），点开只有
   「编辑原始表达式 / 停用」，绝不因为一条旧表达式坏掉整个筛选。
4. **本地谓词**与 CEL 必须同语义、同一份测试数据覆盖（见 §10）。
5. **持久化**：DataStore 按账号 + feed 存 JSON，键形如 `filter.user.{accountId}` /
   `filter.explore.{accountId}`；切账号随 `AccountSession` 一起切换，不跨账号串味。
   设置里给一个「记住上次筛选」开关（默认开）。

---

## 6. 状态与数据流

1. `MemoListState` 去掉 `selectedShortcut` / `selectedHashtag`，换成 `filter: MemoFilter`；
   `shortcuts` 保留（列表数据），同时派生 `views: List<SavedView>`。
2. `MemosViewModel` 的 `filterProvider` 从读两个字段改为 `filter.toCel(...)`——`UserMemoListManager`
   的构造参数不变，改动面最小。
3. 新增 `FilterDelegate`（或并入 `ShortcutDelegate`）：
   ```
   fun apply(filter: MemoFilter, source: FilterSource)
   ```
   - 立即写 state（UI 先走本地重算）；
   - 需要服务器（标签/时间/legacy/视图）时 `fetch(refresh = true)`；仅本地条件（类型）时不请求；
   - 合并 150–250ms 内的连续 token 删除，靠 `BaseListManager` 的 generation 取消旧请求。
4. **本地预筛**：`CacheCallbacks.getCachedData` 的结果与 prefill 都过一遍 `filter.matches()`。
   这样才能同时修好「冷启动 prefill 未过滤」和「离线筛选无效」两个问题。
5. **分页配合本地谓词**：一页被本地条件筛空时，自动 `loadMore` 2–3 页直到凑够一屏或到底（有上限），
   避免「明明有数据却显示空」。
6. **滚动**：应用筛选后 `animateScrollToItem(0)`；刷新期间保留旧内容（`showingCached` 语义不变，
   换筛选时照旧清掉 prefill 标记，见 `MemosViewModel.kt:398-405` 的注释）。
7. 探索 feed 有独立的 `filter`（`visibility` 相关分面只在探索出现）；两个 feed 的筛选互不影响。

---

## 7. 组件与文件划分

| 文件 | 内容 |
| --- | --- |
| `model/filter/MemoFilter.kt` | 模型、`activeCount`、`FilterToken` |
| `model/filter/MemoFilterCel.kt` | `toCel()` / `matches()` / 转义 |
| `model/filter/SavedView.kt` | 视图与 facets 缓存 |
| `viewmodel/delegates/FilterDelegate.kt` | `apply()`、视图选择、保存视图 |
| `data/prefs/FilterStore.kt` | DataStore 读写、按账号隔离 |
| `ui/component/filter/FilterBar.kt` | 摘要条（token + 清除） |
| `ui/component/filter/FilterSheet.kt` | 面板骨架、draft、应用/重置 |
| `ui/component/filter/FilterTagSection.kt` | 标签树多选、搜索、计数 |
| `ui/component/filter/FilterDateSection.kt` | 预设 + 自定义区间（从搜索页抽出复用） |
| `ui/component/filter/FilterViewSection.kt` | 视图单选、编辑/新建 |
| `ui/theme/AppComponents.kt` | 胶囊补 `selectable(role = Role.RadioButton/Checkbox)` 语义；P2 增加 `AppBottomSheet` |

复用而不是重写：`buildTagTree`（`TagTreeDialog.kt:48`）、`userStats.tagCount`、`MemoContentType`、
`resolveMemoOrderBy`（`api/MemosApi.kt`）、搜索页的日期区间语义。

---

## 8. 分阶段实施

### P0 · 位置与样式（不动模型）— 已实现

实际落地与最初设想有一处差异：胶囊行不是「搬到 chrome 继续横向滚动」，而是**整个删掉**，
换成「入口图标 + 菜单」。理由：Miuix 的头部被刻意压成单行（见 `MemosScreen.kt` 的注释），
常驻一条胶囊会与这个设计语言冲突；而横向滚动的胶囊本来就看不全标题。

1. 入口：chrome 右侧常驻筛选图标（Miuix 在标签行里、MD 在浮动搜索栏右侧），有筛选时图标高亮 +
   圆点；点击打开「全部笔记 / 各快捷方式 / 管理快捷方式」菜单。
2. 摘要：有筛选时 chrome 下方常驻一条摘要条（`#标签` 或视图标题的 token + 清除），列表
   `contentPadding` 用 `animateDpAsState` 跟随，不会跳动；摘要条不随列表滚动。
3. 空状态：无结果时说明「被筛选挡住」还是「本来就没有笔记」，并提供清除/修改两个出口。
4. a11y：`AppFilterChip` 的 Miuix 分支改用 `selectable`（会朗读选中状态）；筛选图标有
   「Filter / Filter applied」描述；token 的点击语义是「移除筛选」。

遗留到 P1/P2：条件计数角标（模型当前最多一个条件，圆点足够）、多图标（视图/标签/时间/类型
各一种）、token 分区（`FeedFilterToken` 已预留 `icon`）。

改动：`MemosScreen.kt`、`MemosCommon.kt`、`AppComponents.kt`、
`ui/component/filter/`（新增 3 个文件）、`ScrollUtils.kt`（注释）、`strings.xml`（7 个语言目录）。
验收：见 §8.5 的 1–8 条（其中「条件数」按圆点核对）。

### P1 · 模型与状态 — 已实现

1. **模型**：`model/filter/MemoFilter.kt` + `MemoFilterCel.kt`。字段 `viewName / legacyCel /
   tags / tagMatch / date`；CEL 由 `toCel(creator)` 编译，标签与日期子句分工明确；字面量统一走
   `celString()` 转义（引号、反斜杠），顺手修掉了「标签里有引号就把表达式拼坏」的老问题。
   本地等价语义在 `matches(memo)`：标签（ALL/ANY，忽略 `#`）与创建时间窗口；opaque 的 legacy
   子句本地无法求值，一律视为命中（只有服务器能判断）。
2. **状态**：`MemoListState.filter: MemoFilter` 替换 `selectedShortcut / selectedHashtag`。
   唯一写入口是 `FilterDelegate`（`apply / selectView / toggleTag / clear`），state、请求表达式、
   摘要条不会各说各话。视图 = 预设：选中即整体替换；标签在其上叠加（AND），从「互斥单值」变成
   可组合多值。
3. **持久化**：写进现有的 session snapshot（`SessionSnapshotData.filter`），不另开 DataStore：
   账号删除时随快照一起清理，也避免了跨账号串味。恢复在 `restoreCachedSessionNow()` 里、且排在
   `loadFromCache()` 之前，所以冷启动首帧就是筛选后的缓存。旧版本从未持久化筛选，无需迁移。
   没有「记住上次筛选」开关：默认总是记住（需要关掉时再加，存储层已就位）。
4. **本地预筛**：`BaseListManager.cacheFilterProvider` 让缓存 prefill / `loadFromCache` /
   请求失败回退都过 `matches()`；应用筛选时先把当前列表按新条件剪枝，再发请求；离线时不发注定
   超时的请求，直接按筛选读缓存。摘要条由 `memoFilterTokens()` 生成（视图标题 / `#标签` 各一枚
   token），列表在筛选变化后回到顶部。
5. **测试**：`MemoFilterTest`（11 例：编译、转义、AND/OR、时间窗口、本地匹配、Gson 往返）与
   `FilterDelegateTest`（6 例：应用/去重/预设替换/重复选视图/标签增删/空表达式清空）。
   `AccountSessionTest` 随状态字段更新。

已知边界：legacy 视图离线时仍不过滤（本地无法求值，P2 会在保存视图时缓存 facets）；排序、内容
类型、日期/标签选择面板仍属 P2/P3；筛选变化时若正在看双栏详情，被筛掉的卡片会让详情出现占位。

验收：9–14 条。

### P2 · 筛选面板 — 已实现

入口从「图标 + 菜单」改成**图标直接打开面板**（菜单与面板是两套视图选择，留一个就够了）。面板用
`AppDialog` 承载一个 `LazyColumn`，两种主题一致；标签选择器做成可搜索的平铺列表而不是标签树
——目前的 `tags` 是精确值，选父级（`type`）并不会匹配 `type/book`，树形多选会给出错误暗示，
前缀匹配与树的取舍留到 P3 再谈。

1. **分面**：保存视图（单选，视图 = 预设）/ 标签（搜索 + 计数 + ALL/ANY）/ 时间（不限、今天、
   本周、本月，周起始日跟随 locale）/ 排序（置顶优先、最新、最早）。类型分面移到 P3。
2. **草稿模型**：面板内编辑 `draft`，「应用」才提交一次；「重置」清空草稿。选视图会重置已加的
   标签/日期（预设语义），排序作为偏好保留。
3. **保存为视图**：面板底部把 `draft.toCel(null)` 交给设置页现成的 `ShortcutEditDialog`，因此
   标题校验、帮助链接、服务器错误提示全部复用；每个视图行尾的编辑按钮同样复用该对话框编辑原始
   CEL（先关面板再开对话框，不叠两层窗口）。
4. **排序接入数据层**：`MemoSort` 进入 `MemoFilter`，`UserMemoListManager` 用它决定请求的
   `orderBy` 和缓存合并时的本地比较器；摘要条新增日期与排序 token（排序 token 只在非默认时出现）。
5. **标签来源**：`userStats.tagCount` ∪ 已加载列表的标签，计数取两者较大值，离线/新账号也能选。
6. 面板高度上限 420dp，长标签列表在面板内滚动。

未做（P3）：自定义日期区间、内容类型分面、「已修改」标记与覆盖保存（目前保存总是新建视图）。

验收：15–20 条。

### P3 · 类型、自定义区间与共享编译 — 已实现

1. **内容类型**：`MemoFilter.types` 进入模型；面板新增「内容类型」分面（可多选，OR），摘要条为每种
   类型出一枚 token。服务端用 `has_link` / `has_task_list` / `content.contains("`")` 近似（Memos 没有
   代码类型的过滤器，反引号是本地判定「代码」时用的同一个信号），本地 `matches` 直接复用个人页的
   `matchesContentType`，所以缓存与离线结果按精确规则过滤。
2. **自定义日期区间**：时间分面新增「自定义」，先选开始日、再选结束日（Material `DatePickerDialog`，
   叠在面板这个 `AppDialog` 之上；两者都是独立窗口，互不挤压）。区间含首尾，摘要条显示本地化的
   日期文本（`MemoDateRange.displayLabel`）。
3. **共享编译**：搜索页的标签/日期子句改为经 `MemoFilter.toCel(null)` 生成，离线
   `filterLocalSearchMemos` 改用 `matches()`，删掉了搜索页里重复的 CEL 拼接与匹配逻辑。语义不变
   （AND、`created_ts`、结束日含当天），转义反而更安全；`MapSearchTest` 覆盖了这条路径。
4. **地图**：地图的「已保存视图」读的一直是同一份 `userMemoList.shortcuts`（P1 之后还会随
   `fetchShortcuts` 刷新），没有第二份视图列表；本轮不改它的 UI，只把「统一」限定为同一数据源与
   同一 CEL 语义。
5. `docs/entry-inventory.md` 增加筛选面板这一层入口。

未做：搜索页的标签/日期**选择器**与面板组件的视觉合并。搜索有自己的即时输入与结果面板，把那套
状态搬进筛选模型属于 UI 重构、回归面大，而条件语义已经统一，收益不足以在本轮承担。

验收：21–24 条。

### 8.5 验收清单（真机，交给维护者）

1. 无筛选时 chrome 只有筛选图标，没有多余一行，列表顶部不被空行占据。
2. 选中一个视图后，摘要条出现在 chrome 下方，滚动列表时保持可见。
3. 摘要条横向内容超宽时可滚动，末尾「清除」始终可达。
4. 点 token 的 × 只删除该条件，列表刷新且不整屏白屏。
5. 「清除」后回到无筛选状态，列表回到全量，可 snackbar 撤销。
6. 筛选结果为空时，空态写明当前条件，并提供「清除筛选」「修改筛选」。
7. MD 与 MIUIX 两种主题下，入口、摘要条、面板的间距与点击反馈一致。
8. 读屏：筛选图标读出条件数；胶囊读出「已选中/未选中」；摘要条读出「筛选：2 个条件」。
9. 切账号后筛选不串：A 的筛选不会出现在 B。
10. 杀进程重开，筛选与上次一致（开关打开时）。
11. 飞行模式下选中一个标签，列表按缓存数据正确过滤，不出现加载失败。
12. 离线冷启动时首屏（prefill）已经按筛选过滤，不闪一下未过滤内容。
13. 标签里含 `"`、`/`、空格时，请求表达式不报 400，结果符合预期。
14. 服务器上已有、但本地无法可视化的快捷方式仍可选中，表现为一个 legacy token，其余条件不受影响。
15. 面板里连续勾选 5 个标签只发一次请求（点「应用」时）。
16. 「保存为视图」后，设置 → 快捷方式里出现同名条目，标题/表达式与面板一致。
17. 选中视图后再加标签，摘要条同时出现视图 token 与标签 token，请求为两者 AND。
18. 排序切换后列表顺序变化（置顶优先 / 最新 / 最早），摘要条出现排序 token，清掉后回到默认。
19. 探索 feed 的筛选独立于笔记 feed，来回切页不互相污染。
20. 面板内日期选「本周」后，只显示本周创建的笔记；周起始日与系统语言一致（中文周一起始）。
21. 内容类型选「链接」后只显示含链接的笔记，摘要条出现类型 token；多选两种类型时取并集。
22. 时间选「自定义」并选好起止日后，只显示该区间（含首尾）的笔记，摘要条显示本地化日期区间。
23. 搜索页输入同样的标签/日期条件，结果与升级前一致（共享编译后不回退）。
24. 地图的已保存视图与 feed 的视图列表一致，选中后地图范围正确；深色模式与大字体下摘要条、
    面板、胶囊不换行、不裁切，对比度 ≥ 4.5:1。

---

## 9. 风险与兼容

| 风险 | 处理 |
| --- | --- |
| 服务器 CEL 方言差异（`has_link`、`created_ts`、旧实例无 filter 参数） | 按版本/常量做能力表，未知字段退本地；legacy CEL 报错只在 token 内提示 |
| 表达式注入/转义（标签带引号） | 统一走字面量转义函数，单测覆盖 |
| 筛选变化与本地缓存合并冲突 | 换筛选即丢 prefill 标记（沿用现有约定），本地预筛只在 `matches()` 之后合并 |
| 双主题回归 | 新组件一律 `App*`，MD 分支零变化；两套主题都要过验收 7/23/24 |
| 面板形态返工 | P1 用 `AppDialog`，把分面内容做成无形态依赖的 composable，换 sheet 时只换外壳 |
| 大量标签/视图导致面板卡顿 | 标签树 `remember(tags)` 已有；视图列表用 `LazyColumn`；搜索防抖 |
| 结果计数 | 明确不做；UI 不承诺「共 N 条」 |

---

## 10. 测试

- **JVM 单测**（不依赖设备，符合 `AGENTS.md` 的验证流程）：
  - `MemoFilterCelTest`：编译产物 golden string、转义、AND/OR、日期边界（含当天包含语义）。
  - `MemoFilterMatchesTest`：与 CEL 同语义的本地谓词，覆盖标签、类型、时间、空筛选。
  - `MemoFilterSerializationTest`：JSON 往返、旧字段迁移（`selectedShortcut` → `viewName`）、
    未知字段忽略。
  - `FilterStoreTest`：按账号隔离、默认值、写入失败回退。
- **Compose UI 测试**（可选，需设备）：胶囊可选语义、摘要条 token 删除、面板应用按钮计数。

---

## 11. 待决问题

1. **范围**：P0–P2 只做 feed，P3 统一搜索与地图，还是第一轮就把搜索页也换掉？
2. **视图语义**：视图作为「整套预设」（本文建议），还是作为「可叠加的 scope」？
3. **视图的真相**：继续以服务器 shortcut 为跨设备真相（本文建议，本地只缓存 facets），还是允许纯本地视图？
4. **面板形态**：P1 先用对话框（低风险），还是直接投入自适应 bottom sheet？
5. **默认恢复**：默认恢复上次筛选，还是每次冷启动回到「全部笔记」？
