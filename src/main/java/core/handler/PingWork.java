package core.handler;

/**
 * Created by zhengxgs on 2016/4/29.
 */
public class PingWork implements EventHandler {
	@Override
	public boolean handleEvent(Event event) {
		// System.out.println("ping...");
		return true;
	}

}
