package org.toxiclab.droidg;

import java.util.ArrayList;
import java.util.TreeMap;

import phex.common.Environment;
import phex.event.AndroidEventServiceInterface;
import phex.event.EventHandler;

public class EventThreadService implements AndroidEventServiceInterface{

	//private Handler m_handler;
	TreeMap<String, Integer> topicToNum = new TreeMap<String, Integer>();
	ArrayList<ArrayList<EventHandler> > target = new ArrayList<ArrayList<EventHandler>>();
	
	public EventThreadService(){
		//m_handler = new Handler();
	}
	
	ArrayList<EventHandler> handler_list = new ArrayList<EventHandler>();
	@Override
	public synchronized void publish( String topic, Object o) {
		Integer idx = topicToNum.get(topic);
		if(idx == null) return;
		Environment.getInstance().executeOnThreadPool(new MessageBroadcaster(target.get(idx), topic, o), "Event Thread");
	}

	@Override
	public synchronized void register(EventHandler obj, String[] filter) {
		for(int i=0; i<filter.length; ++i){
			Integer idx = topicToNum.get(filter[i]);
			if(idx == null){
				target.add(new ArrayList<EventHandler>());
				idx = target.size()-1;
				topicToNum.put(filter[i], idx);
			}
			Environment.getInstance().executeOnThreadPool(new HandlerRegister(target.get(idx), obj), "Event Thread");
		}
	}
}

class MessageBroadcaster implements Runnable{
	String topic;
	Object o;
	ArrayList<EventHandler> handler_list;
	
	public MessageBroadcaster(ArrayList<EventHandler> handler, String t, Object obj){
		topic = t;
		o = obj;
		handler_list = handler;
	}

	public void run(){
		for(int i=0; i<handler_list.size(); ++i)
			handler_list.get(i).onEvent(topic, o);
	}
}

class HandlerRegister implements Runnable{
	ArrayList<EventHandler> handler_list;
	EventHandler o;
	
	public HandlerRegister(ArrayList<EventHandler> handler, EventHandler obj){
		handler_list = handler;
		o = obj;
	}
	
	public void run(){
		handler_list.add(o);
	}
}