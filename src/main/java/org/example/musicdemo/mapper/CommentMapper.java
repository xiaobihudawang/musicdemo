package org.example.musicdemo.mapper;

import org.example.musicdemo.entity.Comment;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评论数据访问层接口，操作 comment 表。
 */
public interface CommentMapper {

    /** 查询某音乐下的所有评论（按时间倒序） */
    List<Comment> findByMusicId(@Param("musicId") Integer musicId);

    /** 新增评论 */
    int insert(Comment comment);

    /** 根据 ID 查询单条评论 */
    Comment findById(@Param("id") Integer id);

    /** 根据 ID 删除评论 */
    int deleteById(@Param("id") Integer id);

    /** 删除某音乐的所有评论（级联清理） */
    int deleteByMusicId(@Param("musicId") Integer musicId);
}
