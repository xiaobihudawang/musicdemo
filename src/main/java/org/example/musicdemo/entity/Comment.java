package org.example.musicdemo.entity;


import java.time.LocalDateTime;
import lombok.Data;

/**
 * 评论实体，对应数据库中的 comment 表。
 *
 * <p>每一条记录代表用户对某一首音乐发表的一条评论。评论包含文字内容、归属用户和音乐，
 * 以及评论的创建时间。在展示评论列表时，会额外关联查询用户的昵称，该字段（username）
 * 虽然定义在本实体中，但并非 comment 表的直接字段，而是通过联表查询填充的「视图字段」。</p>
 *
 * <p>数据库表结构（comment）：</p>
 * <pre>
 * id          INT PRIMARY KEY AUTO_INCREMENT   -- 主键
 * content     TEXT NOT NULL                    -- 评论内容
 * user_id     INT NOT NULL                     -- 评论用户 ID，关联 user.id
 * music_id    INT NOT NULL                     -- 被评论音乐 ID，关联 music.id
 * create_time DATETIME DEFAULT CURRENT_TIMESTAMP -- 评论时间
 * </pre>
 *
 * <p>用途说明：</p>
 * <ul>
 *   <li><b>新增评论</b>：用户在音乐详情页填写评论内容后，前端 POST 请求携带 content + musicId，
 *       后端从 JWT 中提取 userId，组装后插入 comment 表。</li>
 *   <li><b>查询评论列表</b>：按 musicId 查询，同时 LEFT JOIN user 表获取 username，
 *       按 createTime 降序排列。</li>
 *   <li><b>删除评论</b>：只有评论作者或管理员可以删除，通过 id 定位记录。</li>
 * </ul>
 *
 * @see org.example.musicdemo.entity.Music
 * @see org.example.musicdemo.entity.User
 */
@Data
public class Comment {
    /** 主键 ID，自增长，唯一标识每一条评论记录 */
    private Integer id;

    /** 评论的文本内容，由用户在前端输入，存储格式为纯文本 */
    private String content;

    /**
     * 发表该评论的用户 ID。
     * 该字段关联 {@link org.example.musicdemo.entity.User#id}，
     * 在新增评论时由后端从 JWT 令牌中解析当前登录用户后自动填充，前端无需传递该字段。
     */
    private Integer userId;

    /**
     * 被评论的音乐 ID。
     * 该字段关联 {@link org.example.musicdemo.entity.Music#id}，
     * 用于在音乐详情页按音乐维度加载所有评论。
     */
    private Integer musicId;

    /**
     * 评论创建时间，由数据库自动填充（DEFAULT CURRENT_TIMESTAMP）。
     * 在查询列表时通常按此字段降序排序，将最新评论展示在前面。
     */
    private LocalDateTime createTime;

    /**
     * 评论用户的昵称（非数据库直接字段）。
     *
     * <p>此字段并未映射到 comment 表的任一列，而是在查询评论列表时通过
     * LEFT JOIN user 表获取（SELECT c.*, u.name AS username FROM comment c LEFT JOIN user u ON c.user_id = u.id）。
     * 这样做的目的是减少前端查询次数，在一次查询中同时拿到评论数据和作者昵称，
     * 避免 N+1 问题。</p>
     *
     * <p>在 MyBatis 的 ResultMap 中不会映射该字段，
     * 需要在 XML 的 SELECT 语句中用别名注入。</p>
     */
    private String username;
}