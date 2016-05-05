package core;

import java.io.Serializable;
import java.util.UUID;

import core.handler.Event;
import core.handler.EventHandler;
import core.handler.EventType;
import core.utils.TransportUtil;

/**
 * Created by zhengxgs on 2016/4/28.
 */
public abstract class DistributionSupport implements EventHandler, Runnable, Serializable {

	private static final long serialVersionUID = 1L;

	// default is true
	private boolean isSegment = false;
	private int failureCount = 0;
	private String uuid = UUID.randomUUID().toString();
	private String nodeName;

	public String getUuid() {
		return uuid;
	}

	public int addFailureCount() {
		failureCount += 1;
		return failureCount;
	}

	public int getFailureCount() {
		return failureCount;
	}

	public boolean isSegment() {
		return isSegment;
	}

	public void setIsSegment(boolean isSegment) {
		this.isSegment = isSegment;
	}

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	@Override
	public boolean handleEvent(Event event) {
		return true;
	}

	/**
	 * 发送事件到服务器
	 * @param event
	 * @return
	 */
	protected boolean eventToServer(Event event) {
		boolean result = false;
		TransportUtil.toServer(event);
		return result;
	}

	/**
	 * 通知服务器工作完成状态
	 * @param workStateType
	 * WORKER_NOTICE_COMPLETE_SUCCESS = 1;// 完成
	 * WORKER_NOTICE_COMPLETE_ERROR = 2;// 异常
	 * @return
	 */
	protected boolean competeWork(int workStateType) {
		boolean result = false;
		try {
			eventToServer(new Event(EventType.W_WORK_COMPLETE, workStateType, this));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

}
