package pyguy.types;

import java.util.Arrays;

public record Achievement(String name, int[] achievementIDs)
{
    @Override
    public String toString()
    {
        return "Achievement<" + name + ", " + Arrays.toString(achievementIDs) + ">";
    }
}