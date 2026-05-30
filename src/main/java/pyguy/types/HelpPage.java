package pyguy.types;

import java.util.Arrays;

public record HelpPage(String header, String imageName, String[] textLines)
{
    @Override
    public String toString()
    {
        return "HelpPage<" + header + ", " + imageName + ", " + Arrays.toString(textLines) + ">";
    }
}
