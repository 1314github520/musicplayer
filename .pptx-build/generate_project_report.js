const fs = require("fs");
const path = require("path");
const pptxgen = require("pptxgenjs");

const pptx = new pptxgen();
pptx.layout = "LAYOUT_16x9";
pptx.author = "SOLO Code Assistant";
pptx.company = "MusicPlayer";
pptx.subject = "MusicPlayer 项目汇报";
pptx.title = "MusicPlayer 项目汇报";
pptx.lang = "zh-CN";

const COLORS = {
  navy: "0F172A",
  blue: "1D4ED8",
  teal: "0F766E",
  cyan: "0891B2",
  sky: "E0F2FE",
  mint: "CCFBF1",
  slate: "334155",
  text: "1E293B",
  muted: "64748B",
  soft: "F8FAFC",
  line: "E2E8F0",
  white: "FFFFFF",
  green: "16A34A",
  amber: "D97706",
  red: "DC2626",
};

const FONT = "Microsoft YaHei";
const OUT_DIR = path.join(__dirname, "output");
const OUT_FILE = path.join(OUT_DIR, "MusicPlayer_Project_Report_20260518.pptx");

fs.mkdirSync(OUT_DIR, { recursive: true });

function shadow() {
  return {
    type: "outer",
    color: "000000",
    blur: 3,
    offset: 1,
    angle: 45,
    opacity: 0.12,
  };
}

function addBg(slide, dark = false) {
  slide.background = { color: dark ? COLORS.navy : COLORS.soft };
  slide.addShape(pptx.shapes.RECTANGLE, {
    x: 0,
    y: 0,
    w: 10,
    h: 0.16,
    line: { color: dark ? COLORS.blue : COLORS.blue, transparency: 100 },
    fill: { color: dark ? COLORS.blue : COLORS.blue },
  });
}

function addHeader(slide, index, title, subtitle) {
  addBg(slide, false);
  slide.addShape(pptx.shapes.RECTANGLE, {
    x: 0.55,
    y: 0.35,
    w: 0.62,
    h: 0.34,
    line: { color: COLORS.blue, transparency: 100 },
    fill: { color: COLORS.blue },
  });
  slide.addText(String(index).padStart(2, "0"), {
    x: 0.55,
    y: 0.38,
    w: 0.62,
    h: 0.26,
    align: "center",
    color: COLORS.white,
    fontFace: FONT,
    fontSize: 13,
    bold: true,
    margin: 0,
  });
  slide.addText(title, {
    x: 1.33,
    y: 0.24,
    w: 6.8,
    h: 0.36,
    fontFace: FONT,
    fontSize: 25,
    bold: true,
    color: COLORS.text,
    margin: 0,
  });
  slide.addText(subtitle, {
    x: 1.33,
    y: 0.58,
    w: 7.3,
    h: 0.24,
    fontFace: FONT,
    fontSize: 9,
    color: COLORS.muted,
    margin: 0,
  });
}

function addFooter(slide, text, page) {
  slide.addText(text, {
    x: 0.6,
    y: 5.18,
    w: 7.5,
    h: 0.16,
    fontFace: FONT,
    fontSize: 8,
    color: COLORS.muted,
    margin: 0,
  });
  slide.addText(String(page), {
    x: 9.1,
    y: 5.1,
    w: 0.35,
    h: 0.2,
    fontFace: FONT,
    fontSize: 9,
    bold: true,
    color: COLORS.muted,
    align: "right",
    margin: 0,
  });
}

function addCard(slide, x, y, w, h, title, body, accent = COLORS.blue, bodySize = 11) {
  slide.addShape(pptx.shapes.RECTANGLE, {
    x,
    y,
    w,
    h,
    line: { color: COLORS.line, width: 1 },
    fill: { color: COLORS.white },
  });
  slide.addShape(pptx.shapes.RECTANGLE, {
    x,
    y,
    w: 0.08,
    h,
    line: { color: accent, transparency: 100 },
    fill: { color: accent },
  });
  slide.addText(title, {
    x: x + 0.18,
    y: y + 0.16,
    w: w - 0.28,
    h: 0.28,
    fontFace: FONT,
    fontSize: 13,
    bold: true,
    color: COLORS.text,
    margin: 0,
  });
  slide.addText(body, {
    x: x + 0.18,
    y: y + 0.5,
    w: w - 0.28,
    h: h - 0.62,
    fontFace: FONT,
    fontSize: bodySize,
    color: COLORS.slate,
    valign: "top",
    margin: 0,
  });
}

function addStat(slide, x, y, w, h, value, label, accent = COLORS.blue) {
  slide.addShape(pptx.shapes.RECTANGLE, {
    x,
    y,
    w,
    h,
    line: { color: COLORS.line, width: 1 },
    fill: { color: COLORS.white },
  });
  slide.addText(value, {
    x: x + 0.18,
    y: y + 0.14,
    w: w - 0.28,
    h: 0.34,
    fontFace: FONT,
    fontSize: 24,
    bold: true,
    color: accent,
    margin: 0,
  });
  slide.addText(label, {
    x: x + 0.18,
    y: y + 0.55,
    w: w - 0.28,
    h: 0.2,
    fontFace: FONT,
    fontSize: 10,
    color: COLORS.muted,
    margin: 0,
  });
}

function addBulletList(slide, items, opts) {
  const runs = [];
  items.forEach((item, idx) => {
    runs.push({
      text: item,
      options: {
        bullet: true,
        breakLine: idx !== items.length - 1,
      },
    });
  });
  slide.addText(runs, {
    x: opts.x,
    y: opts.y,
    w: opts.w,
    h: opts.h,
    fontFace: FONT,
    fontSize: opts.fontSize || 12,
    color: opts.color || COLORS.slate,
    paraSpaceAfterPt: opts.paraSpaceAfterPt || 8,
    breakLine: true,
    margin: 0,
    valign: "top",
  });
}

function addTag(slide, x, y, text, fill, color = COLORS.white, w = 1.2) {
  slide.addShape(pptx.shapes.RECTANGLE, {
    x,
    y,
    w,
    h: 0.28,
    line: { color: fill, transparency: 100 },
    fill: { color: fill },
  });
  slide.addText(text, {
    x,
    y: y + 0.03,
    w,
    h: 0.18,
    align: "center",
    fontFace: FONT,
    fontSize: 9,
    bold: true,
    color,
    margin: 0,
  });
}

function addTimelineNode(slide, x, y, title, desc, color) {
  slide.addShape(pptx.shapes.OVAL, {
    x,
    y,
    w: 0.3,
    h: 0.3,
    line: { color, width: 1.5 },
    fill: { color: COLORS.white },
  });
  slide.addShape(pptx.shapes.OVAL, {
    x: x + 0.08,
    y: y + 0.08,
    w: 0.14,
    h: 0.14,
    line: { color, transparency: 100 },
    fill: { color },
  });
  slide.addText(title, {
    x: x - 0.22,
    y: y + 0.38,
    w: 1.2,
    h: 0.24,
    fontFace: FONT,
    fontSize: 10,
    bold: true,
    align: "center",
    color: COLORS.text,
    margin: 0,
  });
  slide.addText(desc, {
    x: x - 0.45,
    y: y + 0.62,
    w: 1.7,
    h: 0.46,
    fontFace: FONT,
    fontSize: 8.5,
    align: "center",
    color: COLORS.muted,
    margin: 0,
  });
}

function addSectionTitle(slide, text, x, y, w = 2.4) {
  slide.addText(text, {
    x,
    y,
    w,
    h: 0.24,
    fontFace: FONT,
    fontSize: 12.5,
    bold: true,
    color: COLORS.text,
    margin: 0,
  });
}

function slideTitle() {
  const slide = pptx.addSlide();
  addBg(slide, true);

  slide.addShape(pptx.shapes.RECTANGLE, {
    x: 0,
    y: 0,
    w: 10,
    h: 5.625,
    line: { color: COLORS.navy, transparency: 100 },
    fill: { color: COLORS.navy },
  });
  slide.addShape(pptx.shapes.RECTANGLE, {
    x: 6.95,
    y: 0,
    w: 3.05,
    h: 5.625,
    line: { color: COLORS.blue, transparency: 100 },
    fill: { color: COLORS.blue, transparency: 18 },
  });
  slide.addShape(pptx.shapes.RECTANGLE, {
    x: 0.62,
    y: 1.0,
    w: 0.12,
    h: 1.8,
    line: { color: COLORS.cyan, transparency: 100 },
    fill: { color: COLORS.cyan },
  });

  slide.addText("MusicPlayer", {
    x: 0.95,
    y: 0.95,
    w: 3.8,
    h: 0.42,
    fontFace: FONT,
    fontSize: 30,
    bold: true,
    color: COLORS.white,
    margin: 0,
  });
  slide.addText("项目汇报", {
    x: 0.95,
    y: 1.42,
    w: 4.2,
    h: 0.6,
    fontFace: FONT,
    fontSize: 26,
    bold: true,
    color: "BAE6FD",
    margin: 0,
  });
  slide.addText("基于当前仓库代码、文档、构建产物与版本记录整理的阶段性成果汇报", {
    x: 0.95,
    y: 2.12,
    w: 4.9,
    h: 0.38,
    fontFace: FONT,
    fontSize: 12,
    color: "CBD5E1",
    margin: 0,
  });

  addTag(slide, 0.95, 2.7, "Android + Node.js", COLORS.teal, COLORS.white, 1.65);
  addTag(slide, 2.72, 2.7, "MVVM + Media3", COLORS.blue, COLORS.white, 1.5);
  addTag(slide, 4.35, 2.7, "v2.0 / 2026-05-18", COLORS.cyan, COLORS.white, 1.9);

  addStat(slide, 7.25, 0.88, 2.1, 0.92, "43", "Java 源文件", "93C5FD");
  addStat(slide, 7.25, 1.96, 2.1, 0.92, "14", "后端 API 路由", "A5F3FC");
  addStat(slide, 7.25, 3.04, 2.1, 0.92, "22.04 MB", "Release APK", "99F6E4");
  addStat(slide, 7.25, 4.12, 2.1, 0.92, "7", "根目录文档", "BFDBFE");

  slide.addText("汇报口径：项目概述 / 架构设计 / 功能实现 / 关键难点 / 进度与测试 / 成果与规划", {
    x: 0.95,
    y: 4.55,
    w: 5.7,
    h: 0.24,
    fontFace: FONT,
    fontSize: 9.5,
    color: "94A3B8",
    margin: 0,
  });
}

function slideOverview() {
  const slide = pptx.addSlide();
  addHeader(slide, 1, "项目概述", "定位、范围与当前产出规模");

  addCard(
    slide,
    0.65,
    1.0,
    4.45,
    3.52,
    "项目定位",
    "MusicPlayer 是一款面向移动端的音乐播放器项目，采用“Android 客户端 + Node.js 后端 + MySQL 数据源”的轻量全栈方案，覆盖在线歌曲浏览、播放、歌词、下载、收藏、最近播放与账号能力。",
    COLORS.blue,
    12
  );
  addSectionTitle(slide, "范围概览", 0.88, 2.57, 1.4);
  addBulletList(slide, [
    "客户端采用单 Activity + MVVM，统一管理播放、导航与状态。",
    "后端提供歌曲列表、搜索、详情、播放、版本更新与用户接口。",
    "Room 本地数据库承接离线缓存、本地导入、收藏与最近播放。"
  ], { x: 0.88, y: 2.82, w: 3.85, h: 1.3, fontSize: 11.5 });

  addStat(slide, 5.35, 1.05, 1.8, 0.95, "10", "Fragment 页面");
  addStat(slide, 7.22, 1.05, 1.8, 0.95, "4", "Activity 页面", COLORS.teal);
  addStat(slide, 5.35, 2.18, 1.8, 0.95, "23", "布局 XML", COLORS.cyan);
  addStat(slide, 7.22, 2.18, 1.8, 0.95, "27", "Drawable 资源", COLORS.green);
  addStat(slide, 5.35, 3.31, 1.8, 0.95, "6", "Git 提交记录", COLORS.amber);
  addStat(slide, 7.22, 3.31, 1.8, 0.95, "v2.0", "当前版本号", COLORS.blue);

  slide.addShape(pptx.shapes.RECTANGLE, {
    x: 5.35,
    y: 4.46,
    w: 3.67,
    h: 0.4,
    line: { color: COLORS.line, width: 1 },
    fill: { color: "EFF6FF" },
  });
  slide.addText("当前仓库已同时具备 App 端、后端服务、SQL 脚本、部署文档与 Release 产物。", {
    x: 5.55,
    y: 4.56,
    w: 3.3,
    h: 0.18,
    fontFace: FONT,
    fontSize: 9.5,
    color: COLORS.slate,
    margin: 0,
  });

  addFooter(slide, "统计口径来自当前仓库文件结构、构建产物与 Git 历史。", 2);
}

function slideBackground() {
  const slide = pptx.addSlide();
  addHeader(slide, 2, "需求背景", "PRD 目标与场景诉求");

  addCard(slide, 0.65, 1.02, 2.85, 3.7, "业务背景", "传统播放器在沉浸式体验、歌词同步、在线与本地一体化能力上存在体验割裂，项目目标是以酷狗概念版风格实现更强的交互与内容承载。", COLORS.blue, 11.3);
  addCard(slide, 3.62, 1.02, 2.85, 3.7, "核心需求", "围绕发现页、播放器、个人中心三大主界面，补齐搜索、收藏、最近播放、本地音乐、下载导入、账号登录与版本更新等完整链路。", COLORS.teal, 11.3);
  addCard(slide, 6.59, 1.02, 2.75, 3.7, "体验目标", "通过深色沉浸式 UI、动态封面配色、黑胶动画、歌词联动与前台播放，形成具备持续使用价值的移动端音乐产品原型。", COLORS.cyan, 11.3);

  addSectionTitle(slide, "PRD 目标摘要", 0.85, 4.34, 1.7);
  addBulletList(slide, [
    "发现页：搜索栏、推荐内容、分类入口与频道内容承载。",
    "播放页：沉浸式背景、歌词显示、播控、进度拖拽与循环模式。",
    "我的页：收藏、最近播放、本地音乐、设置、编辑资料等入口。"
  ], { x: 0.85, y: 4.58, w: 8.2, h: 0.48, fontSize: 10.5, paraSpaceAfterPt: 6 });

  addFooter(slide, "需求来源于仓库中的 `Music_App_PRD.md` 与当前实现范围。", 3);
}

function slideArchitecture() {
  const slide = pptx.addSlide();
  addHeader(slide, 3, "整体技术架构", "端到端数据链路与技术选型");

  slide.addShape(pptx.shapes.RECTANGLE, {
    x: 0.8, y: 1.15, w: 8.45, h: 0.62,
    fill: { color: "DBEAFE" }, line: { color: COLORS.blue, width: 1 }
  });
  slide.addText("Android Client", {
    x: 1.05, y: 1.34, w: 1.8, h: 0.18, fontFace: FONT, fontSize: 16, bold: true, color: COLORS.blue, margin: 0
  });
  slide.addText("MainActivity / Fragments / MainViewModel / Room / PlaybackService", {
    x: 3.0, y: 1.35, w: 5.6, h: 0.18, fontFace: FONT, fontSize: 10.5, color: COLORS.slate, margin: 0
  });

  slide.addShape(pptx.shapes.RECTANGLE, {
    x: 1.18, y: 2.05, w: 2.15, h: 1.06,
    fill: { color: COLORS.white }, line: { color: COLORS.line, width: 1 }
  });
  slide.addText("界面与状态层", {
    x: 1.36, y: 2.24, w: 1.5, h: 0.18, fontFace: FONT, fontSize: 13, bold: true, color: COLORS.text, margin: 0
  });
  slide.addText("发现 / 播放器 / 我的\nLiveData 驱动 UI", {
    x: 1.36, y: 2.53, w: 1.55, h: 0.42, fontFace: FONT, fontSize: 10.5, color: COLORS.slate, margin: 0
  });

  slide.addShape(pptx.shapes.RECTANGLE, {
    x: 3.92, y: 2.05, w: 2.15, h: 1.06,
    fill: { color: COLORS.white }, line: { color: COLORS.line, width: 1 }
  });
  slide.addText("播放与能力层", {
    x: 4.14, y: 2.24, w: 1.5, h: 0.18, fontFace: FONT, fontSize: 13, bold: true, color: COLORS.text, margin: 0
  });
  slide.addText("Media3 / ExoPlayer\n歌词、下载、封面、通知", {
    x: 4.14, y: 2.53, w: 1.55, h: 0.42, fontFace: FONT, fontSize: 10.5, color: COLORS.slate, margin: 0
  });

  slide.addShape(pptx.shapes.RECTANGLE, {
    x: 6.66, y: 2.05, w: 2.15, h: 1.06,
    fill: { color: COLORS.white }, line: { color: COLORS.line, width: 1 }
  });
  slide.addText("服务与数据层", {
    x: 6.88, y: 2.24, w: 1.5, h: 0.18, fontFace: FONT, fontSize: 13, bold: true, color: COLORS.text, margin: 0
  });
  slide.addText("Express API / MySQL\nLRCLIB / 静态资源", {
    x: 6.88, y: 2.53, w: 1.5, h: 0.42, fontFace: FONT, fontSize: 10.5, color: COLORS.slate, margin: 0
  });

  slide.addShape(pptx.shapes.LINE, {
    x: 2.26, y: 3.25, w: 5.15, h: 0,
    line: { color: COLORS.blue, width: 2 }
  });
  slide.addShape(pptx.shapes.LINE, {
    x: 2.26, y: 3.25, w: 0, h: 0.45,
    line: { color: COLORS.blue, width: 2 }
  });
  slide.addShape(pptx.shapes.LINE, {
    x: 4.98, y: 3.25, w: 0, h: 0.45,
    line: { color: COLORS.blue, width: 2 }
  });
  slide.addShape(pptx.shapes.LINE, {
    x: 7.72, y: 3.25, w: 0, h: 0.45,
    line: { color: COLORS.blue, width: 2 }
  });
  slide.addText("统一通过 HTTP / JSON 连接后端，歌曲流媒体播放支持 Range 分段传输。", {
    x: 2.35, y: 3.55, w: 5.0, h: 0.2, fontFace: FONT, fontSize: 10.2, align: "center", color: COLORS.muted, margin: 0
  });

  addCard(slide, 0.85, 4.0, 2.5, 0.78, "核心依赖", "Media3 1.5.1 / OkHttp 4.12 / Gson 2.11 / Room 2.8.4", COLORS.blue, 10);
  addCard(slide, 3.72, 4.0, 2.5, 0.78, "视觉与体验", "Coil 2.7 / Palette / Lottie / 深色沉浸式 UI", COLORS.teal, 10);
  addCard(slide, 6.59, 4.0, 2.5, 0.78, "后端技术", "Express 4.18 / mysql2 / JWT / bcrypt / 静态资源映射", COLORS.cyan, 10);

  addFooter(slide, "架构说明基于 `MainActivity`、`MainViewModel`、`PlaybackService` 与 `web/server.js`。", 4);
}

function slideAndroid() {
  const slide = pptx.addSlide();
  addHeader(slide, 4, "Android 架构设计", "MVVM、播放服务与本地数据协同");

  addCard(slide, 0.7, 1.08, 2.1, 1.15, "MainActivity", "单 Activity 宿主，负责导航、MediaController 生命周期、权限处理、歌词定时同步与全局播控联动。", COLORS.blue, 10.5);
  addCard(slide, 0.7, 2.45, 2.1, 1.15, "MainViewModel", "统一暴露播放状态、歌曲信息、歌词、用户资料与歌曲列表，后台线程池执行网络与数据库任务。", COLORS.teal, 10.5);
  addCard(slide, 0.7, 3.82, 2.1, 1.15, "PlaybackService", "前台 MediaSessionService，包装 ExoPlayer，处理音频焦点、耳机拔出暂停与系统通知。", COLORS.cyan, 10.5);

  slide.addShape(pptx.shapes.RECTANGLE, {
    x: 3.2, y: 1.0, w: 5.85, h: 4.05,
    fill: { color: COLORS.white }, line: { color: COLORS.line, width: 1 }
  });
  slide.addText("页面与数据协同关系", {
    x: 3.45, y: 1.18, w: 2.3, h: 0.2, fontFace: FONT, fontSize: 15, bold: true, color: COLORS.text, margin: 0
  });

  addCard(slide, 3.45, 1.62, 1.55, 0.82, "页面层", "发现 / 播放器 / 我的 / 搜索", COLORS.blue, 9.5);
  addCard(slide, 5.2, 1.62, 1.55, 0.82, "状态层", "LiveData / 事件分发 / UI 观察", COLORS.teal, 9.5);
  addCard(slide, 6.95, 1.62, 1.55, 0.82, "服务层", "播放、歌词、下载、更新", COLORS.cyan, 9.5);

  addCard(slide, 3.45, 2.72, 1.55, 0.82, "本地库", "Room songs / recent_play", COLORS.green, 9.5);
  addCard(slide, 5.2, 2.72, 1.55, 0.82, "网络层", "OkHttp + Gson + RetryInterceptor", COLORS.amber, 9.5);
  addCard(slide, 6.95, 2.72, 1.55, 0.82, "体验层", "Coil + Palette + 动画", COLORS.blue, 9.5);

  addBulletList(slide, [
    "Song 实体承载远端歌曲、本地路径、封面、歌词、收藏与本地状态。",
    "RecentPlay 保留 7 天窗口，便于最近播放回溯与推荐场景。",
    "下载与导入后的本地文件可回写数据库并参与统一播放列表。"
  ], { x: 3.5, y: 3.9, w: 5.2, h: 0.88, fontSize: 10.4, paraSpaceAfterPt: 6 });

  addFooter(slide, "Android 端围绕状态中心与播放服务形成清晰的职责边界。", 5);
}

function slideBackend() {
  const slide = pptx.addSlide();
  addHeader(slide, 5, "后端与数据设计", "API、鉴权与流媒体资源服务");

  addCard(slide, 0.68, 1.0, 2.75, 1.02, "歌曲服务", "分页/全量列表、详情、关键字搜索、播放地址下发，支持标题/歌手/专辑联合检索。", COLORS.blue, 10.6);
  addCard(slide, 0.68, 2.22, 2.75, 1.02, "用户服务", "注册、登录、JWT 鉴权、资料查询/更新、修改密码、注销与头像上传。", COLORS.teal, 10.6);
  addCard(slide, 0.68, 3.44, 2.75, 1.02, "系统服务", "版本检查、静态资源映射、统一响应封装、错误处理中间件与 404 处理。", COLORS.cyan, 10.6);

  slide.addShape(pptx.shapes.RECTANGLE, {
    x: 3.8, y: 1.0, w: 5.2, h: 3.5,
    fill: { color: COLORS.white }, line: { color: COLORS.line, width: 1 }
  });
  slide.addText("后端关键能力矩阵", {
    x: 4.05, y: 1.18, w: 2.5, h: 0.2, fontFace: FONT, fontSize: 15, bold: true, color: COLORS.text, margin: 0
  });

  addBulletList(slide, [
    "资源目录通过 `/resource/*` 与 `/musicplayer/*` 双路径映射，兼容旧数据。",
    "歌曲播放接口对远端 URL 采用重定向，对本地资源采用文件读取 + Range 206 返回。",
    "SQL 查询增加分页与参数校验，LIKE 查询支持通配符转义与路径越权防护。",
    "MySQL 通过连接池访问，版本信息与用户信息分别独立表结构维护。"
  ], { x: 4.05, y: 1.55, w: 4.45, h: 1.55, fontSize: 10.7, paraSpaceAfterPt: 7 });

  addStat(slide, 4.08, 3.35, 1.35, 0.82, "14", "API 路由", COLORS.blue);
  addStat(slide, 5.6, 3.35, 1.35, 0.82, "3", "核心数据表", COLORS.teal);
  addStat(slide, 7.12, 3.35, 1.35, 0.82, "3000", "默认端口", COLORS.cyan);

  addFooter(slide, "Node.js 后端承担数据聚合、鉴权、更新检查与音频资源分发。", 6);
}

function slideModules() {
  const slide = pptx.addSlide();
  addHeader(slide, 6, "核心功能模块", "已在仓库中落地的主要业务能力");

  const modules = [
    ["发现与分类", "发现页承载内容入口，分类页已独立为二级页面，支持按歌手/专辑分组。", COLORS.blue],
    ["播放与歌词", "全屏播放器、SeekBar、循环模式、歌词高亮滚动、点击歌词定位播放。", COLORS.teal],
    ["下载与导入", "在线下载到本地目录、去重校验、系统文件选择导入、多文件元数据解析。", COLORS.cyan],
    ["收藏与最近播放", "收藏状态随本地数据持久化，最近播放按 7 天窗口写入与清理。", COLORS.green],
    ["搜索与用户中心", "支持歌曲搜索、登录注册、资料编辑、密码修改、头像上传与设置。", COLORS.amber],
    ["版本更新与体验增强", "版本检查、通知播放、黑胶动画、Palette 动态取色、Lottie 动画。", COLORS.blue]
  ];

  let idx = 0;
  for (let row = 0; row < 2; row += 1) {
    for (let col = 0; col < 3; col += 1) {
      const item = modules[idx];
      addCard(slide, 0.7 + col * 3.05, 1.08 + row * 1.78, 2.72, 1.42, item[0], item[1], item[2], 10.2);
      addTag(slide, 2.58 + col * 3.05, 1.18 + row * 1.78, "已实现", item[2], COLORS.white, 0.62);
      idx += 1;
    }
  }

  slide.addShape(pptx.shapes.RECTANGLE, {
    x: 0.72, y: 4.73, w: 8.48, h: 0.26,
    fill: { color: "ECFEFF" }, line: { color: COLORS.line, width: 1 }
  });
  slide.addText("功能闭环已经覆盖“发现 -> 播放 -> 收藏/下载 -> 账号 -> 更新”的完整主流程，可支撑原型展示与阶段性验收。", {
    x: 0.95, y: 4.79, w: 8.1, h: 0.12, fontFace: FONT, fontSize: 9.5, color: COLORS.slate, margin: 0
  });

  addFooter(slide, "模块判断标准基于页面文件、业务类与后端接口是否已在仓库中落地。", 7);
}

function slideChallenges() {
  const slide = pptx.addSlide();
  addHeader(slide, 7, "关键技术难点与解决方案", "围绕播放器、歌词、数据与稳定性的重点攻关");

  addCard(slide, 0.7, 1.0, 4.15, 1.05, "难点 1：播放控制与系统能力协同", "采用 Media3 + MediaSessionService 统一播控入口，处理音频焦点、耳机拔出暂停、通知栏控制与后台持续播放。", COLORS.blue, 11);
  addCard(slide, 5.0, 1.0, 4.15, 1.05, "难点 2：歌词获取、缓存与同步", "通过 LRCLIB 多阶段检索、歌词本地缓存、LRC 解析与二分查找，将当前播放位置精准映射到歌词行。", COLORS.teal, 11);
  addCard(slide, 0.7, 2.35, 4.15, 1.05, "难点 3：远端歌曲与本地状态合并", "ViewModel 先以 Room 兜底，再进行远端全量同步；同步时保护收藏、本地文件与歌词字段，避免覆盖用户状态。", COLORS.cyan, 11);
  addCard(slide, 5.0, 2.35, 4.15, 1.05, "难点 4：稳定性与性能治理", "引入 RetryInterceptor、统一错误处理、连接池复用、数据库索引与图片加载优化，降低失败率与资源消耗。", COLORS.amber, 11);

  addStat(slide, 0.82, 3.92, 1.48, 0.82, "50-80%", "数据库查询优化", COLORS.blue);
  addStat(slide, 2.47, 3.92, 1.48, 0.82, "30-40%", "网络请求优化", COLORS.teal);
  addStat(slide, 4.12, 3.92, 1.48, 0.82, "20-30%", "内存占用优化", COLORS.cyan);
  addStat(slide, 5.77, 3.92, 1.48, 0.82, "5x+", "歌词缓存提升", COLORS.green);
  addStat(slide, 7.42, 3.92, 1.48, 0.82, "15-20%", "图片加载优化", COLORS.amber);

  addFooter(slide, "性能数据引用项目优化报告，难点描述基于关键实现逻辑归纳。", 8);
}

function slideProgress() {
  const slide = pptx.addSlide();
  addHeader(slide, 8, "项目里程碑与开发进度", "从基础能力拉通到 v2.0 交付的阶段演进");

  slide.addShape(pptx.shapes.LINE, {
    x: 1.0, y: 2.42, w: 7.8, h: 0,
    line: { color: COLORS.blue, width: 2 }
  });

  addTimelineNode(slide, 1.0, 2.27, "05-08", "v1.2\n基础资源链路打通", COLORS.blue);
  addTimelineNode(slide, 2.8, 2.27, "05-09", "优化阶段\n安全/性能治理完成", COLORS.teal);
  addTimelineNode(slide, 4.6, 2.27, "05-13", "v1.8\n核心能力扩展", COLORS.cyan);
  addTimelineNode(slide, 6.4, 2.27, "05-16", "v2.0\n文档与版本收敛", COLORS.green);
  addTimelineNode(slide, 8.2, 2.27, "05-18", "当前状态\nRelease APK 与汇报稿", COLORS.amber);

  addCard(slide, 0.75, 0.98, 2.55, 0.9, "里程碑结论", "Git 记录已形成 v1.2 -> v1.8 -> v2.0 的版本迭代路径，当前仓库存在 Release 安装包与多份交付文档。", COLORS.blue, 10.2);
  addCard(slide, 3.7, 0.98, 2.55, 0.9, "开发进度判断", "主业务能力基本闭环，项目已具备阶段性汇报、演示与内部验收条件。", COLORS.teal, 10.2);
  addCard(slide, 6.65, 0.98, 2.55, 0.9, "待完善方向", "自动化测试、密钥与配置治理、Room 无损迁移、后端依赖声明完整性仍需继续加强。", COLORS.amber, 10.2);

  addBulletList(slide, [
    "版本记录：2026-05-08、2026-05-13、2026-05-16、2026-05-18 均有关键提交节点。",
    "优化报告记录两阶段共 15 项优化，涵盖安全、性能、稳定性与代码质量。",
    "当前发布产物：`app/release/app-release.apk`，文件大小 22.04 MB。"
  ], { x: 0.9, y: 4.12, w: 8.2, h: 0.76, fontSize: 10.6, paraSpaceAfterPt: 7 });

  addFooter(slide, "里程碑基于 Git 提交时间、优化报告与 APK 产物时间戳整理。", 9);
}

function slideTestAndPerf() {
  const slide = pptx.addSlide();
  addHeader(slide, 9, "测试结果与性能指标", "真实校验结果 + 已记录优化收益");

  addCard(slide, 0.68, 1.02, 3.2, 1.2, "构建与校验现状", "已存在 Release APK 产物；执行 `testDebugUnitTest` 时共运行 4 项单测，其中 3 项失败，失败点集中在 Robolectric 依赖解析环境。", COLORS.blue, 10.6);
  addCard(slide, 0.68, 2.45, 3.2, 1.2, "测试结论", "功能实现层面已具备演示条件，但自动化测试覆盖与环境稳定性仍需提升，适合作为下一阶段重点治理项。", COLORS.amber, 10.6);
  addCard(slide, 0.68, 3.88, 3.2, 1.0, "已记录指标", "数据库查询 50%-80%，网络请求 30%-40%，内存使用 20%-30%，歌词缓存 5 倍以上，图片加载 15%-20%。", COLORS.teal, 10.4);

  addStat(slide, 4.18, 1.08, 1.45, 0.86, "4", "已执行单测", COLORS.blue);
  addStat(slide, 5.78, 1.08, 1.45, 0.86, "3", "失败用例", COLORS.red);
  addStat(slide, 7.38, 1.08, 1.45, 0.86, "22.04 MB", "Release APK", COLORS.teal);
  addStat(slide, 4.18, 2.15, 1.45, 0.86, "50-80%", "DB 查询优化", COLORS.blue);
  addStat(slide, 5.78, 2.15, 1.45, 0.86, "30-40%", "网络优化", COLORS.teal);
  addStat(slide, 7.38, 2.15, 1.45, 0.86, "5x+", "缓存提升", COLORS.green);

  slide.addShape(pptx.shapes.RECTANGLE, {
    x: 4.18, y: 3.48, w: 4.8, h: 1.38,
    fill: { color: COLORS.white }, line: { color: COLORS.line, width: 1 }
  });
  slide.addText("当前验证口径", {
    x: 4.42, y: 3.65, w: 1.7, h: 0.18, fontFace: FONT, fontSize: 14, bold: true, color: COLORS.text, margin: 0
  });
  addBulletList(slide, [
    "构建产物：Release APK 已生成，更新时间 2026-05-18 13:50。",
    "测试执行：4 项单测已触发，3 项受依赖解析问题失败。",
    "风险提示：项目仍以功能验证为主，需补充更稳定的本地与 CI 测试流程。"
  ], { x: 4.42, y: 3.95, w: 4.2, h: 0.7, fontSize: 10.2, paraSpaceAfterPt: 6 });

  addFooter(slide, "本页区分“已执行测试结果”和“优化报告中的性能提升”两类数据口径。", 10);
}

function slideTeamAndCost() {
  const slide = pptx.addSlide();
  addHeader(slide, 10, "团队分工与成本投入", "按当前仓库产出倒推的职责分工与投入结构");

  slide.addShape(pptx.shapes.RECTANGLE, {
    x: 0.72, y: 1.02, w: 3.95, h: 3.95,
    fill: { color: COLORS.white }, line: { color: COLORS.line, width: 1 }
  });
  slide.addText("角色分工", {
    x: 0.95, y: 1.18, w: 1.3, h: 0.18, fontFace: FONT, fontSize: 15, bold: true, color: COLORS.text, margin: 0
  });
  addBulletList(slide, [
    "产品/设计：定义需求边界、页面风格、交互路径与汇报结构。",
    "Android 开发：负责 MVVM、播放链路、页面实现、歌词、下载与更新能力。",
    "后端开发：负责歌曲接口、用户接口、鉴权、流媒体与静态资源服务。",
    "测试/运维：负责构建、部署、回归验证、问题跟踪与版本发布支持。"
  ], { x: 0.95, y: 1.52, w: 3.35, h: 2.6, fontSize: 11, paraSpaceAfterPt: 10 });

  slide.addShape(pptx.shapes.RECTANGLE, {
    x: 5.0, y: 1.02, w: 4.0, h: 3.95,
    fill: { color: COLORS.white }, line: { color: COLORS.line, width: 1 }
  });
  slide.addText("成本投入分析", {
    x: 5.24, y: 1.18, w: 1.9, h: 0.18, fontFace: FONT, fontSize: 15, bold: true, color: COLORS.text, margin: 0
  });
  slide.addText("仓库未记录实际预算与人天，本页采用结构化口径，便于汇报时替换真实数据。", {
    x: 5.24, y: 1.46, w: 3.35, h: 0.22, fontFace: FONT, fontSize: 9.4, color: COLORS.muted, margin: 0
  });

  addCard(slide, 5.22, 1.92, 3.38, 0.72, "研发人力", "优先级：高，覆盖客户端、服务端、联调与优化修复。", COLORS.blue, 10.2);
  addCard(slide, 5.22, 2.84, 3.38, 0.72, "基础设施与发布", "优先级：中，集中在数据库、服务部署、存储与交付流程。", COLORS.teal, 10.2);
  addCard(slide, 5.22, 3.76, 3.38, 0.72, "软件授权", "优先级：低，当前以开源技术栈为主，授权成本相对可控。", COLORS.green, 10.2);

  addFooter(slide, "如需对外正式汇报，可将角色名称替换为真实成员姓名与实际预算数据。", 11);
}

function slideOutcome() {
  const slide = pptx.addSlide();
  addHeader(slide, 11, "业务价值与成果总结", "从功能闭环、工程资产到阶段性价值体现");

  addCard(slide, 0.68, 1.04, 2.75, 1.25, "成果产出", "Android 客户端、Node.js 后端、MySQL 脚本、Release APK、部署文档、优化报告与产品 PRD 已形成完整交付包。", COLORS.blue, 10.8);
  addCard(slide, 0.68, 2.56, 2.75, 1.25, "业务价值", "项目具备在线 + 本地音乐一体化能力，能支撑展示型产品原型、课堂/答辩汇报或内部验证场景。", COLORS.teal, 10.8);
  addCard(slide, 0.68, 4.08, 2.75, 0.82, "工程价值", "MVVM、Room、统一响应、重试拦截与索引优化，为后续迭代打下可维护基础。", COLORS.cyan, 10);

  addStat(slide, 3.85, 1.05, 1.4, 0.88, "v2.0", "当前版本", COLORS.blue);
  addStat(slide, 5.42, 1.05, 1.4, 0.88, "15", "优化项记录", COLORS.teal);
  addStat(slide, 6.99, 1.05, 1.4, 0.88, "10+", "核心业务能力", COLORS.cyan);

  addCard(slide, 3.82, 2.15, 4.55, 2.75, "阶段总结", "当前项目已完成音乐播放器的核心业务闭环建设，并通过版本迭代、优化治理与产物输出，达到了“可演示、可汇报、可持续演进”的阶段目标。\n\n从展示效果看，项目具有沉浸式播放器、歌词联动、下载导入、账号体系与版本更新等亮点；从工程角度看，项目已经具备继续向稳定性、规范化与商业化方向扩展的基础。", COLORS.blue, 10.6);

  addFooter(slide, "成果总结综合了代码实现、文档资产与发布物三类证据。", 12);
}

function slidePlan() {
  const slide = pptx.addSlide();
  addHeader(slide, 12, "后续迭代计划", "围绕稳定性、安全性与产品深度的下一阶段方向");

  addCard(slide, 0.7, 1.08, 2.7, 3.55, "近期迭代", "1. 修复 Robolectric 测试环境问题，补充单元测试与关键流程回归测试。\n2. 将 JWT 密钥、数据库凭据与环境差异配置迁移到安全配置文件。\n3. 修复 Room destructive migration 风险，补齐无损迁移策略。", COLORS.blue, 11);
  addCard(slide, 3.67, 1.08, 2.7, 3.55, "中期迭代", "1. 引入 Repository / DI（Hilt）等分层方案，降低 Activity 负担。\n2. 强化离线能力、同步机制与缓存命中策略。\n3. 完善 CI 构建、日志监控、异常告警与性能观测。", COLORS.teal, 11);
  addCard(slide, 6.64, 1.08, 2.7, 3.55, "产品深化", "1. 扩展推荐、电台、播放列表与个性化内容。\n2. 强化用户画像、偏好设置与内容运营能力。\n3. 优化视觉系统与组件规范，提升整体交互一致性。", COLORS.cyan, 11);

  slide.addShape(pptx.shapes.RECTANGLE, {
    x: 0.72, y: 4.82, w: 8.62, h: 0.28,
    fill: { color: "ECFDF5" }, line: { color: COLORS.line, width: 1 }
  });
  slide.addText("规划原则：优先补齐测试与安全治理，再推进架构升级与业务增长能力，确保项目从“可展示”走向“可持续”。", {
    x: 0.98, y: 4.89, w: 8.1, h: 0.12, fontFace: FONT, fontSize: 9.5, color: COLORS.slate, margin: 0
  });

  addFooter(slide, "迭代方向同时吸收了仓库中已有风险项、优化报告建议与当前测试结论。", 13);
}

slideTitle();
slideOverview();
slideBackground();
slideArchitecture();
slideAndroid();
slideBackend();
slideModules();
slideChallenges();
slideProgress();
slideTestAndPerf();
slideTeamAndCost();
slideOutcome();
slidePlan();

pptx.writeFile({ fileName: OUT_FILE })
  .then(() => {
    console.log(`PPT generated: ${OUT_FILE}`);
  })
  .catch((err) => {
    console.error(err);
    process.exit(1);
  });
