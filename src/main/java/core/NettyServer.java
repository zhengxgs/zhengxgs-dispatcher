package core;

import core.handler.Event;
import core.handler.EventType;
import core.task.CalculateWork;
import core.utils.FragmentUtil;
import core.utils.TransportUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * Created by zhengxgs on 2016/4/28.
 */
public class NettyServer extends Thread implements Serializable {

	private int clientPointor = 0;
	public boolean running = false;

	private NettyServer() {
	}

	private static NettyServer container;

	private Map<String, WorkState> workPool = new HashMap<>();
	private Map<String, ClientState> clientPool = new HashMap<>();
	private LinkedBlockingQueue<DistributionSupport> workQueue = new LinkedBlockingQueue<>();

	public static NettyServer getInstance() {
		if (container == null) {
			synchronized (NettyServer.class) {
				container = new NettyServer();
				container.running = true;
			}
		}
		return container;
	}

	public static boolean addWork(DistributionSupport work) {
		boolean result = false;
		try {
			getInstance().workQueue.put(work);
			result = true;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return result;
	}

	public static boolean removeWork(String uuid) {
		boolean result = false;
		try {
			getInstance().workPool.remove(uuid);
			result = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public static ClientState getClientState(String clientAddress) {
		return container.clientPool.get(clientAddress);
	}

	public static boolean putClientState(String clientAddress, ClientState clientState) {
		boolean result = false;
		try {
			ClientState cs = getInstance().clientPool.get(clientAddress);
			if (cs == null) {
				getInstance().clientPool.put(clientAddress, clientState);
				System.out.println("客户端注册：" + clientAddress + ",现有客户端数量" + getInstance().clientPool.size());
				result = true;
			} else {

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public static boolean removeClientState(String clientAddress) {
		boolean result = false;
		try {
			getInstance().clientPool.remove(clientAddress);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public void run() {
		new Thread(new NettyRemotingServer()).start();
		while (running) {
			try {
				dispatchWork();
				sleep(500);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 分发任务
	 * @throws InterruptedException
	 */
	private void dispatchWork() throws InterruptedException {

		DistributionSupport work = workQueue.poll();
		while (work != null) {

			if (work.isSegment()) {

				String[] remotingAddress = allRemotingAddress();
				while (remotingAddress.length == 0) {
					System.out.println("没有客户端，等待3秒重新分发任务." + work.getUuid());
					Thread.sleep(3 * 1000);
					remotingAddress = allRemotingAddress();
				}

				List<DistributionSupport> fragment = FragmentUtil.getFragment(work, getClientCount());
				for (int i = 0; i < remotingAddress.length; i++) {
					DistributionSupport segment = fragment.get(i);
					toClient(segment, remotingAddress[i]);
				}
			} else {
				String clientAddress = strategyGetClient();
				while (clientAddress == null) {
					System.out.println("没有客户端，等待3秒重新分发任务." + work.getUuid());
					Thread.sleep(3 * 1000);
					clientAddress = strategyGetClient();
				}
				toClient(work, clientAddress);
			}
			work = workQueue.poll();
		}
	}

	private void toClient(DistributionSupport segment, String remotingAddress) {
		WorkState workState = new WorkState(segment.getUuid());
		workState.setDistributeTimeMillis(System.currentTimeMillis());
		workState.setRemoteAddress(remotingAddress);
		getInstance().workPool.put(workState.getUuid(), workState);

		Event event = new Event(EventType.S_DISTRIBUTE_WORK, segment, null);
		System.out.println("服务器分发任务：" + segment.getUuid());
		TransportUtil.toClient(remotingAddress, event);
	}

	private String strategyGetClient() {
		String result = null;
		try {
			String[] allClientAddress = new String[0];
			allClientAddress = getInstance().clientPool.keySet().toArray(allClientAddress);
			if (allClientAddress.length > 0) {
				result = allClientAddress[clientPointor % allClientAddress.length];
				clientPointor++;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	private String[] allRemotingAddress() {
		String[] remotingAddress = new String[10];
		try {
			remotingAddress = getInstance().clientPool.keySet().toArray(new String[0]);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("获取客户端异常");
		}
		return remotingAddress;
	}

	/**
	 * 获取客户端数量
	 * @return
	 */
	public static int getClientCount() {
		return getInstance().clientPool.size();
	}

	public static void main(String[] args) throws InterruptedException {
		new Thread(NettyServer.getInstance()).start();
		System.out.println("服务器端程序启动....................完成");

		CalculateWork calculateWork = new CalculateWork();
		calculateWork.setNodeName("127.0.0.1");
		NettyServer.addWork(calculateWork);

		/*for (int i = 0; i < 10; i++) {
			Thread.sleep(3000);
			NettyServer.addWork(new CalculateWork());
		}*/

	}
}
