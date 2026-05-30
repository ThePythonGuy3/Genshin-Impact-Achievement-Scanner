package pyguy.types;

import java.util.*;

public record Database(Map<Integer, Achievement> achievements, List<Category> categories)
{
    @Override
    public String toString()
    {
        return "Database<" + achievements + ", " + categories + ">";
    }
}