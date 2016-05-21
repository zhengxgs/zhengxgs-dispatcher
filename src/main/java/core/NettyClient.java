package core;

import java.util.Map;
import java.util.concurrent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import core.factory.NamedThreadFactory;
import core.utils.LoggerUtil;
import io.netty.channel.Channel;

/**
 * Created by zhengxgs on 2016/4/28.
 */
public class NettyClient implements Runnable {

	private Logger logger = LoggerFactory.getLogger("es.log");

	private static boolean running = false;
	private static final NettyClient nettyClient = new NettyClient();
	private final ThreadPoolExecutor threadPool;
	private final ConcurrentHashMap<String, NettyRemotingClient> remotingClients = new ConcurrentHashMap<>();
	private final LinkedBlockingQueue<DistributionSupport> workPool = new LinkedBlockingQueue<>();
	private final ScheduledExecutorService channelConnectExecutorService;
	private final ThreadPoolExecutor channelManageThreadPool;

	public void channelManageStart() {
		channelConnectExecutorService.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				try {
					if (remotingClients.size() > 0) {
						for (final Map.Entry<String, NettyRemotingClient> entry : remotingClients.entrySet()) {
							Channel channel = entry.getValue().getChannel();
							if (channel != null && !channel.isActive() && entry.getValue().isReConnectRunning.get()) {
								channelManageThreadPool.execute(new Runnable() {
									@Override
									public void run() {
										entry.getValue().retryConnect();
									}
								});
							}
						}
					}
				} catch (Exception e) {
					LoggerUtil.error(logger, "检查超时任务异常", e);
				}
			}
		}, 10, 10, TimeUnit.SECONDS);
	}

	private NettyClient() {
		running = true;
		threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(16, new NamedThreadFactory("TaskExecutor"));
		/** core 3, max 3, 剩余全进队列 */
		channelManageThreadPool = new ThreadPoolExecutor(3, 3, 10 * 1000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(),
				new NamedThreadFactory("channelManageExecutor"));
		channelManageThreadPool.allowCoreThreadTimeOut(true);
		channelConnectExecutorService = Executors.newScheduledThreadPool(1, new NamedThreadFactory("channelConnectExecutor"));
	}

	public static NettyClient getInstance() {
		return nettyClient;
	}

	public boolean addWork(DistributionSupport work) {
		boolean result = false;
		try {
			this.workPool.put(work);
		} catch (InterruptedException e) {
			LoggerUtil.error(logger, "添加任务异常", e);
		}
		return result;
	}

	@Override
	public void run() {
		channelManageStart();
		processAll();
	}

	private void processAll() {
		while (running) {
			try {
				DistributionSupport work = workPool.take();
				if (threadPool.getMaximumPoolSize() - threadPool.getActiveCount() > 0) {
					threadPool.execute(work);
				} else {
					LoggerUtil.warn(logger, "线程池满载运行，休息一会再执行吧");
					Thread.sleep(1000);
				}
			} catch (Exception e) {
				LoggerUtil.error(logger, "执行任务异常", e);
			}
		}
	}

	public void addRemotingClient(String nodeName, NettyRemotingClient remotingClient) {
		NettyRemotingClient instance = remotingClients.put(nodeName, remotingClient);
		if (instance != null) {
			LoggerUtil.warn(logger, "remotingClient instance exist");
		}
	}

	public NettyRemotingClient getRemotingClient(String nodeName) {
		return remotingClients.get(nodeName);
	}

	public void removeRemotingClient(String nodeName) {
		remotingClients.remove(nodeName);
	}

	public static void main(String[] args) {
		NettyRemotingClient instanceOne = new NettyRemotingClient("127.0.0.1", 9999);
		NettyClient.getInstance().addRemotingClient("127.0.0.1", instanceOne);
		try {
			new Thread(instanceOne).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
		new Thread(NettyClient.getInstance()).start();
	}
}
