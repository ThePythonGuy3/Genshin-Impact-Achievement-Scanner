package pyguy;

import pyguy.util.CompressionUtils;

import java.io.*;

public class Settings
{
    public enum AppMode
    {
        DARK,
        LIGHT
    }

    public enum Language
    {
        en_us,
        //es
    }

    private static class SettingsData
    {
        public AppMode  appMode    = AppMode.DARK;
        public Language language   = Language.en_us;
        public int      maxThreads = -1; // -1 means max threads available
    }

    private static final File SETTINGS_FILE = new File("settings");

    private static SettingsData settingsData;

    static
    {
        if (SETTINGS_FILE.exists())
        {
            try
            {
                settingsData = CompressionUtils.LoadObject(SETTINGS_FILE, SettingsData.class);
            } catch (IOException ignored)
            {
                settingsData = new SettingsData();

                UpdateSettings();
            }
        }
        else
        {
            settingsData = new SettingsData();

            UpdateSettings();
        }
    }

    private static void UpdateSettings()
    {
        try
        {
            CompressionUtils.StoreObject(SETTINGS_FILE, settingsData);
        } catch (IOException ignored) {}
    }

    public static AppMode GetAppMode()
    {
        return settingsData.appMode;
    }

    public static void SetAppMode(AppMode appMode)
    {
        settingsData.appMode = appMode;

        UpdateSettings();
    }

    public static Language GetLanguage()
    {
        return settingsData.language;
    }

    public static void SetLanguage(Language language)
    {
        settingsData.language = language;

        UpdateSettings();
    }

    public static int GetMaxThreads()
    {
        return settingsData.maxThreads;
    }

    public static void SetMaxThreads(int maxThreads)
    {
        settingsData.maxThreads = maxThreads;

        UpdateSettings();
    }
}
