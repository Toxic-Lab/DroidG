package phex.event;

public class ContainerEvent
{
    public enum Type
    {
        ADDED, CHANGED, REMOVED
    }

    private final Type type;

    private final Object source;

    private final Object container;

    private final int position;

    public ContainerEvent(Type type, Object source, Object container,
        int position)
    {
        super();
        this.type = type;
        this.source = source;
        this.container = container;
        this.position = position;
    }

    public Type getType()
    {
        return type;
    }

    public int getPosition()
    {
        return position;
    }

    /**
     * @return the container
     */
    public Object getContainer()
    {
        return container;
    }

    /**
     * @return the source
     */
    public Object getSource()
    {
        return source;
    }
}
