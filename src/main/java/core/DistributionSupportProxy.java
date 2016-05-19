package core;

import core.handler.Event;

/**
 * Created by zhengxgs on 2016/4/28.
 */
public class DistributionSupportProxy extends DistributionSupport {

	private static final long serialVersionUID = 1L;

	DistributionSupport distributionSupport;

	public DistributionSupportProxy(DistributionSupport distributionSupport) {
		this.distributionSupport = distributionSupport;
	}

	@Override
	public void run() {
		try {
			distributionSupport.run();
			distributionSupport.competeWork(WorkStateType.WORKER_NOTICE_COMPLETE_SUCCESS);
		} catch (Exception e) {
			distributionSupport.setIsSegment(false);
			distributionSupport.addFailureCount();
			distributionSupport.competeWork(WorkStateType.WORKER_NOTICE_COMPLETE_ERROR);
			e.printStackTrace();
		}
	}

	@Override
	protected boolean competeWork(int workStateType) {
		return distributionSupport.competeWork(workStateType);
	}

	@Override
	protected void competeWorkCallBack(int workStateType) {
		distributionSupport.competeWorkCallBack(workStateType);
	}

	@Override
	protected boolean eventToServer(Event event) {
		return distributionSupport.eventToServer(event);
	}

	@Override
	public String getUuid() {
		return distributionSupport.getUuid();
	}
}
