package pyguy;

import com.google.gson.*;
import org.graalvm.polyglot.*;
import pyguy.types.*;
import pyguy.util.CompressionUtils;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.*;
import java.util.*;

public class DatabaseReader
{
    private static final String PAIMON_MOE_BASE_URL     = "https://paimon.moe";
    private static final String PAIMON_MOE_MANIFEST_URL = PAIMON_MOE_BASE_URL + "/manifest.json";
    private static final String PAIMON_MOE_SVELTE_TAG   = "src/routes/achievement/index.svelte";
    private static final String PAIMON_MOE_JSON_TAG     = "const Pa=";
    private static final File   DATABASE_FILE           = new File("db");

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private static String GetHTTP(String url) throws IOException, InterruptedException
    {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build();

        HttpResponse<String> response = CLIENT.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200)
            throw new IOException("HTTP " + response.statusCode());

        return response.body();
    }

    private static JsonObject GetPaimonMoeJSON() throws DatabaseException, IOException, InterruptedException
    {
        JsonObject manifest = JsonParser.parseString(GetHTTP(PAIMON_MOE_MANIFEST_URL)).getAsJsonObject();

        if (!manifest.has(PAIMON_MOE_SVELTE_TAG)) throw new DatabaseException("Svelte Tag Missing.");

        JsonObject svelteTag = manifest.get(PAIMON_MOE_SVELTE_TAG).getAsJsonObject();

        if (!svelteTag.has("file")) throw new DatabaseException("File Tag Missing.");

        String fileURL = svelteTag.get("file").getAsString();

        String rawFile = GetHTTP(PAIMON_MOE_BASE_URL + "/" + fileURL);

        int tagIndex = rawFile.indexOf(PAIMON_MOE_JSON_TAG);

        if (tagIndex == -1) throw new DatabaseException("JSON Tag Missing.");

        String unterminatedJson = rawFile.substring(tagIndex + PAIMON_MOE_JSON_TAG.length());

        int     endIndex       = 0;
        int     bracketCounter = 0;
        boolean inString       = false;
        char    stringChar     = ' ';
        boolean escape         = false;

        while (endIndex < unterminatedJson.length())
        {
            char currentChar = unterminatedJson.charAt(endIndex);

            if (inString)
            {
                if (escape)
                    escape = false;
                else if (currentChar == '\\')
                    escape = true;
                else if (currentChar == stringChar)
                    inString = false;
            }
            else
            {
                if (currentChar == '"' || currentChar == '\'' || currentChar == '`')
                {
                    inString = true;
                    stringChar = currentChar;
                }
                else if (currentChar =='{')
                    bracketCounter++;
                else if (currentChar =='}')
                {
                    bracketCounter--;

                    if (bracketCounter == 0)
                    {
                        endIndex++;
                        break;
                    }
                }
            }

            endIndex++;
        }

        String pseudoJSON = unterminatedJson.substring(0, endIndex);

        try (Context context = Context.create("js"))
        {
            return JsonParser.parseString(context.eval("js", "JSON.stringify(" + pseudoJSON + ");").asString()).getAsJsonObject();
        }
    }

    private static Database ExtractPaimonMoeData() throws IOException, DatabaseException, InterruptedException
    {
        JsonObject resultJSON = GetPaimonMoeJSON();

        Map<Integer, Achievement> achievements = new HashMap<>();
        List<Category>            categories   = new ArrayList<>();

        resultJSON.asMap().forEach((key, value) -> {
            int categoryID;
            try
            {
                categoryID = Integer.parseInt(key);
            }
            catch (Exception e)
            {
                throw new DatabaseException("Wrong Category ID. ID: " + key + ".");
            }

            JsonObject valueObject = value.getAsJsonObject();

            if (!valueObject.has("order")) throw new DatabaseException("Wrong Category Order. ID: " + key + ".");

            int           order          = valueObject.get("order").getAsInt();
            String        name           = valueObject.has("name") ? valueObject.get("name").getAsString() : "";
            List<Integer> achievementMap = new ArrayList<>();

            if (!valueObject.has("achievements")) throw new DatabaseException("Wrong Achievements. ID: " + key + ".");

            JsonArray achievementList = valueObject.get("achievements").getAsJsonArray();

            for (JsonElement achievement : achievementList)
            {
                if (achievement.isJsonArray())
                {
                    int internalID = -1;
                    String achievementName = null;
                    List<Integer> achievementIDs    = new ArrayList<>();

                    for (JsonElement subAchievement : achievement.getAsJsonArray())
                    {
                        if (subAchievement.isJsonObject())
                        {
                            JsonObject map = subAchievement.getAsJsonObject();

                            if (!map.has("id"))
                                throw new DatabaseException("Wrong ID. ID: " + key + ".");

                            int achievementID = map.get("id").getAsInt();

                            if (internalID == -1)
                                internalID = achievementID;

                            if (achievementName == null)
                                achievementName = map.has("name") ? map.get("name").getAsString() : "";

                            achievementIDs.add(achievementID);
                        }
                        else throw new DatabaseException("Wrong Sub-Achievement. ID: " + key + ". Sub: " + subAchievement + ".");
                    }

                    achievements.put(internalID, new Achievement(
                        achievementName,
                        achievementIDs.stream().mapToInt(Integer::intValue).toArray()
                    ));

                    achievementMap.add(internalID);
                }
                else if (achievement.isJsonObject())
                {
                    JsonObject map = achievement.getAsJsonObject();

                    if (!map.has("id"))
                        throw new DatabaseException("Wrong ID. ID: " + key + ".");

                    int achievementID = map.get("id").getAsInt();

                    String achievementName = map.has("name") ? map.get("name").getAsString() : "";

                    achievements.put(achievementID, new Achievement(
                        achievementName,
                        new int[]{ achievementID })
                    );

                    achievementMap.add(achievementID);
                }
                else throw new DatabaseException("Wrong Achievement. ID: " + key + ". Achievement: " + achievement + ".");
            }

            categories.add(
                new Category(
                    categoryID,
                    order,
                    name,
                    achievementMap
                )
            );
        });

        categories.sort(Comparator.comparingInt(Category::order));
        return new Database(achievements, categories);
    }

    private static Database DownloadAndStore() throws IOException, InterruptedException
    {
        Database database = ExtractPaimonMoeData();

        CompressionUtils.StoreObject(DATABASE_FILE, database);

        return database;
    }

    public static Database LoadLocalDatabase()
    {
        if (DATABASE_FILE.exists())
        {
            try
            {
                return CompressionUtils.LoadObject(DATABASE_FILE, Database.class);
            }
            catch (Exception e)
            {
                System.err.println("Offline Database Corrupt.");

                return null;
            }
        }
        else
        {
            try
            {
                return DownloadAndStore();
            } catch (Exception e)
            {
                return null;
            }
        }
    }

    public static Database ForceReload()
    {
        try
        {
            return DownloadAndStore();
        } catch (Exception e)
        {
            return null;
        }
    }

    public static String GetDatabaseFileDate() throws IOException
    {
        return DATE_FORMAT.format(Files.readAttributes(DATABASE_FILE.toPath(), BasicFileAttributes.class).lastModifiedTime().toMillis());
    }

    public static class DatabaseException extends RuntimeException
    {
        public DatabaseException(String message)
        {
            super(message);
        }
    }
}