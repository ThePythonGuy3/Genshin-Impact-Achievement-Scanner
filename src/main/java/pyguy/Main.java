package pyguy;

import org.bytedeco.ffmpeg.global.avutil;

public class Main
{
    public static void main(String[] args)
    {
        // Get rid of library logging
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
        avutil.av_log_set_level(avutil.AV_LOG_QUIET);

        I18N.LoadLanguageMap();

        GUI.OpenMainGUI();
    }
}