package com.example.musicplayer;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.musicplayer.core.theme.ThemeManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class ThemeManagerTest {

    private ThemeManager themeManager;
    private Context context;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    @Before
    public void setUp() {
        context = mock(Context.class);
        sharedPreferences = mock(SharedPreferences.class);
        editor = mock(SharedPreferences.Editor.class);

        when(context.getApplicationContext()).thenReturn(context);
        when(context.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPreferences);
        when(sharedPreferences.edit()).thenReturn(editor);
        when(editor.putInt(anyString(), anyInt())).thenReturn(editor);

        // Reset the singleton instance for testing if possible, 
        // but ThemeManager.instance is private. 
        // We'll just rely on the mock context returning our mocked prefs.
        themeManager = ThemeManager.getInstance(context);
    }

    @Test
    public void testSetThemeMode() {
        int mode = ThemeManager.THEME_DARK;
        themeManager.setThemeMode(mode);

        verify(editor).putInt("theme_mode", mode);
        verify(editor).apply();
    }

    @Test
    public void testGetThemeMode_Default() {
        when(sharedPreferences.getInt(anyString(), anyInt())).thenReturn(ThemeManager.THEME_SYSTEM);
        int mode = themeManager.getThemeMode();
        assertEquals(ThemeManager.THEME_SYSTEM, mode);
    }

    @Test
    public void testGetThemeName() {
        when(sharedPreferences.getInt("theme_mode", ThemeManager.THEME_SYSTEM)).thenReturn(ThemeManager.THEME_LIGHT);
        assertEquals("浅色模式", themeManager.getThemeName(context));

        when(sharedPreferences.getInt("theme_mode", ThemeManager.THEME_SYSTEM)).thenReturn(ThemeManager.THEME_DARK);
        assertEquals("深色模式", themeManager.getThemeName(context));

        when(sharedPreferences.getInt("theme_mode", ThemeManager.THEME_SYSTEM)).thenReturn(ThemeManager.THEME_SYSTEM);
        assertEquals("跟随系统", themeManager.getThemeName(context));
    }
}
