package core.utils;

import java.util.HashMap;
import java.util.Map;

import core.handler.*;

/**
 * Created by zhengxgs on 2016/4/28.
 */

public class EventUtil {

	private static Map<Integer, EventHandler> allEventHandler = new HashMap<>();

	static {
		allEventHandler.put(EventType.S_DISTRIBUTE_WORK, new DistributeWork());
		allEventHandler.put(EventType.S_ECHO_MESSAGE, new EchoMessage());
		allEventHandler.put(EventType.W_ECHO_MESSAGE, new EchoMessage());
		allEventHandler.put(EventType.W_WORK_COMPLETE, new CompleteWork());
		allEventHandler.put(EventType.W_PING, new PingWork());
		allEventHandler.put(EventType.S_PING, new PingWork());
	}

	public final static EventHandler getEventHandler(int eventType) {
		EventHandler handler = null;
		try {
			handler = allEventHandler.get(eventType);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return handler;
	}

	public final static String eventTypeToString(int eventType) {
		String word = "未知时间类型";
		switch (eventType) {
		case EventType.S_DISTRIBUTE_WORK:
			word = "分发任务";
			break;
		case EventType.S_ECHO_MESSAGE:
			word = "消息服务";
			break;
		case EventType.W_ECHO_MESSAGE:
			word = "消息服务";
			break;
		case EventType.W_WORK_COMPLETE:
			word = "任务完成";
			break;
		case EventType.S_PING:
			word = "PING";
			break;
		case EventType.W_PING:
			word = "PING";
			break;
		default:
			break;
		}
		return word;
	}
}
