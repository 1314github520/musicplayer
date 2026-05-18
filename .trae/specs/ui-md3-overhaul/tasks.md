# Tasks

- [ ] Task 1: 添加 Kotlin 和 Jetpack Compose 依赖
  - [ ] SubTask 1.1: 在 build.gradle.kts 中添加 Kotlin 插件和 Compose BOM 依赖
  - [ ] SubTask 1.2: 在 build.gradle.kts 中添加 material3-window-size-class 依赖
  - [ ] SubTask 1.3: 在 build.gradle.kts 中添加 Navigation Component 依赖
  - [ ] SubTask 1.4: 配置 Compose 编译选项（composeOptions, buildFeatures.compose）

- [ ] Task 2: 建立 MD3 颜色系统和主题
  - [ ] SubTask 2.1: 使用品牌色 #006060 作为 seed color，通过 Material Theme Builder 生成完整的 MD3 颜色角色
  - [ ] SubTask 2.2: 更新 values/colors.xml — 定义所有语义化颜色角色（primary, onPrimary, primaryContainer, surface, onSurface 等）
  - [ ] SubTask 2.3: 更新 values-night/colors.xml — 定义深色模式的语义化颜色角色
  - [ ] SubTask 2.4: 更新 values/themes.xml — 完整映射 MD3 颜色角色到主题属性
  - [ ] SubTask 2.5: 更新 values-night/themes.xml — 完整映射深色模式 MD3 颜色角色
  - [ ] SubTask 2.6: 创建 Compose Theme 文件（Theme.kt, Color.kt, Type.kt）支持动态取色

- [ ] Task 3: 建立 MD3 排版系统
  - [ ] SubTask 3.1: 在 themes.xml 中定义完整的 MD3 TextAppearance 样式（headlineLarge → labelSmall）
  - [ ] SubTask 3.2: 在 Compose Type.kt 中定义对应的 Typography Scale

- [ ] Task 4: 定义统一的 MD3 组件样式
  - [ ] SubTask 4.1: 定义 Button 样式（FilledTonalButton, FilledButton, TextButton, OutlinedButton）
  - [ ] SubTask 4.2: 定义 Card 样式（FilledCard, ElevatedCard, OutlinedCard）
  - [ ] SubTask 4.3: 定义 TextInputLayout 样式（OutlinedBox with MD3 colors）
  - [ ] SubTask 4.4: 定义 Dialog 样式（MaterialAlertDialog with MD3 colors）
  - [ ] SubTask 4.5: 定义 Chip 样式（FilterChip, AssistChip, InputChip）
  - [ ] SubTask 4.6: 定义 TopAppBar 样式
  - [ ] SubTask 4.7: 定义 SeekBar/Slider 样式

- [ ] Task 5: 重构底部导航为 Material3 NavigationBar
  - [ ] SubTask 5.1: 更新 activity_main.xml — 替换自定义 ConstraintLayout 底部导航为 NavigationBar 组件
  - [ ] SubTask 5.2: 更新 MainActivity.java — 适配 NavigationBar 的选中状态和点击事件
  - [ ] SubTask 5.3: 更新导航项图标为 Material Symbols（filled + outlined 变体）
  - [ ] SubTask 5.4: 保留中间播放器导航项的圆形专辑封面设计

- [ ] Task 6: 完善 activity_main.xml 主布局
  - [ ] SubTask 6.1: 使用 CoordinatorLayout 作为根布局，支持 TopAppBar 滚动行为
  - [ ] SubTask 6.2: 正确处理 Edge-to-Edge WindowInsets
  - [ ] SubTask 6.3: 优化迷你播放器/歌词切换器的位置和样式

- [ ] Task 7: 完善 fragment_discovery.xml 发现页布局
  - [ ] SubTask 7.1: 替换硬编码颜色为语义化颜色角色
  - [ ] SubTask 7.2: 使用 MD3 TopAppBar 替换自定义 topBar
  - [ ] 7.3: 优化 Hero Card 使用 ElevatedCard 样式
  - [ ] 7.4: 优化新歌速递卡片使用 FilledCard 样式
  - [ ] 7.5: 优化搜索栏使用 SearchBar 组件样式
  - [ ] 7.6: 优化推荐歌曲列表项使用 MD3 ListItem 样式

- [ ] Task 8: 完善 fragment_player.xml 播放器页布局
  - [ ] SubTask 8.1: 替换硬编码播放器颜色为 player 颜色角色
  - [ ] SubTask 8.2: 使用 MD3 Slider 替换 SeekBar
  - [ ] SubTask 8.3: 优化播放控制按钮使用 FilledTonalIconButton 样式
  - [ ] SubTask 8.4: 优化歌词列表项使用 MD3 排版样式
  - [ ] SubTask 8.5: 优化 TopAppBar 使用 MD3 样式

- [ ] Task 9: 完善 fragment_mine.xml 我的页面布局
  - [ ] SubTask 9.1: 替换硬编码颜色为语义化颜色角色
  - [ ] SubTask 9.2: 优化用户资料卡片使用 ElevatedCard 样式
  - [ ] SubTask 9.3: 优化功能网格卡片使用 FilledCard 样式
  - [ ] SubTask 9.4: 优化设置入口使用 MD3 ListItem 样式
  - [ ] SubTask 9.5: 优化 VIP 标签使用 MD3 Badge 样式

- [ ] Task 10: 完善子页面布局（设置、编辑资料、本地音乐、最近播放、导入音乐）
  - [ ] SubTask 10.1: fragment_settings.xml — 使用 MD3 TopAppBar + ListItem 样式
  - [ ] SubTask 10.2: fragment_edit_profile.xml — 使用 MD3 TopAppBar + ListItem 样式
  - [ ] SubTask 10.3: fragment_local_music.xml — 使用 MD3 TopAppBar + ListItem 样式
  - [ ] SubTask 10.4: fragment_recent_play.xml — 使用 MD3 TopAppBar + FilledTonalButton 样式
  - [ ] SubTask 10.5: fragment_imported_music.xml — 使用 MD3 TopAppBar + ListItem 样式

- [ ] Task 11: 完善登录/注册页面布局
  - [ ] SubTask 11.1: activity_login.xml — 替换硬编码颜色，使用 MD3 TextInputLayout + FilledButton 样式
  - [ ] SubTask 11.2: activity_register.xml — 替换硬编码颜色，使用 MD3 TextInputLayout + FilledButton 样式

- [ ] Task 12: 完善搜索页面布局
  - [ ] SubTask 12.1: activity_search.xml — 使用 MD3 SearchBar + Chip 样式

- [ ] Task 13: 完善对话框布局
  - [ ] SubTask 13.1: dialog_confirm.xml — 使用 MD3 MaterialAlertDialog 样式
  - [ ] SubTask 13.2: dialog_change_password.xml — 使用 MD3 MaterialAlertDialog + TextInputLayout 样式
  - [ ] SubTask 13.3: dialog_about.xml — 使用 MD3 MaterialAlertDialog 样式
  - [ ] SubTask 13.4: dialog_update.xml — 使用 MD3 MaterialAlertDialog 样式
  - [ ] SubTask 13.5: dialog_download_progress.xml — 使用 MD3 LinearProgressIndicator 样式

- [ ] Task 14: 完善列表项布局
  - [ ] SubTask 14.1: item_song.xml — 使用 MD3 ListItem 样式
  - [ ] SubTask 14.2: item_song_detailed.xml — 使用 MD3 ListItem 样式
  - [ ] SubTask 14.3: item_lyric.xml — 使用 MD3 排版样式
  - [ ] SubTask 14.4: item_search_result.xml — 使用 MD3 ListItem 样式
  - [ ] SubTask 14.5: item_playlist.xml — 使用 MD3 ListItem 样式

- [ ] Task 15: 更新 Java 代码适配新布局
  - [ ] SubTask 15.1: 更新 MainActivity.java — 适配 NavigationBar 和新颜色系统
  - [ ] SubTask 15.2: 更新 DiscoveryFragment.java — 适配新布局和颜色
  - [ ] SubTask 15.3: 更新 PlayerFragment.java — 适配 Slider 和新颜色
  - [ ] SubTask 15.4: 更新 MineFragment.java — 适配新布局
  - [ ] SubTask 15.5: 更新 SettingsFragment.java — 适配新布局
  - [ ] SubTask 15.6: 更新其他 Fragment/Activity — 适配新布局

- [ ] Task 16: 实现响应式布局适配
  - [ ] SubTask 16.1: 在 MainActivity 中检测 WindowSizeClass
  - [ ] SubTask 16.2: Compact 布局 — NavigationBar + 单列内容
  - [ ] SubTask 16.3: Medium/Expanded 布局 — NavigationRail + 多列内容（可选）

- [ ] Task 17: 创建 Compose 组件库
  - [ ] SubTask 17.1: 创建 Compose Theme（Theme.kt, Color.kt, Type.kt）
  - [ ] SubTask 17.2: 创建 Compose 版 MiniPlayer 组件
  - [ ] SubTask 17.3: 创建 Compose 版 SongListItem 组件
  - [ ] SubTask 17.4: 创建 Compose 版 NowPlayingCard 组件

- [ ] Task 18: 更新 Drawable 资源
  - [ ] SubTask 18.1: 更新 button_bg.xml 使用语义化颜色
  - [ ] SubTask 18.2: 更新 button_login_bg.xml 使用语义化颜色
  - [ ] SubTask 18.3: 更新 play_btn_bg.xml 使用语义化颜色
  - [ ] SubTask 18.4: 更新 btn_interactive_bg.xml 使用语义化颜色
  - [ ] SubTask 18.5: 更新 progress_bar_drawable.xml 使用语义化颜色
  - [ ] SubTask 18.6: 更新其他 drawable 使用语义化颜色

# Task Dependencies

- [Task 2] depends on [Task 1] (Compose Theme 需要 Compose 依赖)
- [Task 3] depends on [Task 1] (Compose Typography 需要 Compose 依赖)
- [Task 4] depends on [Task 2] (组件样式依赖颜色系统)
- [Task 5] depends on [Task 2, Task 4] (NavigationBar 依赖颜色和组件样式)
- [Task 7-14] depend on [Task 2, Task 3, Task 4] (页面布局依赖颜色、排版、组件样式)
- [Task 15] depends on [Task 5-14] (Java 代码适配依赖布局变更)
- [Task 16] depends on [Task 1, Task 5] (响应式布局依赖 WindowSizeClass 和导航)
- [Task 17] depends on [Task 1, Task 2, Task 3] (Compose 组件依赖 Compose 依赖和主题)
- [Task 18] depends on [Task 2] (Drawable 更新依赖颜色系统)

# Parallelizable Work

- Task 2 + Task 3 (颜色系统和排版系统可并行)
- Task 7-14 (各页面布局可并行，但需先完成 Task 2-4)
- Task 17 (Compose 组件可与 XML 布局优化并行)
