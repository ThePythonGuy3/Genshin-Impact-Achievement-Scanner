package pyguy.types;

public record ScanResult(String name, int stars)
{
    @Override
    public String toString()
    {
        return "ScanResult<" + name + ", " + stars + ">";
    }
}