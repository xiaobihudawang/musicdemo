package org.example.musicdemo.service;

import org.example.musicdemo.entity.Comment;
import org.example.musicdemo.mapper.CommentMapper;
import org.example.musicdemo.mapper.MusicMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 评论服务 —— 处理音乐评论的增删查逻辑。
 * <p>
 * 评论与音乐之间是多对一关系。每条评论属于一首歌，一首歌有多条评论。
 * 为了保持数据一致性，修改评论时需要同步更新 {@code music.comment_count} 字段。
 * </p>
 *
 * <h3>事务说明</h3>
 * 涉及写操作的方法（{@link #add(Comment)}、{@link #delete(Integer, Integer)}）
 * 都标注了 {@link Transactional}，确保插入/删除评论和更新评论计数两条 SQL
 * 要么同时成功，要么同时回滚。
 */
@Service
public class CommentService {

    /** 评论表的数据访问层接口 */
    private final CommentMapper commentMapper;

    /** 音乐表的数据访问层接口，用于更新评论数 */
    private final MusicMapper musicMapper;

    /**
     * 构造器注入（符合 AGENTS.md 约定的 Spring Bean 注入方式）。
     *
     * @param commentMapper 评论 Mapper
     * @param musicMapper   音乐 Mapper
     */
    public CommentService(CommentMapper commentMapper, MusicMapper musicMapper) {
        this.commentMapper = commentMapper;
        this.musicMapper = musicMapper;
    }

    /**
     * 根据音乐 ID 查询该歌曲的所有评论。
     * 返回的评论列表按创建时间倒序排列（由 MyBatis XML 中的 ORDER BY 控制）。
     *
     * @param musicId 音乐 ID
     * @return 评论列表，如果该歌曲没有评论则返回空列表
     */
    public List<Comment> listByMusicId(Integer musicId) {
        return commentMapper.findByMusicId(musicId);
    }

    /**
     * 添加一条新评论，同时将对应音乐的 {@code comment_count} 加 1。
     * <p>
     * 此操作在事务中执行：insert + update 两条 DML 要么全成功，要么全回滚。
     * 传入的 {@link Comment} 对象需已设置好 {@code userId}、{@code musicId}、{@code content} 等字段。
     * 插入后 MyBatis 会将自动生成的 {@code id} 和 {@code createTime} 回填到传入对象中。
     * </p>
     *
     * @param comment 待插入的评论实体（id 和 createTime 会被自动填充）
     * @return 插入后的完整 Comment 对象（包含自增 ID 和时间戳）
     */
    @Transactional
    public Comment add(Comment comment) {
        commentMapper.insert(comment);
        musicMapper.updateCommentCount(comment.getMusicId(), 1);
        return comment;
    }

    /**
     * 根据评论 ID 查询单条评论详情。
     *
     * @param id 评论 ID
     * @return 对应的 Comment 实体，如果不存在则返回 null
     */
    public Comment findById(Integer id) {
        return commentMapper.findById(id);
    }

    /**
     * 删除指定评论，同时将对应音乐的 {@code comment_count} 减 1。
     * <p>
     * 操作在事务中执行。注意：此方法要求调用方传入 {@code musicId}，
     * 而不是在方法内部查询，这样可以减少一次数据库查询。
     * </p>
     *
     * @param id      待删除的评论 ID
     * @param musicId 评论所属的音乐 ID（用于更新计数）
     */
    @Transactional
    public void delete(Integer id, Integer musicId) {
        commentMapper.deleteById(id);
        musicMapper.updateCommentCount(musicId, -1);
    }
}
