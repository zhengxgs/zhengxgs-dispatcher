package core.handler;


import core.DistributionSupport;
import core.DistributionSupportProxy;
import core.NettyClient;

/**
 * 任务分发
 * Created by zhengxgs on 2016/4/29.
 */
public class DistributeWork implements EventHandler {

	public int eventType = EventType.S_DISTRIBUTE_WORK;

	@Override
	public boolean handleEvent(Event event) {
		boolean result = false;
		DistributionSupport work = new DistributionSupportProxy((DistributionSupport) event.getParas());
		NettyClient.getInstance().addWork(work);
		System.out.println("客户端添加任务:" + work.getUuid());
		return result;
	}
}
