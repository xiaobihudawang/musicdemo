/**
 * ============================================================
 * 敏感词前端预检（forbidden-words.js）
 *
 * 提供 containsForbiddenWord(text) 全局函数：
 *   - 用于评论提交前的快速反馈，省一次网络往返
 *   - 归一化规则与服务端 sensitive-word 库对齐（去空白 + 去常见分隔符 + 小写）
 *
 * 注意：词表只是服务端 6W+ 库的一个**子集**，仅做"明显"的拦截，
 *      真正的安全防线在 CommentService.add()（服务端）。
 *      词表按需扩充；漏检的内容服务端会兜底拦截。
 * ============================================================
 */
const FORBIDDEN_WORDS = [
    // 侮辱 / 骂人
    '傻逼', '煞笔', '沙雕', '智障', '脑残', '脑瘫', '弱智', '蠢货', '废物', '垃圾',
    '滚蛋', '去死', '变态', '恶心', '贱人', '婊子', '骚货', '绿茶', '白莲花', '心机',
    '小人', '贱', '蠢', '呆', '弱', '烂',
    // 暴力 / 威胁
    '打死', '弄死', '杀你', '砍死', '炸死', '弄死你', '打死你', '杀全家', '灭门',
    '去死吧', '自杀', '爆炸', '枪毙',
    // 黄色 / 性相关
    '色情', '裸体', '裸照', '性交', '做爱', '约炮', '一夜情', '援交', '卖淫', '嫖娼',
    '强暴', '强奸',
    // 英文常见
    'fuck', 'shit', 'bitch', 'asshole', 'bastard', 'damn', 'crap', 'dick', 'pussy',
    'motherfucker', 'cunt', 'wtf', 'stfu', 'nmsl', 'sb', 'nc',
    // 音乐场景补充
    '垃圾歌', '屎', '呕吐', '难听到爆', '狗屎', '歌屎'
];

/**
 * 归一化文本：小写 + 去空白 + 去常见分隔符。
 * 与 sensitive-word 库的 ignoreCase / ignoreWidth / ignoreNumStyle
 * 大致对齐。
 */
function normalizeForCheck(text) {
    if (!text) return '';
    return text.toLowerCase()
        .replace(/[\s\*\·\.\-\_\~!?,，。；;：:、\/\\|@#\$%\^&\(\)\[\]\{\}【】《》"'`~·•]/g, '');
}

/**
 * 检测文本是否包含敏感词（粗粒度）。
 * 服务端会做权威拦截，本函数只是省一次往返。
 */
function containsForbiddenWord(text) {
    if (!text) return false;
    const normalized = normalizeForCheck(text);
    return FORBIDDEN_WORDS.some(w => normalized.indexOf(w) !== -1);
}
