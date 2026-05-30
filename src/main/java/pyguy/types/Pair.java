package pyguy.types;

public record Pair<T, V>(T a, V b)
{
    @Override
    public String toString()
    {
        return "Pair<" + a.toString() + ", " + b.toString() + ">";
    }
}
