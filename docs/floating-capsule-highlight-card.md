# 浮底栏「高光 + 阴影」配方卡

> Compose 胶囊底栏。日间：卡片白 + 很淡的环境阴影；夜间：深灰 + 细亮边。
> 目标是「浮起」，不是「第二层胶囊」。数值是名义值，按目标项目的密度与主题微调。

## 1. 外观参数

| | 日间 | 夜间 |
|---|---|---|
| 填充 | 白（`surfaceContainer`），模糊开 α=0.72，否则 1 | `#242424`（同 `surfaceContainer`），模糊开 α=0.86，否则 1 |
| 阴影：扩散 / 下移 / 总α / 层数 | 34dp / 4dp / 0.05 / 16 | 无 |
| 外缘亮线 | 1dp 白，α `0.55 → 0.18 → 0.08`（上→下） | 1dp 白，α `0.35 → 0.15 → 0.20` |
| 内晕 | 2dp 白，α `0.05 → 0` | 2dp 白，α `0.10 → 0.03 → 0.05` |

- 日/夜判定：`surface.luminance() > 0.5f`。
- 填充用**卡片色**，不要用页面 `surface`；深色主题的 `surface` 常是纯黑，会丢失参考图的 `#242424`。
- 填充保持纯色，**不要**顶部渐变或白色覆盖；高光只来自外缘。

## 2. 绘制顺序

```kotlin
val capsule = RoundedCornerShape(percent = 50)

Row(
    modifier = Modifier
        .width(barWidth)
        .drawWithContent {                    // ① 阴影画在 clip 之外
            drawCapsuleShadow(
                size = size,
                spread = 34.dp.toPx(),
                offsetY = 4.dp.toPx(),
                layers = 16,
                maxAlpha = if (isLight) 0.05f else 0f
            )
            drawContent()
        }
        .clip(capsule)                        // ② 之后的内容裁成胶囊
        .background(fill)
        .drawWithContent {                    // ③ 高光画在内容之上
            drawContent()
            drawCapsuleRim(size, glowBrush, edgeBrush)
        }
) { /* items */ }
```

① 必须在 `.clip()` 之前，否则阴影会被裁掉。
③ 必须在内容之后，否则按压高亮会盖住亮线。

## 3. 阴影：叠层胶囊

```kotlin
private fun DrawScope.drawCapsuleShadow(
    size: Size,
    spread: Float,
    offsetY: Float,
    layers: Int,
    maxAlpha: Float
) {
    if (maxAlpha <= 0f) return
    // 每层 alpha 由总 alpha 反推：1 - (1 - layerAlpha)^layers = maxAlpha
    val layerAlpha = 1f - (1f - maxAlpha).pow(1f / layers)
    val step = spread / layers
    val radius = size.height / 2f
    for (i in layers downTo 1) {              // 从大到小
        val grow = step * i
        drawRoundRect(
            color = Color.Black.copy(alpha = layerAlpha),
            topLeft = Offset(-grow, -grow + offsetY),
            size = Size(size.width + grow * 2f, size.height + grow * 2f),
            cornerRadius = CornerRadius(radius + grow)
        )
    }
}
```

- 越靠外越淡，越靠底栏重叠越多、越深。
- `spread` 控制扩散范围（柔度）；`layers` 只控制渐变颗粒度与每层 alpha。
- 这是环境阴影，不是方向投影；只改 `offsetY` 不改变柔度。

## 4. 高光：1dp 亮线 + 2dp 内晕

```kotlin
private fun DrawScope.drawCapsuleRim(
    size: Size,
    glowBrush: Brush,
    edgeBrush: Brush
) {
    val glowWidth = 2.dp.toPx()
    val edgeWidth = 1.dp.toPx()

    // 内晕
    drawRoundRect(
        brush = glowBrush,
        topLeft = Offset(glowWidth / 2f, glowWidth / 2f),
        size = Size(size.width - glowWidth, size.height - glowWidth),
        cornerRadius = CornerRadius((size.height - glowWidth) / 2f),
        style = Stroke(glowWidth)
    )
    // 外缘亮线
    drawRoundRect(
        brush = edgeBrush,
        topLeft = Offset(edgeWidth / 2f, edgeWidth / 2f),
        size = Size(size.width - edgeWidth, size.height - edgeWidth),
        cornerRadius = CornerRadius((size.height - edgeWidth) / 2f),
        style = Stroke(edgeWidth)
    )
}
```

`edgeBrush` / `glowBrush` 用 `Brush.verticalGradient`，颜色是 `Color.White.copy(alpha = …)`，停靠点（上→下）：
- edge：日间 `0 / 0.45 / 1`，夜间 `0 / 0.4 / 1`
- glow：日间 `0 / 1`，夜间 `0 / 0.5 / 1`

## 5. 关键坑

1. **层数过高 + 总 α 过低会消失**：8-bit 混合下每层 alpha 太小时会被舍入掉。实测 40 层 / 3% 不可见，16 层 / 5% 可见。
2. **层数也会影响实际深浅**：若每层按 1 LSB 累积，层数越多阴影越重；想加深优先调 `maxAlpha`，不要只加层数。
3. **阴影必须画在 clip 外，且父布局不要 `clipToBounds`；高光必须画在内容之后。**
4. **不要用 `Modifier.shadow`**：阴影容易和 scale/fade 显示动画错拍（晚一帧弹出）；叠层与底栏同一次绘制，动画同步。
5. **形状回声**：阴影本质上和底栏同形状；`spread` 大 + `maxAlpha` 小 = 环境暗部，不易看出第二层胶囊。
6. **按压高亮**：item 的 `.clip(capsule)` 放在 `.clickable()` 之前，指示层才会被裁成胶囊。
7. **手势条**：容器加 `navigationBarsPadding()`；内容底部 padding 加上 `WindowInsets.navigationBars` 的底值。阴影**不要**用 `clipRect` 硬截在 inset 上沿——截止线上方有阴影、下方没有，会在手势条那行形成明显的亮度台阶；让阴影自然淡出即可。
8. **系统 scrim**：`enableEdgeToEdge()` 默认的 `auto` 导航栏样式会打开对比强制；关掉 `window.isNavigationBarContrastEnforced` 和 `window.isStatusBarContrastEnforced`，否则系统会在手势条/状态栏区域画一层近似页底色的 scrim。
9. **内容层用逐元素 inset**：根布局不要统一加底栏 padding，也不要在导航栏区域盖渐变/页面色遮罩，否则内容永远画不到手势条下方。滚动容器的 `contentPadding` 用「底栏高 + 间距 + navBar inset」；Scaffold 外层用 `contentWindowInsets = safeDrawing.only(Top + Horizontal)`。
10. **系统栏保持可见**：不要为了透明去 hide 系统栏；小白条由系统绘制，App 只负责它后面画什么。
11. **底栏模糊按页录制**：每个 destination 用独立的 `LayerBackdrop`，底栏只采样当前 destination 的层。若在转场容器外层共享一个录制层，淡出的旧页会留在模糊里，表现为页面已切换、底栏还残留上一页。切页时把「底栏使用哪个 backdrop」延后一帧再换：新页在首帧才录制自己的层，立刻切换会采到空层，表现为点击瞬间模糊消失、透出后面的内容。
12. **冷启动等偏好加载**：不要在首帧用 `AppearancePreferences()` 默认值组合主界面；默认 `floatingNavBar=false` 会先画停靠底栏，等 DataStore 发出真实值后再变悬浮。把 appearance 的首次发射纳入启动 gate，再把加载后的值传给主界面。
13. **冷启动等模糊就绪**：内容层首帧录制、模糊管线首帧初始化都要时间；底栏在首帧就显示会先看到实色 surface，下一帧才变模糊。让底栏晚两帧再进入（并保留 fade-in），或用类似 readiness gate 再显示。
14. **毛玻璃录制源**：`layerBackdrop` 只记录自己的内容，若页面背景来自外层 Surface，录制层必须自己先铺一层不透明底色，否则模糊只会得到 tint。

## 6. 调参表

| 目标 | 调整 |
|---|---|
| 阴影更明显 | `maxAlpha` ↑ |
| 阴影更柔/更远 | `spread` ↑；若变成光晕则 `maxAlpha` ↓ |
| 阴影更贴底栏 | `spread` ↓ |
| 更像下方投影 | `offsetY` ↑ |
| 渐变有台阶 | `spread` ↑ 或 `layers` ↑（注意坑 1） |
| 不想看到胶囊轮廓 | `spread` ↑、`maxAlpha` ↓，夜间直接设 0 |
