package org.example.musicdemo.service;

import org.example.musicdemo.entity.Comment;
import org.example.musicdemo.mapper.CommentMapper;
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
class CommentServiceTest {

    @Mock
    private CommentMapper commentMapper;
    @Mock
    private SensitiveWordService sensitiveWordService;

    private CommentService commentService;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(commentMapper, sensitiveWordService);
    }

    @Test
    void listByMusicId_ShouldReturnComments() {
        Comment c1 = new Comment(); c1.setId(1); c1.setContent("Nice!");
        Comment c2 = new Comment(); c2.setId(2); c2.setContent("Great!");
        when(commentMapper.findByMusicId(5)).thenReturn(List.of(c1, c2));

        List<Comment> result = commentService.listByMusicId(5);

        assertEquals(2, result.size());
        verify(commentMapper).findByMusicId(5);
    }

    @Test
    void add_ShouldInsert_WhenContentIsClean() {
        Comment comment = new Comment();
        comment.setContent("Good song");
        comment.setUserId(1);
        comment.setMusicId(5);
        when(sensitiveWordService.containsForbidden("Good song")).thenReturn(false);

        commentService.add(comment);

        verify(commentMapper).insert(comment);
    }

    @Test
    void add_ShouldThrow_WhenContentContainsForbiddenWords() {
        Comment comment = new Comment();
        comment.setContent("badword");
        when(sensitiveWordService.containsForbidden("badword")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> commentService.add(comment));
        verify(commentMapper, never()).insert(any());
    }

    @Test
    void findById_ShouldReturnComment_WhenExists() {
        Comment mock = new Comment(); mock.setId(1); mock.setContent("Test");
        when(commentMapper.findById(1)).thenReturn(mock);

        Comment result = commentService.findById(1);

        assertNotNull(result);
        assertEquals("Test", result.getContent());
    }

    @Test
    void findById_ShouldReturnNull_WhenNotExists() {
        when(commentMapper.findById(999)).thenReturn(null);
        assertNull(commentService.findById(999));
    }

    @Test
    void delete_ShouldCallMapper() {
        commentService.delete(1);
        verify(commentMapper).deleteById(1);
    }
}
