# start

##CLIENT:
```java
NettyRemotingClient instanceOne = new NettyRemotingClient("127.0.0.1", 9999);
NettyClient.getInstance().addRemotingClient("127.0.0.1", instanceOne);
try {
	new Thread(instanceOne).start();
} catch (Exception e) {
	e.printStackTrace();
}
new Thread(NettyClient.getInstance()).start();
```
----------------------------------------

##SERVER:
```java
new Thread(NettyServer.getInstance()).start();
System.out.println("server start....");

// put task to queue
CalculateWork calculateWork = new CalculateWork();
calculateWork.setNodeName("127.0.0.1");
NettyServer.getInstance().addWork(calculateWork);
```