package core.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import core.DistributionSupport;
import core.NettyServer;
import core.WorkState;
import core.WorkStateType;
import core.utils.LoggerUtil;

/**
 * Created by zhengxgs on 2016/4/29.
 */
public class CompleteWork implements EventHandler {

	private Logger logger = LoggerFactory.getLogger("es.log");

	public int eventType = EventType.B_COMPLETE_WORK;

	@Override
	public boolean handleEvent(Event event) {
		boolean result = true;
		try {
			DistributionSupport work = (DistributionSupport) event.getSource();
			int workStateType = Integer.parseInt(event.getParas().toString());
			LoggerUtil.info(logger, "任务:{},结束,状态:{}", work.getUuid(), WorkState.toString(workStateType));
			switch (workStateType) {
			case WorkStateType.WORKER_NOTICE_COMPLETE_SUCCESS:
				success(work);
				break;
			case WorkStateType.WORKER_NOTICE_COMPLETE_ERROR:
				error(work);
				break;
			default:
				break;
			}
		} catch (Exception e) {
			result = false;
			e.printStackTrace();
		}
		return result;
	}

	private void success(DistributionSupport work) {
		if (work.getFailureCount() > 0) {
			LoggerUtil.info(logger, "经过:{} 次失败，终于完成", work.getFailureCount());
		}
		NettyServer.getInstance().removeWorkStat(work.getUuid());
		NettyServer.getInstance().removeDispatcheredWork(work.getUuid());
	}

	private void error(DistributionSupport work) {
		if (work.getFailureCount() <= 3) {
			NettyServer.getInstance().addWork(work);
		} else {
			LoggerUtil.info(logger, "uuid:{} 失败重试次数达到3，不再执行", work.getUuid());
		}
	}
}
