# 🎵 MusicPlayer

一个功能完备的 Android 音乐播放器应用，采用酷狗风格设计，支持在线播放、本地音乐管理、歌词同步显示、深色模式等特性。

![Version](https://img.shields.io/badge/version-v2.0-blue.svg)
![Platform](https://img.shields.io/badge/platform-Android%2012+-green.svg)
![License](https://img.shields.io/badge/license-MIT-orange.svg)

## ✨ 核心功能

### 🎧 音乐播放
- **在线流媒体播放** - 通过后端 API 流式传输音频，支持 HTTP Range 断点续传
- **本地音乐播放** - 支持下载的音乐和用户导入的音乐（content:// URI）
- **完整播放控制** - 播放/暂停、上一首/下一首、进度条拖拽 Seek
- **循环模式** - 列表循环 / 单曲循环切换
- **前台服务** - PlaybackService 确保后台持续播放，支持音频焦点管理

### 📝 歌词系统
- **LRC 格式解析** - 标准 LRC 歌词解析为时间轴条目
- **实时同步** - 200ms 定时器 + 二分查找算法精确匹配当前歌词行
- **智能缓存** - 本地数据库存储 + 内存缓存（500 条/30 天过期）
- **LRCLIB 集成** - 支持按 ID 精确查找或按元数据模糊搜索网络歌词
- **交互式歌词** - 播放器内可点击歌词跳转到对应时间点

### 🎨 视觉体验
- **深色/浅色主题** - 完整的双主题系统 + 跟随系统自动切换
- **Palette 动态取色** - 从专辑封面提取主色调，动态设置界面颜色
- **毛玻璃效果** - 播放器背景 100px 高斯模糊（RenderEffect, Android 12+）
- **黑胶唱片动画** - 播放时恒速旋转，暂停时停止的拟物化效果
- **共享元素过渡** - 底部唱片到全屏播放器的平滑过渡动画
- **Lottie 动画** - 个人中心 AI 助手动画增强趣味性

### 👤 用户系统
- **JWT 认证** - 7 天有效期 Token 安全认证
- **完整资料管理** - 头像上传、昵称、签名、性别、生日等个人信息编辑
- **密码安全** - bcrypt 加密存储，支持修改密码和注销账号
- **登录状态持久化** - SharedPreferences 保存会话信息

### 📚 音乐库管理
- **发现页** - Hero Card 动态背景 + 推荐歌单 + 新歌速递
- **本地搜索** - 支持歌曲名/歌手/专辑的不区分大小写模糊搜索
- **收藏功能** - 收藏/取消收藏，独立页面展示收藏列表
- **最近播放** - 7 天滑动窗口自动清理过期记录
- **歌曲下载** - OkHttp 多线程下载到外部 Music 目录
- **本地导入** - SAF FilePicker 选取音频文件，自动提取元数据入库

## 🛠️ 技术栈

### 前端 (Android)
| 技术 | 版本 | 用途 |
|------|------|------|
| **Media3 (ExoPlayer)** | 1.5.1 | 音视频播放引擎、UI 控件、MediaSession 服务 |
| **Room Database** | 2.8.4 | 本地 SQLite ORM 框架 |
| **OkHttp** | 4.12.0 | HTTP 网络请求（含重试机制） |
| **Coil** | 2.7.0 | Kotlin 协程图片加载库 |
| **Gson** | 2.11.0 | JSON 序列化/反序列化 |
| **Palette** | 1.0.0 | 从图片提取主色调 |
| **Lottie** | 6.4.0 | Airbnb 矢量动画渲染 |
| **Material Components** | 1.12.0 | Material Design 3 UI 组件 |
| **opencc4j** | 1.14.0 | 简繁体中文转换 |

### 后端 (Node.js)
| 技术 | 版本 | 用途 |
|------|------|------|
| **Express** | ^4.18.2 | Web 应用框架 |
| **MySQL2** | ^3.6.0 | MySQL 驱动（连接池模式） |
| **bcryptjs** | latest | 密码哈希加密 |
| **jsonwebtoken** | latest | JWT 生成与验证 |
| **Nodemon** | ^3.0.1 | 开发热重载工具 |

### 数据库
- **远程数据库**: MySQL (`musicplayer` 数据库)
- **本地数据库**: Room SQLite (`music_db`, 版本 9)

## 📁 项目结构

```
MusicPlayer/
├── app/src/main/
│   ├── java/com/example/musicplayer/    # Java 源码 (39 个文件)
│   │   ├── MainActivity.java            # 主 Activity (1173 行)
│   │   ├── MainViewModel.java           # MVVM 状态中心 (372 行)
│   │   ├── PlaybackService.java         # 前台播放服务
│   │   │
│   │   ├── fragment/                    # UI 页面 (10 个 Fragment)
│   │   │   ├── DiscoveryFragment.java   # 发现页
│   │   │   ├── PlayerFragment.java      # 全屏播放器
│   │   │   ├── MineFragment.java        # 个人中心
│   │   │   ├── SettingsFragment.java    # 设置页
│   │   │   └── ...                      # 其他 Fragment
│   │   │
│   │   ├── database/                    # 数据层
│   │   │   ├── AppDatabase.java         # Room 数据库单例
│   │   │   ├── Song.java                # 歌曲 Entity
│   │   │   ├── SongDao.java             # 歌曲 DAO (17 个方法)
│   │   │   ├── RecentPlay.java          # 最近播放 Entity
│   │   │   └── RecentPlayDao.java       # 最近播放 DAO
│   │   │
│   │   ├── network/                     # 网络层
│   │   │   ├── HttpClient.java          # OkHttp 单例
│   │   │   ├── RetryInterceptor.java    # 重试拦截器 (3 次)
│   │   │   ├── LrcLibService.java       # LRCLIB 歌词 API
│   │   │   ├── DownloadManager.java     # 下载管理器
│   │   │   └── UpdateManager.java       # 应用更新检查
│   │   │
│   │   ├── user/                        # 用户系统
│   │   │   ├── UserManager.java         # 用户会话管理
│   │   │   ├── User.java                # 用户数据模型
│   │   │   ├── LoginActivity.java       # 登录页
│   │   │   └── RegisterActivity.java    # 注册页
│   │   │
│   │   ├── lyric/                       # 歌词系统
│   │   │   ├── LyricUtils.java          # LRC 解析器
│   │   │   ├── LyricAdapter.java        # 歌词适配器
│   │   │   └── LyricCacheManager.java   # 歌词缓存管理
│   │   │
│   │   └── utils/                       # 工具类
│   │       ├── ThemeManager.java        # 主题管理器
│   │       ├── ErrorHandler.java        # 统一错误处理
│   │       ├── Constants.java           # 全局常量
│   │       └── ...                      # 其他工具
│   │
│   └── res/                             # 资源文件
│       ├── layout/                      # 24 个布局文件
│       ├── drawable/                    # 28 个 Drawable 资源
│       ├── values/                      # 浅色主题资源
│       ├── values-night/                # 深色主题资源
│       ├── anim/                        # 8 个动画资源
│       └── raw/                         # Lottie 动画 JSON
│
├── web/                                 # 后端服务
│   ├── server.js                        # Express API 服务器
│   ├── package.json                     # Node.js 依赖
│   ├── sql/users.sql                    # 数据库建表脚本
│   └── index.html                       # 默认首页
│
├── database_indexes.sql                 # MySQL 性能优化索引
├── build.gradle.kts                     # 项目级构建配置
├── app/build.gradle.kts                # 应用级构建配置
└── gradle.properties                   # Gradle 属性配置
```

## 🚀 快速开始

### 环境要求

**Android 端:**
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 11+
- Android SDK (compileSdk: 35, minSdk: 31)
- Gradle 8.7+

**后端:**
- Node.js 16+
- MySQL 5.7+ 或 MySQL 8.0+

### 1️⃣ 克隆项目

```bash
git clone https://github.com/your-username/MusicPlayer.git
cd MusicPlayer
```

### 2️⃣ 后端部署

#### 安装依赖

```bash
cd web
npm install
```

#### 配置数据库

1. 创建 MySQL 数据库:

```sql
CREATE DATABASE musicplayer CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 导入表结构:

```bash
mysql -u your_username -p musicplayer < sql/users.sql
```

3. 创建 `.env` 文件 (参考 `DEPLOYMENT_GUIDE.md`):

```env
DB_HOST=localhost
DB_USER=your_username
DB_PASSWORD=your_password
DB_NAME=musicplayer
JWT_SECRET=your_jwt_secret_key
PORT=3000
```

#### 启动服务器

```bash
# 开发模式 (自动重启)
npm run dev

# 生产模式
npm start
```

服务器将在 `http://localhost:3000` 启动

### 3️⃣ Android 端配置

#### 配置签名密钥

在项目根目录创建 `keystore.properties` 文件:

```properties
KEYSTORE_FILE=path/to/your.keystore
KEYSTORE_PASSWORD=your_password
KEY_ALIAS=your_alias
KEY_PASSWORD=your_key_password
```

#### 同步 Gradle

在 Android Studio 中打开项目，等待 Gradle 同步完成。

#### 修改 API 地址 (可选)

如果后端不在本地运行，修改 [Constants.java](app/src/main/java/com/example/musicplayer/Constants.java):

```java
public static final String BASE_URL = "http://your-server-ip:3000";
```

### 4️⃣ 运行应用

1. 连接 Android 设备或启动模拟器
2. 点击 Android Studio 的 ▶️ Run 按钮
3. 应用将安装并自动启动

## 📡 API 接口文档

### 用户认证

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/api/user/register` | 否 | 用户注册 |
| POST | `/api/user/login` | 否 | 用户登录 (返回 JWT) |
| GET | `/api/user/profile` | 是 | 获取当前用户资料 |
| PUT | `/api/user/profile` | 是 | 更新用户资料 |
| POST | `/api/user/change-password` | 是 | 修改密码 |
| POST | `/api/user/delete` | 是 | 注销账号 |
| POST | `/api/user/upload-avatar` | 是 | 上传头像 (base64) |

### 音乐数据

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/api/songs` | 否 | 分页获取歌曲列表 |
| GET | `/api/songs/all` | 否 | 获取全部歌曲 |
| GET | `/api/song/detail?id=` | 否 | 单首歌曲详情 |
| GET | `/api/song/search?q=` | 否 | 搜索歌曲 |
| GET | `/api/song/play?id=` | 否 | 音频流播放 (支持 Range) |

### 其他

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/api/app/version` | 否 | 应用版本检查 |

**详细 API 文档请查看 [server.js](web/server.js) 源码注释**

## 🎨 主题系统

应用支持三种主题模式，可在 **设置 > 主题模式** 中切换：

| 模式 | 说明 |
|------|------|
| 🌞 浅色模式 | 白色背景 + 深色文字，适合日间使用 |
| 🌙 深色模式 | 近黑色背景 + 浅色文字，适合夜间/护眼 |
| 💻 跟随系统 | 自动跟随操作系统主题设置 (默认) |

**快捷键**: 在连接键盘时按 `Ctrl+T` 可快速切换主题

**设计亮点:**
- 播放器界面统一采用深色沉浸式设计 (`#121214`)，不受全局主题影响
- 所有 UI 元素在两种模式下均有良好的对比度和可读性
- 平滑的主题过渡动画提升用户体验

## 📸 界面预览

> *截图待补充*

### 主要界面
- 登录/注册页面 - 渐变背景 + 圆角卡片设计
- 发现页 - Hero Card + 推荐歌单 + 新歌速递
- 全屏播放器 - 黑胶唱片 + 模糊背景 + 歌词同步
- 个人中心 - 用户资料 + 功能网格 + AI 助手
- 设置页 - iOS 风格设置列表

## 🔧 架构设计

### MVVM + 单 Activity 多 Fragment

```
┌─────────────────────────────────────┐
│              MainActivity            │
│         (唯一 Activity 入口)          │
│                                     │
│  ┌───────────┬──────────┬─────────┐ │
│  │Discovery  │  Player  │  Mine   │ │
│  │Fragment   │Fragment  │Fragment │ │
│  └───────────┴──────────┴─────────┘ │
│                                     │
│  ┌─────────────────────────────┐   │
│  │      MainViewModel          │   │
│  │   (LiveData 状态管理中心)     │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

### 数据流架构

```
[用户操作]
      ↓
[MainActivity] → [MainViewModel] (LiveData 状态变更)
      ↓                    ↓
      ├→ Room 本地数据库   → OkHttp 网络请求
      │  (SongDao/          (HttpClient +
      │   RecentPlayDao)     RetryInterceptor)
      ↓                    ↓
[Fragment 观察者]    [后端 server.js]
(UI 自动刷新)       (MySQL 数据库)
      ↓
[PlaybackService]
(ExoPlayer 实际播放)
      ↓
[MediaController 回调]
(歌词同步/进度更新/切歌事件)
```

## 💡 核心技术亮点

### 1. 网络请求优化
- **OkHttp 单例模式** - 双重检查锁定，连接复用
- **三级重试机制**:
  - OkHttp 内置连接级重试
  - 自定义 RetryInterceptor (3 次 + 线性退避)
  - 业务层异常捕获重试
- **超时配置**: 连接 15s / 读写 30s

### 2. 歌词同步算法
```java
// 200ms 定时器 + 二分查找 O(log n)
// 实现 < 100ms 的歌词定位精度
private void updateLyricPosition() {
    int position = binarySearch(lyricEntries, currentPosition);
    if (position != currentPositionIndex) {
        lyricAdapter.setCurrentPosition(position);
        recyclerView.smoothScrollToPosition(position);
    }
}
```

### 3. 内存泄漏防护
- WeakReference Handler 避免 Activity 泄漏
- Fragment 销毁时取消图片请求/移除监听器/停止动画
- try-with-resources 确保网络响应释放
- 统一 ToastHelper 防止 Window 泄漏

### 4. 数据同步策略
- 远端 songs 表作为"权威数据源"
- 本地 Room 作为离线缓存 + 本地状态扩展
- `isLocal`、`isFavorite`、`lyrics` 字段本地独有，不被覆盖
- 批量同步使用 IGNORE 策略保护本地修改

### 5. 性能优化
- MySQL 连接池 (最大 10 连接)
- 组合索引优化搜索性能
- Room LiveData 响应式查询
- Coil 图片加载 + crossfade 过渡
- Palette 异步提取主色调

## 📊 数据库设计

### users 表 (远程 MySQL)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INT | PK, AUTO_INCREMENT | 主键 |
| username | VARCHAR(50) | NOT NULL, UNIQUE | 用户名 |
| email | VARCHAR(50) | NOT NULL, UNIQUE | 邮箱 |
| password | VARCHAR(255) | NOT NULL | bcrypt 加密密码 |
| nickname | VARCHAR(50) | DEFAULT '' | 昵称 |
| avatar | VARCHAR(255) | DEFAULT '/resource/img/default_avatar.jpg' | 头像路径 |
| phone | VARCHAR(20) | DEFAULT '' | 手机号 |
| gender | TINYINT | DEFAULT 0 | 性别 (0未知/1男/2女) |
| birthday | DATE | DEFAULT NULL | 生日 |
| status | TINYINT | DEFAULT 1 | 状态 (0禁用/1正常) |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| last_login_at | DATETIME | DEFAULT NULL | 最后登录时间 |

**索引**: `idx_username`, `idx_email`, `idx_status`

### songs 表 (远程 MySQL)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT | PK |
| title | VARCHAR | 歌曲标题 |
| artist | VARCHAR | 作曲家/艺术家 |
| singer | VARCHAR | 歌手 |
| photo | VARCHAR | 封面图路径 |
| url | VARCHAR | 音频文件路径或 URL |
| duration | INT | 时长 (秒) |
| album | VARCHAR | 专辑名 |
| lrc_id | BIGINT | LRCLIB 歌词 ID |
| created_at | DATETIME | 创建时间 |

**索引**: `idx_songs_title`, `idx_songs_singer`, `idx_songs_album`, `idx_songs_search`(组合)

### 本地 Room 数据库 (music_db)

**Song 实体:**
- id, title, artist, singer, path, coverUrl, album, duration
- isLocal (是否本地), isFavorite (是否收藏)
- lrcId (歌词 ID), lyrics (歌词内容)

**RecentPlay 实体:**
- id, songId, timestamp (播放时间戳)

**特点:**
- 版本 9，启用破坏性迁移
- 最近播放 7 天滑动窗口自动清理
- 支持本地模糊搜索 (title/singer/album)

## 🧪 测试

当前测试覆盖率较低，建议补充以下核心模块的单元测试:

**已有测试:**
- ✅ ThemeManagerTest - 主题管理器基本功能

**待补充测试:**
- ⬜ MainViewModel - 数据同步逻辑
- ⬜ SongDao / RecentPlayDao - 数据库操作
- ⬜ LyricUtils - LRC 解析算法
- ⬜ UserManager - 用户认证流程
- ⬜ DownloadManager - 下载逻辑
- ⬜ HttpClient / RetryInterceptor - 网络重试机制

运行现有测试:

```bash
./gradlew testDebugUnitTest
```

## 📝 开发规范

### 代码风格
- Java 11 语法特性
- MVVM 架构模式 (LiveData + ViewModel)
- 单 Activity + 多 Fragment 导航
- 命名遵循 Android 官方规范

### 提交规范 (建议)

```
feat: 新功能
fix: Bug 修复
docs: 文档更新
style: 代码格式调整
refactor: 重构
perf: 性能优化
test: 测试相关
chore: 构建/工具链
```

### 分支策略 (建议)

```
main          - 生产分支
develop       - 开发分支
feature/*     - 功能分支
bugfix/*      - 修复分支
release/*     - 发布分支
```

## ⚠️ 已知问题 & 待优化

### 当前版本限制
- ⚠️ JWT Secret 硬编码，生产环境需改用环境变量
- ⚠️ Room 使用破坏性迁移，版本升级会丢失本地数据
- ⚠️ 缺少 HTTPS/TLS 配置
- ⚠️ 后端无请求频率限制 (Rate Limiting)
- ⚠️ ProGuard 未配置有效混淆规则
- ⚠️ 测试覆盖率极低 (< 5%)

### 建议优化项
- 🔧 实现 Room Migration 替代破坏性迁移
- 🔧 添加 HTTPS 支持
- 🔧 实现请求限流中间件
- 🔧 补充核心模块单元测试
- 🔧 配置 ProGuard 混淆规则
- 🔧 添加 CI/CD 自动化构建
- 🔧 集成 Crashlytics 错误监控
- 🔧 实现离线模式完整支持

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 👥 贡献者

- **开发者**: xqf
- **联系方式**: email@584399.xyz

## 🙏 致谢

- [ExoPlayer/Media3](https://github.com/google/ExoPlayer) - 强大的音视频播放引擎
- [Room](https://developer.android.com/training/data-storage/room) - Android 官方 ORM 框架
- [OkHttp](https://github.com/square/okHttp) - 高效的 HTTP 客户端
- [Coil](https://github.com/coil-kt/coil) - Kotlin 图片加载库
- [Lottie](https://github.com/airbnb/lottie-android) - Airbnb 动画库
- [Material Design 3](https://m3.material.io/) - Google 最新设计语言
- [LRCLIB](https://lrclib.net/) - 开放歌词数据库

---

<div align="center">

**⭐ 如果这个项目对你有帮助，请给一个 Star！⭐**

Made with ❤️ by [xqf](mailto:email@584399.xyz)

</div>
