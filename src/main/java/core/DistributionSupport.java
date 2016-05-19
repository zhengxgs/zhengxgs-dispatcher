package core;

import java.io.Serializable;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import core.handler.Event;
import core.handler.EventType;
import core.utils.TransportUtil;

/**
 * 任务抽象类（如需任务拆分，需要到FragmentUtil中添加拆分支持）
 * Created by zhengxgs on 2016/4/28.
 */
public abstract class DistributionSupport implements Runnable, Serializable {

	private static final long serialVersionUID = 1L;
	private Logger logger = LoggerFactory.getLogger("es.log");

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

	/**
	 * 发送事件到服务器
	 * @param event
	 * @return
	 */
	protected boolean eventToServer(Event event) {
		logger.info("客户端发向{}, uuid:{}, 发送事件:{}", getNodeName(), getUuid(), event);
		return TransportUtil.toServer(event);
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
			competeWorkCallBack(workStateType);
			result = eventToServer(new Event(EventType.B_COMPLETE_WORK, workStateType, this));
		} catch (Exception e) {
			logger.error("客户端发送服务端异常", e);
		}
		return result;
	}

	/**
	 * 任务返回后，执行一些操作
	 * @param workStateType 执行状态
	 */
	protected abstract void competeWorkCallBack(int workStateType);

}
