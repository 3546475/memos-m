<div align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" width="128" alt="MemosM 应用图标" />

# MemosM

面向 [Memos](https://usememos.com/) 的原生 Android 客户端，使用 Jetpack Compose 构建，采用 Miuix（HyperOS）设计语言。

[English](README.md) · [简体中文](README.zh-CN.md)

![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/yamada-sexta/memos-m/canary-build.yml?style=flat-square)
![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/yamada-sexta/memos-m/nightly-test.yml?style=flat-square&label=nightly%20API%20tests)
![Android](https://img.shields.io/badge/Android-7.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)
![GitHub Downloads (all assets, all releases)](https://img.shields.io/github/downloads/yamada-sexta/memos-m/total?style=flat-square)
![GitHub License](https://img.shields.io/github/license/yamada-sexta/memos-m?style=flat-square)
![GitHub Repo stars](https://img.shields.io/github/stars/yamada-sexta/memos-m?style=flat-square)
![GitHub Issues or Pull Requests](https://img.shields.io/github/issues/yamada-sexta/memos-m?style=flat-square)

</div>

---

**MemosM** 是一个跟随 Memos 当前服务端版本的 Android 客户端。支持 Memos v0.26 及以上：应用会探测实例版本，绑定对应的 API 适配器；对更新或未知的版本，回退到最新的适配器——见[服务器支持](#服务器支持)。支持 Android 7.0（API 24）及以上。

本分支提供 **仅 Miuix** 的构建：只有一种设计语言，整体沿用 HyperOS 风格——成组的设置卡片、悬浮胶囊底栏、磨砂表面，以及应用自绘的点阵加载器。`main` 另外保留了 Material 3 路径和样式切换；除此之外，两个分支保持同步。见[分支](#分支)。

应用追求原生 Android 手感、快速同步，以及对当前 Memos 功能的完整支持，并带有离线优先的数据层与结构化筛选系统。

## 截图

<img height="300"  alt="MemosM 截图" src="https://github.com/user-attachments/assets/aba051d0-0c6d-4d0a-9bd0-9b7d0389d000" />

<img height="300"  alt="MemosM 截图" src="https://github.com/user-attachments/assets/c1c3ac79-1d17-402a-b538-1eafe6b624e8" />

## 为什么选择 MemosM？

- **原生 Android**：为 Android 及其平板打造，而不是跨平台移植——底栏或导航栏、桌面小组件、分享入口都是应用本身的一部分，而不是附加功能。
- **高效同步**：分页拉取加本地缓存，而不是每次刷新都重新同步全部历史。
- **默认离线优先**：本地优先缓存、离线写入排队、冲突处理与可选的预下载，让应用在无网络时仍然可用。
- **贴合的筛选**：保存视图、可折叠标签树、日期区间、内容类型与排序——编译为服务端的 CEL 过滤条件，并在本地镜像以支持离线使用。
- **跟上 Memos**：跟随较新的服务端版本，换来现代 Markdown、表情反应与 SSO，而不是背负旧版本兼容的包袱。

## 功能

- **账号与登录**
  - 令牌、密码或 SSO 登录
  - 多账号快速切换；缓存、筛选与排队写入均按账号隔离
  - Android 17 及以上对本地服务器的本地网络访问处理
- **笔记**
  - 撰写、编辑、删除、置顶、归档与取消归档
  - 作为分享目标，接收文本、图片与文件
  - 评论、表情反应（使用服务端的 emoji 列表）、关联笔记与位置
  - 按内容、标签和日期范围搜索，并支持排序
  - 结构化筛选面板：保存视图、可折叠标签树、日期预设与自定义区间、内容类型（链接/代码/待办）与排序。已应用的筛选按账号持久化，并以可移除的标签条显示在顶栏下方
- **Markdown**
  - GFM 渲染：标题、列表、任务列表、表格、引用、链接与图片
  - 代码块语法高亮，可选行号与自动换行
  - LaTeX 公式、链接预览、段落缩进与中西文混排间距选项
- **媒体**
  - 全屏图片查看器（可缩放）；头像裁剪
  - 视频播放（ExoPlayer）与音频录制/播放
  - 附件浏览、流式下载，以及有大小上限的离线媒体缓存
- **整理**
  - 个人页标签树、笔记类型（链接、代码、待办）、归档笔记、统计与活动日历
  - 通知列表（不做后台轮询）
- **离线与同步**
  - 本地优先缓存，信息流、搜索、地图与附件均可离线读取
  - 离线写入排队，重连后回放，并带冲突解决对话框
  - 可选的文本与附件预下载（仅 Wi-Fi），缓存大小上限
  - 同步状态面板、同步诊断（审计日志）、失败上传的重试/丢弃
- **外观**
  - Miuix（HyperOS）风格；浅色、深色或跟随系统
  - 跟随系统（Material You）、Memos、单色或自定义强调色；字体与标题字号选项
  - 边到边或悬浮胶囊底栏、滚动隐藏、底栏磨砂、条目排序；平板上使用导航栏
  - 笔记卡片的编辑手势（无、单击、双击或长按）
- **小组件与快捷方式**
  - 草稿小组件与用户统计小组件（带独立配置界面）
  - 四个启动器快捷方式：新建笔记、草稿、通知与搜索
- **备份与恢复**
  - 导出 `.mmbackup`（MessagePack + gzip，可选密码保护），涵盖账号、设置、缓存、媒体与草稿——见[备份说明](docs/backup.md)
- **地图**
  - 原生地图，支持笔记 / 探索 / 全部三种范围、位置图钉，并可在选定地点撰写笔记
  - OpenFreeMap 底图，附带 OpenStreetMap 栅格兜底
- **语言**
  - 英语、德语、日语、韩语、波兰语、简体中文与繁体中文

地图入口只在连接的实例接受 `has_location` 笔记筛选后出现；旧版或未知实例不会显示。此前验证过的账户可以离线浏览已缓存的位置（使用可用的缓存瓦片）。保存视图需要联网；地图区域不会预先下载。

## 服务器支持

实例探测读取服务端版本，并绑定对应的适配器：

| 服务端版本 | 适配器 |
| --- | --- |
| `0.26.x` | v0.26.0 |
| `0.27.x` | v0.27.0 |
| `0.28.x`、`0.29.x` | v0.28.0 |
| `0.30.x`、更新或未知 | v0.30.0（已实现的最新版本） |

探测使用当前的 v0.35.3 接口，并以无版本的 `/api` 接口兜底，因此隐藏版本号的服务端同样可以连接。

## SSO 登录

选择 **SSO**，输入你的 Memos 服务器地址，然后点 **查找提供商**。选择提供商后在浏览器中完成认证。MemosM 随后会像密码登录那样为该账号创建个人访问令牌。密码与手动令牌登录仍然可用。

管理员需要把应用的回调 URI 加入 Memos 中 OAuth2 客户端的允许重定向地址；原有的 Memos 网页回调也要保留。

| 构建 | 回调 URI |
| --- | --- |
| Release | `org.example.memosm://oauth/callback` |
| Insider | `org.example.memosm.insider://oauth/callback` |
| Canary | `org.example.memosm.canary://oauth/callback` |
| Debug | `org.example.memosm.debug://oauth/callback` |

提供商必须接受自定义 URI scheme 与 S256 PKCE；只接受 HTTPS 回调的提供商可以改用手动令牌登录。SSO 由 v0.26+ 适配器与较新的 Memos SSO 格式支持；注册与身份供给遵循服务端设置。登录尝试十分钟后过期；如果 Android 在认证过程中终止了应用，请重新发起登录。

## 下载

### 稳定版

> MemosM 仍在积极开发中，目前没有稳定版。

### Insider 构建

Insider 构建面向想使用较新功能、又不愿承受持续不稳定的用户。它通常可用，但仍可能存在缺陷和未打磨的功能。

[<img src="https://github.com/user-attachments/assets/713d71c5-3dec-4ec4-a3f2-8d28d025a9c6"
     alt="Get it on Obtinium"
     height="80">](<https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22org.example.memosm.insider%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fyamada-sexta%2Fmemos-m%22%2C%22author%22%3A%22yamada-sexta%22%2C%22name%22%3A%22MemosM%20(Insider)%22%2C%22preferredApkIndex%22%3A0%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Atrue%2C%5C%22fallbackToOlderReleases%5C%22%3Atrue%2C%5C%22filterReleaseTitlesByRegEx%5C%22%3A%5C%22Insider%20Build*%5C%22%2C%5C%22filterReleaseNotesByRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22verifyLatestTag%5C%22%3Afalse%2C%5C%22sortMethodChoice%5C%22%3A%5C%22date%5C%22%2C%5C%22useLatestAssetDateAsReleaseDate%5C%22%3Afalse%2C%5C%22releaseTitleAsVersion%5C%22%3Afalse%2C%5C%22trackOnly%5C%22%3Afalse%2C%5C%22versionExtractionRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22matchGroupToUse%5C%22%3A%5C%22%5C%22%2C%5C%22versionDetection%5C%22%3Afalse%2C%5C%22releaseDateAsVersion%5C%22%3Afalse%2C%5C%22useVersionCodeAsOSVersion%5C%22%3Afalse%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22invertAPKFilter%5C%22%3Afalse%2C%5C%22autoApkFilterByArch%5C%22%3Atrue%2C%5C%22appName%5C%22%3A%5C%22%5C%22%2C%5C%22appAuthor%5C%22%3A%5C%22%5C%22%2C%5C%22shizukuPretendToBeGooglePlay%5C%22%3Afalse%2C%5C%22allowInsecure%5C%22%3Afalse%2C%5C%22exemptFromBackgroundUpdates%5C%22%3Afalse%2C%5C%22skipUpdateNotifications%5C%22%3Afalse%2C%5C%22about%5C%22%3A%5C%22%5C%22%2C%5C%22refreshBeforeDownload%5C%22%3Afalse%2C%5C%22includeZips%5C%22%3Afalse%2C%5C%22zippedApkFilterRegEx%5C%22%3A%5C%22%5C%22%7D%22%2C%22overrideSource%22%3Anull%7D>)

### Canary 构建

Canary 构建是"最前沿"的版本，包含最新的代码、功能与改进，但出现严重回归或崩溃的风险也更高。

**预期：频繁更新、实验性改动，以及潜在的不稳定。**

[<img src="https://github.com/user-attachments/assets/713d71c5-3dec-4ec4-a3f2-8d28d025a9c6"
     alt="Get it on Obtinium"
     height="80">](<https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22org.example.memosm.canary%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fyamada-sexta%2Fmemos-m%22%2C%22author%22%3A%22yamada-sexta%22%2C%22name%22%3A%22MemosM%22%2C%22preferredApkIndex%22%3A0%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Atrue%2C%5C%22fallbackToOlderReleases%5C%22%3Atrue%2C%5C%22filterReleaseTitlesByRegEx%5C%22%3A%5C%22Latest*%5C%22%2C%5C%22filterReleaseNotesByRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22verifyLatestTag%5C%22%3Afalse%2C%5C%22sortMethodChoice%5C%22%3A%5C%22date%5C%22%2C%5C%22useLatestAssetDateAsReleaseDate%5C%22%3Atrue%2C%5C%22releaseTitleAsVersion%5C%22%3Afalse%2C%5C%22trackOnly%5C%22%3Afalse%2C%5C%22versionExtractionRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22matchGroupToUse%5C%22%3A%5C%22%5C%22%2C%5C%22versionDetection%5C%22%3Afalse%2C%5C%22releaseDateAsVersion%5C%22%3Afalse%2C%5C%22useVersionCodeAsOSVersion%5C%22%3Afalse%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22invertAPKFilter%5C%22%3Afalse%2C%5C%22autoApkFilterByArch%5C%22%3Atrue%2C%5C%22appName%5C%22%3A%5C%22MemosM%20(Canary)%5C%22%2C%5C%22appAuthor%5C%22%3A%5C%22%5C%22%2C%5C%22shizukuPretendToBeGooglePlay%5C%22%3Afalse%2C%5C%22allowInsecure%5C%22%3Afalse%2C%5C%22exemptFromBackgroundUpdates%5C%22%3Afalse%2C%5C%22skipUpdateNotifications%5C%22%3Afalse%2C%5C%22about%5C%22%3A%5C%22A%20lightweight%2C%20feature-rich%20Android%20client%20for%20Memos.%5C%22%2C%5C%22refreshBeforeDownload%5C%22%3Afalse%2C%5C%22includeZips%5C%22%3Afalse%2C%5C%22zippedApkFilterRegEx%5C%22%3A%5C%22%5C%22%7D%22%2C%22overrideSource%22%3Anull%7D>)

也可以直接从 [releases](https://github.com/yamada-sexta/memos-m/releases/tag/latest) 下载。

在 Android 17 及更高版本上，连接本地 Memos 服务器可能需要 **附近设备** 权限。MemosM 会在登录本地服务器或打开已保存的本地账号时请求该权限。如果被拒绝，可在登录页或应用设置中使用 **本地网络访问** 开启。公共服务器不需要该权限。

## 从源码构建

环境要求：

- JDK 21
- Android SDK，需安装 platform 37.1（`compileSdk = 37`，`minSdk = 24`）
- 仅 API 集成测试需要 Docker

```bash
# Debug 构建，以 <applicationId>.debug 与其他渠道并存安装
./gradlew :app:assembleDebug

# 其他渠道
./gradlew :app:assembleInsider
./gradlew :app:assembleCanary
./gradlew :app:assembleRelease
```

APK 按 ABI 拆分（`arm64-v8a` 与 `armeabi-v7a`），没有通用包。产物位于 `app/build/outputs/apk/<构建类型>/`。

| 构建类型 | Application ID | 桌面名称 | R8 | 签名 |
| --- | --- | --- | --- | --- |
| `debug` | `org.example.memosm.debug` | MemosM (Debug) | 否 | debug |
| `canary` | `org.example.memosm.canary` | MemosM | 否 | release |
| `insider` | `org.example.memosm.insider` | MemosM (Insider) | 否 | release |
| `release` | `org.example.memosm` | MemosM | 是 | release |
| `benchmark` | `org.example.memosm` | MemosM | 是 | release |

Release 构建（含 `canary`、`insider` 与 `benchmark`）使用仓库根目录的 `keystore.properties` 签名，CI 则使用 `KEYSTORE_PATH`、`KEYSTORE_PASSWORD`、`KEY_ALIAS` 与 `KEY_PASSWORD` 环境变量——见 `app/build.gradle.kts`。Debug 构建不需要这些。

`benchmark` 是 release 代码的可分析（profileable）但不可调试版本，并使用 release 签名，因此可以覆盖已安装的 release 版本而不丢数据；宏基准测试模块用它运行。

## 测试

```bash
# JVM 单元测试，无需设备
./gradlew :app:testDebugUnitTest

# Compose UI / 仪器化测试，需要设备或模拟器
./gradlew :app:connectedDebugAndroidTest
```

`MemosApiIntegrationTest` 通过 Testcontainers 启动真实的 Memos 容器，因此需要 Docker：

```bash
./gradlew :app:testDebugUnitTest --tests org.example.memosm.api.MemosApiIntegrationTest
```

该套件属于常规 JVM 测试任务；nightly 工作流还会显式地针对 canary 镜像和每个受支持的 release 镜像运行它。`baselineprofile` 与 `benchmark` 模块分别提供基线配置文件生成器与宏基准测试。

## 项目结构

| 路径 | 内容 |
| --- | --- |
| `app/src/main/java/org/example/memosm/api` | Retrofit 接口与按版本区分的服务端适配器（v0.26 至今，外加无版本兜底） |
| `app/src/main/java/org/example/memosm/data` | 缓存、离线待发队列、同步、备份、媒体、同步诊断与 DataStore 持久化 |
| `app/src/main/java/org/example/memosm/viewmodel` | ViewModel、列表管理器与各功能委托 |
| `app/src/main/java/org/example/memosm/ui` | Compose 界面、组件与 Miuix 主题层 |
| `app/src/main/java/org/example/memosm/model` | API 模型与结构化筛选模型 |
| `app/src/main/java/org/example/memosm/network` | 本地网络权限处理 |
| `app/src/main/java/org/example/memosm/widget` | Glance 桌面小组件 |
| `app/src/main/res/values-*` | 翻译 |
| `baselineprofile`、`benchmark` | 基线配置文件与宏基准测试模块 |
| `docs` | 设计与实现说明 |

设计说明：

- [筛选系统](docs/filter-system-refactor.md) —— 结构化筛选、CEL 编译、离线匹配
- [Markdown 渲染](docs/markdown-rendering.md)
- [备份格式](docs/backup.md)
- [悬浮胶囊底栏](docs/floating-capsule-highlight-card.md) —— 高光、阴影与模糊配方
- [页面入口](docs/entry-inventory.md) —— 界面盘点与各页面的入口方式

## 分支

- **`main`** —— 同时包含两种设计语言：Material 3 与 Miuix，可在外观设置中切换。
- **`small`** —— 当前分支：仅 Miuix。Material 3 路径与样式切换已移除，其余内容与 `main` 保持同步。

## 参与贡献

欢迎以代码、缺陷报告或功能建议的形式参与贡献。

- **Issue：** 提交 issue 讨论缺陷或设计想法。
- **Pull Request：** 欢迎聚焦、易于评审的 PR。
- **翻译：** 目前没有专门的翻译流程；欢迎针对 `app/src/main/res/values-*` 下的 `strings.xml` 提交 PR。

## 相关项目

- **Memos 官方项目：** [usememos.com](https://usememos.com/)
- **MoeMemos Android：** [github.com/mudkipme/MoeMemosAndroid](https://github.com/mudkipme/MoeMemosAndroid)

## 许可证

[MIT](LICENSE)
