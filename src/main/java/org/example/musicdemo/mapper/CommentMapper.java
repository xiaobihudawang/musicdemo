package org.example.musicdemo.mapper;

import org.example.musicdemo.entity.Comment;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评论数据访问层接口（MyBatis Mapper）—— 操作 comment 表
 *
 * 对应 XML 映射文件：src/main/resources/mapper/CommentMapper.xml
 *
 * comment 表结构：
 * - id：          主键，自增
 * - user_id：     评论者的用户 ID（外键关联 user 表）
 * - music_id：    被评论的音乐 ID（外键关联 music 表）
 * - content：     评论内容（文本）
 * - create_time： 评论创建时间
 *
 * 本系统评论设计为"单层"（非嵌套，没有楼中楼），
 * 因此 findById 仅用于验证评论是否存在，不做级联查询。
 */
public interface CommentMapper {

    /**
     * 根据音乐 ID 查询该音乐下的所有评论列表
     * 结果按创建时间倒序排列（最新的在前）
     *
     * 关联查询：通过 SQL JOIN 获取评论者的用户名（user.username），
     * 这样前端展示时无需额外调用用户接口。
     *
     * @param musicId 音乐 ID
     * @return 该音乐的所有评论列表（按时间倒序）
     */
    List<Comment> findByMusicId(@Param("musicId") Integer musicId);

    /**
     * 新增一条评论记录
     * 插入成功时，MyBatis 会自动将自增主键值回填到 comment.id 属性中
     *
     * @param comment 评论对象（需包含 userId、musicId、content）
     * @return 受影响的行数（正常为 1）
     */
    int insert(Comment comment);

    /**
     * 根据主键 ID 查询单条评论
     * 主要用于验证评论是否存在（如删除前的检查、权限校验）
     *
     * @param id 评论 ID（主键）
     * @return 评论对象，若不存在返回 null
     */
    Comment findById(@Param("id") Integer id);

    /**
     * 根据主键 ID 删除评论
     * 只有评论作者本人或管理员可以删除评论（由 Service 层做权限校验）
     *
     * @param id 评论 ID（主键）
     * @return 受影响的行数（正常为 1，不存在为 0）
     */
    int deleteById(@Param("id") Integer id);

    /**
     * 根据音乐 ID 删除所有关联的评论
     * 用于删除音乐时级联清理
     *
     * @param musicId 音乐 ID
     * @return 删除的记录数
     */
    int deleteByMusicId(@Param("musicId") Integer musicId);
}
