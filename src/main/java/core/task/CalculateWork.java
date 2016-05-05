package core.task;

import core.DistributionSupport;
import core.handler.Event;
import core.handler.EventType;

import java.util.Random;


/**
 * Created by zhengxgs on 2016/4/28.
 */
public class CalculateWork extends DistributionSupport {

	@Override
	public void run() {
		int i = new Random().nextInt(100);
		int j = new Random().nextInt(100);
		if (new Random().nextInt(100) < 30) {
			int errorInt = 1 / 0;
		}
		int k = i + j;
		String message = "计算结果: " + k;
		System.out.println(Thread.currentThread() + ":" + message);
		eventToServer(new Event(EventType.W_ECHO_MESSAGE, message, this));
	}
}
