package pyguy;

import com.google.gson.*;
import pyguy.types.*;
import pyguy.util.Utils;

import java.io.*;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

public class SaveFileTools
{
    public record SaveFileValidity(boolean valid, String result, String reason) {}

    private static String SimplifyName(String input)
    {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD).replaceAll("\\p{M}", "");

        return normalized.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }

    private static boolean IsSameAchievement(String scanned, String database)
    {
        String simplifiedScannedName  = SimplifyName(scanned);
        String simplifiedDatabaseName = SimplifyName(database);

        // The achievement name is too long, it got split in two (or more??)
        if (simplifiedScannedName.length() > 25 &&
            simplifiedScannedName.length() < simplifiedDatabaseName.length())
            return Utils.LevenshteinDistance(
                simplifiedScannedName,
                simplifiedDatabaseName.substring(
                    0,
                    simplifiedScannedName.length()
                )
            ) < 4;
        else
            return Utils.LevenshteinDistance(
                simplifiedScannedName,
                simplifiedDatabaseName
            ) < 4;
    }

    public static List<Achievement> GetObtainedAchievements(int categoryID, Database db, List<ScanResult> achievements)
    {
        Map<String, Achievement> obtainedAchievements = new HashMap<>();

        Category category = db.categories().get(categoryID);

        for (ScanResult result : achievements)
        {
            boolean found = false;
            for (int achievementID : category.achievementMap())
            {
                Achievement achievement = db.achievements().get(achievementID);

                if (IsSameAchievement(result.name(), achievement.name()))
                {
                    if (result.stars() > achievement.achievementIDs().length)
                        System.out.println("Extra stars were counted during the scan for: " + result.name() + ".");
                    else
                    {
                        if (!obtainedAchievements.containsKey(achievement.name()) ||
                            obtainedAchievements.get(achievement.name()).achievementIDs().length > result.stars())
                        {
                            obtainedAchievements.put(
                                achievement.name(),
                                new Achievement(
                                    achievement.name(),
                                    Arrays.copyOf(
                                        achievement.achievementIDs(),
                                        result.stars()
                                    )
                                )
                            );
                        }
                    }

                    found = true;
                    break;
                }
            }
            if (!found) System.out.println("Missing key: " + result.name() + ", " + SimplifyName(result.name()) + ".");
        }

        return new ArrayList<>(obtainedAchievements.values());
    }

    public static SaveFileValidity GenerateSaveFile(int categoryID, Database db, List<Achievement> achievements, File inputFile, boolean merge, int accountID)
    {
        if (inputFile == null || !inputFile.exists())
            return new SaveFileValidity(false, null, I18N.GetString("save-file-moved-text"));

        List<String> accountNames = GetPaimonDatabaseInternalAccounts(inputFile);

        if (accountNames == null)
            return null;

        if (accountID >= accountNames.size())
            return null;

        String accountPrefix = "";
        String account = accountNames.get(accountID);

        if (!account.equals("main"))
            accountPrefix = account + "-";

        String inputAchievementKey = accountPrefix + "achievement";
        String internalCategoryID  = String.valueOf(db.categories().get(categoryID).categoryID()); // This is necessary because category order != internal category id

        JsonElement inputJSON;
        try
        {
            inputJSON = JsonParser.parseReader(new FileReader(inputFile));
        } catch (Exception e)
        {
            return new SaveFileValidity(false, null, I18N.GetString("save-file-invalid-text"));
        }

        if (!inputJSON.isJsonObject())
            return new SaveFileValidity(false, null, I18N.GetString("save-file-invalid-text"));

        JsonObject baseObject = inputJSON.getAsJsonObject();

        JsonObject achievementsObject;
        if (baseObject.has(inputAchievementKey))
        {
            JsonElement achievementsElement = baseObject.get(inputAchievementKey);

            if (achievementsElement.isJsonObject())
                achievementsObject = achievementsElement.getAsJsonObject();
            else
                return new SaveFileValidity(false, null, I18N.GetString("save-file-invalid-text"));
        }
        else
        {
            achievementsObject = new JsonObject();

            baseObject.add(inputAchievementKey, achievementsObject);
        }

        JsonObject categoryObject;
        if (merge && achievementsObject.has(internalCategoryID))
        {
            JsonElement categoryElement = achievementsObject.get(internalCategoryID);

            if (categoryElement.isJsonObject())
                categoryObject = categoryElement.getAsJsonObject();
            else
                return new SaveFileValidity(false, null, I18N.GetString("save-file-invalid-text"));
        }
        else
        {
            categoryObject = new JsonObject();

            achievementsObject.add(internalCategoryID, categoryObject);
        }

        for (Achievement achievement : achievements)
        {
            for (int id : achievement.achievementIDs())
            {
                categoryObject.add(String.valueOf(id), new JsonPrimitive(true));
            }
        }

        return new SaveFileValidity(true, baseObject.toString(), null);
    }

    public static List<String> GetPaimonDatabaseInternalAccounts(File file)
    {
        try
        {
            JsonElement element = JsonParser.parseReader(new FileReader(file));

            if (element.isJsonObject())
            {
                JsonObject object = element.getAsJsonObject();

                List<String> accountNames = new ArrayList<>();
                accountNames.add("main");

                if (object.has("accounts"))
                {
                    JsonElement accountsElement = object.get("accounts");

                    String[] originalNames = accountsElement.getAsString().split(",");

                    accountNames.addAll(Arrays.asList(originalNames));
                }

                return accountNames;
            }
        } catch (JsonSyntaxException | JsonIOException | FileNotFoundException e)
        {
            return null;
        }

        return null;
    }

    public static List<String> GetPaimonDatabaseAccounts(File file)
    {
        List<String> internalNames = GetPaimonDatabaseInternalAccounts(file);
        List<String> output = new ArrayList<>();

        if (internalNames == null)
            return null;

        for (String name : internalNames)
        {
            output.add(
                Arrays.stream(
                    name
                        .replaceAll("([a-z])([A-Z])", "$1 $2")
                        .replaceAll("([A-Za-z])([0-9])", "$1 $2")
                        .replaceAll("([0-9])([A-Za-z])", "$1 $2")
                        .split("\\s+")
                ).map(s ->
                    s.substring(0, 1).toUpperCase() +
                        s.substring(1).toLowerCase()
                ).collect(
                    Collectors.joining(" ")
                )
            );
        }

        return output;
    }
}
