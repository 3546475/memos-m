<div align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" width="128" alt="Moxia 应用图标" />

# Moxia

面向 [Memos](https://usememos.com/) 的 Android 客户端，基于 [yamada-sexta/memos-m](https://github.com/yamada-sexta/memos-m) 开发，整体换成 Miuix（HyperOS）设计语言 —— 本分支只有 Miuix 一种设计语言。

**简体中文** · [English](README.en.md)

</div>

---

**应用本身的一切请看原仓库**：它是什么、安装方式、完整功能、服务端支持、SSO、下载与构建方法——见
[yamada-sexta/memos-m](https://github.com/yamada-sexta/memos-m)。本仓库在此基础上继续开发，下面只列出本仓库相对原仓库做了哪些改动。

## 截图

<div align="center">
  <img src=".github/screenshots/feed.png" height="470" alt="笔记" />
  <img src=".github/screenshots/filter.png" height="470" alt="结构化筛选" />
  <img src=".github/screenshots/profile.png" height="470" alt="我的" />
  <br />
  <img src=".github/screenshots/appearance.png" height="470" alt="外观设置" />
  <img src=".github/screenshots/navigation.png" height="470" alt="导航设置" />
  <img src=".github/screenshots/composer.png" height="470" alt="新建笔记" />
</div>

## 本仓库的改动

- **Miuix（HyperOS）界面重做**：主题基础、主壳、设置、弹窗、表单、笔记卡片、Markdown 区块、动效以及所有加载指示器都换成了 Miuix 组件——成组卡片、磨砂表面、悬浮胶囊底栏与点阵加载器都来自这轮改造。`small`（当前分支）走到底：Material 3 路径与样式切换已移除，Miuix 是唯一的设计语言；`main` 两者都保留。
- **结构化筛选系统**：信息流上的筛选面板，包含保存视图、可折叠标签树、内容类型（链接/代码/待办）、日期预设与自定义区间；编译为服务端的 CEL 过滤条件，并在本地镜像以支持离线。已应用的筛选以可移除的标签条显示在顶栏下方。
- **导航设置**：底栏可贴边或使用悬浮胶囊、滚动隐藏、磨砂、条目排序，平板上还有导航栏；笔记卡片的编辑手势可配置。
- **启动器快捷方式**：新建笔记、草稿、通知、搜索四个动态快捷方式。
- **Markdown 相关**：代码块语法高亮、可选行号与换行，LaTeX 渲染，预览与分享用的纯文本渲染，链接预览与分享弹窗。
- **启动性能**：在启动前的判定完成前就绘制第一帧，启动壳镜像真实信息流的几何位置，信息流以骨架卡片开场，并有基线配置与宏基准测试兜底。
- **离线与同步加固**：笔记先在本地创建，离线队列在写入、重放、附件与恢复各环节得到加固，后台轮询不再在每次覆盖层出现时做对账。
- **滚动与手势打磨**：页面只按内容移动（没有越界回弹），下拉刷新需要明确下拉，程序化列表位移不再误触发顶栏/底栏显隐。
- **发布工程**：按 ABI 拆分 APK、带 keep 规则的 R8（反射驱动的 API 与模型类）、从 `keystore.properties` 或 CI 环境变量读取签名，以及 `benchmark` 构建类型。
- **测试**：筛选模型、Markdown、导航、外观与账号隔离的单元测试；API 集成测试仍由 Testcontainers 驱动。

## 许可证

[MIT](LICENSE)，与原仓库一致。
