package org.example.musicdemo.mapper;

import org.example.musicdemo.entity.Comment;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评论数据访问层，操作 comment 表
 */
public interface CommentMapper {

    /** 根据音乐 ID 查询评论列表 */
    List<Comment> findByMusicId(@Param("musicId") Integer musicId);

    /** 新增评论 */
    int insert(Comment comment);

    /** 根据 ID 查询单条评论 */
    Comment findById(@Param("id") Integer id);

    /** 根据 ID 删除评论 */
    int deleteById(@Param("id") Integer id);
}
