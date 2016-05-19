package core.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import core.DistributionSupport;
import core.DistributionSupportProxy;
import core.NettyClient;
import core.utils.LoggerUtil;

/**
 * 任务分发
 * Created by zhengxgs on 2016/4/29.
 */
public class DistributeWork implements EventHandler {

	public int eventType = EventType.A_DISTRIBUTE_WORK;
	private Logger logger = LoggerFactory.getLogger("es.log");

	@Override
	public boolean handleEvent(Event event) {
		boolean result = false;
		DistributionSupport work = new DistributionSupportProxy((DistributionSupport) event.getParas());
		NettyClient.getInstance().addWork(work);
		// logger.info("客户端添加任务:" + work.getUuid());
		LoggerUtil.info(logger, "客户端添加任务:{}", work.getUuid());
		return result;
	}
}
