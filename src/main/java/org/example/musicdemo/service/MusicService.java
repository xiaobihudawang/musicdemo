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
 * 音乐服务，提供音乐的增删改查及文件上传下载功能。
 */
@Service
public class MusicService {

    private static final Logger log = LoggerFactory.getLogger(MusicService.class);

    private final MusicMapper musicMapper;
    private final DownloadRecordMapper downloadRecordMapper;
    private final CoverService coverService;

    @Value("${music.file-path}")
    private String filePath;

    public MusicService(MusicMapper musicMapper, DownloadRecordMapper downloadRecordMapper, CoverService coverService) {
        this.musicMapper = musicMapper;
        this.downloadRecordMapper = downloadRecordMapper;
        this.coverService = coverService;
    }

    /** 分页查询音乐列表，支持关键词模糊搜索 */
    public List<Music> list(int page, int size, String keyword) {
        int offset = (page - 1) * size;
        return musicMapper.findList(offset, size, keyword);
    }

    /** 统计符合条件的音乐总数（用于分页） */
    public int count(String keyword) {
        return musicMapper.countList(keyword);
    }

    /** 根据 ID 查询单首音乐 */
    public Music findById(Integer id) {
        return musicMapper.findById(id);
    }

    /** 允许上传的文件扩展名白名单 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".mp3", ".flac", ".wav", ".aac", ".ogg", ".m4a", ".mp4");

    /**
     * 上传音乐文件并保存信息到数据库。
     * 流程：校验扩展名 → 写入磁盘 → 入库 → 异步获取封面
     * 先写文件再入库，如果入库失败则删除已上传的文件。
     */
    @Transactional
    public Music upload(MultipartFile file, String title, String artist,
                        String description, Integer userId) throws IOException {
        // 文件名校验
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new RuntimeException("文件名不能为空");
        }
        String ext = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new RuntimeException("仅支持 MP3、FLAC、WAV、AAC、OGG、M4A、MP4 格式");
        }

        // 生成唯一文件名
        String newFilename = UUID.randomUUID() + ext;

        // 确保目录存在
        File dir = new File(filePath);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new RuntimeException("无法创建目录: " + dir.getAbsolutePath());
        }

        // 先写入磁盘
        File destFile = new File(filePath + newFilename);
        file.transferTo(destFile);

        // 再插入数据库，如果失败则删除已上传的文件
        try {
            Music music = new Music();
            music.setTitle(title);
            music.setArtist(artist);
            music.setDescription(description);
            music.setFilePath(newFilename);
            music.setFileSize(file.getSize());
            music.setLikeCount(0);
            music.setCommentCount(0);
            music.setDownloadCount(0);
            music.setUserId(userId);
            musicMapper.insert(music);
            coverService.fetchCoverAndUpdate(music.getId(), title, artist);
            return music;
        } catch (Exception e) {
            boolean deleted = destFile.delete();
            if (!deleted) {
                log.warn("无法删除已上传的文件: {}", destFile.getAbsolutePath());
            }
            throw e;
        }
    }

    /**
     * 删除音乐，同时删除音频文件和封面文件。
     * 关联记录（评论、点赞、下载记录）由数据库 ON DELETE CASCADE 自动处理。
     */
    @Transactional
    public void delete(Integer id) {
        Music music = musicMapper.findById(id);
        if (music == null) return;

        // 删除音频文件
        if (music.getFilePath() != null) {
            File file = new File(filePath + music.getFilePath());
            if (file.exists() && !file.delete()) {
                log.warn("无法删除音频文件: {}", file.getAbsolutePath());
            }
        }

        // 删除封面文件
        if (music.getCoverPath() != null) {
            File cover = new File(filePath + music.getCoverPath());
            if (cover.exists() && !cover.delete()) {
                log.warn("无法删除封面文件: {}", cover.getAbsolutePath());
            }
        }

        musicMapper.deleteById(id);
    }

    /**
     * 记录下载并更新下载计数。
     * 每次下载插入 download_record 记录，并将 music.download_count +1。
     */
    @Transactional
    public Music download(Integer musicId, Integer userId) {
        Music music = musicMapper.findById(musicId);
        if (music == null) {
            throw new RuntimeException("音乐不存在");
        }

        DownloadRecord record = new DownloadRecord();
        record.setUserId(userId);
        record.setMusicId(musicId);
        downloadRecordMapper.insert(record);

        musicMapper.updateDownloadCount(musicId);

        return music;
    }
}
