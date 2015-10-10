package phex.event;

import java.util.ArrayList;

public class PhexEventServiceImpl
    implements PhexEventService
{
	public static AndroidEventServiceInterface inf;
	
	public PhexEventServiceImpl(){
	}
	
	ArrayList<EventHandler> handler_list = new ArrayList<EventHandler>();
	@Override
	public synchronized void publish( String topic, Object o) {
		if(inf == null) return;
		inf.publish(topic, o);
	}

	@Override
	public synchronized void register(EventHandler obj, String[] filters) {
		if(inf == null) return;
		inf.register(obj, filters);
	}
}