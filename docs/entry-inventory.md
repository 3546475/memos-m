# 页面入口盘点与入口方式方案

基线：`app/src/main`（AndroidManifest + Compose 主界面 + 各 Activity + 两个 Glance 小组件）。
统计口径：**页面** = 用户可感知的独立界面单元（Activity / 主界面目的地 / 二级屏 / 全屏覆盖层）；
**入口** = 任一能触发页面打开的触发点（同一页面多个触发点分别计数）。

## 一、结论速览

| 指标 | 盘点时 | 现在 | 说明 |
| --- | --- | --- | --- |
| Activity | 16 | 15 | 含 1 个抽象基类 `ProfileActivity`；`SyncLogActivity` 已合并进设置分区 |
| 主界面目的地 | 4 | 4 | 底栏（手机）/ NavigationRail（平板），可在导航设置里显隐与排序 |
| 列表覆盖层 | 2 | 2 | 搜索、同步状态面板（非独立 Activity） |
| 设置分区 | 10 | 10 | 1 个 `SettingsActivity` 用 `EXTRA_SECTION` 复用出 10 个页面 |
| 系统组件 | 3 | 3 | 草稿小组件、统计小组件、统计小组件配置页 |
| 页面/面板位 | 33 | 32 | 1 个纯回调（`SsoCallbackActivity`） |
| 入口触发点 | 51 | 57 | 新增 4 条系统快捷方式、长按 FAB 及其 3 个动作；删除 3 条冗余分享与 4 条个人页直链 |
| 已有路由表 | 0 | 0 | 无 Navigation Compose、无路由枚举、无统一 Intent 工厂 |
| 通用 deep link | 0 | 0 | 仅 SSO 回调一条（`org.example.memosm://oauth/callback`） |
| App Shortcuts | 0 | 4 | 动态发布：新建 / 草稿 / 通知 / 搜索 |

那句话的结论没变：**外部入口有了，寻址能力仍然没有**——App Shortcuts 只能落到固定页面，页面本身依然不可寻址（memo 链接按第八节暂缓）。

## 二、页面清单（原 33 项，现 32 项）

### A. 主界面（单 Activity：`MainActivity`）

> 下表的"入口数"按**去重后的机制**计数（例如 5 条分享 filter 记 1 个分享入口），
> 因此合计不等于第四节逐条计数的 51。

| # | 页面 | 实现 | 去重入口数 |
| --- | --- | --- | --- |
| 1 | 时间线 MEMOS | `MemosScreen`（`MainDestination.MEMOS`） | 5 |
| 2 | 探索 EXPLORE | 时间线内 `HorizontalPager` 的第二页 | 1 |
| 3 | 地图 MAP | `MapScreen`（仅服务端支持地图时出现） | 2 |
| 4 | 附件 ATTACHMENTS | `AttachmentsScreen` | 2 |
| 5 | 个人 PROFILE | `ProfileScreen` | 2 |
| 6 | Memo 详情 | `MemosScaffold` 内的详情面板（单/双栏自适应） | 5 |
| 7 | 搜索覆盖层 | `MemoSearchBar` + 结果面板 | 1 |
| 8 | 同步状态面板 | `SyncStatusPanel`（仅非 IDLE 时出现） | 1 |

### B. 独立页面（Activity）

| # | 页面 | Activity | 入口数 |
| --- | --- | --- | --- |
| 9 | Memo 编辑器 | `MemoEditorActivity` | 4 |
| 10 | 草稿箱 | `DraftsActivity` | 1 |
| 11 | 通知 | `NotificationsActivity` | 1 |
| 12 | 归档 | `ArchivedMemosActivity` | 1 |
| 13 | 编辑资料 | `EditProfileActivity` | 1 |
| 14 | 编辑账号凭据 | `EditAccountCredentialsActivity` | 1 |
| 15 | 设置总览 | `SettingsActivity`（无 section） | 1 |
| 16 | 设置 · 通用 GENERAL | 同上 + `section=GENERAL` | 1 |
| 17 | 设置 · 外观 APPEARANCE | 同上 + `section=APPEARANCE` | 1 |
| 18 | 设置 · 导航 NAVIGATION | 同上 + `section=NAVIGATION` | 1 |
| 19 | 设置 · 快捷方式 SHORTCUTS | 同上 + `section=SHORTCUTS` | 1 |
| 20 | 设置 · Webhooks | 同上 + `section=WEBHOOKS` | 1 |
| 21 | 设置 · 离线 OFFLINE | 同上 + `section=OFFLINE` | 3 |
| 22 | 设置 · 恢复 RECOVERY | 同上 + `section=RECOVERY` | 1 |
| 23 | 设置 · 审计 AUDIT | 同上，但实际被重定向到 `SyncLogActivity` | 1 |
| 24 | 设置 · 关于 ABOUT | 同上 + `section=ABOUT` | 1 |
| 25 | 设置 · 实例 INSTANCE | 同上 + `section=INSTANCE` | 1 |
| 26 | 同步日志 | `SyncLogActivity` | 1 |
| 27 | 开源许可 | `OpenSourceLicensesActivity` | 1 |
| 28 | 引导欢迎页 | `SetupActivity` | 2 |
| 29 | 引导 · 登录 | `SetupLoginActivity` | 1 |
| 30 | SSO 登录 | `SsoLoginActivity` | 1 |
| 31 | SSO 回调 | `SsoCallbackActivity`（无 UI，转发后 finish） | 1 |
| 32 | 备份导入/导出 | `BackupTransferActivity` | 4 |
| 33 | 统计小组件配置 | `UserStatsWidgetConfigActivity` | 1 |

### C. 系统组件

| 组件 | 类型 | 点击落点 |
| --- | --- | --- |
| 草稿小组件 | Glance AppWidget | `MainActivity` + `ACTION_OPEN_COMPOSER` → 自动开编辑器 |
| 统计小组件 | Glance AppWidget | `MainActivity`（停留在上一次的目的地，不指定页面） |
| 统计小组件配置 | Activity（`APPWIDGET_CONFIGURE`） | 选择账号后关闭 |

## 三、入口清单（51）

### A. 系统 / 外部入口（11）

| # | 触发点 | 落点 |
| --- | --- | --- |
| 1 | 桌面图标 | `MainActivity` → 时间线 |
| 2 | 分享 `SEND text/*` | 编辑器（新建，预填文本） |
| 3 | 分享 `SEND image/*` | 编辑器（新建，带图片） |
| 4 | 分享 `SEND */*` | 编辑器（新建，带文件） |
| 5 | 分享 `SEND_MULTIPLE image/*` | 编辑器（新建，多图） |
| 6 | 分享 `SEND_MULTIPLE */*` | 编辑器（新建，多文件） |
| 7 | SSO 回调 deep link | `SsoCallbackActivity` → `SsoLoginActivity` |
| 8 | 草稿小组件点击 | `MainActivity` → 时间线 + 自动开编辑器 |
| 9 | 统计小组件点击 | `MainActivity`（默认目的地） |
| 10 | 统计小组件未配置点击 | 统计小组件配置页 |
| 11 | 首次启动 / 引导未完成 | `SetupActivity`（`MainActivity` 自动拉起） |

### B. 主界面内入口（13）

| # | 触发点 | 落点 |
| --- | --- | --- |
| 12 | 底栏 Memos | 时间线 |
| 13 | 底栏 Map | 地图（可被用户隐藏） |
| 14 | 底栏 Attachments | 附件 |
| 15 | 底栏 Profile | 个人 |
| 16 | 平板 NavigationRail（4 项） | 同 12–15（另一套控件） |
| 17 | 搜索图标 | 搜索覆盖层 |
| 18 | FAB 新建 | 编辑器 |
| 19 | 草稿卡片 | 草稿箱 |
| 20 | 同步状态图标 | 同步状态面板 |
| 21 | 列表项点击（时间线/探索/地图/归档/搜索） | Memo 详情 |
| 22 | Feed 标签页切换 | 探索 |
| 23 | 附件项点击 | 全屏查看器（覆盖层） |
| 24 | 地图地点点击 | 地点面板（覆盖层，非独立页） |

### C. 个人中心入口（11）

| # | 触发点 | 落点 |
| --- | --- | --- |
| 25 | 资料头点击 | 账号切换 sheet |
| 26 | 资料头「编辑」 | 编辑资料 |
| 27 | 通知行 | 通知 |
| 28 | 快捷方式行 | SHORTCUTS 分区 |
| 29 | Webhooks 行 | WEBHOOKS 分区 |
| 30 | 实例信息行 | INSTANCE 分区 |
| 31 | 归档行 | 归档 |
| 32 | 设置行 | 设置总览 |
| 33 | 关于行 | ABOUT 分区 |
| 34 | 账号 sheet → 编辑凭据 | 编辑账号凭据 |
| 35 | 账号 sheet → 添加账号 | 登录弹窗（非页面） |

### D. 设置 / 关于及其它深层入口（12）

| # | 触发点 | 落点 |
| --- | --- | --- |
| 36 | 设置总览 · 通用 | GENERAL |
| 37 | 设置总览 · 外观 | APPEARANCE |
| 38 | 设置总览 · 导航 | NAVIGATION |
| 39 | 设置总览 · 离线 | OFFLINE |
| 40 | 设置总览 · 恢复 | RECOVERY |
| 41 | 关于卡 → 开源许可 | 开源许可 |
| 42 | 关于卡 → 重新运行引导 | `SetupActivity(fromSettings=true)` |
| 43 | 关于卡 → 同步日志 | AUDIT → `SyncLogActivity` |
| 44 | 搜索页缓存提示 | OFFLINE |
| 45 | 时间线缓存提示 | OFFLINE |
| 46 | 恢复卡 → 导出备份 | 备份导出 |
| 47 | 恢复卡 → 导入备份 | 备份导入 |

### E. 引导 / 登录 / 备份（4）

| # | 触发点 | 落点 |
| --- | --- | --- |
| 48 | 引导页 → 登录 | 引导登录 |
| 49 | 引导页 → 导入备份 | 备份导入 |
| 50 | 登录页 → 导入备份 | 备份导入 |
| 51 | 登录页 → SSO 登录 | SSO 登录 |

## 四、结构性问题

1. **没有路由表，入口 = 散落的 Intent 工厂。**
   `ProfileScreen` 自己持 4 个 `rememberLauncherForActivityResult`（通知/归档/设置/编辑资料），
   `SettingsActivity` 再持 3 个（许可/引导/分区跳转），`MemosScreen`、`SearchScreen`、`LoginScreen`、
   `RecoveryCard`、`SetupActivity` 各持 1–2 个。同一个目标（如 OFFLINE 分区）在 3 处被分别拼装。

2. **deep link 只有 SSO 一条，且 `onNewIntent` 只认分享和小组件 action。**
   `MainActivity.onNewIntent` 只解析 `ACTION_SEND*` 与 `ACTION_OPEN_COMPOSER`；任何新增的 `VIEW` 型
   Intent 都会被静默忽略（因为 `launchMode=singleTask`）。这是加 deep link 前必须先改的点。

3. **设置入口三重重复。** 个人页直链 SHORTCUTS / WEBHOOKS / INSTANCE 三个分区；个人页又链到设置总览；
   关于页再链一次。`SettingsOverview` 只列 5 个分区，另外 5 个只能从个人页/关于页进——同一个
   `SettingsActivity` 有两个"首页"。

4. **AUDIT 分区与 `SyncLogActivity` 是同一内容的两套入口。** `SettingsActivity.createIntent` 把
   `AUDIT` 重定向到 `SyncLogActivity`；`SettingsScreen` 里 `AUDIT` 分支又是死代码（`Unit`）。

5. **分享入口 5 条，落点全是同一个编辑器。** `text/*` 与 `image/*` 已被 `*/*` 覆盖，分享面板里会
   出现 3 个同名"新建 memo"；`SEND_MULTIPLE` 同理。

6. **小组件不能直达页面。** 草稿小组件只能"开 App + 开编辑器"，统计小组件只能"开 App"，且落点是
   上一次的目的地（`rememberSaveable`），不是它展示的数据页。

7. **页面不可寻址 → 也无法被外部引用。** 一条 memo 没有可分享的 App 内链接；标签、搜索结果、
   归档列表同理。

## 五、方案

### 方案 A：入口注册表 + 系统快捷入口（保持 Activity 架构）

| 项 | 内容 |
| --- | --- |
| 入口形态 | 外部（快捷方式 / scheme / 小组件）与内部入口共用同一张"入口表" |
| 改动 | ① 新增 `ui/EntryPoints.kt`：`sealed interface AppEntry`（Memos / Map / Attachments / Profile / SettingsSection / Notifications / Archived / Drafts / Editor(request) / Search(query)）；② `EntryRouter.open(context, entry)` 唯一出口；③ `MainActivity.onNewIntent` 统一走 Router，未登录时把 entry 入队（复用 `pendingShareDataFlow` 模式）；④ `res/xml/shortcuts.xml` 4 条静态快捷方式（新建 / 草稿 / 通知 / 搜索）；⑤ 给 `MainActivity` 加 `VIEW` filter（`memosm://open?...`），参数白名单校验 |
| 优点 | 增量、可回退；立刻获得"从系统直达页面"的能力；为 C/D 铺路；不动现有 16 个 Activity |
| 代价 | 仍是 Intent 语义，回退栈不统一；scheme 参数需要校验与降级（无效 entry → 时间线） |
| 成本 | M（约 400–600 行 + 单测：entry 解析矩阵、未登录排队） |
| 风险 | 低。唯一硬约束是 `singleTask` + `onNewIntent` 必须补全 |

### 方案 B：单 Activity + 类型安全路由

| 项 | 内容 |
| --- | --- |
| 入口形态 | 页面 = 路由，入口 = 路由 + 参数；深链直接映射路由 |
| 改动 | ① 引入 `androidx.navigation.compose`；② `MainScreen` 4 目的地变顶层 graph；③ `ProfileActivity` 的 8 个子类改为 route；④ `SettingsSection` 变为路由参数（现在是字符串 extra）；⑤ 编辑器的 session 回传改 `SavedStateHandle`；⑥ `refreshAfterProfileDetails` 的"广播式刷新"改为返回结果驱动；⑦ `navDeepLink` 集中声明 |
| 优点 | 入口、回退栈、深链三者统一；任何页面可被 URL 寻址；predictive back 更自然 |
| 代价 | 16 → 3 个 Activity（Main / Setup / SsoCallback）；编辑器的 Activity result 协议、转场动画、双栏详情、每页的 `onResume` 刷新都要重写 |
| 成本 | L（跨 20+ 文件） |
| 风险 | 中高。这个 App 目前重度依赖"每个页面一个 Activity + `onResume` 刷新 + `RESULT_OK` 回传"，迁移面比页数看起来大 |
| 何时值得 | 产品需要"分享 memo 链接 / 浏览器打开 memo / 通知直达某条 memo"时才投入 |

### 方案 C：入口分层与中枢化（IA，不动架构）

| 项 | 内容 |
| --- | --- |
| 入口形态 | 减少入口数量、提高可发现性：个人页三组、设置单入口、FAB 长按动作 |
| 改动 | ① 个人页重排为「身份 / 内容（通知·归档·草稿）/ 系统（设置·关于·实例·快捷方式·Webhooks）」；② `SettingsOverview` 补齐 10 个分区（现在只有 5），个人页只留一个"设置"入口，去掉 4 处直链；③ 审计统一：删掉 `SyncLogActivity` 直链，AUDIT 走同一路由；④ 长按 FAB 展开快捷动作（新建 / 草稿 / 拍照 / 语音 / 搜索），作为"动作入口"；⑤ 把导航设置里已有的目的地显隐/排序能力扩展到个人页快捷行 |
| 优点 | 不改架构、可一天内看到效果；入口从 51 降到约 40，重复入口消失；长按 FAB 提供一种"新入口方式"而无需新页面 |
| 代价 | 只解决"找得到"，不解决"外部可达"；需要同步改 3 个测试 |
| 成本 | S/M |
| 风险 | 低（纯 UI/IA） |

### 方案 D：系统入口矩阵（App Links / 小组件直达 / 分享收敛）

| 项 | 内容 |
| --- | --- |
| 入口形态 | 把 App 变成"可被系统调用的一组入口"：App Shortcuts、小组件直达、HTTPS App Links、分享收敛、Quick Settings |
| 改动 | ① 分享 filter 5 → 2（`SEND` / `SEND_MULTIPLE` 各一条 `*/*`）；② 草稿小组件 → 草稿页、统计小组件 → 资料页；③ `https://<instance>/memos/{uid}` 的 App Links（`assetlinks.json`，可在无 App 时回落到浏览器）；④ Quick Settings tile（新建 memo）；⑤ 通知入口（当前没有通知发送代码，需先补通知能力） |
| 优点 | 外部可达性质变：浏览器/memo 链接/桌面 widget 都能落到具体页面；分享面板不再出现 3 个同名项 |
| 代价 | App Links 需要域名与服务端配合；scheme + App Links + SSO 三条入口要统一解析；小组件直达需要 Router（依赖方案 A） |
| 成本 | M（不含通知） |
| 风险 | 中（外部分发/域名配置）；通知部分另算 |

### 入口方式对比

| 方式 | 外部可达 | 回退栈 | 成本 | 说明 |
| --- | --- | --- | --- | --- |
| 底栏 / 侧栏 | 否 | 无 | S | 只适合一级目的地，已有且可配置 |
| 个人页 / 设置行 | 否 | 无 | S | 二级页面的默认方式，易重复 |
| Activity + 显式 Intent | 部分 | 系统栈 | S | 现状主力，新增入口时每条都要手写 |
| Scheme deep link | 是 | 需解析 | M | 无需域名，适合 App 内分发 |
| App Shortcuts | 是 | 冷启动 | S | 长按图标的"入口面板"，性价比最高 |
| 小组件直达 | 是 | 冷启动 | S/M | 需要 Router 才能指定页面 |
| HTTPS App Links | 是（浏览器） | 需解析 | M/L | 需服务端 `assetlinks.json` |
| 通知 | 是 | 需解析 | M | 当前无通知能力 |

## 六、推荐路线（当前范围）

最新确认：**memo 单条分享 / 链接直达暂不做**（P2、P3 全部搁置），不新增页面。
连带结论：方案 A 的"入口注册表 + `onNewIntent` + 未登录排队"整套基建也**不再必需**——它原本只为
"外部链接直达任意页面"服务；不做分享后，剩下的外部入口只有快捷方式和小组件，用 `Intent` extra 即可。

当前可做的三件事（都很小，互不依赖）：

| 优先级 | 事项 | 改动 | 成本 | 状态 |
| --- | --- | --- | --- | --- |
| 1 | 分享 filter 5 → 2 | 纯 `AndroidManifest.xml`，删掉被 `*/*` 覆盖的 3 条 | 10 分钟 | ✅ 已完成 |
| 2 | App Shortcuts（新建 / 草稿 / 通知 / 搜索）+ 小组件直达页面 | 新增动态快捷方式；`MainActivity` 解析 action/`EXTRA_DESTINATION`；小组件带上目的地 | S | ✅ 已完成 |
| 3 | 入口收敛（方案 C） | 设置单入口、个人页三组、长按 FAB 动作条 | S/M | ✅ 已完成 |

不做：方案 B（单 Activity 路由）、App Links、deep link 解析、memo 链接、通知入口。

### 落地清单

| 入口 | 实现 | 文件 |
| --- | --- | --- |
| 长按图标 · 新建 / 草稿 / 通知 / 搜索 | `AppShortcuts` 动态发布（4 条），`MemosApplication` 启动时后台同步 | `AppShortcuts.kt`、`MemosApplication.kt`、`res/drawable/shortcut_*.xml` |
| 快捷方式路由 | `MainActivity.handleEntryIntent` 统一收口，消费后清掉标记（避免旋转重放） | `MainActivity.kt` |
| 未登录 / 引导中 | 请求挂在 StateFlow 上，绑定账号后才落地 | `MainActivity.kt` |
| 搜索直达 | `MainScreen` 切到 MEMOS → `MemosScaffold` 展开搜索框 → 回报消费 | `MainScreen.kt`、`MemosCommon.kt`、`MemosScreen.kt` |
| 统计小组件 → 资料页 | Intent 附带 `EXTRA_DESTINATION=PROFILE` | `UserStatsWidget.kt` |
| 长按 FAB | 非消费式长按检测 + `ActionSheet`（新建 / 草稿 / 搜索），点击不被吞掉 | `MemosScreen.kt` |
| 分享入口 | `SEND`、`SEND_MULTIPLE` 各保留一条 `*/*` | `AndroidManifest.xml` |
| 设置单入口 | 个人页只保留「通知 / 归档 / 草稿」+「设置」，`SettingsOverview` 列出全部 10 个分区 | `ProfileScreen.kt`、`SettingsOverview.kt` |
| 审计统一 | 删掉 `SyncLogActivity` 直链与 `SyncLogScreen`，AUDIT 分区直接由 `SettingsActivity` 渲染 | `ProfileActivity.kt`、`SettingsOverview.kt` |

### 验证

- `:app:compileDebugKotlin`、`:app:compileDebugAndroidTestKotlin`、`:app:assembleDebug` 全部通过。
- `:app:testDebugUnitTest`：339 项中 61 项失败，全部是 `ContainerTest` / `MemosApiIntegrationTest`
  （报错 `Could not find a valid Docker environment`），与本轮改动无关；其余 278 项通过。
- 真机（Redmi K30 Pro / `lmipro`）已安装 `app-debug.apk`，包名 `org.example.memosm.debug`；
  `cmd shortcut get-shortcuts` 确认 4 条动态快捷方式均已发布（新建 / 草稿 / 通知 / 搜索）。
- 长按 FAB 的首次实现有缺陷并已修复：`waitForUpOrCancellation()` 对**被消费**的 up 事件返回 `null`，
  而按钮自身的 `clickable` 在 Main 阶段先处理该事件，于是每次单击都被判定成长按。现改为直接用
  `awaitPointerEvent()` 等"所有指针抬起"，超时才判定长按。
- 归档后列表不刷新的缺陷已修复：`MemosViewModel.handleMemoStateChange` 里
  `memo.state ?: "NORMAL"` 会被推断成 `Any`，再与字符串 `"ARCHIVED"` 比较永远为假，
  于是归档只写到了服务端和缓存，内存里的列表没有移动（重启后才从缓存读回正确结果）。
  现改为比较 `MemoState` 枚举，判定逻辑抽成 `memoStateMove()` 并加了单测。
- 其余 UI 行为由人工验收，清单见第九节。

## 九、真机验收清单

安装包：`app/build/outputs/apk/debug/app-debug.apk`，包名 `org.example.memosm.debug`。
下面每条给出「操作 → 期望」，不通过时记录现象即可。

### 1. 分享入口去重

| # | 操作 | 期望 |
| --- | --- | --- |
| 1.1 | 任意 App 分享一段文字 / 一张图片到 Memos M | 分享面板里 **只有 1 个**「新建笔记」条目（改前是 3 个同名项） |
| 1.2 | 多选几张图片后分享 | 只有 1 个「新建笔记」条目，编辑器内带全部图片 |
| 1.3 | 分享纯文本 | 编辑器新建，正文已预填 |

### 2. 长按图标快捷方式

| # | 操作 | 期望 |
| --- | --- | --- |
| 2.1 | 桌面长按应用图标 | 出现 4 个：**新建 / 草稿 / 通知 / 搜索** |
| 2.2 | 冷启动（先划掉后台）点「新建」 | 进主界面并直接打开编辑器 |
| 2.3 | 冷启动点「草稿」 | 直接进草稿页（空态显示「无草稿」） |
| 2.4 | 冷启动点「通知」 | 直接进通知页 |
| 2.5 | 冷启动点「搜索」 | 进时间线且搜索框已展开、键盘弹出 |
| 2.6 | 热启动（App 在后台且停在「我的」页）点「新建」 | 自动切回「笔记」页并打开编辑器 |
| 2.7 | 热启动点「草稿」 | 直接打开草稿页 |
| 2.8 | 从草稿页返回，再旋转屏幕 | **不应**再次自动打开草稿页（意图已被消费） |

### 3. 小组件直达

| # | 操作 | 期望 |
| --- | --- | --- |
| 3.1 | 桌面添加「用户统计」小组件并配置账号，点击它 | 打开 App 并停在**「我的」页**（改前停在上次所在页） |
| 3.2 | 未配置账号时点击统计小组件 | 仍进入配置页（原行为不变） |
| 3.3 | 点击「草稿」小组件 | 打开 App 并直接进入编辑器（原行为不变） |

### 4. 个人页入口收敛

| # | 操作 | 期望 |
| --- | --- | --- |
| 4.1 | 进「我的」页 | 分组为：头部资料 + 统计卡 +「通知 / 归档 / **草稿** / 设置」 |
| 4.2 | 检查是否有「快捷方式」「Webhooks」「实例信息」「关于」行 | **应全部消失** |
| 4.3 | 点「草稿」行 | 进草稿页（新入口） |
| 4.4 | 点「设置」行 | 进设置总览 |

### 5. 设置总览分区

| # | 操作 | 期望 |
| --- | --- | --- |
| 5.1 | 进设置总览 | 分 4 组共 **10 项**：通用 / 外观 / 导航；快捷方式 / Webhooks / 实例信息；离线缓存 / 数据恢复 / 审计日志；关于 |
| 5.2 | 点「快捷方式」「Webhooks」「实例信息」 | 均能正常进入对应分区（改前只能从个人页进） |
| 5.3 | 点「审计日志」 | 进入审计日志页，标题「审计日志」，折叠头部正常滚动 |
| 5.4 | 设置 → 关于 → 点「同步日志」 | 仍能打开审计日志（与 5.3 同一页面，不再是独立 Activity） |

### 6. 长按 FAB

| # | 操作 | 期望 |
| --- | --- | --- |
| 6.1 | 在「笔记」页**轻点** FAB | 正常新建（若已有草稿则先弹草稿提示）——行为不变 |
| 6.2 | **按住** FAB 约 0.5 秒 | 轻微振动 + 弹出底部面板「快捷操作」：新建 / 草稿 / 搜索 |
| 6.3 | 长按后松手 | **不应**同时触发新建（面板打开即止） |
| 6.4 | 面板选「搜索」 | 面板收起，搜索框展开 |
| 6.5 | 面板选「草稿」 | 面板收起，进入草稿页 |
| 6.6 | 面板外点击 / 下滑关闭 | 面板关闭；再轻点 FAB 仍能正常新建 |
| 6.7 | 切换主题到 MD 风格后再试 6.1–6.6 | 行为一致 |

### 7. 回归检查

| # | 操作 | 期望 |
| --- | --- | --- |
| 7.1 | 底栏四个目的地（笔记 / 地图 / 附件 / 我的）逐个切换 | 正常，无闪退 |
| 7.2 | 设置 → 导航 → 隐藏「地图」 | 底栏与设置总览均正常 |
| 7.3 | 双指/滚动收起底栏、双击底栏刷新 | 与改前一致 |
| 7.4 | 平板/横屏（>600dp） | 侧边 NavigationRail 正常，四个目的地可切换 |
| 7.5 | 编辑资料、编辑账号凭据、开源许可、数据恢复导入导出 | 均能正常打开并返回 |

### 已知限制（非缺陷）

- 快捷方式为**动态发布**，由 MIUI 桌面缓存；若刚安装后长按看不到，杀进程重开 App 或重启桌面即可。
- 分享出去的 memo Web 链接仍是普通网页链接（App Links 已确认不做），点开落在浏览器版 Memos。

## 七、已确认结论

| 问题 | 结论 | 影响 |
| --- | --- | --- |
| 是否分享单条 memo 链接并在 App 内打开 | **先不做** | P2/P3 搁置；不需要入口注册表与深链解析 |
| 是否新增页面 | **不新增** | 所有入口落现有页面 |
| 浏览器→App 的域名策略 | **C：不做 App Links** | 不做 manifest 域名绑定 |

## 八、memo 单条链接入口设计（暂缓，保留作参考）

> 本节方案暂不实施；若将来重启"memo 分享/链接直达"，从这里开始，不必重新调研。

### 8.1 链接契约

| 形态 | 例 | 用途 | 生成方 |
| --- | --- | --- | --- |
| Web（当前服务端路由） | `https://{host}/memos/{uid}` | 分享给任何人；未装 App 时走浏览器 | App「分享链接」 |
| Web（旧版兼容） | `https://{host}/m/{uid}` | 老版本 memos Web，解析时兼容 | — |
| App scheme | `memosm://memo/{uid}?host={urlencoded}` | App↔App、App 内链接 | App「分享链接」可选附加 |
| 公开分享 | `https://{host}/memos/shares/{token}` | 服务端生成的公开分享 | 服务端；App 仅解析并转浏览器 |

> 服务端 Web 路由以 `memos/:uid` 为准（usememos/memos `web/src/router`），早期版本为 `/m/:uid`，所以**解析两种都收、生成只写一种**。

### 8.2 打开链路（全部复用现有页面）

```
VIEW Intent / scheme
  → MainActivity.onCreate / onNewIntent（singleTask，两条路径必须同一处理函数）
  → EntryRouter.parse(uri) = AppEntry.OpenMemo(uid, host?)
  → 校验 host ∈ 已配置账号；不匹配 → 外部浏览器（绝不用当前 token 请求未配置 host）
  → 未登录 / 引导未完成 → pendingEntry 排队（沿用 pendingShareDataFlow 模式）
  → destination = MEMOS，pager 回到「我的」feed
  → MemosViewModel.openMemo(uid)
        1) 列表命中（user / explore / archived / search）
        2) 缓存命中（cached_memos.name = "memos/{uid}"）
        3) api.getMemo("memos/{uid}")；离线则提示并中止
  → navigator.navigateTo(Detail, MemoKey(name)) + selectMemo(memo)
  → 现有 MemoDetailView 展示（手机为滑入层，返回键回列表）
```

### 8.3 两处必须先补的缺口（代码事实）

1. **详情面板的打开只由列表点击驱动。** `MemosScaffold` 的 `navigator` 是 Compose 局部状态：
   只有 `listPane` 的点击回调会 `navigator.navigateTo(Detail, …)`；外部改 `detailPane.selectedMemo`
   不会打开面板（同步 Effect 的依赖里没有 `selectedMemo`，方向是 navigator → selectedMemo）。
   → 需要一个 `openMemoRequest` 信号，由 `MemosCommon` 消费后主动 `navigateTo`。
2. **详情内容只从"当前列表池"解析。** 渲染时按 `MemoKey` 在 `memos` / `searchMemoList` 里 `find`，
   找不到就什么都不显示（双栏时才显示占位）。一条不在当前 feed 里的旧 memo 即使拿到了也显示不出来。
   → 给 `detailPane` 加一个外部打开专用的 `overrideMemo` 槽位（零列表副作用），渲染时优先用它。

### 8.4 改动清单

| 文件 | 改动 |
| --- | --- |
| `AndroidManifest.xml` | `MainActivity` 增加 `VIEW` + `memosm` scheme（host=`memo`）；App Links 另加（8.5） |
| `MainActivity.kt` | `onNewIntent` / `onCreate` 统一 `dispatchEntry(intent)`；`pendingEntry` 队列；`TextUtils` 判断 |
| 新增 `ui/entry/EntryPoints.kt`、`EntryRouter.kt` | URL 解析矩阵、host 白名单、降级到时间线 |
| `MemosViewModel.kt` | `openMemo(uid)`（列表→缓存→API）、`openMemoRequest` 状态、失败提示 |
| `UiState.kt` | `openMemoRequest: OpenMemoRequest?`（uid/name/序号），`DetailPaneState.overrideMemo` |
| `MemosCommon.kt` | 消费 `openMemoRequest` → `navigateTo(Detail)`；渲染时优先 `overrideMemo` |
| `MemoItem.kt` / `MemoDetailView.kt` | 「分享链接」菜单项（复用 `Intent.createChooser` 模式） |
| `MemoItem.kt` / `NativeMarkdownNode.kt` / `NativeLinkPreview.kt` | 同实例 `/{memos\|m}/{uid}` 链接改走 `EntryRouter`，其余仍 `ACTION_VIEW` |
| 测试 | 解析矩阵（含旧路由/坏参数/异地 host）、未登录排队、openMemo 三级降级、返回栈 |

### 8.5 「浏览器点链接进 App」的三种做法（已选 C）

| 方案 | 做法 | 覆盖范围 | 成本 |
| --- | --- | --- | --- |
| A 固定实例域名 + App Links | manifest 写死该实例 host + 实例根目录放 `/.well-known/assetlinks.json` | 只对自己一个实例生效 | S |
| B 自有中转域名 | `https://<域名>/m/{host}/{uid}` 静态页 + assetlinks，页内跳 `memosm://` | 任意实例，需要域名与托管 | M |
| **C 不做 App Links（已选）** | 只保留 scheme + App 内链接拦截；分享出去的 Web 链接永远落在服务端 Web UI | 分享给他人时无法进 App | 0 |

**C 的体验边界（必须写进分享文案/README）**：Web 链接是"给任何人的可读链接"，不是"App 深链"；
App 侧只保证两条路径可达详情——① 在 App 内点同实例 memo 链接；② 系统/其他 App 打开 `memosm://`。

### 8.7 C 之下的三个低成本补强

1. **分享文案双行**：第一行 Web 链接，第二行 `在 Memos M 中打开：memosm://memo/{uid}?host=…`。
   Telegram/Slack 等会把自定义 scheme 渲染成可点链接，点开直接进 App；微信等不支持则自动只用到第一行。
2. **分享进 App 时识别 memo 链接**：App 已是 `text/*` 分享目标；若分享文本里含"已配置实例的 memo URL"，
   直接走 `openMemo` 而不是预填编辑器（对"从浏览器分享到 App"这个动作闭环）。需要一次确认弹窗，避免误吞普通文本。
3. **App 内链接长按菜单**：Markdown 里的同实例链接，长按提供"在 App 内打开 / 用浏览器打开"。

### 8.6 风险

- **Token 泄漏**：链接里的 host 必须命中已配置账号才发请求；uid 做白名单校验（`[A-Za-z0-9_-]+`）。
- **冷启动丢弃**：`singleTask` 下新 Intent 走 `onNewIntent`，现有实现只认分享与小组件，`VIEW` 会被静默吞掉——P1 必须一起改。
- **引导/无账号**：链接必须先排队，setup 完成后再消费，否则被 `MainActivity` 的引导流程覆盖。
- **版本差异**：v0.26 与 v0.35 资源名/路由不同，`memos/{uid}` 的构造应走 `MemosApiFactory` 已有的版本分支。
- **私有 memo**：Web 链接对未登录者不可读，分享文案需注明"需登录该实例"。
- **双栏**：平板/桌面打开链接时只应选中详情一次，不要重复 `navigateTo` 造成返回栈叠层。
