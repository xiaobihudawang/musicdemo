package org.example.musicdemo.service;

import org.example.musicdemo.entity.Comment;
import org.example.musicdemo.mapper.CommentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 评论服务，处理音乐评论的增删查逻辑。
 * 不再维护冗余计数字段，评论数通过 comment 表实时统计。
 */
@Service
public class CommentService {

    private final CommentMapper commentMapper;
    private final SensitiveWordService sensitiveWordService;

    public CommentService(CommentMapper commentMapper, SensitiveWordService sensitiveWordService) {
        this.commentMapper = commentMapper;
        this.sensitiveWordService = sensitiveWordService;
    }

    /** 查询指定音乐的所有评论 */
    public List<Comment> listByMusicId(Integer musicId) {
        return commentMapper.findByMusicId(musicId);
    }

    /**
     * 添加评论。
     *
     * 提交前调用 SensitiveWordService 拦截包含脏话 / 暴力 / 侮辱等
     * 敏感词的内容，命中即抛 RuntimeException，由 CommentController
     * 已有 try/catch 转成 Result.fail(msg) 返回给前端。
     */
    @Transactional
    public Comment add(Comment comment) {
        if (sensitiveWordService.containsForbidden(comment.getContent())) {
            throw new RuntimeException("评论包含不当内容，请修改后重试");
        }
        commentMapper.insert(comment);
        return comment;
    }

    /** 根据 ID 查询评论 */
    public Comment findById(Integer id) {
        return commentMapper.findById(id);
    }

    /** 删除评论 */
    @Transactional
    public void delete(Integer id, Integer musicId) {
        commentMapper.deleteById(id);
    }
}
