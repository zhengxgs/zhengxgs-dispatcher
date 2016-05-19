package core.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import core.utils.LoggerUtil;

/**
 * echo msg
 * Created by zhengxgs on 2016/4/29.
 */
public class EchoMessage implements EventHandler {

	public int eventType = EventType.AB_ECHO_MESSAGE;
	private Logger logger = LoggerFactory.getLogger("es.log");

	@Override
	public boolean handleEvent(Event event) {
		boolean result = false;
		LoggerUtil.info(logger, event.getParas().toString());
		// if (event.getSource() instanceof DistributionSupport) {
		// System.out.println((((DistributionSupport) event.getSource()).getUuid()) + " 完成任务了");
		// }
		result = true;
		return result;
	}
}
