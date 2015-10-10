package phex.chat;

public class ChatEvent
{
    public enum Type
    {
        OPENED,
        FAILED,
        MSG_REC
    }
    
    private final Type type;
    private final ChatEngine engine;
    private final String chatMsg;
    
    public ChatEvent(Type type, ChatEngine engine, String chatMsg)
    {
        this.type = type;
        this.engine = engine;
        this.chatMsg = chatMsg;
    }

    public ChatEngine getEngine()
    {
        return engine;
    }

    public String getChatMsg()
    {
        return chatMsg;
    }

    public Type getType()
    {
        return type;
    }
}
