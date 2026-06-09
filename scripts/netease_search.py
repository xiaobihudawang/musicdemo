"""
网易云音乐搜索、封面获取、歌词获取。
用法:
  python netease_search.py cover <title> <artist>    → 输出 COVER:<url> 或空
  python netease_search.py lyric <trackId>           → 输出多行 LRC 歌词
  python netease_search.py search <keywords>         → 输出 JSON 搜索结果
  python netease_search.py trackid <title> <artist>  → 输出 TRACKID:<id>
"""
import sys
import json
import urllib.request
import urllib.parse

UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"


def cloudsearch(keywords, limit=20):
    """调用 cloudsearch/pc 接口，返回解析后的 songs 列表"""
    url = "https://music.163.com/api/cloudsearch/pc"
    data = urllib.parse.urlencode({
        "s": keywords,
        "offset": 0,
        "limit": limit,
        "type": 1
    }).encode("utf-8")
    req = urllib.request.Request(url, data=data, headers={
        "User-Agent": UA,
        "Referer": "https://music.163.com/",
        "Content-Type": "application/x-www-form-urlencoded"
    })
    with urllib.request.urlopen(req, timeout=15) as resp:
        body = json.loads(resp.read().decode("utf-8"))
    if body.get("code") != 200:
        return []
    result = body.get("result", {})
    return result.get("songs", [])


def find_best_track(songs, title, artist):
    """从搜索结果中匹配最佳歌曲，返回 (id, name, artist_name)"""
    # 优先匹配歌名+歌手
    for s in songs:
        name = s.get("name", "")
        ar = s.get("ar", [])
        ar_name = ar[0].get("name", "") if ar else ""
        if name == title and artist in ar_name:
            return s["id"], name, ar_name
    # 其次只匹配歌名
    for s in songs:
        name = s.get("name", "")
        if name == title:
            ar = s.get("ar", [])
            ar_name = ar[0].get("name", "") if ar else ""
            return s["id"], name, ar_name
    # 兜底取第一个
    if songs:
        s = songs[0]
        ar = s.get("ar", [])
        ar_name = ar[0].get("name", "") if ar else ""
        return s["id"], s.get("name", ""), ar_name
    return None, None, None


def get_lyric(track_id):
    """获取 LRC 歌词"""
    url = f"https://music.163.com/api/song/lyric?id={track_id}&lv=-1&tv=-1"
    req = urllib.request.Request(url, headers={
        "User-Agent": UA,
        "Referer": "https://music.163.com/"
    })
    with urllib.request.urlopen(req, timeout=15) as resp:
        body = json.loads(resp.read().decode("utf-8"))
    lrc = body.get("lrc", {})
    return lrc.get("lyric", "")


def cmd_cover(title, artist):
    keywords = f"{title} {artist}"
    songs = cloudsearch(keywords)
    track_id, name, ar_name = find_best_track(songs, title, artist)
    if not track_id:
        return
    # 从搜索结果中直接取封面
    for s in songs:
        if s.get("name") == title:
            al = s.get("al", {})
            pic = al.get("picUrl", "")
            if pic:
                print(f"COVER:{pic}")
                return
    # 兜底用第一个
    if songs:
        al = songs[0].get("al", {})
        pic = al.get("picUrl", "")
        if pic:
            print(f"COVER:{pic}")


def cmd_lyric(track_id):
    lrc = get_lyric(track_id)
    if lrc:
        print(lrc)


def cmd_search(keywords):
    songs = cloudsearch(keywords)
    output = []
    for s in songs:
        ar = s.get("ar", [])
        al = s.get("al", {})
        output.append({
            "id": s.get("id"),
            "name": s.get("name"),
            "artist": ar[0].get("name", "") if ar else "",
            "album": al.get("name", ""),
            "cover": al.get("picUrl", "")
        })
    print(json.dumps(output, ensure_ascii=False))


def cmd_trackid(title, artist):
    keywords = f"{title} {artist}"
    songs = cloudsearch(keywords)
    track_id, name, ar_name = find_best_track(songs, title, artist)
    if track_id:
        print(f"TRACKID:{track_id}")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        sys.exit(1)
    cmd = sys.argv[1]
    if cmd == "cover" and len(sys.argv) >= 4:
        cmd_cover(sys.argv[2], sys.argv[3])
    elif cmd == "lyric" and len(sys.argv) >= 3:
        cmd_lyric(sys.argv[2])
    elif cmd == "search" and len(sys.argv) >= 3:
        cmd_search(" ".join(sys.argv[2:]))
    elif cmd == "trackid" and len(sys.argv) >= 4:
        cmd_trackid(sys.argv[2], sys.argv[3])
    else:
        sys.exit(1)
