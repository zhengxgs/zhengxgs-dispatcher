package core.handler;


import core.DistributionSupport;
import core.NettyServer;
import core.WorkState;
import core.WorkStateType;

/**
 * Created by zhengxgs on 2016/4/29.
 */
public class CompleteWork implements EventHandler {

	public int eventType = EventType.W_WORK_COMPLETE;

	@Override
	public boolean handleEvent(Event event) {
		boolean result = false;
		try {
			DistributionSupport work = (DistributionSupport) event.getSource();
			System.out.println("任务(" + work.getUuid() + ")结束,状态：" + WorkState.toString(Integer.parseInt(event.getParas().toString())));

			int workStateType = Integer.parseInt(event.getParas().toString());
			switch (workStateType) {
			case WorkStateType.WORKER_NOTICE_COMPLETE_SUCCESS:
				if (work.getFailureCount() > 0) {
					System.out.println("经过 " + work.getFailureCount() + " 次失败，终于完成");
				}
				NettyServer.removeWork(work.getUuid());
				break;
			case WorkStateType.WORKER_NOTICE_COMPLETE_ERROR:
				// TODO 失败后重新加入队列继续执行....
				NettyServer.addWork(work);
				break;
			default:
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
}
