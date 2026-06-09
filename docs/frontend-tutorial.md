# 前端代码教学（面向初学者 · 边读边学）

> 本文档是"前端入门 + 项目源码导读"的合订本。
> **读法建议**：先通读第一部分（基础概念），再对着本项目 `src/main/resources/static/` 下的源文件看第二部分。
> 本项目是**纯 HTML / CSS / JS** 的前端，**没有用任何框架**（Vue/React/打包工具），所以你看到的代码就是浏览器真正执行的代码。

---

## 目录

### 上篇：基础（不会基础也能看懂代码）
1. [浏览器是怎么渲染一个网页的？](#1-浏览器是怎么渲染一个网页的)
2. [HTML — 网页的骨架](#2-html--网页的骨架)
3. [CSS — 网页的外衣](#3-css--网页的外衣)
   - 选择器
   - 盒模型
   - 布局：display / flex / grid
   - 变量与继承
   - 动画
4. [JavaScript — 网页的行为](#4-javascript--网页的行为)
   - 数据类型 / 变量
   - 函数
   - 对象 / 数组
   - 异步：`Promise` / `async await`
   - DOM 操作
   - 事件
   - fetch（HTTP 请求）
5. [本项目怎么把这些基础串起来？](#5-本项目怎么把这些基础串起来)

### 下篇：本项目源码导读
6. [项目文件结构](#6-项目文件结构)
7. [CSS 四层结构](#7-css-四层结构)
8. [`common.js` — 全局工具箱](#8-commonjs--全局工具箱)
9. [`api.js` — 跟后端说话的封装](#9-apijs--跟后端说话的封装)
10. [`app.js` — 应用启动](#10-appjs--应用启动)
11. [`pages/index.js` — 列表页（最常用的列表模式）](#11-pagesindexjs--列表页最常用的列表模式)
12. [`pages/detail.js` — 详情页（重点：局部更新 + 歌词同步）](#12-pagesdetailjs--详情页重点局部更新--歌词同步)
13. [`pages/admin/users.js` — 管理后台（事件委托 + 乐观更新）](#13-pagesadminusersjs--管理后台事件委托--乐观更新)
14. [`forbidden-words.js` — 敏感词预检（一个完整的"小算法"例子）](#14-forbidden-wordsjs--敏感词预检一个完整的小算法例子)
15. [整体架构总结](#15-整体架构总结)

### 附录
- [调试技巧：Chrome DevTools](#调试技巧chrome-devtools)
- [扩展阅读](#扩展阅读)

---

# 上篇：基础

## 1. 浏览器是怎么渲染一个网页的？

打开 `http://localhost:8443/index.html`，浏览器做了这些事：

```
1. 解析 HTML   →  生成 DOM 树（每个标签是一个"节点"）
2. 解析 CSS    →  生成 CSSOM 树
3. 合成渲染树   →  DOM + CSSOM 合并：哪些节点长什么样
4. 布局（Layout）→ 算每个节点的位置和大小
5. 绘制（Paint）→ 把像素画到屏幕
6. 合成（Composite）→ 多层合并
```

JS 在哪一步执行？**任何时候都可以**。JS 能改 DOM（`document.body.innerHTML = ...`）和 CSS（`element.style.color = 'red'`），这会触发"重排"或"重绘"。

理解这点对你看后面的代码很重要：**JS 不是"提前编译好"的，它就是浏览器按顺序执行的脚本。**

---

## 2. HTML — 网页的骨架

HTML = **超文本标记语言**。它不是编程语言，没有逻辑，只是"标签的集合"。浏览器看到 `<p>` 就知道这是段落，`<img>` 就知道是图片。

### 2.1 一个最简的 HTML 文档

```html
<!DOCTYPE html>                      <!-- 声明这是 HTML5 -->
<html lang="zh-CN">                  <!-- 根元素，lang 告诉浏览器"这是中文" -->
<head>
    <meta charset="UTF-8">           <!-- 字符编码：支持中文 -->
    <meta name="viewport" content="width=device-width, initial-scale=1.0">  <!-- 移动端适配 -->
    <title>音乐列表</title>          <!-- 浏览器标签页文字 -->
    <link rel="stylesheet" href="/css/base.css">  <!-- 引入 CSS -->
</head>
<body>                               <!-- 浏览器显示的内容都在这 -->
    <h1>音乐列表</h1>                <!-- 一级标题 -->
    <div class="container">          <!-- div = 块级容器，无语义 -->
        <input id="keyword">         <!-- 输入框 -->
        <button onclick="search()">搜索</button>  <!-- 点击调用 search() 函数 -->
    </div>
    <script src="/js/app.js"></script>  <!-- 引入 JS -->
</body>
</html>
```

### 2.2 关键概念

| 概念 | 解释 |
|---|---|
| **标签（Tag）** | `<p>` 是开始标签，`</p>` 是结束标签，中间是内容 |
| **属性（Attribute）** | `class="container"` 给标签加"属性"，用来选中和设置样式 |
| **DOM 节点** | 浏览器把每个标签变成一个 JS 对象，可以用 `document.getElementById('xxx')` 拿到 |
| **空标签** | `<img> <br> <input> <link>` 没有结束标签，叫"自闭合" |

### 2.3 本项目的 HTML 范式

看 `index.html`：

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>音乐列表</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link href="https://fonts.googleapis.com/css2?..." rel="stylesheet">  <!-- Google 字体 -->
    <link rel="stylesheet" href="/css/base.css">
    <link rel="stylesheet" href="/css/layout.css">
    <link rel="stylesheet" href="/css/components.css">
</head>
<body>
    <div class="loading-overlay" id="loadingOverlay">     <!-- 全屏遮罩 -->
        <img src="/video/logo.png" alt="Loading">
    </div>

    <div class="container">
        <div class="page-header">
            <h1>音乐列表</h1>
        </div>

        <div class="search-bar">
            <input type="text" id="keyword" placeholder="搜索...">
            <button onclick="search()">搜索</button>
        </div>

        <div class="track-list" id="trackList">          <!-- JS 动态往里塞东西 -->
            <div id="listSkeleton">...</div>             <!-- 加载前的占位 -->
        </div>

        <nav class="pagination" id="pagination"></nav>  <!-- JS 动态渲染分页按钮 -->
    </div>

    <!-- JS 加载顺序：common → api → page → app -->
    <script src="/js/common.js"></script>
    <script src="/js/api.js"></script>
    <script src="/js/pages/index.js"></script>
    <script src="/js/app.js"></script>
</body>
</html>
```

**为什么按这个顺序？** 因为后面的 JS 要用到前面 JS 暴露的全局函数。`index.js` 里调 `get(...)`，`get` 来自 `api.js`；`api.js` 里调 `getToken()` 和 `showToast(...)`，都来自 `common.js`。`app.js` 最后加载是因为它要等页面元素都注册好。

---

## 3. CSS — 网页的外衣

CSS = **层叠样式表**。它告诉浏览器"这个标签长什么样"。

### 3.1 选择器

```css
h1 { ... }                  /* 选所有 h1 标签 */
.title { ... }              /* 选 class="title" 的所有元素 */
#keyword { ... }            /* 选 id="keyword" 的那个元素 */
.btn-primary { ... }        /* 选 class 含 btn-primary 的 */
a:hover { ... }             /* 鼠标悬停时 */
input:focus { ... }          /* 聚焦时（点击输入框） */
.track .play-btn { ... }   /* 后代选择器：.track 里的 .play-btn */
```

### 3.2 盒模型（**最核心的概念**）

每个 HTML 元素都是一个"盒子"：

```
┌─────────────────────────────────┐  ← margin（外边距：和别人的距离）
│  ┌───────────────────────────┐  │
│  │      border (边框)        │  │
│  │  ┌─────────────────────┐  │  │
│  │  │   padding (内边距)   │  │  │
│  │  │  ┌───────────────┐  │  │  │
│  │  │  │  content 内容  │  │  │  │
│  │  │  └───────────────┘  │  │  │
│  │  └─────────────────────┘  │  │
│  └───────────────────────────┘  │
└─────────────────────────────────┘
```

```css
* { box-sizing: border-box; }   /* 改盒子尺寸算法：宽高包含 border+padding，更直观 */
```

### 3.3 布局：display

`display` 决定元素怎么"摆放"：

| 值 | 效果 |
|---|---|
| `block` | 独占一行（div / p / h1） |
| `inline` | 排成一行，不能设宽高（span / a） |
| `inline-block` | 排成一行但能设宽高（img / input） |
| `none` | 不显示也不占位（JS 切显示常用） |
| `flex` | 弹性盒（看下面） |
| `grid` | 网格（看下面） |

### 3.4 Flex 布局（**必学**）

Flex 是"一维"布局：横着排或竖着排。

```css
.navbar {
    display: flex;                          /* 启用 flex */
    justify-content: space-between;         /* 主轴：两端对齐 */
    align-items: center;                    /* 交叉轴：垂直居中 */
}
```

```css
.search-bar {
    display: flex;                          /* 横排 */
    gap: 10px;                              /* 子元素间距 */
}
.search-bar input { flex: 1; }              /* 输入框占满剩余空间 */
```

`flex: 1` = `flex: 1 1 0`，意思是"放大、缩小、起始尺寸都是 1"，把剩余空间全占了。

### 3.5 Grid 布局（二维）

Grid 是"二维"布局：横竖一起管。

```css
.player-main {
    display: grid;
    grid-template-columns: minmax(320px, 35%) 1fr;   /* 左栏最小 320px、最大 35%，右栏占剩余 */
    gap: 0;                                            /* 列间距 0 */
}
```

`minmax(320px, 35%)` = "至少 320px，至多 35%"。响应式神器。

### 3.6 变量与继承

```css
:root {
    --bg: #f5f0e6;                 /* 定义变量 */
    --accent: #8b6914;
}

body {
    background: var(--bg);         /* 使用变量 */
    color: var(--text-primary);
}

.btn-primary {
    background: var(--accent);
}
```

**好处**：要换主题色只改一处。`var()` 还支持默认值：`var(--undefined, #000)`。

### 3.7 动画

```css
@keyframes fadeUp {                       /* 定义动画关键帧 */
    0%   { opacity: 0; transform: translateY(20px); }
    100% { opacity: 1; transform: translateY(0); }
}

.card {
    opacity: 0;
    animation: fadeUp 0.6s ease 0.2s forwards;
    /*  ↑ 名称  0.6秒 缓动 延迟0.2秒 播放完后保持终态 */
}
```

### 3.8 伪元素

```css
body::after {
    content: 'MUSIC';                     /* 在 body 末尾插入文字 */
    position: fixed; bottom: 24px; right: 32px;
    opacity: 0.025;                       /* 极淡当水印 */
    pointer-events: none;                 /* 不响应鼠标，避免挡点击 */
}
```

`::before` 是开头插入，`::after` 是末尾插入。

---

## 4. JavaScript — 网页的行为

JS = 浏览器执行的脚本语言。本节只讲本项目用到的部分。

### 4.1 数据类型与变量

```js
const a = 10;              // 常量，定义后不能改
let b = 'hello';           // 变量，可改
var c = true;              // 老写法，现在基本不用

// 数组
const arr = [1, 2, 3];
arr.push(4);               // [1, 2, 3, 4]
arr.map(x => x * 2);       // [2, 4, 6, 8]
arr.filter(x => x > 2);    // [3, 4]

// 对象
const user = { name: 'Tom', age: 18 };
user.name;                 // 'Tom'
user['name'];              // 'Tom'（方括号可以用变量）
```

### 4.2 函数

```js
// 传统
function add(a, b) {
    return a + b;
}

// 箭头函数
const add = (a, b) => a + b;

// 默认参数
function setToken(token, persistent = true) { ... }

// 模板字符串（反引号）
const html = `<h1>${user.name}</h1>`;

// 解构
const { name, age } = user;          // name='Tom', age=18
const [first, second] = [1, 2, 3];   // first=1, second=2
```

### 4.3 异步：Promise / async await

JS 是单线程的（一次只做一件事）。但网络请求可能要等 1 秒，怎么办？

**Promise**：表示"未来会完成的操作"。

```js
const p = fetch('/api/music');        // 立刻返回一个 Promise
p.then(res => res.json())             // 1 秒后：拿到响应 → 解析 JSON
 .then(data => console.log(data))     // 拿到数据
 .catch(err => console.error(err));   // 出错时
```

**async/await**：把上面写成"看起来同步"的代码：

```js
async function loadList() {
    try {
        const res = await get('/api/music/list');     // 等待
        const data = res.data;                         // 直接用
        console.log(data);
    } catch (e) {
        console.error('出错:', e);
    }
}
```

`await` 只能在 `async` 函数里用。`async` 函数返回 `Promise`。

### 4.4 DOM 操作

DOM = 浏览器把 HTML 变成的 JS 对象树。

```js
// 查元素
const el = document.getElementById('keyword');          // 按 id 找
const list = document.querySelectorAll('.track');      // 按选择器找（多个）
const first = document.querySelector('.track');         // 第一个

// 改内容
el.textContent = 'hello';                               // 纯文本（安全，不会 XSS）
el.innerHTML = '<b>hello</b>';                          // HTML（要小心 XSS）

// 改属性
el.setAttribute('disabled', '');
el.classList.add('active');                             // 加 class
el.classList.remove('hidden');
el.classList.toggle('active');                          // 切换

// 改样式
el.style.color = 'red';

// 增删
const newEl = document.createElement('div');
parentEl.appendChild(newEl);                            // 加到末尾
newEl.remove();                                         // 删
```

### 4.5 事件

```js
// 方式 1：HTML 里 onclick="search()"
<button onclick="search()">

// 方式 2：JS 里 addEventListener
button.addEventListener('click', function(e) {
    console.log('被点击了', e.target);
});

// 方式 3：事件委托（推荐）
// 在祖先元素监听一次，处理所有后代
document.addEventListener('click', function(e) {
    if (e.target.id === 'logoutBtn') logout();
    if (e.target.matches('button[data-action="delete"]')) {
        const id = e.target.dataset.id;
        deleteItem(id);
    }
});
```

**事件委托的优势**：动态生成的元素也能响应（不用每生成一个就绑事件）。

### 4.6 fetch（HTTP 请求）

```js
// GET
const res = await fetch('/api/music/list');
const data = await res.json();

// POST JSON
await fetch('/api/comment', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ content: '好听' })
});

// POST FormData（文件上传）
const fd = new FormData();
fd.append('file', fileInput.files[0]);
await fetch('/api/music/upload', { method: 'POST', body: fd });
// 注意：FormData 不能手动设 Content-Type，浏览器会自动加 boundary
```

### 4.7 本地存储

```js
localStorage.setItem('token', 'xxx');       // 关浏览器还在
localStorage.getItem('token');
sessionStorage.setItem('token', 'xxx');     // 关标签页就清
```

---

## 5. 本项目怎么把这些基础串起来？

| 基础 | 本项目用在哪 |
|---|---|
| HTML 标签 | `static/*.html` 7 个页面 |
| CSS 选择器 / 盒模型 / flex / grid | `static/css/*.css` 4 个文件 |
| 函数 / 异步 / DOM / 事件 | `static/js/common.js`、`api.js`、`pages/*.js` |
| fetch | `static/js/api.js`（封装 get/post/put/del/postForm） |
| localStorage | `static/js/common.js`（存 token / username / role） |

**最大特点**：因为没有框架，所以**没有 import / require**。所有 JS 文件按顺序加载到 `<script>` 标签里，函数都挂在全局（`window`）上，谁都能调。

这听起来"土"，但对于**学习目的**反而更好：你打开 DevTools 的 Sources 面板，**看到的就是浏览器真正跑的代码**，没有任何"魔法"。

---

# 下篇：本项目源码导读

## 6. 项目文件结构

```
static/
├── css/                          4 个 CSS 文件
│   ├── base.css                  变量 / reset / 动画
│   ├── layout.css                navbar / container / 遮罩
│   ├── components.css            btn / card / form / toast / table ...
│   └── player.css                详情页播放器 + 歌词
│
├── js/                           JS 模块
│   ├── common.js                 ⭐ 全局：token / toast / 格式化 / navbar / loading
│   ├── api.js                    ⭐ 全局：get / post / put / del / postForm
│   ├── forbidden-words.js        评论敏感词预检
│   ├── app.js                    ⭐ 启动入口（最后加载）
│   └── pages/                    当前页业务
│       ├── index.js              首页：列表 + 搜索 + 分页
│       ├── login.js              登录
│       ├── register.js           注册
│       ├── detail.js             详情：播放 / 点赞 / 评论 / 歌词
│       ├── upload.js             上传
│       ├── ranking.js            排行榜
│       ├── bilibili.js           B 站下载
│       └── admin/
│           ├── users.js          用户管理
│           └── music.js          音乐管理
│
├── admin/                        2 个管理页 HTML
│   ├── users.html
│   └── music.html
│
├── index.html / login.html / register.html
├── detail.html / upload.html / ranking.html / bilibili.html
└── video/logo.png
```

**⭐ 是核心模块**。其他页面脚本都依赖它们。

---

## 7. CSS 四层结构

按"依赖关系"分四层，每层用一段注释说明用途。

### 7.1 `base.css` — 基础（变量、reset、动画）

```css
:root {
    --bg: #f5f0e6;                    /* 米色背景 */
    --surface: #faf8f5;               /* 卡片白色 */
    --accent: #8b6914;                /* 主色：琥珀金 */
    --text-primary: #2c2821;          /* 主文字：墨色 */
    --radius: 8px;
    --shadow: 0 2px 20px rgba(44, 40, 33, 0.06);
}

* { margin: 0; padding: 0; box-sizing: border-box; }

body {
    background-color: var(--bg);
    /* 内嵌 SVG 噪点纹理，做出纸质感 */
    background-image: url("data:image/svg+xml,...");
    font-family: 'Noto Serif SC', serif;
    min-height: 100vh;
}

/* 关键帧：被多个组件复用的动画 */
@keyframes shimmer { ... }   /* 骨架屏闪动 */
@keyframes fadeUp { ... }    /* 卡片淡入 */
@keyframes slideDown { ... } /* toast 滑下 */
```

**学习点**：

- 变量集中在 `:root`，换主题只改一处
- `box-sizing: border-box` 让宽高计算更直观
- 内嵌 SVG 做纹理（`data:image/svg+xml,...`）省一次 HTTP 请求

### 7.2 `layout.css` — 布局

```css
.navbar {
    background: var(--surface);
    height: 56px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    position: sticky;          /* 滚动时钉在顶部 */
    top: 0;
    z-index: 100;
}

.container {
    max-width: 800px;
    margin: 0 auto;
    padding: 56px 24px 100px;  /* 上 56 = navbar 高；下 100 留底部空间 */
}

.container-fluid {
    width: 100%;               /* 详情页用这个，绕开 800px 限制 */
}

.loading-overlay {
    position: fixed; inset: 0; /* 铺满全屏 */
    background: var(--bg);
    z-index: 10000;
    transition: opacity 0.6s;
}
.loading-overlay.fade-out {
    opacity: 0;
    pointer-events: none;      /* 淡出后不挡点击 */
}
```

**学习点**：

- `position: sticky` 比 `fixed` 智能：滚到顶才粘住
- `inset: 0` = `top:0; right:0; bottom:0; left:0`（CSS 简写）
- `pointer-events: none` 让元素"还在但点不到"，淡出时不挡操作

### 7.3 `components.css` — 通用组件

#### 按钮（BEM 风格变体）

```css
.btn { padding: 9px 22px; border-radius: var(--radius); cursor: pointer; }
.btn-primary { background: var(--text-primary); color: #fff; }
.btn-danger  { background: #c0392b; color: #fff; }
.btn-sm      { padding: 5px 14px; font-size: 12px; }
```

用法：`<button class="btn btn-primary btn-sm">确定</button>`

#### 卡片

```css
.card {
    background: var(--surface);
    border-radius: var(--radius);
    padding: 32px;
    box-shadow: var(--shadow);
    opacity: 0;
    animation: fadeUp 0.6s ease 0.2s forwards;
    /* ↑ forwards 让动画结束后保持终态（opacity=1） */
}
```

#### Toast 提示

```css
.toast {
    position: fixed;
    top: 72px; left: 50%;
    transform: translateX(-50%);
    animation: slideDown 0.3s ease;
    z-index: 9999;
}
.toast.success { background: var(--accent); }
.toast.error   { background: #c0392b; }
```

#### 骨架屏

```css
.skeleton {
    background: linear-gradient(90deg, #eee 25%, #f5f5f5 50%, #eee 75%);
    background-size: 400px 100%;
    animation: shimmer 1.5s infinite;
}
.hidden { display: none !important; }
```

**学习点**：

- `linear-gradient` + `background-size` + 动画 = 经典骨架屏效果
- 加载时显示骨架，数据回来后 `addClass('hidden')` 隐藏

### 7.4 `player.css` — 详情页专用

#### 主区 grid 布局

```css
.player-main {
    display: grid;
    grid-template-columns: minmax(320px, 35%) 1fr;
    /*                    ↑ 最小 320、最大 35%  ↑ 剩余 */
    border-radius: 12px 12px 0 0;
}
```

#### 歌词视口的渐隐效果

```css
.lyrics-viewport {
    -webkit-mask-image: linear-gradient(to bottom,
        transparent 0, #000 80px,
        #000 calc(100% - 80px), transparent 100%);
    /* ↑ 顶部 80px 渐隐 → 中间实色 → 底部 80px 渐隐 */
}
```

**学习点**：纯 CSS 实现"内容上下渐隐"，无需 JS 或图片。

#### 歌词高亮

```css
.lyrics-line.active {
    color: var(--accent);
    font-weight: 600;
    font-size: 1.1rem;
    transform: scale(1.03);  /* 略微放大 */
    transition: all 0.3s ease;
}
.lyrics-line.passed {
    color: var(--text-muted);
    opacity: 0.7;
}
```

JS 切 class：`el.classList.toggle('active', i === currentIdx)`。

---

## 8. `common.js` — 全局工具箱

这是**最重要的 JS 文件**，所有页面都依赖它。它没有用 IIFE / 命名空间，直接把函数挂到全局（`window.getToken`）。

### 8.1 认证相关

```js
const TOKEN_KEY    = 'music_token';
const USERNAME_KEY = 'music_username';
const ROLE_KEY     = 'music_role';

function getToken() {
    return localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY);
}

function setToken(token, persistent = true) {
    (persistent ? localStorage : sessionStorage).setItem(TOKEN_KEY, token);
}
```

**为什么两种 storage？**

- `localStorage`：关浏览器再开还在（"记住我"勾选时用）
- `sessionStorage`：关标签页就清（不勾选时用，更安全）

`getToken()` 先查 localStorage 再查 sessionStorage，哪个有就用哪个。

### 8.2 JWT 解码

```js
function parseJwt(token) {
    try {
        const payloadBase64 = token.split('.')[1];   // JWT = 头.载荷.签名
        const base64 = payloadBase64
            .replace(/-/g, '+')                      // Base64URL → Base64
            .replace(/_/g, '/');
        // 字节数组 → UTF-8 字符串（处理中文）
        const jsonStr = decodeURIComponent(
            atob(base64).split('')
                .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
                .join('')
        );
        return JSON.parse(jsonStr);
    } catch (e) {
        return null;
    }
}
```

**这段有点绕**，逐行解释：

1. JWT 是 `header.payload.signature` 三段，`.split('.')[1]` 取中间
2. JWT 用 Base64URL（`-` 和 `_` 代替 `+` 和 `/`），要换回标准 Base64
3. `atob` 解码 Base64 得到的是**字节字符串**（每字符 charCode 0-255），直接 `JSON.parse` 会乱码（中文 3 字节，UTF-8 编码后是 0xE4 0xB8 0xAD 这种）
4. 把每字节转 `%XX`（URL 编码），再用 `decodeURIComponent` 解码为 UTF-8 字符串
5. `JSON.parse` 得到对象

### 8.3 Toast 提示

```js
function showToast(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = 'toast ' + type;
    toast.textContent = message;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 2500);
}
```

**学习点**：

- `document.createElement('div')` 创建一个内存里的元素（不在页面里）
- `.textContent = ...` 设置文字（比 `innerHTML` 安全，防 XSS）
- `appendChild` 把它加到页面（此刻浏览器才开始渲染）
- 2.5 秒后 `remove()` 把自己从 DOM 删掉，自动清理

### 8.4 转义 HTML

```js
function escapeHtml(text) {
    if (text === null || text === undefined) return '';
    const div = document.createElement('div');
    div.textContent = String(text);
    return div.innerHTML;
}
```

**技巧**：用 `textContent` 写入再用 `innerHTML` 读出，浏览器自动转义 `<`、`>`、`&`、`"`。这比自己写正则替换安全。

**为什么必须？** 用户输入的 `<script>alert(1)</script>` 如果直接 `innerHTML` 注入会执行。`escapeHtml` 把 `<` 变 `&lt;`，浏览器就只显示文字，不执行。

### 8.5 导航栏

```js
function renderNavbar() {
    const nav = document.createElement('nav');
    nav.className = 'navbar';
    nav.innerHTML = `<div class="logo">...</div><div class="nav-links">...</div>`;

    const links = nav.querySelector('.nav-links');
    if (isLogined()) {
        // 显示用户名 + 退出 + 管理员链接
        let inner = '<a href="/upload.html">上传音乐</a>...';
        if (getUserRole() === 'admin') {
            inner += '<a href="/admin/users.html">用户管理</a>...';
        }
        inner += `<span>${escapeHtml(getUsername() || '')}</span>`;
        inner += '<button class="btn-logout" id="navLogoutBtn">退出</button>';
        links.insertAdjacentHTML('beforeend', inner);
    } else {
        links.innerHTML += '<a href="/login.html">登录</a><a href="/register.html">注册</a>';
    }
    return nav;
}
```

**注意**：用户名走 `escapeHtml` 转义，防 XSS；硬编码 HTML 是安全的。

### 8.6 Loading 遮罩（细节：最短显示 1.2 秒）

```js
const PAGE_LOAD_TS = performance.now();    // common.js 加载时刻

function fadeOutLoading() {
    const overlay = document.getElementById('loadingOverlay');
    if (!overlay) return;

    const minDuration = 1200;                // 至少显示 1.2 秒
    const elapsed = performance.now() - PAGE_LOAD_TS;
    const remaining = Math.max(0, minDuration - elapsed);

    setTimeout(() => {
        overlay.classList.add('fade-out');
        setTimeout(() => overlay.remove(), 600);
    }, remaining);
}
```

**为什么至少 1.2 秒？** 如果数据 100ms 就回来，遮罩闪一下就消失，用户体验很糟。强制最少 1.2 秒让"加载"的感觉更连贯。CSS 淡出动画是 0.6 秒，总计最多 1.8 秒。

**`performance.now()` 是什么？** 浏览器提供的高精度计时（毫秒小数），比 `Date.now()` 准。

### 8.7 事件委托处理退出按钮

```js
document.addEventListener('click', function (e) {
    if (e.target && e.target.id === 'navLogoutBtn') logout();
});
```

**为什么用事件委托？** `renderNavbar()` 在 DOMContentLoaded 之后才被 `app.js` 调用，生成的 `<button id="navLogoutBtn">` 在它绑定 `onclick` 之前不存在。事件委托绑到 `document` 上，"未来"的元素也能响应。

---

## 9. `api.js` — 跟后端说话的封装

后端所有接口都返回 `Result<T>` = `{code, message, data}`。`api.js` 把 fetch 包成 4 个全局函数。

### 9.1 核心：`request()`

```js
async function request(url, options = {}) {
    const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
    const token = getToken();
    if (token) headers['Authorization'] = 'Bearer ' + token;

    try {
        const response = await fetch(url, { ...options, headers });

        if (response.status === 401) {
            clearToken();
            window.location.href = '/login.html?expired=1';
            return;
        }

        return await response.json();
    } catch (error) {
        console.error('请求失败:', error);
        showToast('网络请求失败，请稍后重试', 'error');
        throw error;
    }
}
```

**学习点**：

- `...options.headers || {}` 展开语法（ES6）：合并对象
- 自动从 `common.js` 读 token，塞到 `Authorization` 头
- 401 = token 过期或无效，自动清登录态跳登录页
- 网络异常统一 toast 提示，业务代码不需重复处理

### 9.2 4 个便捷函数

```js
function get(url)           { return request(url, { method: 'GET' }); }
function post(url, body)    { return request(url, { method: 'POST', body: JSON.stringify(body) }); }
function put(url, body)     { return request(url, { method: 'PUT',  body: JSON.stringify(body) }); }
function del(url)           { return request(url, { method: 'DELETE' }); }
```

### 9.3 文件上传：`postForm()`

```js
function postForm(url, formData) {
    const headers = {};
    const token = getToken();
    if (token) headers['Authorization'] = 'Bearer ' + token;

    return fetch(url, { method: 'POST', headers, body: formData }).then(async response => {
        if (response.status === 401) {
            clearToken();
            window.location.href = '/login.html?expired=1';
            return;
        }
        return response.json();
    });
}
```

**为什么单独写？** `FormData` 上传时浏览器**自动**加 `Content-Type: multipart/form-data; boundary=xxx`。如果手动设了 `Content-Type: application/json`，boundary 没了，文件解析失败。所以**不能**复用 `request()` 那个默认 header。

### 9.4 业务侧使用示例

```js
// 列表（GET）
const res = await get('/api/music/list?page=1&size=12');
if (res.code === 200) renderList(res.data.list);

// 评论（POST）
await post('/api/music/' + musicId + '/comments', { content: '好听' });

// 启用/禁用用户（PUT）
await put('/api/admin/users/' + id + '/status', { enabled: false });

// 删除评论（DELETE）
await del('/api/comments/' + commentId);

// 上传音乐（POST FormData）
const fd = new FormData();
fd.append('file', fileInput.files[0]);
fd.append('title', '夜曲');
fd.append('artist', '周杰伦');
await postForm('/api/music/upload', fd);
```

**特点**：函数名简短（一目了然是 CRUD）、自动处理 token、自动处理 401。

---

## 10. `app.js` — 应用启动

```js
window.addEventListener('DOMContentLoaded', function () {
    if (!document.body.classList.contains('auth-page')) {
        const navbar = renderNavbar();
        document.body.insertBefore(navbar, document.body.firstChild);
    }
    fadeOutLoading();
});
```

**总共 18 行**，做的事：

1. 等 DOM 解析完（`DOMContentLoaded`）
2. 登录/注册页（`<body class="auth-page">`）不插导航栏
3. 其他页：调用 `common.js` 的 `renderNavbar()` 生成导航栏，插到 `body` 最前面
4. 调用 `fadeOutLoading()` 淡出遮罩

**为什么 `app.js` 最后加载？** 让前面各页的 `DOMContentLoaded` 监听器先注册，最后跑 `app.js` 的监听器时，**业务代码的 init 已经发起 API 调用**，导航栏插入和遮罩淡出是"后置收尾"，视觉上不会出现"导航栏先空着"。

**`<body class="auth-page">` 怎么加？** 在 `login.html` / `register.html` 的 `<body>` 标签上：

```html
<body class="auth-page">
    <div class="loading-overlay" id="loadingOverlay">...</div>
    <div class="auth-container">
        <h1>登录</h1>
        ...
    </div>
</body>
```

---

## 11. `pages/index.js` — 列表页（最常用的列表模式）

这是**最常见的前端业务模式**：发请求 → 拿数据 → 渲染列表 → 处理分页。

### 11.1 入口

```js
document.addEventListener('DOMContentLoaded', init);

function init() {
    document.getElementById('keyword').addEventListener('keydown', e => {
        if (e.key === 'Enter') search();        // 回车搜索
    });
    handleAutoLogin().then(() => loadList(1, ''));  // 验证 token → 加载第 1 页
}
```

### 11.2 加载列表

```js
async function loadList(page, keyword) {
    currentPage = page;
    currentKeyword = keyword || '';
    let url = `/api/music/list?page=${page}&size=${PAGE_SIZE}`;
    if (currentKeyword) url += `&keyword=${encodeURIComponent(currentKeyword)}`;

    const skeleton = document.getElementById('listSkeleton');
    if (skeleton) skeleton.classList.remove('hidden');   // 显示骨架屏

    const res = await get(url);
    if (!res || res.code !== 200) {
        if (skeleton) skeleton.classList.add('hidden');
        return;
    }

    if (skeleton) skeleton.classList.add('hidden');     // 隐藏骨架屏
    renderList(res.data.list);                          // 渲染列表
    renderPagination(res.data.total, page);             // 渲染分页
}
```

**学习点**：

- `encodeURIComponent` 编码关键词（防"中文/特殊字符"破坏 URL）
- **乐观 UX**：先显示骨架屏（让用户知道在加载），数据回来再换真实内容
- 接口失败的兜底：`if (!res) return` 防止 401 跳转时 `res` 是 undefined

### 11.3 渲染列表

```js
function renderList(list) {
    const container = document.getElementById('trackList');
    if (!list || list.length === 0) {
        container.innerHTML = '<div class="tip">暂无音乐</div>';
        return;
    }

    container.innerHTML = list.map((item, index) => {
        const realIndex = (currentPage - 1) * PAGE_SIZE + index + 1;
        return `
            <div class="track" onclick="location.href='/detail.html?id=${item.id}'">
                <span class="track-index">${String(realIndex).padStart(2, '0')}</span>
                <div class="track-cover">
                    ${item.coverPath
                        ? `<img src="/api/music/cover/${item.coverPath}" loading="lazy">`
                        : `<div class="placeholder">♪</div>`}
                </div>
                <div class="track-meta">
                    <div class="track-title">${escapeHtml(item.title)}</div>
                    <div class="track-artist">${escapeHtml(item.artist)}</div>
                </div>
                <button class="track-play" onclick="event.stopPropagation();location.href='/detail.html?id=${item.id}'">
                    <svg viewBox="0 0 24 24"><polygon points="8,5 20,12 8,19"/></svg>
                </button>
            </div>
        `;
    }).join('');
}
```

**学习点**：

- `.map(...).join('')`：把数组转成一段 HTML 字符串（最常用的"伪模板引擎"）
- `String(realIndex).padStart(2, '0')`：把 `3` 变 `'03'`（序号补零）
- `escapeHtml(item.title)`：用户输入必须转义！
- `loading="lazy"`：图片懒加载（视口外的图不下载，省流量）
- `event.stopPropagation()`：阻止按钮点击冒泡到外层 `.track` 的 onclick（不然点按钮会跳两次）

### 11.4 渲染分页

```js
function renderPagination(total, page) {
    const totalPages = Math.ceil(total / PAGE_SIZE);
    const container = document.getElementById('pagination');
    if (totalPages <= 1) { container.innerHTML = ''; return; }   // 只有 1 页就不显示

    let html = `<button class="page-arrow" ${page <= 1 ? 'disabled' : ''} onclick="goPage(${page - 1})">&lsaquo;</button>`;
    for (let i = 1; i <= totalPages; i++) {
        html += `<button class="page-btn ${i === page ? 'active' : ''}" onclick="goPage(${i})">${i}</button>`;
    }
    html += `<button class="page-arrow" ${page >= totalPages ? 'disabled' : ''} onclick="goPage(${page + 1})">&rsaquo;</button>`;
    container.innerHTML = html;
}
```

**学习点**：

- `&lsaquo;` `&rsaquo;` 是 HTML 实体，`‹` `›` 漂亮的箭头
- `${... ? '...' : ''}` 三元表达式直接给属性
- `class="page-btn active"` 标记当前页（CSS 里 `.active` 高亮）

### 11.5 搜索

```js
function search() {
    loadList(1, document.getElementById('keyword').value.trim());
}

function goPage(page) {
    if (page < 1) return;
    loadList(page, currentKeyword);
    window.scrollTo({ top: 0, behavior: 'smooth' });   // 翻页后滚回顶部
}
```

---

## 12. `pages/detail.js` — 详情页（重点：局部更新 + 歌词同步）

详情页是项目里**最复杂**的前端页面，演示了三个关键技术：

1. **不刷新整页**的点赞/评论（保护当前播放）
2. **LRC 歌词解析 + 时间同步高亮**
3. **流式音频下载**（Blob + 临时 URL）

### 12.1 加载流程

```js
const musicId = new URLSearchParams(window.location.search).get('id');

async function loadDetail() {
    if (!musicId) { showDetailError('未找到音乐'); return; }

    // 解析 token 拿当前用户 ID（用于显示"我的评论"删除按钮）
    const token = getToken();
    if (token) {
        const payload = parseJwt(token);
        if (payload && payload.sub) currentUserId = parseInt(payload.sub);
    }

    const res = await get('/api/music/' + musicId);
    if (!res || res.code !== 200) { showDetailError('音乐不存在'); return; }
    document.getElementById('detailSkeleton').classList.add('hidden');

    musicData = res.data.music;
    renderDetail(musicData, res.data.comments || []);
    if (isLogined()) checkLikeStatus();
    loadLyrics();
}
```

**学习点**：

- `URLSearchParams` 是浏览器内置的 URL 参数解析器
- `parseInt(payload.sub)`：JWT 的 `sub` 字段是字符串，要转数字

### 12.2 LRC 歌词解析（重点算法）

```js
function parseLRC(lrcText) {
    if (!lrcText) return [];
    const lines = lrcText.split('\n');
    const result = [];
    const timeRegex = /\[(\d{2}):(\d{2})\.(\d{2,3})\]/g;

    for (const line of lines) {
        const times = [];
        let match;
        while ((match = timeRegex.exec(line)) !== null) {
            const min = parseInt(match[1]);
            const sec = parseInt(match[2]);
            const ms = parseInt(match[3].padEnd(3, '0'));  // 补齐 3 位
            times.push(min * 60 + sec + ms / 1000);
        }
        const text = line.replace(/\[\d{2}:\d{2}\.\d{2,3}\]/g, '').trim();
        if (times.length > 0 && text) {
            for (const t of times) result.push({ time: t, text });
        }
    }
    return result.sort((a, b) => a.time - b.time);
}
```

**LRC 格式示例**：

```
[00:00.00] 标题：夜曲
[00:01.50] 一群嗜血的蚂蚁
[00:04.20] 被风吹进旋律里
[00:32.50] 这里的每一行
[00:35.00] (重复一句)代表我爱你
```

**算法**：

1. 按行切分
2. 每行用正则提取所有时间戳 `[mm:ss.ms]`
3. 去掉时间戳，剩余是歌词文本
4. 一行歌词可能对应多个时间（LRC 复读），展开成多条
5. 按时间排序

**正则解释**：`\[(\d{2}):(\d{2})\.(\d{2,3})\]` 匹配 `[01:23.456]` 这种格式，捕获分组是分、秒、毫秒。

### 12.3 歌词高亮同步

```js
function updateLyricsHighlight(currentTime) {
    if (parsedLyrics.length === 0) return;
    let newIndex = -1;
    // 倒序找：最后一个 time <= currentTime 的
    for (let i = parsedLyrics.length - 1; i >= 0; i--) {
        if (currentTime >= parsedLyrics[i].time) {
            newIndex = i;
            break;
        }
    }
    if (newIndex === currentLyricIndex) return;     // 没变就跳过
    currentLyricIndex = newIndex;

    const lines = document.querySelectorAll('.lyrics-line');
    lines.forEach((line, i) => {
        line.classList.toggle('active', i === newIndex);
        line.classList.toggle('passed', i < newIndex);
    });

    // 滚动到"略偏上"位置（约 40% 高度），让当前行成为视觉焦点
    const line = lines[newIndex];
    const containerH = container.clientHeight;
    const lineH = line.offsetHeight;
    const target = line.offsetTop + lineH / 2 - containerH * 0.4;
    container.scrollTo({ top: target, behavior: 'smooth' });
}
```

**触发**：`audio.addEventListener('timeupdate', () => updateLyricsHighlight(audio.currentTime))`

`timeupdate` 事件在播放时每 250ms 触发一次（HTML5 标准）。

**学习点**：

- 倒序找比正序快（歌词越长越明显，因为大多数时间点都在末尾之后）
- `newIndex === currentLyricIndex` 提前 return：减少无意义的 DOM 操作
- `classList.toggle(class, condition)`：condition 为 true 加 class，否则移除
- 滚动到 40% 而非 50%，给上方留更多"已唱过"内容，下方留"将唱"内容

### 12.4 点赞的"局部更新"（不刷新整页）

```js
async function toggleLike() {
    if (!isLogined()) { showToast('请先登录', 'error'); return; }
    const btn = document.getElementById('likeBtn');
    if (btn) { btn.disabled = true; btn.textContent = '处理中...'; }

    const res = await post('/api/music/' + musicId + '/like');
    if (btn) { btn.disabled = false; }

    if (res && res.code === 200) {
        liked = !!res.data.liked;
        updateLikeButton();
        // 只更新点赞数这一处文字，不 reload
        if (musicData && typeof res.data.likeCount === 'number') {
            musicData.likeCount = res.data.likeCount;
        }
        const stat = document.getElementById('likeCountStat');
        if (stat) stat.textContent = '♥ ' + musicData.likeCount;
        showToast(liked ? '点赞成功' : '已取消点赞', 'success');
    } else {
        showToast((res && res.message) || '操作失败', 'error');
    }
}
```

**为什么这么重要？** 整页 reload 会让 `<audio>` 重新加载，**正在播放的音乐会从头开始**。局部更新只改 `textContent`，音频不中断，体验极好。

### 12.5 评论的"局部增删"

```js
function appendComment(c) {
    const list = document.getElementById('commentList');
    const emptyTip = list.querySelector('.tip');
    if (emptyTip) emptyTip.remove();   // 删"暂无评论"占位
    list.insertAdjacentHTML('beforeend', renderCommentItem(c, getUserRole() === 'admin'));
    updateCommentsHeader();            // 同步"评论 (N)" 计数
}

async function deleteComment(commentId, btn) {
    if (!confirm('确定删除这条评论吗？')) return;
    const res = await del('/api/comments/' + commentId);
    if (res && res.code === 200) {
        // 局部移除 li
        const li = btn.closest('.comment-item');
        if (li) li.remove();
        updateCommentsHeader();
    }
}
```

**学习点**：

- `insertAdjacentHTML('beforeend', html)`：比 `innerHTML +=` 快（不重新解析整个列表）
- `btn.closest('.comment-item')`：找最近的祖先元素，拿到对应 li
- 列表为空时再加回"暂无评论"占位（`list.querySelectorAll('.comment-item').length === 0`）

### 12.6 流式下载（Blob 临时 URL）

```js
async function downloadMusic() {
    const token = getToken();
    const response = await fetch('/api/music/' + musicId + '/download', {
        headers: { 'Authorization': 'Bearer ' + token }
    });
    if (!response.ok) { showToast('下载失败', 'error'); return; }

    const blob = await response.blob();         // 转成二进制大对象
    const url = URL.createObjectURL(blob);      // 生成"临时 URL"
    const a = document.createElement('a');
    a.href = url;
    a.download = musicData.title + '.mp3';      // 下载文件名
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);                   // 释放内存
}
```

**学习点**：

- `URL.createObjectURL(blob)`：把内存里的 Blob 变成一个可下载的 URL
- 用完一定要 `revokeObjectURL` 释放，不然内存泄漏
- `a.click()` 模拟点击触发下载，浏览器看到 `download` 属性就保存为文件而不是打开

---

## 13. `pages/admin/users.js` — 管理后台（事件委托 + 乐观更新）

后台有两个关键技术：**事件委托**（动态生成的按钮也能响应）和**乐观更新**（操作立刻生效，失败再回滚）。

### 13.1 守卫模式

```js
function guard() {
    if (getUserRole() !== 'admin') {
        showToast('无权访问', 'error');
        setTimeout(() => { window.location.href = '/'; }, 1000);
        return false;
    }
    return true;
}

function init() { if (guard()) loadAdminUsers(); }
```

**学习点**：先检查权限，通过了才执行主逻辑。虽然后端 Spring Security 也会校验，但前端先拦一道体验更好（少一次请求）。

### 13.2 事件委托

```js
document.addEventListener('click', function (e) {
    const btn = e.target.closest('button[data-action="toggle-status"]');
    if (!btn) return;
    const id = parseInt(btn.dataset.id);
    const enabled = btn.dataset.nextEnabled === 'true';
    toggleUserStatus(id, enabled, btn);
});
```

**为什么用委托？** 列表是 `loadAdminUsers()` 后用 `innerHTML` 动态渲染的，传统的 `btn.onclick = ...` 在渲染前绑定时按钮还不存在。事件委托绑到 `document`，"未来"的元素也能响应。

**`e.target.closest(selector)`**：从触发元素向上找最近的匹配祖先。

**`btn.dataset.xxx`**：读 `data-xxx` 属性（HTML5 的"自定义数据"标准）。

### 13.3 乐观更新

```js
async function toggleUserStatus(id, enabled, btn) {
    if (!confirm(`确定${enabled ? '启用' : '禁用'}该用户吗？`)) return;
    btn.disabled = true;
    btn.textContent = (enabled ? '启用' : '禁用') + '中...';

    const res = await put('/api/admin/users/' + id + '/status', { enabled });

    if (res && res.code === 200) {
        showToast('操作成功', 'success');
        updateUserRowStatus(id, enabled);    // 立即更新该行
        loadAdminUsers().catch(() => {});    // 后台静默重拉一次，确保与服务端一致
    } else {
        // 失败回滚（按钮恢复可点）
        btn.disabled = false;
        btn.textContent = enabled ? '启用' : '禁用';
        showToast(res.message || '操作失败', 'error');
    }
}
```

**学习点**：

- **乐观更新**：不等后端返回就更新 UI（这里其实没改 DOM 状态，而是先等后端成功再改；更激进的方案是改 DOM 然后失败再回滚）
- **后台静默 refresh**：成功后异步重拉一次完整列表，`.catch(() => {})` 静默吞错避免打扰
- 失败时把按钮 disabled / textContent 恢复原样

---

## 14. `forbidden-words.js` — 敏感词预检（一个完整的"小算法"例子）

评论要过滤黄色/暴力/侮辱性词汇。前端先粗检（省一次网络往返），后端用 DFA 库权威拦截。

### 14.1 词表

```js
const FORBIDDEN_WORDS = [
    '傻逼', '煞笔', '智障', '脑残', ...     // 60+ 词，覆盖侮辱/暴力/色情
];
```

**注意**：词表是**子集**。完整 6W+ 词在服务端 `sensitive-word` 库里。客户端只做"明显的"拦截，真正的安全防线在服务端。

### 14.2 归一化

```js
function normalizeForCheck(text) {
    if (!text) return '';
    return text.toLowerCase()
        .replace(/[\s\*\·\.\-\_\~!?,，。；;：:、\/\\|@#\$%\^&\(\)\[\]\{\}【】《》"'`~·•]/g, '');
}
```

**为什么要归一化？** 用户可能输入：

- `S h i t`（加空格）
- `S-H-I-T`（加连字符）
- `S,H,I,T`（加逗号）

归一化后都是 `shit`，命中词表。

### 14.3 检测

```js
function containsForbiddenWord(text) {
    if (!text) return false;
    const normalized = normalizeForCheck(text);
    return FORBIDDEN_WORDS.some(w => normalized.indexOf(w) !== -1);
}
```

**学习点**：

- `.some(callback)`：数组有一个满足条件就返回 true
- `indexOf(w) !== -1`：检查子串是否存在（朴素匹配，O(n×m)）
- 词表 60 条 + 短评论用朴素匹配够用；服务端上百万条才需要 DFA 树

### 14.4 在 `submitComment` 里调用

```js
if (containsForbiddenWord(content)) {
    showToast('评论包含不当内容，请修改后重试', 'error');
    return;
}
```

`detail.html` 加载顺序：common → api → **forbidden-words** → pages/detail → app。

---

## 15. 整体架构总结

### 15.1 文件组织

```
HTML（结构）    → 7 个页面 HTML + 2 个 admin HTML
CSS（样式）     → base / layout / components / player（按依赖顺序层叠覆盖）
JS（行为）      → common / api / [forbidden-words] / pages / app
                 ↑                              ↑
                 工具/封装                       当前页业务
                         ↑
                         app.js 最后跑（插 navbar + 淡出遮罩）
```

### 15.2 单次点击的完整链路

以"首页点一首歌"为例：

```
1. 用户点击 .track
2. onclick 触发 location.href = '/detail.html?id=24'
3. 浏览器加载 detail.html
4. 按顺序加载 common.js / api.js / forbidden-words.js / detail.js / app.js
5. common.js 执行：定义 PAGE_LOAD_TS = performance.now()
6. detail.js 注册 DOMContentLoaded → 调 loadDetail()
7. app.js 注册 DOMContentLoaded → 调 renderNavbar() + fadeOutLoading()
8. DOMContentLoaded 触发：
   a. detail.js 的 loadDetail() 先跑：发 GET /api/music/24
   b. app.js 后跑：插入导航栏 + 淡出遮罩（强制至少 1.2 秒）
9. 后端返回数据 → renderDetail() 渲染页面
10. 播放页加载完成
```

### 15.3 业务逻辑核心循环

```
HTML 的 onclick（如 onclick="toggleLike()"）
  → 全局函数 toggleLike()
    → get / post / put / del / postForm   (api.js 自动加 Authorization / 处理 401)
      → fetch → Spring Boot 后端
    ← Result<T> {code, message, data}
  → showToast() 提示
  → renderXxx() 局部更新 DOM
```

### 15.4 鉴权流

```
登录：POST /api/auth/login → 后端签 JWT → 存 localStorage
  ↓
后续请求：api.js 自动读 token → 加 Authorization: Bearer xxx
  ↓
后端：JwtAuthFilter 解析 → SecurityContext 注入 userId/role
  ↓
Controller：@AuthenticationPrincipal 或 Authentication 参数拿到 userId
  ↓
Service：执行业务（点赞 / 评论 / 删除自己 / 等等）
  ↓
Spring Security：hasRole('ADMIN') 校验
  ↓
异常 → GlobalExceptionHandler → Result.fail(msg) → 前端 toast
```

---

# 附录

## 调试技巧：Chrome DevTools

按 F12 打开。

| 面板 | 用途 |
|---|---|
| **Elements** | 看 DOM 树、CSS 实时改（不改源码） |
| **Console** | 跑 JS（`getToken()` 试一下），看 console.log |
| **Sources** | 打断点（点行号），看变量、单步执行 |
| **Network** | 看 HTTP 请求（Headers / Response / Timing） |
| **Application** | 看 localStorage / sessionStorage / Cookie |

**最常用的调试技巧**：

1. `console.log(musicData)` 在 Sources 里打断点看变量
2. Network 面板看哪个请求慢、哪个 404
3. Elements 面板右键 "Copy → Copy selector" 拿到选择器

---

## 扩展阅读

**HTML / CSS**：
- [MDN HTML 元素](https://developer.mozilla.org/zh-CN/docs/Web/HTML/Element)
- [Flexbox 完整指南](https://css-tricks.com/snippets/css/a-guide-to-flexbox/)
- [Grid 完整指南](https://css-tricks.com/snippets/css/complete-guide-grid/)

**JavaScript**：
- [MDN JavaScript 指南](https://developer.mozilla.org/zh-CN/docs/Web/JavaScript/Guide)
- [JavaScript Promises 详解](https://developer.mozilla.org/zh-CN/docs/Web/JavaScript/Reference/Global_Objects/Promise)
- [async/await 教程](https://developer.mozilla.org/zh-CN/docs/Learn/JavaScript/Asynchronous/Async_await)

**HTTP**：
- [RESTful API 设计指南](https://restfulapi.net/)
- [HTTP 状态码](https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Status)

**项目进阶**（学完本项目可以挑战）：
- 引入 Vue/React 重写
- 用 Webpack / Vite 打包
- TypeScript 加类型
- Pinia / Redux 状态管理
- WebSocket 实时评论

— END —
