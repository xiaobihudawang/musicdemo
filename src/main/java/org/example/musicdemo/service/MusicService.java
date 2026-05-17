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

/**
 * 音乐服务，提供音乐的增删改查及文件上传下载功能
 */
@Service
public class MusicService {

    private final MusicMapper musicMapper;
    private final DownloadRecordMapper downloadRecordMapper;

    @Value("${music.file-path}")
    private String filePath;

    public MusicService(MusicMapper musicMapper, DownloadRecordMapper downloadRecordMapper) {
        this.musicMapper = musicMapper;
        this.downloadRecordMapper = downloadRecordMapper;
    }

    /**
     * 分页查询音乐列表，支持关键词模糊搜索
     */
    public List<Music> list(int page, int size, String keyword) {
        int offset = (page - 1) * size;
        return musicMapper.findList(offset, size, keyword);
    }

    /**
     * 统计符合关键词条件的音乐总数
     */
    public int count(String keyword) {
        return musicMapper.countList(keyword);
    }

    /**
     * 根据ID查询单首音乐
     */
    public Music findById(Integer id) {
        return musicMapper.findById(id);
    }

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".mp3", ".flac", ".wav", ".aac", ".ogg", ".m4a", ".mp4");

    /**
     * 上传音乐文件并保存信息到数据库
     */
    @Transactional
    public Music upload(MultipartFile file, String title, String artist,
                        String description, Integer userId) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new RuntimeException("文件名不能为空");
        }
        String ext = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new RuntimeException("仅支持 MP3、FLAC、WAV、AAC、OGG、M4A、MP4 格式");
        }

        String newFilename = UUID.randomUUID().toString() + ext;

        File dir = new File(filePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File destFile = new File(filePath + newFilename);
        file.transferTo(destFile);

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
        return music;
    }

    /**
     * 根据ID删除音乐记录
     */
    @Transactional
    public void delete(Integer id) {
        musicMapper.deleteById(id);
    }

    /**
     * 记录下载行为并更新下载计数
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
