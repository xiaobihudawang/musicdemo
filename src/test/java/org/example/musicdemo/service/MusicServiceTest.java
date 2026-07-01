package org.example.musicdemo.service;

import org.example.musicdemo.entity.Music;
import org.example.musicdemo.mapper.DownloadRecordMapper;
import org.example.musicdemo.mapper.MusicMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MusicServiceTest {

    @Mock
    private MusicMapper musicMapper;
    @Mock
    private DownloadRecordMapper downloadRecordMapper;
    @Mock
    private CoverService coverService;
    @Mock
    private LyricsService lyricsService;

    private MusicService musicService;

    @BeforeEach
    void setUp() {
        musicService = new MusicService(musicMapper, downloadRecordMapper, coverService, lyricsService);
    }

    @Test
    void list_ShouldReturnPagedResults() {
        Music m1 = new Music(); m1.setId(1); m1.setTitle("Song A");
        Music m2 = new Music(); m2.setId(2); m2.setTitle("Song B");
        when(musicMapper.findList(0, 10, null)).thenReturn(List.of(m1, m2));

        List<Music> result = musicService.list(1, 10, null);

        assertEquals(2, result.size());
        assertEquals("Song A", result.get(0).getTitle());
        verify(musicMapper).findList(0, 10, null);
    }

    @Test
    void list_ShouldApplyKeywordFilter() {
        when(musicMapper.findList(0, 5, "love")).thenReturn(List.of());
        List<Music> result = musicService.list(1, 5, "love");
        assertTrue(result.isEmpty());
        verify(musicMapper).findList(0, 5, "love");
    }

    @Test
    void count_ShouldReturnTotal() {
        when(musicMapper.countList("test")).thenReturn(42);
        assertEquals(42, musicService.count("test"));
    }

    @Test
    void findById_ShouldReturnMusic_WhenExists() {
        Music mock = new Music(); mock.setId(1); mock.setTitle("Test");
        when(musicMapper.findById(1)).thenReturn(mock);

        Music result = musicService.findById(1);
        assertNotNull(result);
        assertEquals("Test", result.getTitle());
    }

    @Test
    void findById_ShouldReturnNull_WhenNotExists() {
        when(musicMapper.findById(999)).thenReturn(null);
        assertNull(musicService.findById(999));
    }

    @Test
    void delete_ShouldCallMapper() {
        Music mock = new Music(); mock.setId(1);
        when(musicMapper.findById(1)).thenReturn(mock);

        musicService.delete(1);

        verify(musicMapper).deleteById(1);
    }

    @Test
    void upload_ShouldRejectInvalidExtension() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("song.exe");

        assertThrows(RuntimeException.class, () ->
            musicService.upload(file, "Song", "Artist", "desc", 1)
        );
        verify(musicMapper, never()).insert(any());
    }

    @Test
    void download_ShouldRecordAndReturnMusic() {
        Music mock = new Music(); mock.setId(1); mock.setTitle("Test");
        when(musicMapper.findById(1)).thenReturn(mock);

        Music result = musicService.download(1, 42);

        assertNotNull(result);
        assertEquals("Test", result.getTitle());
        verify(downloadRecordMapper).insert(argThat(r ->
            r.getUserId() == 42 && r.getMusicId() == 1
        ));
    }

    @Test
    void download_ShouldThrow_WhenMusicNotFound() {
        when(musicMapper.findById(999)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> musicService.download(999, 1));
    }
}
