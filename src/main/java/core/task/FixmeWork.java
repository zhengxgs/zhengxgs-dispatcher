package core.task;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.collections.CollectionUtils;

import core.DistributionSupport;
import core.WorkStateType;
import core.handler.Event;
import core.handler.EventType;

/**
 * Created by zhengxgs on 2016/5/4.
 */
public class FixmeWork extends DistributionSupport {

	List<Integer> ids;

	public FixmeWork(List<Integer> ids) {
		this.ids = ids;
	}

	@Override
	public void run() {
		int a = new Random().nextInt(50);
		int b = new Random().nextInt(50);
		int c = a + b;
		String msg;
		if (CollectionUtils.isNotEmpty(ids)) {
			msg = "任务结果：" + ids.size();
		} else {
			msg = "任务结果: " + c;
		}
		try {
			Thread.sleep(ThreadLocalRandom.current().nextLong(100, 500));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		eventToServer(new Event(EventType.AB_ECHO_MESSAGE, msg, this));
	}

	@Override
	protected void competeWorkCallBack(int workStateType) {
		if (workStateType == WorkStateType.WORKER_NOTICE_COMPLETE_SUCCESS) {
			System.out.println("任务执行完毕，清空集合等信息");
			this.ids = null;
		}
	}

	public List<Integer> getIds() {
		return ids;
	}
}
