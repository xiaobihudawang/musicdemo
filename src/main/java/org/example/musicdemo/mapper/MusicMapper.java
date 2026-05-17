package org.example.musicdemo.mapper;

import org.apache.ibatis.annotations.Param;
import org.example.musicdemo.entity.Music;

import java.util.List;

/**
 * 音乐数据访问层，操作 music 表
 */
public interface MusicMapper {

    /** 分页 + 搜索查询音乐列表 */
    List<Music> findList(@Param("offset") int offset,
                         @Param("size") int size,
                         @Param("keyword") String keyword);

    /** 统计总条数（分页用） */
    int countList(@Param("keyword") String keyword);

    /** 根据 ID 查询单个音乐 */
    Music findById(@Param("id") Integer id);

    /** 插入音乐 */
    int insert(Music music);

    /** 删除音乐 */
    int deleteById(@Param("id") Integer id);

    /** 更新点赞数 */
    int updateLikeCount(@Param("id") Integer id, @Param("delta") int delta);

    /** 更新评论数 */
    int updateCommentCount(@Param("id") Integer id, @Param("delta") int delta);

    /** 更新下载数 */
    int updateDownloadCount(@Param("id") Integer id);
}