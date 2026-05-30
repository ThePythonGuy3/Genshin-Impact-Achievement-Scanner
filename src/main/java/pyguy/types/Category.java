package pyguy.types;

import java.util.List;

public record Category(int categoryID, int order, String name, List<Integer> achievementMap)
{
    @Override
    public String toString()
    {
        return "Category<" + categoryID + ", " + order + ", " + name + ", " + achievementMap + ">";
    }
}