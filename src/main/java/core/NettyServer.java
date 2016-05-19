package core;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import core.factory.NamedThreadFactory;
import core.handler.Event;
import core.handler.EventType;
import core.task.CalculateWork;
import core.utils.FragmentUtil;
import core.utils.LoggerUtil;
import core.utils.TransportUtil;

/**
 * Created by zhengxgs on 2016/4/28.
 */
public class NettyServer extends Thread implements Serializable {

	private final Logger logger = LoggerFactory.getLogger("es.log");

	private static final NettyServer instance = new NettyServer();

	private int clientPointor = 0;
	private long retry = 5 * 1000;
	private int maxSize = 300;
	private long taskTimeOut = (long) (0.5 * 60 * 60 * 1000);
	public boolean running = false;
	private final ConcurrentHashMap<String, WorkState> workPool = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, ClientState> clientPool = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, DistributionSupport> workQueueDispatchered = new ConcurrentHashMap<>();
	private final LinkedBlockingQueue<DistributionSupport> workQueue = new LinkedBlockingQueue<>(512);

	// 用来定时检查已经超时的work
	private final ScheduledExecutorService workCheckExecutorService = Executors.newScheduledThreadPool(1, new NamedThreadFactory("checkTimeOutTask"));
	private ScheduledFuture<?> scheduledFuture;

	public void checkTaskStart() {
		scheduledFuture = workCheckExecutorService.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				try {
					if (workPool.size() > 0) {
						for (Map.Entry<String, WorkState> entry : workPool.entrySet()) {
							// 清除超时的work, 可能网络异常 或 其他异常导致无法通知到服务端，重新处理
							if (new Date().getTime() - entry.getValue().getDistributeTimeMillis() > taskTimeOut) {
								String uuid = entry.getKey();
								LoggerUtil.warn(logger, "任务uuid:({}) 超过0.5小时未完成，重新加入队列", uuid);
								workPool.remove(uuid);
								// 如果1.5小时未完成，继续拆分数据丢进队列进行分发执行
								DistributionSupport work = workQueueDispatchered.remove(uuid);
								if (work != null && FragmentUtil.isSupportFragment(work)) {
									work.setIsSegment(true);
								}
								addWork(work);
							}
						}
					}
				} catch (Exception e) {
					LoggerUtil.error(logger, "检查超时任务异常", e);
				}
			}
		}, 5 * 60, 5 * 60, TimeUnit.SECONDS);
	}

	private NettyServer() {
		running = true;
	}

	public static NettyServer getInstance() {
		return instance;
	}

	public boolean addWork(DistributionSupport work) {
		boolean result = false;
		if (work != null) {
			try {
				if (this.workQueue.size() <= maxSize) {
					this.workQueue.put(work);
					result = true;
				} else {
					LoggerUtil.warn(logger, "workQueue size[{}] size = maxsize[{}]", workQueue.size(), maxSize);
				}
			} catch (Exception e) {
				LoggerUtil.error(logger, "add work error", e);
			}
		} else {
			LoggerUtil.warn(logger, "add work warn!, the work is null");
		}
		return result;
	}

	public boolean removeWorkStat(String uuid) {
		boolean result = false;
		try {
			this.workPool.remove(uuid);
			result = true;
		} catch (Exception e) {
			LoggerUtil.error(logger, "remove work error, uuid:{}", uuid, e);
		}
		return result;
	}

	public ClientState getClientState(String clientAddress) {
		return this.clientPool.get(clientAddress);
	}

	public boolean putClientState(String clientAddress, ClientState clientState) {
		boolean result = false;
		try {
			ClientState cs = this.clientPool.get(clientAddress);
			if (cs == null) {
				this.clientPool.put(clientAddress, clientState);
				LoggerUtil.info(logger, "客户端注册:{},现有客户端数量:{}", clientAddress, this.getClientCount());
				result = true;
			} else {
				LoggerUtil.warn(logger, "客户端有相同IP注册:{}", clientAddress);
			}
		} catch (Exception e) {
			LoggerUtil.error(logger, "put client error, ip:{}", clientAddress, e);
		}
		return result;
	}

	public boolean removeClientState(String clientAddress) {
		boolean result = false;
		try {
			this.clientPool.remove(clientAddress);
		} catch (Exception e) {
			LoggerUtil.error(logger, "removeClientState error, ip:{}", clientAddress, e);
		}
		return result;
	}

	@Override
	public void run() {
		checkTaskStart();
		new Thread(new NettyRemotingServer()).start();
		while (running) {
			try {
				dispatchWork();
				sleep(500);
			} catch (Exception e) {
				LoggerUtil.error(logger, "dispatch error", e);
			}
		}
	}

	/**
	 * 分发任务
	 * @throws InterruptedException
	 */
	private void dispatchWork() throws Exception {

		DistributionSupport work = workQueue.poll();
		while (work != null) {

			if (work.isSegment()) {
				boolean retry = true;
				while (retry) {
					String[] clientAddress = allClientAddress();
					while (clientAddress.length == 0) {
						LoggerUtil.warn(logger, "没有执行节点，等待5秒后重试...");
						Thread.sleep(this.retry);
						clientAddress = allClientAddress();
					}
					List<DistributionSupport> fragment = FragmentUtil.getFragment(work, clientAddress.length);
					if (fragment.size() == getClientCount()) {
						for (int i = 0; i < clientAddress.length; i++) {
							DistributionSupport segment = fragment.get(i);
							try {
								toClient(segment, clientAddress[i]);
							} catch (Exception e) {
								addWork(segment);
								LoggerUtil.error(logger, "分发任务异常，重新加入队列继续分发", e);
							}
						}
						retry = false;
					} else {
						LoggerUtil.error(logger, "执行节点有变化，重新分片");
					}
				}
			} else {
				String clientAddress = strategyGetClient();
				while (clientAddress == null) {
					LoggerUtil.warn(logger, "没有执行节点，等待5秒后重试...");
					Thread.sleep(retry);
					clientAddress = strategyGetClient();
				}
				try {
					toClient(work, clientAddress);
				} catch (Exception e) {
					addWork(work);
					LoggerUtil.error(logger, "分发任务异常，重新加入队列继续分发", e);
				}
			}
			work = workQueue.poll();
		}
	}

	private boolean toClient(DistributionSupport segment, String remotingAddress) {
		WorkState workState = new WorkState(segment.getUuid());
		workState.setDistributeTimeMillis(System.currentTimeMillis());
		workState.setRemoteAddress(remotingAddress);
		this.workPool.put(workState.getUuid(), workState);

		Event event = new Event(EventType.A_DISTRIBUTE_WORK, segment, null);
		LoggerUtil.info(logger, "分发任务({}):{}", remotingAddress, segment.getUuid());
		boolean result = TransportUtil.toClient(remotingAddress, event);
		if (result) {
			this.addDispatcheredWork(segment.getUuid(), segment);
		}
		return result;
	}

	private String strategyGetClient() {
		String result = null;
		try {
			String[] allClientAddress = new String[0];
			allClientAddress = this.clientPool.keySet().toArray(allClientAddress);
			if (allClientAddress.length > 0) {
				result = allClientAddress[clientPointor % allClientAddress.length];
				clientPointor++;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	private String[] allClientAddress() {
		String[] clientAddress = new String[10];
		try {
			clientAddress = this.clientPool.keySet().toArray(new String[0]);
		} catch (Exception e) {
			LoggerUtil.error(logger, "get All clients error", e);
		}
		return clientAddress;
	}

	public int getClientCount() {
		return this.clientPool.size();
	}

	public boolean removeDispatcheredWork(String uuid) {
		boolean result = true;
		try {
			workQueueDispatchered.remove(uuid);
		} catch (Exception e) {
			result = false;
			LoggerUtil.error(logger, "remove workQueueDispatchered error", e);
		}
		return result;
	}

	public boolean addDispatcheredWork(String uuid, DistributionSupport segment) {
		boolean result = true;
		try {
			if (segment != null) {
				workQueueDispatchered.put(uuid, segment);
			}
		} catch (Exception e) {
			result = false;
			LoggerUtil.error(logger, "add workQueueDispatchered error", e);
		}
		return result;
	}

	public static void main(String[] args) throws InterruptedException {
		new Thread(NettyServer.getInstance()).start();
		System.out.println("服务器端程序启动....................完成");

		CalculateWork calculateWork = new CalculateWork();
		calculateWork.setNodeName("127.0.0.1");
		NettyServer.getInstance().addWork(calculateWork);

		/*for (int i = 0; i < 10; i++) {
			Thread.sleep(3000);
			NettyServer.addWork(new CalculateWork());
		}*/
		// System.out.println(TimeUnit.SECONDS.convert(240000, TimeUnit.MILLISECONDS));
		// System.out.println(TimeUnit.MILLISECONDS.convert(7200, TimeUnit.SECONDS));

	}
}
