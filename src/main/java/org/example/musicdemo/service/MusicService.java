package org.example.musicdemo.service;

import org.example.musicdemo.entity.DownloadRecord;
import org.example.musicdemo.entity.Music;
import org.example.musicdemo.mapper.DownloadRecordMapper;
import org.example.musicdemo.mapper.MusicMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 音乐服务 —— 核心业务服务，提供音乐的增删改查及文件上传下载功能。
 * <p>
 * 音乐模块是本系统最核心的模块，涉及：
 * <ul>
 *   <li>文件存储：音乐文件上传到本地磁盘（路径由 {@code music.file-path} 配置）</li>
 *   <li>数据库记录：文件名、大小、上传者、点赞/评论/下载计数等</li>
 *   <li>文件格式校验：仅允许常见的音频/视频格式</li>
 *   <li>下载记录：每次下载都会在 {@code download_record} 表中插入一条记录</li>
 * </ul>
 * </p>
 *
 * <h3>事务边界</h3>
 * <ul>
 *   <li>{@link #upload}：文件写入磁盘 + 数据库插入，事务只覆盖 DB 操作</li>
 *   <li>{@link #delete}：删除数据库记录（文件本身暂不删除）</li>
 *   <li>{@link #download}：插入下载记录 + 更新下载计数</li>
 * </ul>
 */
@Service
public class MusicService {

    private static final Logger log = LoggerFactory.getLogger(MusicService.class);

    /** 音乐表的数据访问层接口 */
    private final MusicMapper musicMapper;

    /** 下载记录表的数据访问层接口 */
    private final DownloadRecordMapper downloadRecordMapper;

    /** 封面图搜索下载服务 */
    private final CoverService coverService;

    /** 音乐文件存储的根目录路径，从配置文件中注入 */
    @Value("${music.file-path}")
    private String filePath;

    /**
     * 构造器注入。
     *
     * @param musicMapper          音乐 Mapper
     * @param downloadRecordMapper 下载记录 Mapper
     * @param coverService         封面服务
     */
    public MusicService(MusicMapper musicMapper, DownloadRecordMapper downloadRecordMapper, CoverService coverService) {
        this.musicMapper = musicMapper;
        this.downloadRecordMapper = downloadRecordMapper;
        this.coverService = coverService;
    }

    /**
     * 分页查询音乐列表，支持关键词模糊搜索（标题/歌手匹配）。
     * <p>
     * 分页使用 MySQL 的 LIMIT 语法，偏移量 = (page - 1) * size。
     * 如果 keyword 为 null 或空，则查询全部。
     * </p>
     *
     * @param page    当前页码（从 1 开始）
     * @param size    每页条数
     * @param keyword 搜索关键词（可为 null）
     * @return 音乐列表
     */
    public List<Music> list(int page, int size, String keyword) {
        int offset = (page - 1) * size;
        return musicMapper.findList(offset, size, keyword);
    }

    /**
     * 统计符合关键词条件的音乐总数（用于前端分页组件计算总页数）。
     *
     * @param keyword 搜索关键词（可为 null）
     * @return 匹配的音乐总数
     */
    public int count(String keyword) {
        return musicMapper.countList(keyword);
    }

    /**
     * 根据 ID 查询单首音乐的完整信息。
     *
     * @param id 音乐 ID
     * @return Music 实体，如果不存在则返回 null
     */
    public Music findById(Integer id) {
        return musicMapper.findById(id);
    }

    /** 允许上传的音频/视频文件扩展名白名单（小写） */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".mp3", ".flac", ".wav", ".aac", ".ogg", ".m4a", ".mp4");

    /**
     * 上传音乐文件并保存信息到数据库。
     * <p>
     * 处理流程：
     * <ol>
     *   <li>检查文件扩展名是否在白名单中</li>
     *   <li>生成 UUID 文件名防止重名冲突</li>
     *   <li>确保目标目录存在，不存在则自动创建</li>
     *   <li>将 {@link MultipartFile} 写入磁盘</li>
     *   <li>构建 {@link Music} 实体，初始点赞/评论/下载数均为 0</li>
     *   <li>插入数据库</li>
     * </ol>
     * </p>
     *
     * <h3>事务说明</h3>
     * 标注了 {@link Transactional}，但事务仅覆盖数据库插入操作。
     * 如果文件写入成功但数据库插入失败，会导致磁盘出现废弃文件（孤文件）。
     * 可通过定时任务或回滚后主动删除文件来优化。
     *
     * @param file        上传的 multipart 文件
     * @param title       歌曲标题
     * @param artist      歌手名
     * @param description 歌曲简介（可为 null）
     * @param userId      上传用户的 ID
     * @return 包含数据库自增 ID 的完整 Music 对象
     * @throws IOException          文件写入磁盘失败时抛出
     * @throws RuntimeException     文件名为空或格式不支持时抛出
     */
    @Transactional
    public Music upload(MultipartFile file, String title, String artist,
                        String description, Integer userId) throws IOException {
        // ---- 1. 文件名校验 ----
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new RuntimeException("文件名不能为空");
        }
        // 提取扩展名并转小写，匹配白名单
        String ext = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new RuntimeException("仅支持 MP3、FLAC、WAV、AAC、OGG、M4A、MP4 格式");
        }

        // ---- 2. 生成唯一文件名 ----
        // 使用 UUID 避免中文文件名乱码和文件名冲突
        String newFilename = UUID.randomUUID().toString() + ext;

        // ---- 3. 确保目录存在 ----
        File dir = new File(filePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // ---- 4. 写入磁盘 ----
        File destFile = new File(filePath + newFilename);
        file.transferTo(destFile);

        // ---- 5. 构建实体 ----
        Music music = new Music();
        music.setTitle(title);
        music.setArtist(artist);
        music.setDescription(description);
        music.setFilePath(newFilename);      // 只存相对文件名，完整路径由 WebConfig 静态映射拼接
        music.setFileSize(file.getSize());   // 单位：字节
        music.setLikeCount(0);
        music.setCommentCount(0);
        music.setDownloadCount(0);
        music.setUserId(userId);

        // ---- 6. 入库 ----
        musicMapper.insert(music);

        // ---- 7. 后台异步获取封面图 ----
        try {
            String coverPath = coverService.fetchCover(title, artist);
            if (coverPath != null) {
                music.setCoverPath(coverPath);
                // 直接用新 SQL 更新 cover_path 字段
                musicMapper.updateCoverPath(music.getId(), coverPath);
            }
        } catch (Exception e) {
            log.warn("fetch cover failed for {} - {}: {}", title, artist, e.getMessage());
        }

        return music;
    }

    /**
     * 根据 ID 删除音乐，级联关联记录由数据库 ON DELETE CASCADE 自动处理。
     * <p>
     * <b>注意：</b>同时会删除服务器上的音频文件和封面文件。
     * </p>
     *
     * @param id 要删除的音乐 ID
     */
    @Transactional
    public void delete(Integer id) {
        Music music = musicMapper.findById(id);
        if (music == null) return;

        // 删除音频文件
        if (music.getFilePath() != null) {
            File file = new File(filePath + music.getFilePath());
            if (file.exists()) file.delete();
        }

        // 删除封面文件
        if (music.getCoverPath() != null) {
            File cover = new File(filePath + music.getCoverPath());
            if (cover.exists()) cover.delete();
        }

        musicMapper.deleteById(id);
    }

    /**
     * 记录用户的下载行为并更新音乐的下载计数。
     * <p>
     * 每次下载都会在 {@code download_record} 表中插入一条记录，
     * 并将 {@code music.download_count} 字段加 1。
     * 此操作用于后续的周下载排行榜统计。
     * </p>
     *
     * <h3>异常处理</h3>
     * 如果 musicId 对应的音乐不存在，抛出 {@link RuntimeException}。
     *
     * @param musicId 待下载的音乐 ID
     * @param userId  下载用户的 ID
     * @return 被下载的 Music 完整信息（前端可据此拼接文件 URL 或展示详情）
     * @throws RuntimeException 音乐不存在时抛出
     */
    @Transactional
    public Music download(Integer musicId, Integer userId) {
        // 先查询音乐是否存在
        Music music = musicMapper.findById(musicId);
        if (music == null) {
            throw new RuntimeException("音乐不存在");
        }

        // 插入下载记录
        DownloadRecord record = new DownloadRecord();
        record.setUserId(userId);
        record.setMusicId(musicId);
        downloadRecordMapper.insert(record);

        // 更新下载计数
        musicMapper.updateDownloadCount(musicId);

        return music;
    }
}
