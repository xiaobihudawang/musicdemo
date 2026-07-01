package org.example.musicdemo.service;

import org.example.musicdemo.entity.LikeRecord;
import org.example.musicdemo.mapper.LikeRecordMapper;
import org.example.musicdemo.mapper.MusicMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock
    private LikeRecordMapper likeRecordMapper;
    @Mock
    private MusicMapper musicMapper;

    private LikeService likeService;

    @BeforeEach
    void setUp() {
        likeService = new LikeService(likeRecordMapper, musicMapper);
    }

    @Test
    void toggle_ShouldLike_WhenNotLiked() {
        when(musicMapper.getLikeCountById(5)).thenReturn(3);

        Map<String, Object> result = likeService.toggle(1, 5);

        assertTrue((Boolean) result.get("liked"));
        assertEquals(3, result.get("likeCount"));
        verify(likeRecordMapper).insert(any());
        verify(likeRecordMapper, never()).delete(anyInt(), anyInt());
    }

    @Test
    void toggle_ShouldUnlike_WhenAlreadyLiked() {
        when(likeRecordMapper.insert(any())).thenThrow(new DuplicateKeyException("dup"));
        when(musicMapper.getLikeCountById(5)).thenReturn(2);

        Map<String, Object> result = likeService.toggle(1, 5);

        assertFalse((Boolean) result.get("liked"));
        assertEquals(2, result.get("likeCount"));
        verify(likeRecordMapper).delete(1, 5);
        verify(likeRecordMapper).insert(any());
    }

    @Test
    void isLiked_ShouldReturnTrue_WhenRecordExists() {
        when(likeRecordMapper.findByUserAndMusic(1, 5)).thenReturn(new LikeRecord());
        assertTrue(likeService.isLiked(1, 5));
    }

    @Test
    void isLiked_ShouldReturnFalse_WhenNoRecord() {
        when(likeRecordMapper.findByUserAndMusic(1, 5)).thenReturn(null);
        assertFalse(likeService.isLiked(1, 5));
    }
}
