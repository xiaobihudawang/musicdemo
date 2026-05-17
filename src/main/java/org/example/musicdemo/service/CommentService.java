package org.example.musicdemo.service;

import org.example.musicdemo.entity.Comment;
import org.example.musicdemo.mapper.CommentMapper;
import org.example.musicdemo.mapper.MusicMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 评论服务，处理音乐评论的增删查逻辑
 */
@Service
public class CommentService {

    private final CommentMapper commentMapper;
    private final MusicMapper musicMapper;

    public CommentService(CommentMapper commentMapper, MusicMapper musicMapper) {
        this.commentMapper = commentMapper;
        this.musicMapper = musicMapper;
    }

    /**
     * 根据音乐ID查询所有评论
     */
    public List<Comment> listByMusicId(Integer musicId) {
        return commentMapper.findByMusicId(musicId);
    }

    /**
     * 添加评论并更新对应音乐的评论计数
     */
    @Transactional
    public Comment add(Comment comment) {
        commentMapper.insert(comment);
        musicMapper.updateCommentCount(comment.getMusicId(), 1);
        return comment;
    }

    /**
     * 根据评论ID查询单条评论
     */
    public Comment findById(Integer id) {
        return commentMapper.findById(id);
    }

    /**
     * 删除评论并更新对应音乐的评论计数
     */
    @Transactional
    public void delete(Integer id, Integer musicId) {
        commentMapper.deleteById(id);
        musicMapper.updateCommentCount(musicId, -1);
    }
}
