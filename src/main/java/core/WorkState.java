package core;

import java.io.Serializable;

/**
 * Created by zhengxgs on 2016/4/28.
 */
public class WorkState implements Serializable {

	private static final long serialVersionUID = 1L;

	String uuid;// 唯一标识符
	int stateType;// 状态类型
	String remoteAddress;// 工作者ip地址
	long initTimeMillis;// 初始化时间
	long distributeTimeMillis;// 分发时间
	long completeTimeMillis;// 完成时间

	public WorkState(String uuid) {
		this.uuid = uuid;
		initTimeMillis = System.currentTimeMillis();
	}

	public int getStateType() {
		return stateType;
	}

	public void setStateType(int stateType) {
		this.stateType = stateType;
	}

	public String getRemoteAddress() {
		return remoteAddress;
	}

	public void setRemoteAddress(String remoteAddress) {
		this.remoteAddress = remoteAddress;
	}

	public long getInitTimeMillis() {
		return initTimeMillis;
	}

	public void setInitTimeMillis(long initTimeMillis) {
		this.initTimeMillis = initTimeMillis;
	}

	public long getDistributeTimeMillis() {
		return distributeTimeMillis;
	}

	public void setDistributeTimeMillis(long distributeTimeMillis) {
		this.distributeTimeMillis = distributeTimeMillis;
	}

	public long getCompleteTimeMillis() {
		return completeTimeMillis;
	}

	public void setCompleteTimeMillis(long completeTimeMillis) {
		this.completeTimeMillis = completeTimeMillis;
	}

	public String getUuid() {
		return uuid;
	}

	/**
	 *
	 * 将状态转换为文字
	 *
	 * @param workStateType
	 * @return
	 */
	public static String toString(int workStateType) {
		String word = "";
		switch (workStateType) {
		case WorkStateType.WORKER_NOTICE_COMPLETE_SUCCESS:
			word = "正常完成";
			break;
		case WorkStateType.WORKER_NOTICE_COMPLETE_ERROR:
			word = "异常，重新加入工作队列";
			break;
		default:
			break;
		}
		return word;
	}

}