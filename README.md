# registration
KV存储，基于Raft,Netty实现

## Keypoint
- 基于Raft协议
- 集群状态是基于K-V形式的
- 静态集群注册，需要预先在配置文件中写入集群节点
- 访问方式，基于Push-Poll模式


## Samples
### Quick Startup

#### simple example
1.Server
进入registration-core/test/
分别启动三个节点类
- Server1
- Server2
- Server3

2.Client
进入registration-client/test
测试Demo
```
public class ClientApp {
    private KVRegClient regClient;

    @Before
    public void testPutAndGet() {
        this.regClient = new KVRegClient();
        regClient.addNodes("127.0.0.1:7800", "127.0.0.1:7801", "127.0.0.1:7802");
        regClient.start();

    }

    @Test
    public void putAndGet() throws SendException {
        final SendResult sendResult = regClient.put("time2", "xxxxx", 3000);
        if(sendResult.isSuccess()) {
            final SendResult result = regClient.get("time1", 1000);
            System.out.println(new String(result.getResult(), StandardCharsets.UTF_8));
        }
    }

    @After
    public void after() {
        this.regClient.shutdown();
    }
}
```
3.Watcher - 服务端推送
```
   public static void main(String[] args) {
    RegClient regClient = new KVRegClient(8100);
        regClient.addClients("127.0.0.1:7800", "127.0.0.1:7801", "127.0.0.1:7802");
        regClient.start();

        SendResult res = regClient.createNode("time2");
        if(!res.isSuccess()) {
            System.out.println("create node err: " + res.getInfo());
            return ;
        }

        res = regClient.addWatcher("time2", new InvokeCallback() {
            @Override
            public void onKeyChanged(String key, String oldV, String newV) throws Exception {
                System.out.println(key);
                System.out.println(oldV);
                System.out.println(newV);
            }
        });

        if(!res.isSuccess()) {
            System.out.println(res.getInfo());
        }
}
```

### Startup
#### 1.Server
- 服务器位于registration-core下，需要修改resources/registration.yaml
```yaml
listenPort: 7800
addrNodes:
    - 127.0.0.1:7800
    - 127.0.0.1:7801
    - 127.0.0.1:7802

```
- 启动
```java

public class RegistrationStartup {

    public static void main(String[] args) {
        main0();
    }

    private static void main0() {
        RegistrationConfig registrationConfig = new RegistrationConfig("registration.yaml");

        Registration registration = new DefaultRegistrationImpl(registrationConfig);
        registration.start();

        registration.shutdown();
    }
}

```
#### 2.Client
- Client位于registration-client下
```java
public class ClientApp {
    private KVRegClient regClient;

    @Before
    public void testPutAndGet() {
        this.regClient = new KVRegClient();
        regClient.addNodes("127.0.0.1:7800", "127.0.0.1:7801", "127.0.0.1:7802");
        regClient.start();

    }

    @Test
    public void putAndGet() throws SendException {
        final SendResult sendResult = regClient.put("time2", "xxxxx", 3000);
        if(sendResult.isSuccess()) {
            final SendResult result = regClient.get("time1", 1000);
            System.out.println(new String(result.getResult(), StandardCharsets.UTF_8));
        }
    }

    @After
    public void after() {
        this.regClient.shutdown();
    }
}
```
