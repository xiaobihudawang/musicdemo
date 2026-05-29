# listen1-api 使用说明

## 概述

listen1-api 是一个聚合六大音乐平台（网易云、QQ音乐、虾米、酷狗、酷我、B站）的统一 API 库，支持获取歌单、搜索歌曲、获取歌词和播放地址。

---

## 安装

### 方式一：本地构建

```bash
git clone <仓库地址>
cd listen1-api
npm install
npm run build
```

构建后在 `dist/` 目录生成：
- `listen1-api.js` — 开发版（未压缩，1.85 MB）
- `listen1-api.min.js` — 生产版（压缩，893 KB）

### 方式二：Git 依赖（在你的项目中使用）

在你的 `package.json` 中添加：

```json
{
  "dependencies": {
    "listen1-api": "github:listen1/listen1-api"
  }
}
```

然后：

```bash
npm install
```

---

## 快速开始

### Node.js 环境

```js
const listen1Api = require('./dist/listen1-api.min.js');

// 初始化 Node.js HTTP 客户端和 Cookie 管理器
listen1Api.loadNodejsDefaults();

async function main() {
  // 获取网易云热门歌单
  const data = await listen1Api.apiGet('/show_playlist?source=netease');
  console.log(data.result);
}

main();
```

### 浏览器环境（Chrome 扩展 / Electron）

```html
<script src="listen1-api.min.js"></script>
<script>
  // 浏览器环境下会自动加载平台适配，无需手动初始化
  listen1Api.apiGet('/show_playlist?source=netease').then(data => {
    console.log(data.result);
  });
</script>
```

---

## API 详细说明

所有请求统一通过 `listen1Api.apiGet(url)` 调用，返回 `Promise<JSON>`。

---

### 1. 获取热门歌单

**请求**

```
/show_playlist?source=<platform>&offset=<number>
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| source | string | 是 | 平台名称（见下方平台列表） |
| offset | number | 否 | 偏移量，用于分页（默认 0） |

**返回**

```json
{
  "result": [
    {
      "cover_img_url": "https://...",
      "title": "歌单标题",
      "id": "neplaylist_762840531",
      "source_url": "http://music.163.com/#/playlist?id=762840531"
    }
  ]
}
```

**示例**

```js
// 获取网易云热门歌单（第一页）
const data = await listen1Api.apiGet('/show_playlist?source=netease');

// 获取 QQ 音乐热门歌单（第二页，偏移 50）
const data = await listen1Api.apiGet('/show_playlist?source=qq&offset=50');
```

---

### 2. 获取歌单 / 专辑 / 歌手详情

**请求**

```
/playlist?list_id=<item_id>
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| list_id | string | 是 | 歌单/专辑/歌手 ID（含前缀） |

**返回**

```json
{
  "info": {
    "cover_img_url": "https://...",
    "title": "歌单标题",
    "id": "neplaylist_762840531",
    "source_url": "http://music.163.com/#/playlist?id=762840531"
  },
  "tracks": [
    {
      "id": "netrack_25642119",
      "title": "歌曲名",
      "artist": "歌手名",
      "artist_id": "neartist_31226",
      "album": "专辑名",
      "album_id": "nealbum_501208",
      "source": "netease",
      "source_url": "http://music.163.com/#/song?id=25642119",
      "img_url": "https://...",
      "url": "netrack_25642119",
      "disabled": false
    }
  ]
}
```

**示例**

```js
// 获取歌单详情
const data = await listen1Api.apiGet('/playlist?list_id=neplaylist_762840531');

// 获取歌手热门歌曲
const data = await listen1Api.apiGet('/playlist?list_id=neartist_31226');

// 获取专辑歌曲
const data = await listen1Api.apiGet('/playlist?list_id=nealbum_501208');
```

---

### 3. 搜索歌曲

**请求**

```
/search?source=<platform>&keywords=<keyword>&curpage=<page>
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| source | string | 是 | 平台名称 |
| keywords | string | 是 | 搜索关键词 |
| curpage | number | 否 | 页码（默认 1） |

**返回**

```json
{
  "result": [
    {
      "id": "qqtrack_004J80Df0WKD7L",
      "title": "歌曲名",
      "artist": "歌手名",
      "artist_id": "qqartist_...",
      "album": "专辑名",
      "album_id": "qqalbum_...",
      "source": "qq",
      "source_url": "http://y.qq.com/#type=song&mid=...",
      "img_url": "https://...",
      "url": "qqtrack_...",
      "disabled": false
    }
  ],
  "total": 100
}
```

**示例**

```js
const data = await listen1Api.apiGet('/search?source=qq&keywords=周杰伦&curpage=1');
console.log(`共找到 ${data.total} 首歌曲`);
```

---

### 4. 获取歌词

**请求**

```
/lyric?track_id=<track_id>
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| track_id | string | 是 | 歌曲 ID（含前缀） |

**返回**

```json
{
  "lyric": "[00:00.00] 歌词内容..."
}
```

**示例**

```js
const data = await listen1Api.apiGet('/lyric?track_id=netrack_25642119');
console.log(data.lyric);
```

---

### 5. 获取播放地址

**请求**

```
/bootstrap_track?track_id=<track_id>
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| track_id | string | 是 | 歌曲 ID（含前缀） |

**返回**

```json
{
  "url": "https://.../song.mp3"
}
```

**示例**

```js
const data = await listen1Api.apiGet('/bootstrap_track?track_id=netrack_25642119');
// 返回的 url 可以直接用于 <audio> 播放
console.log(data.url);
```

---

## 平台列表

| 平台 | source 值 | ID 前缀 | 支持功能 |
|------|-----------|---------|----------|
| 网易云音乐 | `netease` | `ne` | 全部 |
| QQ 音乐 | `qq` | `qq` | 全部 |
| 虾米音乐 | `xiami` | `xm` | 全部 |
| 酷狗音乐 | `kugou` | `kg` | 全部 |
| 酷我音乐 | `kuwo` | `kw` | 全部 |
| Bilibili | `bilibili` | `bi` | 歌单、播放地址（不支持搜索和歌词） |

### ID 前缀规则

```
netrack_25642119    → 网易云歌曲 ID
neplaylist_762840531 → 网易云歌单 ID
neartist_31226      → 网易云歌手 ID
nealbum_501208      → 网易云专辑 ID
qqtrack_xxx         → QQ 音乐歌曲 ID
xmtrack_xxx         → 虾米歌曲 ID
kgtrack_xxx         → 酷狗歌曲 ID
kwtrack_xxx         → 酷我歌曲 ID
bitrack_xxx         → B 站歌曲 ID
```

---

## 完整示例

### Node.js 完整示例

```js
const listen1Api = require('./dist/listen1-api.min.js');
listen1Api.loadNodejsDefaults();

(async () => {
  try {
    // 1. 搜索周杰伦的歌曲
    const searchResult = await listen1Api.apiGet(
      '/search?source=netease&keywords=周杰伦&curpage=1'
    );
    const firstTrack = searchResult.result[0];
    console.log(`找到歌曲：${firstTrack.title} - ${firstTrack.artist}`);

    // 2. 获取歌词
    const lyricData = await listen1Api.apiGet(
      `/lyric?track_id=${firstTrack.id}`
    );
    console.log('歌词：', lyricData.lyric);

    // 3. 获取播放地址
    const trackData = await listen1Api.apiGet(
      `/bootstrap_track?track_id=${firstTrack.id}`
    );
    console.log('播放地址：', trackData.url);
  } catch (err) {
    console.error('请求失败', err);
  }
})();
```

---

## 高级用法

### 自定义 HTTP 客户端

你可以传入自定义的 HTTP 函数来替换默认实现：

```js
const customHttpClient = (params) => {
  // params: { url, method, data, headers, cookieProvider, transformResponse }
  // 必须返回 Promise<{ data: any }>
  return fetch(params.url, {
    method: params.method || 'GET',
    headers: params.headers,
    body: params.method === 'POST' ? params.data : undefined,
  }).then(res => res.json()).then(data => ({ data }));
};

listen1Api.apiGet('/show_playlist?source=netease', customHttpClient);
```

### 获取所有 providers

```js
const providers = listen1Api.getAllProviders();
// 返回 [NeteaseFactory, QQFactory, XiamiFactory, KugouFactory, KuwoFactory, BiliFactory]
```

### 直接访问 provider 内部方法

```js
const netease = listen1Api.getProviderByName('netease');
// 或
const netease = listen1Api.getProviderByItemId('netrack_25642119');
```

---

## 注意事项

1. **Node.js 环境必须先调用 `loadNodejsDefaults()`**，否则 HTTP 客户端和 Cookie 管理器未初始化
2. **部分平台接口可能因反爬策略失效**，如频繁请求可能被限制
3. **B 站不支持搜索和歌词**功能
4. **虾米音乐已停止服务**，其接口可能已不可用
5. 返回的播放地址（`bootstrap_track`）**有时效性**，建议播放前实时获取

---

## 常见问题

**Q：打包后体积较大怎么办？**
A：如果只需要一个平台，可以只复制对应的 provider 文件和相关依赖，无需全量引入。

**Q：`apiGet` 返回 null 是什么情况？**
A：URL 路径不匹配任何已知路由（show_playlist / playlist / search / lyric / bootstrap_track），请检查 URL 格式。

**Q：请求报跨域错误？**
A：浏览器环境需要运行在 Chrome 扩展或 Electron 中，普通网页会有跨域限制。Node.js 环境无此问题。
