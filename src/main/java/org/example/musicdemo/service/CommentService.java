package org.example.musicdemo.service;

import org.example.musicdemo.entity.Comment;
import org.example.musicdemo.mapper.CommentMapper;
import org.example.musicdemo.mapper.MusicMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 评论服务，处理音乐评论的增删查逻辑。
 * 修改评论时同步更新 music.comment_count。
 */
@Service
public class CommentService {

    private final CommentMapper commentMapper;
    private final MusicMapper musicMapper;

    public CommentService(CommentMapper commentMapper, MusicMapper musicMapper) {
        this.commentMapper = commentMapper;
        this.musicMapper = musicMapper;
    }

    /** 查询指定音乐的所有评论 */
    public List<Comment> listByMusicId(Integer musicId) {
        return commentMapper.findByMusicId(musicId);
    }

    /** 添加评论，同时将 music.comment_count 加 1 */
    @Transactional
    public Comment add(Comment comment) {
        commentMapper.insert(comment);
        musicMapper.updateCommentCount(comment.getMusicId(), 1);
        return comment;
    }

    /** 根据 ID 查询评论 */
    public Comment findById(Integer id) {
        return commentMapper.findById(id);
    }

    /** 删除评论，同时将 music.comment_count 减 1 */
    @Transactional
    public void delete(Integer id, Integer musicId) {
        commentMapper.deleteById(id);
        musicMapper.updateCommentCount(musicId, -1);
    }
}
