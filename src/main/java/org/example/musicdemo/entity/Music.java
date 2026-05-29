package org.example.musicdemo.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 音乐实体，对应数据库中的 music 表。
 *
 * <p>这是整个系统的核心实体，代表了平台上一首完整的音乐资源。
 * 包含了音乐元数据（标题、艺术家、描述、文件信息）、
 * 互动统计数据（点赞数、评论数、下载数），以及归属信息（上传用户）。</p>
 *
 * <p>数据库表结构（music）：</p>
 * <pre>
 * id              INT PRIMARY KEY AUTO_INCREMENT
 * title           VARCHAR(255) NOT NULL              -- 音乐标题
 * artist          VARCHAR(255) DEFAULT '未知艺术家'   -- 歌手/艺术家
 * description     TEXT                               -- 音乐描述
 * file_path       VARCHAR(500) NOT NULL              -- 音频文件存储路径
 * file_size       BIGINT DEFAULT 0                   -- 文件大小（字节）
 * like_count      INT DEFAULT 0                      -- 点赞数
 * comment_count   INT DEFAULT 0                      -- 评论数
 * download_count  INT DEFAULT 0                      -- 下载数
 * user_id         INT NOT NULL                       -- 上传用户 ID，关联 user.id
 * create_time     DATETIME DEFAULT CURRENT_TIMESTAMP -- 上传时间
 * </pre>
 *
 * <p>重要设计说明：</p>
 * <ul>
 *   <li><b>统计计数（like_count / comment_count / download_count）</b>：
 *       这三个字段属于「冗余缓存字段」，即在 music 表中直接存储统计数据，
 *       而不是每次查询时通过 COUNT(*) 实时计算。这样做的优点是列表查询时无需
 *       关联子查询或 JOIN 聚合表，性能极高；缺点是每当发生点赞/评论/下载操作时，
 *       需要同时更新 music 表的对应计数字段（通过 UPDATE ... SET count = count +/- 1），
 *       保证数据一致性。这是一种典型的「以写换读」的优化策略。</li>
 *   <li><b>文件路径与外部存储</b>：filePath 存储的是服务器本地或网络存储上的实际文件路径，
 *       通过 WebConfig 中的资源映射配置（{@code /api/music/file/**} 映射到 {@code file:${music.file-path}}），
 *       前端可直接通过 URL 访问音频文件。默认存储在 {@code D:/workspace/music/} 目录下。</li>
 *   <li><b>username 非数据库字段</b>：用于在音乐列表页展示上传者的昵称，减少前端查询次数，
 *       通过关联 user 表查询填充。</li>
 * </ul>
 *
 * @see org.example.musicdemo.entity.User
 * @see org.example.musicdemo.entity.Comment
 * @see org.example.musicdemo.entity.LikeRecord
 * @see org.example.musicdemo.entity.DownloadRecord
 */
@Data
public class Music {
    /** 主键 ID，自增长，唯一标识每首音乐 */
    private Integer id;

    /** 音乐标题，例如 "晴天"、"夜曲" 等，由上传者填写 */
    private String title;

    /**
     * 艺术家/歌手名称，例如 "周杰伦"、"Taylor Swift" 等。
     * 如果上传者未填写，默认为 "未知艺术家"。
     */
    private String artist;

    /**
     * 音乐描述或简介，支持较长文本。
     * 上传者可以用此字段填写歌词、创作背景、风格标签等信息；
     * 前端在详情页展示，列表页可截取前 N 个字符作为摘要。
     */
    private String description;

    /**
     * 音频文件在服务器上的存储路径。
     *
     * <p>该路径是相对于系统配置的 music.file-path 根目录的路径，
     * 或者是完整的物理路径。例如：如果 music.file-path = D:/workspace/music/，
     * filePath = "2024/01/abc123.mp3"，则完整路径为 D:/workspace/music/2024/01/abc123.mp3。
     * 前端通过 {@code /api/music/file/2024/01/abc123.mp3} 即可访问该文件。</p>
     *
     * <p>文件上传时由后端生成唯一文件名（通常使用 UUID 或时间戳 + 随机字符串），
     * 避免重名冲突，并按照日期分目录存储以优化文件系统性能。</p>
     */
    private String filePath;

    /**
     * 封面图片路径（相对路径，位于 covers/ 子目录下）。
     * 前端通过 {@code /api/music/cover/{filename}} 访问。
     * 上传音乐时自动从外部源搜索并下载，若未找到则为 null。
     */
    private String coverPath;

    /**
     * 音频文件的字节大小。
     * 在上传完成时由后端通过 java.io.File.length() 获取并记录，
     * 前端在列表页可用于展示文件大小（如 "5.2 MB"），
     * 但依赖于前端自行做单位换算。
     */
    private Long fileSize;

    /**
     * 点赞总数，冗余字段。
     *
     * <p>每次有点赞行为发生时，后端在此字段上执行原子增减操作：
     * {@code UPDATE music SET like_count = like_count + 1 WHERE id = ?}（点赞）
     * 或 {@code like_count = like_count - 1}（取消点赞）。
     * 查询时直接读取该字段，无需 COUNT 子查询。</p>
     *
     * <p>初始值为 0，仅当有用户实际点赞时才递增。</p>
     *
     * @see org.example.musicdemo.entity.LikeRecord
     */
    private Integer likeCount;

    /**
     * 评论总数，冗余字段。
     *
     * <p>新增评论时递增，删除评论时递减。逻辑与 likeCount 一致。
     * 注意：评论的删除可能需要考虑是物理删除还是软删除，
     * 目前项目采用物理删除（直接从 comment 表 DELETE），
     * 因此 commentCount 必须同步递减。</p>
     *
     * @see org.example.musicdemo.entity.Comment
     */
    private Integer commentCount;

    /**
     * 下载总数，冗余字段。
     *
     * <p>每次用户点击下载按钮成功后递增。
     * 下载记录写入了 download_record 表用于详细追踪，
     * 而 downloadCount 则是纯数值统计，用于排行。</p>
     *
     * @see org.example.musicdemo.entity.DownloadRecord
     */
    private Integer downloadCount;

    /**
     * 上传该音乐的用户的 ID。
     * 关联 {@link org.example.musicdemo.entity.User#id}，
     * 在音乐上传接口中由后端从 JWT 令牌中解析当前登录用户后自动填充。
     * 用户只能删除或修改自己上传的音乐（管理员除外）。
     */
    private Integer userId;

    /**
     * 音乐上传时间（即记录创建时间），
     * 由数据库自动填充 DEFAULT CURRENT_TIMESTAMP。
     * 列表页通常按此字段降序排列，将最新上传的音乐展示在前面。
     */
    private LocalDateTime createTime;

    /**
     * 上传用户的昵称（非数据库直接字段）。
     *
     * <p>此字段并未映射到 music 表的任一列，而是在查询音乐列表时通过
     * LEFT JOIN user 表获取（SELECT m.*, u.name AS username FROM music m LEFT JOIN user u ON m.user_id = u.id）。
     * 这样在列表展示时可以直接显示上传者昵称，避免了针对每首音乐额外查询用户表的 N+1 问题。</p>
     *
     * <p>如果当前音乐没有关联用户（理论上不应发生），此字段为 null。</p>
     */
    private String username;
}