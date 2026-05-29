package org.example.musicdemo.mapper;

import org.apache.ibatis.annotations.Param;
import org.example.musicdemo.entity.Music;

import java.util.List;

/**
 * 音乐数据访问层接口（MyBatis Mapper）—— 操作 music 表（核心业务表）
 *
 * 对应 XML 映射文件：src/main/resources/mapper/MusicMapper.xml
 *
 * music 表是本系统的核心表，存储所有音乐信息，包括：
 * - id：             主键，自增
 * - title：          歌曲名称
 * - artist：         歌手/艺术家
 * - album：          专辑名称
 * - file_path：      文件存储路径（相对路径，结合 WebConfig 映射）
 * - cover_path：     封面图片路径
 * - duration：       音频时长（秒）
 * - uploader_id：    上传者的用户 ID（外键关联 user 表）
 * - like_count：     点赞数（反范式冗余字段，提高查询性能）
 * - comment_count：  评论数（反范式冗余字段）
 * - download_count： 下载数（反范式冗余字段）
 * - create_time：    上传时间
 *
 * ─── 反范式设计说明 ───
 * like_count、comment_count、download_count 这三个计数是"反范式"存储的，
 * 它们本应通过 COUNT(*) 查询关联表计算得出，但为了提高列表展示性能，
 * 直接冗余存储在 music 表中。每次点赞/评论/下载操作时同步更新这些字段。
 *
 * ─── 分页设计 ───
 * 使用 MySQL 的 LIMIT 语法（offset, size）实现分页。
 * keyword 参数支持模糊搜索（LIKE %keyword%），搜索 title、artist、album 字段。
 */
public interface MusicMapper {

    /**
     * 分页查询音乐列表（支持关键词搜索）
     *
     * 分页参数：
     * - offset = (pageNum - 1) * pageSize
     * - size = pageSize
     *
     * 搜索逻辑：当 keyword 不为空时，在 title、artist、album 三个字段中模糊匹配
     * 排序规则：按 create_time 倒序（最新上传的在前）
     *
     * @param offset  偏移量（从第几条开始，从 0 开始计数）
     * @param size    每页条数
     * @param keyword 搜索关键词（可选，传空字符串或 null 表示查询全部）
     * @return 音乐列表
     */
    List<Music> findList(@Param("offset") int offset,
                         @Param("size") int size,
                         @Param("keyword") String keyword);

    /**
     * 统计符合搜索条件的音乐总数（用于前端计算总页数）
     *
     * 与 findList 使用相同的 WHERE 条件（keyword 模糊匹配），
     * 但使用 COUNT(*) 而非 SELECT *，性能更高。
     *
     * @param keyword 搜索关键词（与 findList 保持一致）
     * @return 符合条件的总记录数
     */
    int countList(@Param("keyword") String keyword);

    /**
     * 根据 ID 查询单个音乐的完整信息
     * 用于音乐详情页展示（包括所有字段）
     *
     * @param id 音乐 ID
     * @return 音乐对象，若不存在返回 null
     */
    Music findById(@Param("id") Integer id);

    /**
     * 插入一条新的音乐记录
     * 当用户上传音乐时调用，插入成功后 MyBatis 会回填自增主键到 music.id
     *
     * 注意：file_path 和 cover_path 存储的是文件的相对路径或文件名，
     * 前端访问时需要拼上 /api/music/file/ 前缀（由 WebConfig 做映射）。
     *
     * @param music 待插入的音乐对象
     * @return 受影响的行数（正常为 1）
     */
    int insert(Music music);

    /**
     * 根据 ID 删除音乐记录
     * 只有管理员可以调用此操作（由 Service 层做权限校验）
     * 注意：删除数据库记录后，磁盘上的音乐文件也需要清理（由 Service 层处理）
     *
     * @param id 要删除的音乐 ID
     * @return 受影响的行数（正常为 1，不存在为 0）
     */
    int deleteById(@Param("id") Integer id);

    /**
     * 更新音乐的点赞数（增量更新）
     *
     * SQL: UPDATE music SET like_count = like_count + #{delta} WHERE id = #{id}
     *
     * @param id    音乐 ID
     * @param delta 变化量（点赞为 +1，取消点赞为 -1）
     * @return 受影响的行数
     */
    int updateLikeCount(@Param("id") Integer id, @Param("delta") int delta);

    /**
     * 更新音乐的评论数（增量更新）
     *
     * SQL: UPDATE music SET comment_count = comment_count + #{delta} WHERE id = #{id}
     *
     * @param id    音乐 ID
     * @param delta 变化量（新增评论为 +1，删除评论为 -1）
     * @return 受影响的行数
     */
    int updateCommentCount(@Param("id") Integer id, @Param("delta") int delta);

    /**
     * 更新音乐的下载数（+1）
     *
     * 每次用户下载音乐时调用，下载数只增不减
     *
     * @param id 音乐 ID
     * @return 受影响的行数
     */
    int updateDownloadCount(@Param("id") Integer id);

    /**
     * 更新封面图路径
     *
     * @param id        音乐 ID
     * @param coverPath 封面图相对路径（或 null 清空）
     * @return 受影响的行数
     */
    int updateCoverPath(@Param("id") Integer id, @Param("coverPath") String coverPath);

    /**
     * 查询本周点赞数 TOP10 的音乐
     * 统计范围：本周一 00:00:00 到本周日 23:59:59
     *
     * 查询逻辑：
     * 通过 like_record 表统计本周内点赞次数最多的前 10 首音乐，
     * 并关联 music 表获取音乐详细信息。
     *
     * @param start 统计起始时间（本周一 00:00:00，格式 yyyy-MM-dd HH:mm:ss）
     * @param end   统计结束时间（本周日 23:59:59，格式 yyyy-MM-dd HH:mm:ss）
     * @return 本周点赞排行 TOP10 音乐列表
     */
    List<Music> findWeeklyLikeTop10(@Param("start") String start, @Param("end") String end);

    /**
     * 查询本周下载数 TOP10 的音乐
     * 统计范围：本周一 00:00:00 到本周日 23:59:59
     *
     * 查询逻辑：
     * 通过 download_record 表统计本周内下载次数最多的前 10 首音乐，
     * 关联 music 表获取详细信息。
     *
     * @param start 统计起始时间
     * @param end   统计结束时间
     * @return 本周下载排行 TOP10 音乐列表
     */
    List<Music> findWeeklyDownloadTop10(@Param("start") String start, @Param("end") String end);

    /**
     * 查询本周评论数 TOP10 的音乐
     * 统计范围：本周一 00:00:00 到本周日 23:59:59
     *
     * 查询逻辑：
     * 通过 comment 表统计本周内评论数量最多的前 10 首音乐，
     * 关联 music 表获取详细信息。
     *
     * @param start 统计起始时间
     * @param end   统计结束时间
     * @return 本周评论排行 TOP10 音乐列表
     */
    List<Music> findWeeklyCommentTop10(@Param("start") String start, @Param("end") String end);
}