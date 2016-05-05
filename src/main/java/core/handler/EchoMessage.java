package core.handler;

import com.jjshome.esf.core.remoting.DistributionSupport;

/**
 * echo msg
 * Created by zhengxgs on 2016/4/29.
 */
public class EchoMessage implements EventHandler {

	public int eventType = EventType.S_ECHO_MESSAGE;

	@Override
	public boolean handleEvent(Event event) {
		boolean result = false;
		System.out.println(event.getParas().toString());
		if (event.getSource() instanceof DistributionSupport) {
			// System.out.println((((DistributionSupport) event.getSource()).getUuid()) + " 完成任务了");
		}
		result = true;
		return result;
	}
}
