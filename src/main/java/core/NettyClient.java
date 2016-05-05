package core;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by zhengxgs on 2016/4/28.
 */
public class NettyClient implements Runnable {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private static boolean running = false;

	private static final NettyClient container = new NettyClient();

	private ThreadPoolExecutor threadPool;

	private Map<String, NettyRemotingClient> remotingClients = new HashMap();

	private LinkedBlockingQueue<DistributionSupport> workPool = new LinkedBlockingQueue<>();

	private NettyClient() {
		running = true;
		threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(30);
	}

	public static NettyClient getInstance() {
		return container;
	}

	public boolean addWork(DistributionSupport work) {
		boolean result = false;
		try {
			getInstance().workPool.put(work);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public void run() {
		/*try {
			new Thread(NettyRemotingClient.getInstance("127.0.0.1")).start();
		} catch (Exception e) {
			e.printStackTrace();
		}*/
		processAll();
	}

	private void processAll() {
		while (running) {
			try {
				while (running) {
					DistributionSupport work = workPool.take();
					if (threadPool.getMaximumPoolSize() - threadPool.getActiveCount() > 0) {
						threadPool.execute(work);
					} else {
						Thread.sleep(1000);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void addRemotingClient(String nodeName, NettyRemotingClient remotingClient) {
		NettyRemotingClient instance = remotingClients.put(nodeName, remotingClient);
		if (instance != null) {
			System.out.println("instance exist");
		}
	}

	public NettyRemotingClient getRemotingClient(String nodeName) {
		return remotingClients.get(nodeName);
	}

	public static void main(String[] args) {
		NettyRemotingClient instanceOne = new NettyRemotingClient("127.0.0.1", 9999);
		NettyClient.getInstance().addRemotingClient("node1", instanceOne);
		try {
			new Thread(instanceOne).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
		new Thread(NettyClient.getInstance()).start();
	}
}
