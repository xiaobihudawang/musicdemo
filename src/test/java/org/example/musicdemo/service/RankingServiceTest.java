package org.example.musicdemo.service;

import org.example.musicdemo.entity.Music;
import org.example.musicdemo.mapper.MusicMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

    @Mock
    private MusicMapper musicMapper;

    private RankingService rankingService;

    @BeforeEach
    void setUp() {
        rankingService = new RankingService(musicMapper);
    }

    @Test
    void getWeeklyLikeTop10_ShouldReturnList() {
        Music m1 = new Music(); m1.setId(1); m1.setTitle("Top Hit");
        when(musicMapper.findWeeklyLikeTop10(anyString(), anyString())).thenReturn(List.of(m1));

        List<Music> result = rankingService.getWeeklyLikeTop10();

        assertEquals(1, result.size());
        assertEquals("Top Hit", result.get(0).getTitle());
        verify(musicMapper).findWeeklyLikeTop10(anyString(), anyString());
    }

    @Test
    void getWeeklyDownloadTop10_ShouldReturnList() {
        when(musicMapper.findWeeklyDownloadTop10(anyString(), anyString())).thenReturn(List.of());
        List<Music> result = rankingService.getWeeklyDownloadTop10();
        assertTrue(result.isEmpty());
    }

    @Test
    void getWeeklyCommentTop10_ShouldReturnList() {
        Music m1 = new Music(); m1.setId(1);
        Music m2 = new Music(); m2.setId(2);
        when(musicMapper.findWeeklyCommentTop10(anyString(), anyString())).thenReturn(List.of(m1, m2));

        List<Music> result = rankingService.getWeeklyCommentTop10();

        assertEquals(2, result.size());
    }
}
