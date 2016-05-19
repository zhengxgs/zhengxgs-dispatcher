package core.utils;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import core.handler.*;

/**
 * Created by zhengxgs on 2016/4/28.
 */

public class EventUtil {

	private static Logger logger = LoggerFactory.getLogger("es.log");

	private static Map<Integer, EventHandler> allEventHandler = new HashMap<>();

	static {
		allEventHandler.put(EventType.A_DISTRIBUTE_WORK, new DistributeWork());
		allEventHandler.put(EventType.B_COMPLETE_WORK, new CompleteWork());
		allEventHandler.put(EventType.AB_ECHO_MESSAGE, new EchoMessage());
		allEventHandler.put(EventType.AB_PING, new PingWork());
	}

	public final static EventHandler getEventHandler(int eventType) {
		EventHandler handler = null;
		try {
			handler = allEventHandler.get(eventType);
		} catch (Exception e) {
			LoggerUtil.error(logger, "getEventHandler error", e);
		}
		return handler;
	}

	public final static String eventTypeToString(int eventType) {
		String word = "未知时间类型";
		switch (eventType) {
		case EventType.A_DISTRIBUTE_WORK:
			word = "分发任务";
			break;
		case EventType.AB_ECHO_MESSAGE:
			word = "消息服务";
			break;
		case EventType.B_COMPLETE_WORK:
			word = "任务完成";
			break;
		case EventType.AB_PING:
			word = "PING";
			break;
		default:
			break;
		}
		return word;
	}
}
