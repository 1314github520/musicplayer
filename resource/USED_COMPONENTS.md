# MusicPlayer 使用组件说明

## 文档说明

本文档整理了 MusicPlayer 项目中实际使用到的主要组件、框架和自定义业务模块，方便用于项目汇报、README 补充、答辩介绍或技术说明。

组件内容按以下维度划分：

- Android 基础组件
- Android 第三方库
- 后端组件
- 项目自定义业务组件
- 测试相关组件

## 一、Android 基础组件

### 1. Activity

- `MainActivity`
  - 项目的主入口 Activity
  - 负责页面容器管理、底部导航、迷你播放器、`MediaController` 初始化和全局播放联动

- `LoginActivity`
  - 登录页面
  - 负责用户登录表单提交与登录态进入主页面

- `RegisterActivity`
  - 注册页面
  - 负责新用户注册

- `SearchActivity`
  - 搜索页面
  - 负责输入关键词、显示搜索结果与历史搜索

### 2. Fragment

项目采用单 Activity + 多 Fragment 结构，主要页面由 Fragment 承载：

- `DiscoveryFragment`
  - 发现页
  - 展示推荐歌曲、新歌和功能入口

- `CategoryFragment`
  - 分类页
  - 按歌手、专辑等维度展示歌曲

- `PlayerFragment`
  - 全屏播放器页面
  - 显示封面、进度条、歌词、收藏与下载操作

- `MineFragment`
  - 个人中心
  - 展示用户资料、统计数据和功能入口

- `FavoriteMusicFragment`
  - 收藏音乐页面

- `LocalMusicFragment`
  - 本地音乐页面

- `RecentPlayFragment`
  - 最近播放页面

- `ImportedMusicFragment`
  - 导入音乐页面

- `SettingsFragment`
  - 设置页
  - 负责主题切换、检查更新、改密、退出登录等

- `EditProfileFragment`
  - 编辑个人资料页

### 3. ViewModel + LiveData

- `MainViewModel`
  - 项目的全局状态中心
  - 通过 `LiveData` / `MutableLiveData` 管理：
    - 当前歌曲
    - 播放状态
    - 歌词
    - 进度
    - 用户资料
    - 搜索结果
    - 数据同步状态

### 4. Service

- `PlaybackService`
  - 基于 `MediaSessionService`
  - 负责后台音乐播放
  - 管理 ExoPlayer、通知栏播放控制、音频焦点和耳机拔出暂停逻辑

### 5. Room 数据库组件

- `AppDatabase`
  - Room 数据库入口

- `SongDao`
  - 歌曲数据访问对象
  - 管理歌曲查询、收藏、本地音乐、下载音乐、歌词缓存合并等

- `RecentPlayDao`
  - 最近播放数据访问对象
  - 管理播放历史和 7 天窗口统计

- `Song`
  - 歌曲实体类

- `RecentPlay`
  - 最近播放实体类

## 二、Android 第三方库

以下组件来自 `app/build.gradle.kts` 中的依赖声明。

### 1. AndroidX 基础组件

- `androidx.appcompat`
  - 提供兼容性支持，保证应用在不同 Android 版本下表现一致

- `com.google.android.material`
  - Material Design 组件库
  - 用于按钮、输入框、对话框等 UI 元素

- `androidx.activity`
  - Activity 扩展支持

- `androidx.constraintlayout`
  - 复杂页面布局组件

### 2. 播放组件

- `androidx.media3:media3-exoplayer:1.5.1`
  - 实际音频播放引擎
  - 负责音频加载、播放、暂停、切歌、进度控制等

- `androidx.media3:media3-ui:1.5.1`
  - Media3 的 UI 支持组件

- `androidx.media3:media3-session:1.5.1`
  - 提供 `MediaSession`、`MediaController`、`MediaSessionService`
  - 支持后台播放和系统媒体控制

- `androidx.media:media:1.7.0`
  - 提供额外的媒体兼容支持

### 3. 数据库组件

- `androidx.room:room-runtime:2.8.4`
  - Room 核心运行库

- `androidx.room:room-compiler:2.8.4`
  - Room 注解处理器

- `androidx.room:room-ktx:2.8.4`
  - Room 的扩展支持库

### 4. 网络与数据解析组件

- `com.squareup.okhttp3:okhttp:4.12.0`
  - 网络请求库
  - 用于歌曲列表、用户接口、歌词接口、更新接口等 HTTP 请求

- `com.google.code.gson:gson:2.11.0`
  - JSON 序列化与反序列化
  - 用于解析服务端返回的数据

### 5. 图片与视觉组件

- `io.coil-kt:coil:2.7.0`
  - 图片加载库
  - 用于歌曲封面、头像等图片加载

- `androidx.palette:palette:1.0.0`
  - 从图片中提取主色调
  - 用于播放器背景和界面颜色联动

- `com.airbnb.android:lottie:6.4.0`
  - Lottie 动画组件
  - 用于页面中的动画展示

### 6. 文本处理组件

- `com.github.houbb:opencc4j:1.14.0`
  - 简繁体转换相关库
  - 用于歌词或文本兼容处理

## 三、后端组件

### 1. Node.js 基础组件

- `Express`
  - 后端 Web 框架
  - 用于构建 REST API

- `mysql2`
  - MySQL 数据库驱动
  - 通过连接池访问远程数据库

- `nodemon`
  - 开发时自动重启服务

### 2. 实际运行时使用的组件

虽然 `web/package.json` 当前只声明了 `express` 和 `mysql2`，但 `web/server.js` 实际还使用了以下组件：

- `bcryptjs`
  - 用于密码加密和校验

- `jsonwebtoken`
  - 用于 JWT Token 生成与鉴权

### 3. 后端中间件与能力组件

- `express.json()`
  - 处理 JSON 请求体

- `express.urlencoded()`
  - 处理表单请求

- 自定义 `corsHeaders`
  - 处理跨域响应头

- 静态资源映射
  - 使用 `express.static()` 暴露歌曲、图片和 APK 资源

## 四、项目自定义业务组件

这些组件不是第三方库，而是项目中自行封装的核心业务模块。

### 1. 网络层组件

- `HttpClient`
  - OkHttp 单例封装
  - 统一超时、连接池和客户端配置

- `RetryInterceptor`
  - 网络重试拦截器
  - 失败时自动重试，提升请求稳定性

- `ApiResponse`
  - 接口响应数据封装类

### 2. 歌词组件

- `LrcLibService`
  - 歌词接口服务
  - 对接 LRCLIB，支持按 ID 或元数据检索歌词

- `LyricUtils`
  - LRC 解析工具
  - 将歌词文本解析为时间轴结构

- `LyricAdapter`
  - 歌词列表适配器
  - 用于 RecyclerView 显示和高亮当前歌词

- `LyricCacheManager`
  - 歌词缓存管理器
  - 提供持久化歌词缓存能力

- `ChnConverter`
  - 文本转换辅助组件

### 3. 下载组件

- `DownloadManager`
  - 负责歌曲下载、写入本地、回调进度

- `DownloadNotificationManager`
  - 负责下载通知展示

### 4. 用户组件

- `UserManager`
  - 负责登录、注册、Token 保存、退出登录、资料更新

- `User`
  - 用户数据模型

### 5. 更新组件

- `UpdateManager`
  - 负责版本检测、更新日志处理、APK 下载和安装

### 6. 主题与工具组件

- `ThemeManager`
  - 管理浅色、深色、跟随系统主题

- `ToastHelper`
  - 统一 Toast 提示

- `ErrorHandler`
  - 统一错误处理

- `Logger`
  - 日志输出辅助

- `Constants`
  - 项目全局常量

## 五、测试相关组件

### 1. 单元测试

- `JUnit`
  - 基础单元测试框架

- `Robolectric`
  - Android 本地单元测试框架
  - 用于在 JVM 环境下运行部分 Android 测试

- `Mockito`
  - Mock 组件
  - 用于模拟依赖对象

### 2. Android 测试

- `Espresso`
  - Android UI / Instrumentation 测试组件

- `AndroidJUnitRunner`
  - Android 测试运行器

## 六、从架构角度看这些组件的作用

可以把这个项目理解成以下几层：

### 1. UI 层

- Activity
- Fragment
- RecyclerView Adapter
- Material 组件
- ConstraintLayout

### 2. 状态管理层

- ViewModel
- LiveData

### 3. 播放层

- Media3
- ExoPlayer
- MediaSessionService
- MediaController

### 4. 数据层

- Room
- DAO
- 实体类
- Gson

### 5. 网络层

- OkHttp
- RetryInterceptor
- Express API
- MySQL2

### 6. 增强体验层

- Coil
- Palette
- Lottie
- ThemeManager
- LyricCacheManager

## 七、适合项目介绍时的简短说法

如果你在答辩、面试或汇报里介绍这个项目，可以这样概括：

> 这个项目 Android 端主要使用了 MVVM、LiveData、Room、Media3/ExoPlayer、OkHttp、Gson、Coil、Palette 和 Lottie 等组件；后端使用 Node.js、Express、MySQL、JWT 和 bcrypt 实现歌曲接口、播放流、用户系统与版本更新能力。

## 八、总结

MusicPlayer 使用的组件并不只是基础 UI 或播放器库，而是覆盖了：

- 页面与交互组件
- 播放组件
- 数据缓存组件
- 网络通信组件
- 用户认证组件
- 下载与更新组件
- 测试组件

这些组件共同构成了一个完整的 Android 音乐播放器项目。
