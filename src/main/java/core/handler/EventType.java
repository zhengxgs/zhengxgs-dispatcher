package core.handler;

/**
 * 事件类型
 * Created by zhengxgs on 2016/4/27.
 */
public interface EventType {

	//	//10001-20000 服务器到工作线程，加前缀S(Server)
	//	int S_ECHO_MESSAGE = 10001;// 消息服务
	//	int S_DISTRIBUTE_WORK = S_ECHO_MESSAGE + 1;// 分发任务
	//	int S_PING = S_DISTRIBUTE_WORK + 1; // PING
	//
	//	//20001-30000工作线程到服务器，加前缀W(Worker)
	//	int W_ECHO_MESSAGE = 20001;// 消息服务
	//	int W_WORK_COMPLETE = W_ECHO_MESSAGE + 1;// 消息服务
	//	int W_PING = W_WORK_COMPLETE + 1;
	/** server to client */
	int A_DISTRIBUTE_WORK = 10000;

	/** client to server */
	int B_COMPLETE_WORK = 20000;

	/** server <<>> client */
	int AB_PING = 30000;
	int AB_ECHO_MESSAGE = 30001;
}
