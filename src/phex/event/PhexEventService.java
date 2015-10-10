package phex.event;


/**
 * The event service allows modules to publish events
 * that other can subscribe to.
 * <br>
 * The naming convention is:
 * &lt;app&gt;:&lt;module&gt;/&lt;key-word or key-word-path&gt;
 * <br>
 * For the example the Phex core uses:
 * <ul>
 *   <li>phex:upload/added
 *   <li>phex:download/started
 *   <li>phex:networt/caughthost
 * </ul>
 * 
 * @see org.bushe.swing.event.EventService
 */
public interface PhexEventService
{
    public void register( EventHandler obj, String[] filters);
    
    /**
     * Publishes an object on a topic name so that all subscribers to that 
     * name will be notified about it.
     *
     * @param topic The name of the topic subscribed to
     * @param o the object to publish
     */
    public void publish(String topic, Object o);
}