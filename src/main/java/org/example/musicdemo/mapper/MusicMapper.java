package org.example.musicdemo.mapper;

import org.apache.ibatis.annotations.Param;
import org.example.musicdemo.entity.Music;

import java.util.List;

/**
 * 音乐数据访问层接口，操作 music 表。
 */
public interface MusicMapper {

    /** 分页查询音乐列表（支持关键词搜索） */
    List<Music> findList(@Param("offset") int offset,
                         @Param("size") int size,
                         @Param("keyword") String keyword);

    /** 统计符合条件的音乐总数 */
    int countList(@Param("keyword") String keyword);

    /** 根据 ID 查询单个音乐 */
    Music findById(@Param("id") Integer id);

    /** 插入新音乐记录 */
    int insert(Music music);

    /** 根据 ID 删除音乐 */
    int deleteById(@Param("id") Integer id);

    /** 更新封面图路径 */
    int updateCoverPath(@Param("id") Integer id, @Param("coverPath") String coverPath);

    /** 更新歌词内容 */
    int updateLyrics(@Param("id") Integer id, @Param("lyrics") String lyrics);

    /** 统计歌曲的点赞数 */
    int countLikes(@Param("id") Integer id);

    /** 统计歌曲的评论数 */
    int countComments(@Param("id") Integer id);

    /** 统计歌曲的下载数 */
    int countDownloads(@Param("id") Integer id);

    /** 本周点赞 TOP10 */
    List<Music> findWeeklyLikeTop10(@Param("start") String start, @Param("end") String end);

    /** 本周下载 TOP10 */
    List<Music> findWeeklyDownloadTop10(@Param("start") String start, @Param("end") String end);

    /** 本周评论 TOP10 */
    List<Music> findWeeklyCommentTop10(@Param("start") String start, @Param("end") String end);
}