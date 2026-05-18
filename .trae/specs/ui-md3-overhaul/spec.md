# UI Material Design 3 全面完善 Spec

## Why

当前应用虽然使用了 `Theme.Material3.DayNight.NoActionBar` 作为基础主题，但存在大量硬编码颜色值、不统一的组件样式、缺失的 MD3 语义化颜色角色、非标准的导航组件、缺乏响应式布局适配等问题。需要全面统一设计语言，遵循 Material Design 3 规范，引入 Jetpack Compose 构建现代化 UI，并实现响应式布局以适配不同屏幕尺寸。

## What Changes

- **统一 MD3 颜色系统**：使用语义化颜色角色（primary, onPrimary, surface, onSurface 等）替换所有硬编码颜色，支持动态取色（Android 12+）和静态品牌色回退
- **统一排版系统**：定义完整的 MD3 Typography Scale（displayLarge → labelSmall），替换所有硬编码 textSize/textStyle
- **统一组件样式**：为 Button、Card、TextInput、Chip、Dialog、NavigationBar 等组件定义统一的 MD3 Widget Style
- **引入 Jetpack Compose**：添加 Kotlin + Compose 依赖，通过 ComposeView 互操作逐步迁移关键 UI 组件
- **重构底部导航**：使用 Material3 `NavigationBar` 组件替换自定义 ConstraintLayout 导航
- **实现响应式布局**：引入 `material3-window-size-class`，根据 WindowSizeClass 切换 Compact/Medium/Expanded 布局
- **优化页面导航**：使用 Navigation Component 替换手动 Fragment 事务管理
- **完善所有页面布局**：主界面、发现页、播放器页、我的页面、设置页、搜索页、登录/注册页等
- **Edge-to-Edge 适配**：正确处理 WindowInsets，确保内容不被系统栏遮挡
- **深色/浅色主题完善**：确保所有颜色角色在两种模式下都有正确的映射

## Impact

- Affected code: 所有 XML 布局文件（21个）、所有 Fragment/Activity Java 文件、themes.xml、colors.xml、build.gradle.kts
- 新增文件：Compose Theme 文件、Compose 组件文件、Navigation Graph、WindowSizeClass 适配
- **BREAKING**：底部导航结构从自定义 ConstraintLayout 变更为 Material3 NavigationBar
- **BREAKING**：颜色系统从硬编码值变更为语义化颜色角色

## ADDED Requirements

### Requirement: MD3 语义化颜色系统

系统 SHALL 提供完整的 Material Design 3 颜色角色定义，包括 primary/secondary/tertiary 及其 container/on 变体、surface 系列角色、error 系列角色、outline 系列角色。

#### Scenario: 浅色模式颜色正确映射
- **WHEN** 应用运行在浅色模式
- **THEN** 所有 UI 元素使用浅色主题的语义化颜色角色，背景使用 surface，文本使用 onSurface，强调色使用 primary

#### Scenario: 深色模式颜色正确映射
- **WHEN** 应用运行在深色模式
- **THEN** 所有 UI 元素自动切换到深色主题的语义化颜色角色，无需手动判断

#### Scenario: 动态取色（Android 12+）
- **WHEN** 设备运行 Android 12+ 且支持动态取色
- **THEN** 应用颜色从用户壁纸动态生成，提供个性化体验

### Requirement: MD3 排版系统

系统 SHALL 定义完整的 Material Design 3 Typography Scale，所有文本样式通过 `MaterialTheme.typography` 或 XML style 引用，不使用硬编码 textSize。

#### Scenario: 文本样式统一
- **WHEN** 开发者设置文本样式
- **THEN** 使用 `textAppearanceHeadlineMedium`、`textAppearanceBodyLarge` 等 MD3 标准样式，而非硬编码 sp 值

### Requirement: Material3 NavigationBar 底部导航

系统 SHALL 使用 Material3 `NavigationBar` 组件作为底部导航，包含"发现"、"播放器"、"我的"三个目的地，选中状态使用 filled icon + `secondaryContainer` 指示器。

#### Scenario: 导航切换
- **WHEN** 用户点击底部导航项
- **THEN** 切换到对应 Fragment，导航项显示选中状态（filled icon + label 高亮）

#### Scenario: 播放器导航特殊处理
- **WHEN** 用户点击中间播放器导航项
- **THEN** 以共享元素动画过渡到全屏播放器页面，隐藏底部导航

### Requirement: Jetpack Compose 集成

系统 SHALL 支持 Jetpack Compose 与现有 View 系统的互操作，通过 ComposeView 在 Fragment/Activity 中嵌入 Compose UI，逐步迁移关键组件。

#### Scenario: Compose 组件在 Fragment 中使用
- **WHEN** Fragment 需要使用 Compose 组件
- **THEN** 通过 ComposeView 嵌入 Compose 内容，与现有 View 系统无缝协作

### Requirement: 响应式布局适配

系统 SHALL 根据 WindowSizeClass（Compact < 600dp, Medium 600-839dp, Expanded ≥ 840dp）提供不同的布局方案。

#### Scenario: 手机竖屏（Compact）
- **WHEN** 屏幕宽度 < 600dp
- **THEN** 使用 NavigationBar 底部导航 + 单列内容布局

#### Scenario: 平板/折叠屏（Medium/Expanded）
- **WHEN** 屏幕宽度 ≥ 600dp
- **THEN** 使用 NavigationRail 侧边导航 + 多列/列表详情布局

### Requirement: 统一组件样式

系统 SHALL 为所有常用组件定义统一的 MD3 Widget Style：Button（FilledTonalButton）、Card（FilledCard）、TextInputLayout（OutlinedBox）、Dialog（MaterialAlertDialog）、Chip 等。

#### Scenario: 按钮样式统一
- **WHEN** 页面需要主操作按钮
- **THEN** 使用 FilledTonalButton 样式，颜色为 primaryContainer，文字为 onPrimaryContainer

#### Scenario: 卡片样式统一
- **WHEN** 页面需要内容卡片
- **THEN** 使用 MaterialCardView + Filled Card 样式，圆角 16dp，背景 surfaceContainerHighest

### Requirement: Edge-to-Edge 与 WindowInsets 正确处理

系统 SHALL 正确实现 Edge-to-Edge 显示，内容绘制在系统栏后面，通过 WindowInsets padding 确保内容不被遮挡。

#### Scenario: 状态栏区域
- **WHEN** 内容滚动到顶部
- **THEN** 内容可以滚动到状态栏后面，但固定内容（如 TopAppBar）有正确的顶部 padding

#### Scenario: 导航栏区域
- **WHEN** 内容滚动到底部
- **THEN** 底部内容有 NavigationBar 高度的 padding，不被系统导航栏遮挡

### Requirement: 页面导航流畅性

系统 SHALL 使用 Navigation Component 管理页面导航，支持共享元素过渡动画、预测性返回手势。

#### Scenario: 发现页 → 播放器页过渡
- **WHEN** 用户从发现页点击播放按钮或导航项进入播放器
- **THEN** 专辑封面以共享元素动画过渡，底部导航平滑隐藏

#### Scenario: 返回手势
- **WHEN** 用户在子页面执行系统返回手势
- **THEN** 页面以预测性动画返回上一页

## MODIFIED Requirements

### Requirement: 主题系统

原有 ThemeManager 仅支持浅色/深色/跟随系统切换。修改后 SHALL 同时支持 MD3 动态取色（Android 12+）和品牌色回退，颜色角色通过语义化名称引用。

### Requirement: 播放器页面

原有播放器页面使用硬编码的深色背景。修改后 SHALL 使用从专辑封面提取的动态颜色作为背景，所有文本和控件颜色使用 player 颜色角色（playerOnBackground, playerOnSurfaceVariant 等），确保在任意背景色下都有足够对比度。

## REMOVED Requirements

### Requirement: 硬编码颜色值
**Reason**: 所有硬编码颜色值（如 `#006060`, `#26D1D1`, `#88FFFFFF` 等）应替换为语义化颜色角色
**Migration**: 在 colors.xml 中保留语义化颜色名称定义，在 themes.xml 中通过 MD3 颜色角色映射

### Requirement: 自定义底部导航布局
**Reason**: 自定义 ConstraintLayout 底部导航不符合 MD3 规范
**Migration**: 替换为 Material3 NavigationBar 组件
