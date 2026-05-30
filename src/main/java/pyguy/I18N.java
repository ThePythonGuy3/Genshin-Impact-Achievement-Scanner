package pyguy;

import com.google.gson.*;

import java.io.*;
import java.util.*;

public class I18N
{
    private static final String GLOBAL_MAP = "/i18n/global.json";

    private static final Map<String, String> languageMap = new HashMap<>();

    public static void LoadLanguageMap()
    {
        String resource = "/i18n/" + Settings.GetLanguage() + ".json";

        languageMap.clear();

        // Global map
        try (InputStream stream = I18N.class.getResourceAsStream(GLOBAL_MAP))
        {
            if (stream == null)
                return;

            JsonElement parsed = JsonParser.parseReader(new BufferedReader(new InputStreamReader(stream)));
            if (!parsed.isJsonObject())
                return;

            JsonObject jsonLanguageMap = parsed.getAsJsonObject();
            for (String key : jsonLanguageMap.keySet())
            {
                languageMap.put(key, jsonLanguageMap.get(key).getAsString());
            }
        } catch (IOException ignored) {}

        // Language-specific map
        try (InputStream stream = I18N.class.getResourceAsStream(resource))
        {
            if (stream == null)
                return;

            JsonElement parsed = JsonParser.parseReader(new BufferedReader(new InputStreamReader(stream)));
            if (!parsed.isJsonObject())
                return;

            JsonObject jsonLanguageMap = parsed.getAsJsonObject();
            for (String key : jsonLanguageMap.keySet())
            {
                languageMap.put(key, jsonLanguageMap.get(key).getAsString());
            }
        } catch (IOException ignored) {}
    }

    public static String GetString(String key)
    {
        return languageMap.getOrDefault(key, "NULL");
    }
}
