package org.example.musicdemo.mapper;

import org.example.musicdemo.entity.LikeRecord;
import org.apache.ibatis.annotations.Param;

/**
 * 点赞记录数据访问层接口，操作 like_record 表。
 */
public interface LikeRecordMapper {

    /** 新增点赞记录 */
    int insert(LikeRecord likeRecord);

    /** 取消点赞（删除记录） */
    int delete(@Param("userId") Integer userId, @Param("musicId") Integer musicId);

    /** 查询用户是否已点赞某音乐 */
    LikeRecord findByUserAndMusic(@Param("userId") Integer userId, @Param("musicId") Integer musicId);
}
