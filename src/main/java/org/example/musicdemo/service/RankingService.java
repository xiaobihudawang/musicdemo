package org.example.musicdemo.service;

import org.example.musicdemo.entity.Music;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 排行榜服务，使用原生JDBC查询每周点赞/下载/评论排行数据
 */
@Service
public class RankingService {

    private final DataSource dataSource;

    public RankingService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 获取当前周周一至周日的时间范围（字符串数组[start, end]）
     */
    private String[] getWeekRange() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);

        String start = monday.atStartOfDay().toString().replace("T", " ");
        String end = sunday.atTime(LocalTime.MAX).toString().replace("T", " ");
        return new String[]{start, end};
    }

    /**
     * 查询本周点赞数最高的前10首音乐
     */
    public List<Music> getWeeklyLikeTop10() {
        String[] range = getWeekRange();
        String sql = """
            SELECT m.*, u.username, COUNT(lr.id) AS weekly_count
            FROM like_record lr
            JOIN music m ON lr.music_id = m.id
            JOIN user u ON m.user_id = u.id
            WHERE lr.create_time BETWEEN ? AND ?
            GROUP BY lr.music_id
            ORDER BY weekly_count DESC
            LIMIT 10
            """;
        return queryRanking(sql, range[0], range[1]);
    }

    /**
     * 查询本周下载数最高的前10首音乐
     */
    public List<Music> getWeeklyDownloadTop10() {
        String[] range = getWeekRange();
        String sql = """
            SELECT m.*, u.username, COUNT(dr.id) AS weekly_count
            FROM download_record dr
            JOIN music m ON dr.music_id = m.id
            JOIN user u ON m.user_id = u.id
            WHERE dr.create_time BETWEEN ? AND ?
            GROUP BY dr.music_id
            ORDER BY weekly_count DESC
            LIMIT 10
            """;
        return queryRanking(sql, range[0], range[1]);
    }

    /**
     * 查询本周评论数最高的前10首音乐
     */
    public List<Music> getWeeklyCommentTop10() {
        String[] range = getWeekRange();
        String sql = """
            SELECT m.*, u.username, COUNT(c.id) AS weekly_count
            FROM comment c
            JOIN music m ON c.music_id = m.id
            JOIN user u ON m.user_id = u.id
            WHERE c.create_time BETWEEN ? AND ?
            GROUP BY c.music_id
            ORDER BY weekly_count DESC
            LIMIT 10
            """;
        return queryRanking(sql, range[0], range[1]);
    }

    /**
     * 执行原始排行SQL并手动映射结果到Music对象列表
     */
    private List<Music> queryRanking(String sql, String start, String end) {
        List<Music> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, start);
            ps.setString(2, end);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Music music = new Music();
                    music.setId(rs.getInt("id"));
                    music.setTitle(rs.getString("title"));
                    music.setArtist(rs.getString("artist"));
                    music.setDescription(rs.getString("description"));
                    music.setFilePath(rs.getString("file_path"));
                    music.setFileSize(rs.getLong("file_size"));
                    music.setLikeCount(rs.getInt("like_count"));
                    music.setCommentCount(rs.getInt("comment_count"));
                    music.setDownloadCount(rs.getInt("download_count"));
                    music.setUserId(rs.getInt("user_id"));
                    music.setUsername(rs.getString("username"));
                    list.add(music);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("排行查询失败", e);
        }
        return list;
    }
}
