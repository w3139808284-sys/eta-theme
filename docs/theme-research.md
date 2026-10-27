# Operit 主题系统研究结论（借鉴设计）

## 许可证
- Operit：LGPL-3.0（允许链接、非商用/商用使用，修改需开源回馈）
- Eta：PolyForm Noncommercial 1.0.0（非商用）
- 结论：**不直接复制 Operit 代码**，借鉴其架构思想，在 Eta 内重新实现，避免 LGPL 传染与 Eta 非商用约束冲突。

## Operit 主题系统核心架构（可借鉴点）

### 1. 解耦的主题快照（最重要）
`ThemePreferenceValues` = 4 个类型化 Map（strings/booleans/ints/floats），
`ThemePreferenceSnapshot` 从快照按 key 取类型化值，通过 `compositionLocalOf` 暴露给渲染层。
- 优点：主题配置与业务逻辑完全解耦；多主题（角色卡/全局）天然支持 source/sourceId 前缀；
  持久化简单（Map 序列化）；官方升级时只需维护 key 集合，不影响业务代码。
- Eta 落点：Eta 已有 `AppearanceSettings`（强类型 data class + DataStore），我新增一层
  「主题快照」KV 包装，让它成为多主题档案的底层存储格式。

### 2. 颜色方案解析
`resolveThemeColorScheme(context, snapshot)`：
- 系统暗色判断 → dynamic color（API 31+）→ 自定义 primary/secondary 生成完整方案
- `getContrastingTextColor`（WCAG 亮度 → 黑/白）
- lighten/darken/blend 工具函数
- Eta 落点：Eta 用 Miuix `ThemeController`，我可复用其 paletteStyle，但加「自定义 primary/secondary
  覆盖 + 对比度文字色自动计算」。

### 3. 背景层 AppBackgroundLayer
- 图片（Coil）+ opacity + blur；视频（ExoPlayer）+ 遮罩
- 关键：**背景画在最底层，主题 surface 用不透明色覆盖内容**，不打穿分层
- Eta 落点：已有 WallpaperStore/背景图导入，改为「聊天页 + 设置页统一背景层」。

### 4. 玻璃拟态 LiquidGlass / WaterGlass
- liquidGlass：kyant backdrop（vibrancy+blur+lens），API 33+，**降级 = shadow+border+半透明 tint**
- waterGlass：fletchmckee liquid（frost+refraction+dispersion），API 33+，同样降级
- Eta 落点：不引入这两个依赖，**自行实现轻量玻璃**（Compose graphicsLayer 采样 + 降级为
  shadow+border+translucent），避免新增大型依赖与兼容风险。

### 5. 聊天气泡样式
BubbleStyleChatMessage 按 sender 分发到 User/Ai/Summary 三个 composable：
- 用户/AI 独立：背景色、文字色、玻璃材质（liquid/water 二选一+互斥）、圆角开关、内边距、
  头像（形状/圆角/URI）、名称显示
- Eta 落点：Eta 的 ChatMessageItem 已按 sender 分支（UserMessageBubble / AgentMessageBlock），
  我注入「气泡样式」CompositionLocal，分别驱动用户/AI 的气泡颜色、圆角、内边距、材质。

### 6. 字体与排版
`applyFontFamilyToTypography` + `resolveConfiguredFontFamily`（系统字体名/custom path/scale）
- Eta 落点：Eta 用 Miuix TextStyles（非 Material Typography），字体定制需改 Miuix 层，
  本期先做「全局字号缩放」（已有 interfaceScale），字体族后续。

## 已读关键文件
- data/model/SerializableColorScheme.kt（Material3 27 字段序列化）
- data/preferences/ThemePreferenceSnapshot.kt（KV 快照 + 默认值，~100 个 key）
- data/preferences/ThemeScopeMigrationPolicy.kt / ThemeTargetOperationCoordinator.kt（多目标主题迁移）
- ui/theme/Theme.kt（OperitTheme 总入口，658 行）
- ui/theme/ThemeColorSchemeResolver.kt（颜色方案解析）
- ui/theme/AppBackgroundLayer.kt / LiquidGlass.kt / WaterGlass.kt
- ui/features/chat/components/style/bubble/*.kt（气泡）
- ui/features/settings/screens/theme/*.kt（设置 UI，多 Tab）

## Eta 现有主题机制（已梳理）
- `AgentAppTheme`（ui/app/AgentAppTheme.kt）：MiuixTheme + ThemeController + AppearanceSettings
- `AppearanceSettings`（data/model/AppearanceSettings.kt）：themeMode/monet/palette/accent/pureBlack/
  blur/topBarBlurStyle/swipeDismiss/predictiveBack/interfaceScale/bubbleCornerScale/homeGreeting/wallpaper
- `SettingsDataStore`（data/datastore）：Preferences DataStore 强类型键
- `AppearanceSettingsScreen`（ui/AppearanceSettingsScreen.kt）：设置 UI
- 已实现：WallpaperStore/壁纸（聊天页局部渲染，上轮已修复分层穿透）
- 注意：Eta 是 Miuix 主题，非 Material3；Miuix Colors 有 53 个字段
