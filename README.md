# 🎵 MusicPlayer

一个前后端分离的 Android 音乐播放器项目，包含 Android 客户端、Node.js 后端和 MySQL 数据存储。项目围绕在线听歌、本地管理、歌词同步、用户系统和版本更新实现了一套完整的音乐应用基础能力。

![Version](https://img.shields.io/badge/version-v2.0-blue.svg)
![Platform](https://img.shields.io/badge/platform-Android%2012+-green.svg)
![Stack](https://img.shields.io/badge/stack-Android%20%2B%20Node.js-orange.svg)

## 📚 目录

- [项目简介](#-项目简介)
- [项目亮点](#-项目亮点)
- [核心功能](#-核心功能)
- [技术栈](#-技术栈)
- [项目结构](#-项目结构)
- [快速开始](#-快速开始)
- [配置说明](#-配置说明)
- [API 接口文档](#-api-接口文档)
- [架构设计](#-架构设计)
- [典型使用流程](#-典型使用流程)
- [测试](#-测试)
- [已知问题与待优化](#-已知问题--待优化)
- [相关文档](#-相关文档)

## 📖 项目简介

MusicPlayer 的整体目标不是只做一个简单的播放器 Demo，而是做一套更接近真实产品形态的音乐应用：

- Android 客户端负责界面展示、播放控制、歌词展示、本地缓存、下载管理和用户交互。
- Node.js 后端负责歌曲数据、音频流服务、用户鉴权、资料管理和版本更新接口。
- Room 负责端侧缓存，MySQL 负责服务端持久化数据。

当前仓库中的主要版本信息如下：

- Android 版本：`versionCode 20` / `versionName v2.0`
- Android SDK：`minSdk 31`、`targetSdk 35`、`compileSdk 35`
- Android 语言级别：`Java 11`
- 后端默认端口：`3000`

## 🌟 项目亮点

- 完整业务闭环：从发现歌曲、搜索、播放、歌词、收藏、下载，到账号登录、资料维护、版本更新，链路完整。
- 在线与本地双栈能力：既支持服务端歌曲流式播放，也支持下载歌曲和用户手动导入本地音频统一管理。
- 后台持续播放：基于 Media3 `MediaSessionService + ExoPlayer`，支持通知栏控制、音频焦点处理、耳机拔出自动暂停。
- 歌词体验突出：支持 LRCLIB 歌词获取、LRC 解析、缓存复用，并结合播放进度进行实时同步显示。
- 启动体验友好：采用“本地缓存先展示，远程数据后台同步”的双层数据策略，减少白屏并保留收藏、本地路径等状态。
- 展示效果完整：全屏播放器包含黑胶旋转、背景模糊、动态取色、歌词滚动等视觉细节，适合作为课程设计、毕业设计或作品集项目展示。

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
- **资料管理** - 头像上传、昵称、性别、生日等个人信息编辑
- **密码安全** - bcrypt 加密存储，支持修改密码和注销账号
- **登录状态持久化** - SharedPreferences 保存会话信息

### 📚 音乐库管理
- **发现页** - Hero Card 动态背景 + 推荐歌单 + 新歌速递
- **分类浏览** - 支持按歌手、专辑等维度查看歌曲分类
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
| **bcryptjs** | 实际运行依赖 | 密码哈希加密 |
| **jsonwebtoken** | 实际运行依赖 | JWT 生成与验证 |
| **Nodemon** | ^3.0.1 | 开发热重载工具 |

### 数据库
- **远程数据库**: MySQL (`musicplayer` 数据库)
- **本地数据库**: Room SQLite (`music_db`, 版本 10)

> 注意：`web/server.js` 已实际使用 `bcryptjs` 和 `jsonwebtoken`，但这两个包当前尚未写入 `web/package.json`，首次启动前需要手动安装。

## 📁 项目结构

```text
MusicPlayer/
├─ app/
│  ├─ build.gradle.kts
│  └─ src/main/
│     ├─ AndroidManifest.xml
│     ├─ java/com/example/musicplayer/
│     │  ├─ app/                     # Application 初始化
│     │  ├─ core/                    # 常量、网络、歌词、主题等核心能力
│     │  ├─ data/                    # Room 数据库与数据模型
│     │  └─ feature/
│     │     ├─ auth/                 # 登录注册与用户管理
│     │     ├─ discovery/            # 发现页与分类浏览
│     │     ├─ library/              # 收藏、本地、最近播放
│     │     ├─ main/                 # MainActivity / MainViewModel
│     │     ├─ player/               # 播放器、下载、播放服务
│     │     ├─ profile/              # 个人中心与设置
│     │     ├─ search/               # 搜索页
│     │     └─ update/               # 版本更新
│     └─ res/
├─ web/
│  ├─ server.js
│  ├─ package.json
│  ├─ sql/
│  │  ├─ users.sql
│  │  └─ version.sql
│  ├─ index.html
│  └─ 404.html
├─ PROJECT_INTRODUCTION.md
├─ DEPLOYMENT_GUIDE.md
├─ CLAUDE.md
└─ database_indexes.sql
```

## 🚀 快速开始

### 环境要求

**Android 端:**
- Android Studio 较新版本
- 建议使用 JDK 17 运行 Gradle
- Android SDK (`compileSdk 35`, `minSdk 31`)
- 使用仓库自带 Gradle Wrapper

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

3. 如需版本更新能力，可继续导入:

```bash
mysql -u your_username -p musicplayer < sql/version.sql
```

4. 当前仓库中的后端配置不是 `.env` 驱动，而是直接写在 `web/server.js` 中，包括：

- 端口：`3000`
- JWT Secret
- MySQL 连接信息
- 静态资源目录：`/musicplayer`

如果你要本地部署，请先修改这些配置。

#### 启动服务器

```bash
# 开发模式 (自动重启)
npm run dev

# 生产模式
npm start
```

服务器将在 `http://localhost:3000` 启动

#### 补充依赖

根据当前仓库代码，首次启动前建议补装：

```bash
npm install bcryptjs jsonwebtoken
```

### 3️⃣ Android 端配置

#### 配置签名密钥

在项目根目录创建 `keystore.properties` 文件:

```properties
keystore.path=path/to/your.keystore
keystore.password=your_password
keystore.alias=your_alias
keystore.keyPassword=your_key_password
```

#### 同步 Gradle

在 Android Studio 中打开项目，等待 Gradle 同步完成。

#### 修改 API 地址 (可选)

如果后端不在默认地址运行，优先检查这些位置：

- `app/src/main/java/com/example/musicplayer/core/Constants.java`
- `app/src/main/java/com/example/musicplayer/feature/main/MainViewModel.java`
- `app/src/main/java/com/example/musicplayer/feature/update/UpdateManager.java`

其中 `Constants.java` 中的写法如下：

```java
public static final class API {
    public static final String BASE_URL = "http://your-server-ip:3000";
}
```

### 4️⃣ 运行应用

1. 连接 Android 设备或启动模拟器
2. 点击 Android Studio 的 ▶️ Run 按钮
3. 应用将安装并自动启动

## ⚙️ 配置说明

### Android 端权限

应用在清单中声明了这些关键权限：

- `INTERNET`：网络请求
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_MEDIA_PLAYBACK`：后台播放
- `POST_NOTIFICATIONS`：通知权限
- `REQUEST_INSTALL_PACKAGES`：APK 更新安装

同时还声明了 `FileProvider`，用于更新下载后的 APK 安装流程。

### 网络与资源

- Android 端默认使用明文 HTTP，并在清单中开启了 `usesCleartextTraffic="true"`。
- 后端通过 `/resource` 和 `/musicplayer` 暴露静态文件，实际资源根目录默认写死为 `/musicplayer`。
- 如果你的本地环境没有这个目录，歌曲和图片资源访问会失败。

### 数据库初始化说明

仓库中提供了：

- `web/sql/users.sql`
- `web/sql/version.sql`
- `database_indexes.sql`

但歌曲主表数据并未完整包含在上述 SQL 中。如果只导入现有 SQL，用户和版本相关接口可初始化，歌曲接口仍需要你自行准备 `songs` 表及数据。

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

## 🔄 典型使用流程

### 1. 启动与数据准备
1. 应用启动后先检查登录状态，未登录则进入登录页。
2. 主界面优先从 Room 读取本地歌曲列表，避免启动白屏。
3. 后台再请求服务端歌曲数据，同步并合并到本地数据库。
4. 同步过程中尽量保留收藏、下载路径和导入状态等本地信息。

### 2. 发现与播放
1. 用户在发现页浏览推荐歌曲或进入搜索页查找目标歌曲。
2. 点击歌曲后，`MainActivity` 将歌曲转换为 `MediaItem` 并交给 `MediaController`。
3. `PlaybackService` 中的 ExoPlayer 开始播放，通知栏同步出现播放控制。
4. 播放状态、歌曲信息、进度和歌词通过 `MainViewModel` 分发给各页面。

### 3. 歌词加载与同步
1. 播放开始后，优先读取数据库或缓存中的歌词。
2. 若本地没有歌词，则请求 LRCLIB。
3. 获取到 LRC 后解析为时间轴结构并写回本地数据库。
4. 播放过程中根据当前进度定位歌词行，实现实时高亮与滚动同步。

### 4. 收藏、下载与本地管理
1. 用户可在播放器或列表页直接收藏歌曲。
2. 下载功能将歌曲保存到应用外部音乐目录，并通过通知栏展示进度。
3. 下载完成后自动更新数据库，将该歌曲标记为本地文件。
4. 用户也可手动导入本地音频，系统会和下载音乐分组展示。

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
- `isLocal`、`isFavorite`、`lyrics` 等本地状态尽量保留，不被远程同步覆盖
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
- 当前版本 10，已包含 `9 -> 10` 迁移
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
- ⚠️ 后端配置仍然硬编码在 `web/server.js` 中，尚未抽离到 `.env`
- ⚠️ 缺少 HTTPS/TLS 配置
- ⚠️ 后端无请求频率限制 (Rate Limiting)
- ⚠️ ProGuard 未配置有效混淆规则
- ⚠️ 测试覆盖率极低 (< 5%)
- ⚠️ `web/package.json` 未声明 `bcryptjs` 和 `jsonwebtoken`
- ⚠️ 仓库中当前没有 `LICENSE` 文件

### 建议优化项
- 🔧 补齐后端依赖声明并增加锁文件
- 🔧 引入 `.env` 配置替代硬编码
- 🔧 添加 HTTPS 支持
- 🔧 实现请求限流中间件
- 🔧 补充核心模块单元测试
- 🔧 配置 ProGuard 混淆规则
- 🔧 添加 CI/CD 自动化构建
- 🔧 集成 Crashlytics 错误监控
- 🔧 实现离线模式完整支持

## 📄 相关文档

- [PROJECT_INTRODUCTION.md](./PROJECT_INTRODUCTION.md) - 项目介绍书，适合答辩或项目汇报
- [DEPLOYMENT_GUIDE.md](./DEPLOYMENT_GUIDE.md) - 部署与上线说明
- [CLAUDE.md](./CLAUDE.md) - 开发者视角的架构速览
- [database_indexes.sql](./database_indexes.sql) - MySQL 查询优化索引脚本
- [web/sql/users.sql](./web/sql/users.sql) - 用户表初始化脚本
- [web/sql/version.sql](./web/sql/version.sql) - 版本更新表初始化脚本

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
