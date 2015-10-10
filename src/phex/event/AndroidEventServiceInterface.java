package phex.event;

public interface AndroidEventServiceInterface {
	public void publish( String topic, Object o);
	public void register(EventHandler obj, String[] filters);
}
