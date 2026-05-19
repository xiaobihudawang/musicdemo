"""
Bilibili 音频下载工具（命令行版）

用法:
    python bilibili_demo.py <BV_ID>

功能:
    1. 调用 B 站开放 API 获取视频标题和 cid
    2. 通过 WBI 签名机制获取音频流地址
    3. 将标题和音频 URL 打印到标准输出（不下载到本地）

通信协议:
    本脚本由 Java 端的 BilibiliService 通过 ProcessBuilder 启动。
    Java 和 Python 之间通过标准输入输出流通信:
    - Java 通过命令行参数传入 BV 号
    - Python 将 TITLE 和 URL 分别打印到 stdout，格式:
        TITLE:视频标题
        URL:音频下载地址
    - Python 通过 exit code 报告成功/失败（0=成功，非0=失败）

日志约定:
    所有日志输出到 stderr，不污染 stdout。
    Java 端只解析 stdout 中 TITLE: 和 URL: 开头的行。
"""

import requests
import hashlib
import re
import time
import os
import sys
import logging

# ---------------------------------------------------------------
# 编码设置: 强制 stdout/stderr 使用 UTF-8，与 Java 端的读取编码保持一致
# Windows 默认控制台编码是 GBK，如果不设置，Java 读 stdout 时中文会乱码
# ---------------------------------------------------------------
if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')
if hasattr(sys.stderr, 'reconfigure'):
    sys.stderr.reconfigure(encoding='utf-8')

# ---------------------------------------------------------------
# 日志配置
# ---------------------------------------------------------------

logger = logging.getLogger("bilibili_demo")
logger.setLevel(logging.DEBUG)

console_handler = logging.StreamHandler(sys.stderr)
console_handler.setLevel(logging.DEBUG)
formatter = logging.Formatter(
    "%(asctime)s [%(levelname)s] %(name)s - %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S"
)
console_handler.setFormatter(formatter)
logger.addHandler(console_handler)


HEADERS = {
    "Referer": "https://www.bilibili.com",
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
}


def step1_get_info(bvid):
    """获取视频标题和 cid"""
    logger.info("step1_get_info 开始: bvid=[%s]", bvid)
    url = f"https://api.bilibili.com/x/web-interface/view?bvid={bvid}"
    resp = requests.get(url, headers=HEADERS)
    data = resp.json()["data"]
    title, cid = data["title"], data["cid"]
    logger.info("step1_get_info 成功: title=[%s], cid=%s", title, cid)
    return title, cid


def get_wbi_keys():
    """获取 WBI 签名所用的 mix_key"""
    logger.debug("get_wbi_keys: 开始获取")
    resp = requests.get("https://api.bilibili.com/x/web-interface/nav", headers=HEADERS)
    data = resp.json()["data"]["wbi_img"]
    img_key = data["img_url"].rsplit("/", 1)[1].rsplit(".", 1)[0][:32]
    sub_key = data["sub_url"].rsplit("/", 1)[1].rsplit(".", 1)[0][:32]
    return img_key + sub_key


def calc_wbi_sign(params, mix_key):
    """计算 WBI 签名"""
    params["wts"] = int(time.time())
    sorted_keys = sorted(params.keys())
    query_str = "&".join(f"{k}={params[k]}" for k in sorted_keys)
    w_rid = hashlib.md5((query_str + mix_key).encode()).hexdigest()
    return w_rid, params["wts"]


def step3_get_playurl(bvid, cid):
    """获取音频流 URL"""
    logger.info("step3_get_playurl 开始: bvid=[%s], cid=%s", bvid, cid)
    mix_key = get_wbi_keys()
    params = {"bvid": bvid, "cid": str(cid), "qn": "64", "fnval": "4048"}
    w_rid, wts = calc_wbi_sign(params, mix_key)
    full_params = {**params, "wts": wts, "w_rid": w_rid}
    resp = requests.get(
        "https://api.bilibili.com/x/player/playurl",
        params=full_params,
        headers=HEADERS
    )
    data = resp.json()["data"]
    audios = data["dash"]["audio"]
    best_audio = max(audios, key=lambda x: x["bandwidth"])
    base_url = best_audio["baseUrl"]
    backup_url = best_audio.get("backupUrl", [None])[0]
    logger.info("step3_get_playurl 成功, 选择最高码率: bandwidth=%s", best_audio["bandwidth"])
    return base_url, backup_url


# ========== 脚本入口 ==========
if __name__ == "__main__":
    logger.info("=" * 50)
    logger.info("bilibili_demo 启动")
    logger.info("=" * 50)

    if len(sys.argv) < 2:
        logger.error("缺少 BV_ID 参数")
        print("用法: python bilibili_demo.py <BV_ID>", file=sys.stderr)
        sys.exit(1)

    bvid = sys.argv[1]
    logger.info("参数: bvid=[%s]", bvid)

    try:
        # 步骤1: 获取视频标题和 cid
        title, cid = step1_get_info(bvid)
        safe_title = re.sub(r'[\\/:*?"<>|]', '', title)

        # 步骤3: 获取音频流地址
        audio_url, backup_url = step3_get_playurl(bvid, cid)

        # 输出标题和音频 URL 到 stdout（Java 读取这两行）
        logger.info("输出 TITLE 到 stdout: [%s]", safe_title)
        print(f"TITLE:{safe_title}")

        logger.info("输出 URL 到 stdout: [%s...]", audio_url[:80])
        print(f"URL:{audio_url}")

        # 如果有备用地址也输出，Java 端主地址下载失败时可以用
        if backup_url:
            logger.info("输出备用 URL 到 stdout")
            print(f"BACKUP_URL:{backup_url}")

    except Exception as e:
        logger.error("执行失败: %s", e, exc_info=True)
        sys.exit(1)

    logger.info("bilibili_demo 正常结束")