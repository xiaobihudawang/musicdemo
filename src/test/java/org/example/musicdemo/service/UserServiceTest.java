package org.example.musicdemo.service;

import org.example.musicdemo.entity.User;
import org.example.musicdemo.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserService(userMapper, passwordEncoder);
    }

    @Test
    void findByUsername_ShouldReturnUser_WhenExists() {
        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setUsername("testuser");
        when(userMapper.findByUsername("testuser")).thenReturn(mockUser);

        User result = userService.findByUsername("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userMapper).findByUsername("testuser");
    }

    @Test
    void findByUsername_ShouldReturnNull_WhenNotExists() {
        when(userMapper.findByUsername("nonexistent")).thenReturn(null);

        User result = userService.findByUsername("nonexistent");

        assertNull(result);
    }

    @Test
    void register_ShouldCreateUser_WhenUsernameIsNew() {
        when(userMapper.findByUsername("newuser")).thenReturn(null);
        when(userMapper.insert(any())).thenReturn(1);

        User result = userService.register("newuser", "password123", "New User", "new@test.com");

        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        assertEquals("New User", result.getName());
        assertTrue(result.getEnabled());
        assertEquals("user", result.getRole());
        assertTrue(passwordEncoder.matches("password123", result.getPassword()));
        verify(userMapper).insert(any());
    }

    @Test
    void register_ShouldThrow_WhenUsernameExists() {
        User existing = new User();
        existing.setUsername("existing");
        when(userMapper.findByUsername("existing")).thenReturn(existing);

        assertThrows(RuntimeException.class, () ->
            userService.register("existing", "pass", "Name", "e@t.com")
        );
        verify(userMapper, never()).insert(any());
    }

    @Test
    void deleteById_ShouldCallBatchDelete() {
        userService.deleteById(1);
        verify(userMapper).batchDeleteUser(1);
    }

    @Test
    void validatePassword_ShouldReturnTrue_WhenMatch() {
        String raw = "mypassword";
        String encoded = passwordEncoder.encode(raw);
        assertTrue(userService.validatePassword(raw, encoded));
    }

    @Test
    void validatePassword_ShouldReturnFalse_WhenNotMatch() {
        String encoded = passwordEncoder.encode("correct");
        assertFalse(userService.validatePassword("wrong", encoded));
    }

    @Test
    void findAll_ShouldReturnAllUsers() {
        when(userMapper.findAll()).thenReturn(java.util.List.of(new User(), new User()));
        assertEquals(2, userService.findAll().size());
    }

    @Test
    void toggleEnabled_ShouldCallUpdateEnabled() {
        userService.toggleEnabled(1, false);
        verify(userMapper).updateEnabled(1, false);
    }
}
