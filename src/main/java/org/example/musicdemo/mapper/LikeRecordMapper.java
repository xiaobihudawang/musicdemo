package org.example.musicdemo.mapper;

import org.example.musicdemo.entity.LikeRecord;
import org.apache.ibatis.annotations.Param;

/**
 * 点赞记录数据访问层接口（MyBatis Mapper）—— 操作 like_record 表
 *
 * 对应 XML 映射文件：src/main/resources/mapper/LikeRecordMapper.xml
 *
 * like_record 表结构：
 * - id：        主键，自增
 * - user_id：   点赞用户的 ID（外键关联 user 表）
 * - music_id：  被点赞的音乐 ID（外键关联 music 表）
 * - create_time：点赞时间（TIMESTAMP）
 *
 * ─── 业务规则 ───
 * 1. 一个用户对一首音乐只能点赞一次（通过唯一索引防止重复点赞）
 * 2. 点击点赞按钮 → 如果未点赞则 insert，如果已点赞则 delete（切换式设计）
 * 3. 每次 insert/delete 操作后，同步更新 music 表中的 like_count 字段
 *
 * ─── 设计思路 ───
 * 为什么不直接用 like_count 字段 ++/-- 而不建表？
 * - 需要知道"某个用户是否点赞了某首歌"（前端展示红心状态）
 * - 需要统计分析（如按周统计点赞排行）
 * - 需要防止刷赞（一个用户只能点赞一次）
 */
public interface LikeRecordMapper {

    /**
     * 新增一条点赞记录（用户点赞某首音乐）
     * 插入前 Service 层会先调用 findByUserAndMusic 检查是否已点赞
     *
     * @param likeRecord 点赞记录对象（包含 userId、musicId）
     * @return 受影响的行数（正常为 1）
     */
    int insert(LikeRecord likeRecord);

    /**
     * 取消点赞（删除点赞记录）
     * 用户再次点击已点赞的音乐时调用此方法
     *
     * 联合主键：userId + musicId（删除时只需这两个字段即可定位记录）
     *
     * @param userId  当前用户 ID
     * @param musicId 目标音乐 ID
     * @return 受影响的行数（正常为 1，如果用户未点赞则为 0）
     */
    int delete(@Param("userId") Integer userId, @Param("musicId") Integer musicId);

    /**
     * 查询某用户是否已点赞某音乐
     * 用于前端展示"红心"状态（已点赞显示红色，未点赞显示灰色）
     * 也用于防止重复点赞（Service 层调用此方法做判断）
     *
     * @param userId  当前用户 ID
     * @param musicId 目标音乐 ID
     * @return 点赞记录对象（存在表示已点赞），若未点赞返回 null
     */
    LikeRecord findByUserAndMusic(@Param("userId") Integer userId, @Param("musicId") Integer musicId);

    /**
     * 根据音乐 ID 删除所有点赞记录
     * 用于删除音乐时级联清理
     *
     * @param musicId 音乐 ID
     * @return 删除的记录数
     */
    int deleteByMusicId(@Param("musicId") Integer musicId);
}
