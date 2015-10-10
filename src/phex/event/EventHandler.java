package phex.event;

public interface EventHandler {
	public void onEvent(String topic, Object event);
}
