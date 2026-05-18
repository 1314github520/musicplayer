# UI MD3 全面完善 Checklist

## 颜色系统
- [ ] values/colors.xml 包含完整的 MD3 语义化颜色角色（primary, onPrimary, primaryContainer, onPrimaryContainer, secondary, tertiary, surface, onSurface, surfaceVariant, onSurfaceVariant, outline, outlineVariant, error, errorContainer 等）
- [ ] values-night/colors.xml 包含深色模式的完整 MD3 语义化颜色角色
- [ ] 所有 XML 布局文件中无硬编码颜色值（#RRGGBB 格式），全部使用 @color/ 语义化名称或 ?attr/ 主题属性
- [ ] 所有 Drawable XML 中无硬编码颜色值，全部使用语义化颜色
- [ ] Android 12+ 设备上动态取色正常工作
- [ ] 浅色模式和深色模式下所有页面颜色正确显示

## 排版系统
- [ ] themes.xml 中定义了完整的 MD3 TextAppearance 样式（displayLarge → labelSmall 共 15 个）
- [ ] 所有 XML 布局中的 textSize/textStyle 通过 style 或 textAppearance 引用，无硬编码 sp 值
- [ ] Compose Typography.kt 定义了完整的 MD3 Typography Scale

## 组件样式
- [ ] 定义了 FilledTonalButton 样式，颜色使用 primaryContainer/onPrimaryContainer
- [ ] 定义了 FilledButton 样式，颜色使用 primary/onPrimary
- [ ] 定义了 TextButton 样式，颜色使用 primary
- [ ] 定义了 FilledCard/ElevatedCard/OutlinedCard 样式
- [ ] 定义了 TextInputLayout OutlinedBox 样式，颜色使用 MD3 语义化角色
- [ ] 定义了 MaterialAlertDialog 样式
- [ ] 定义了 FilterChip/AssistChip 样式
- [ ] 定义了 TopAppBar 样式
- [ ] 定义了 Slider/ProgressBar 样式

## 底部导航
- [ ] 使用 Material3 NavigationBar 组件替换自定义 ConstraintLayout 导航
- [ ] NavigationBar 包含 3 个 NavigationBarItem（发现、播放器、我的）
- [ ] 选中项使用 filled icon，未选中使用 outlined icon
- [ ] 选中项 label 使用 primary 颜色，未选中使用 onSurfaceVariant
- [ ] 中间播放器项保留圆形专辑封面设计
- [ ] 导航切换时 Fragment 正确替换

## 页面布局 — 主界面
- [ ] activity_main.xml 使用 CoordinatorLayout 或合适的根布局
- [ ] Edge-to-Edge 正确实现，内容绘制在系统栏后面
- [ ] WindowInsets 正确处理，内容不被系统栏遮挡
- [ ] 迷你歌词切换器位置正确，在 NavigationBar 上方

## 页面布局 — 发现页
- [ ] 使用 MD3 TopAppBar 或 LargeTopAppBar
- [ ] Hero Card 使用 ElevatedCard 样式
- [ ] 搜索栏使用 SearchBar 组件样式
- [ ] 新歌速递卡片使用 FilledCard 样式
- [ ] 推荐歌曲列表项使用 MD3 ListItem 样式
- [ ] 所有颜色使用语义化颜色角色

## 页面布局 — 播放器页
- [ ] 播放器背景使用动态提取色
- [ ] 使用 MD3 Slider 替换 SeekBar
- [ ] 播放控制按钮使用 FilledTonalIconButton 样式
- [ ] 歌词列表项使用 MD3 排版样式
- [ ] TopAppBar 使用 MD3 样式
- [ ] 所有播放器颜色使用 player 颜色角色

## 页面布局 — 我的页面
- [ ] 用户资料卡片使用 ElevatedCard 样式
- [ ] 功能网格卡片使用 FilledCard 样式
- [ ] 设置入口使用 MD3 ListItem 样式
- [ ] VIP 标签使用 MD3 Badge 样式
- [ ] 所有颜色使用语义化颜色角色

## 页面布局 — 子页面
- [ ] 设置页使用 MD3 TopAppBar + ListItem 样式
- [ ] 编辑资料页使用 MD3 TopAppBar + ListItem 样式
- [ ] 本地音乐页使用 MD3 TopAppBar + ListItem 样式
- [ ] 最近播放页使用 MD3 TopAppBar + FilledTonalButton 样式
- [ ] 导入音乐页使用 MD3 TopAppBar + ListItem 样式

## 页面布局 — 登录/注册
- [ ] 登录页使用 MD3 TextInputLayout + FilledButton 样式
- [ ] 注册页使用 MD3 TextInputLayout + FilledButton 样式
- [ ] 所有颜色使用语义化颜色角色

## 页面布局 — 搜索页
- [ ] 搜索页使用 MD3 SearchBar + Chip 样式

## 页面布局 — 对话框
- [ ] 确认对话框使用 MD3 MaterialAlertDialog 样式
- [ ] 修改密码对话框使用 MD3 MaterialAlertDialog + TextInputLayout 样式
- [ ] 关于对话框使用 MD3 MaterialAlertDialog 样式
- [ ] 更新对话框使用 MD3 MaterialAlertDialog 样式
- [ ] 下载进度对话框使用 MD3 LinearProgressIndicator 样式

## 列表项布局
- [ ] item_song.xml 使用 MD3 ListItem 样式
- [ ] item_song_detailed.xml 使用 MD3 ListItem 样式
- [ ] item_lyric.xml 使用 MD3 排版样式
- [ ] item_search_result.xml 使用 MD3 ListItem 样式

## Java 代码适配
- [ ] MainActivity.java 正确适配 NavigationBar
- [ ] MainActivity.java 使用语义化颜色（ContextCompat.getColor 替换为主题属性）
- [ ] DiscoveryFragment.java 适配新布局
- [ ] PlayerFragment.java 适配 Slider 和新颜色
- [ ] MineFragment.java 适配新布局
- [ ] SettingsFragment.java 适配新布局

## 响应式布局
- [ ] Compact 屏幕使用 NavigationBar + 单列内容
- [ ] Medium/Expanded 屏幕使用 NavigationRail 或自适应布局（可选）

## Compose 集成
- [ ] build.gradle.kts 包含 Kotlin + Compose 依赖
- [ ] Compose Theme 文件存在（Theme.kt, Color.kt, Type.kt）
- [ ] Compose Theme 支持动态取色
- [ ] 至少一个 Compose 组件通过 ComposeView 成功集成

## 构建验证
- [ ] 项目可以成功编译（./gradlew assembleDebug）
- [ ] 浅色模式下所有页面正常显示
- [ ] 深色模式下所有页面正常显示
- [ ] 导航切换流畅无闪烁
- [ ] 播放器页面共享元素过渡动画正常
