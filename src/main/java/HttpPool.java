import consistenthash.ConHashMap;
import util.ByteArrayUtil;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @description: 节点网络核心,Http服务端
 * @author: zmj
 * @date 2021/3/26 10:21
 */
public class HttpPool {
    private final Lock lock = new ReentrantLock();
    private String defaultBasePath = "/catzCache/"; //默认路径
    private String basePath; //节点通讯地址前缀
    private int port;     //端口号
    private String ip;    //ip地址/主机名称
    private ConHashMap peers;  //一致性HashMap,根据具体的key选择节点
    private Map<String, HttpGetter> httpGetters; //http客户端，一个远程节点对应一个HttpGetter，peer唯一
    private CatzCache catzCache; //服务启动相关

    public HttpPool(int port, String ip, CatzCache catzCache) {
        this.port = port;
        this.ip = ip;
        this.basePath = defaultBasePath;
        this.catzCache = catzCache;
        this.httpGetters = new HashMap<>();
    }

    //实例化一致性hash算法，使用add方法添加节点，每一个远程节点peer对应一个HttpGetter
    public void set(String... peers) {
        lock.lock();
        this.peers = new ConHashMap(3);
        this.peers.add(peers);
        for (String peer: peers) {
            httpGetters.put(peer, new HttpGetter(peer));
        }
        lock.unlock();
    }

    //根据具体的key,选择节点，返回节点对应的Http客户端
    public HttpGetter pickPeer(String key) {
        lock.lock();
        String peer = this.peers.get(key);
        //客户端不能和服务端的ip:port冲突
        if (peer != null && !peer.equals(this.ip + ":" + this.port)) {
            System.out.println("Pick peer " + peer);
            lock.unlock();
            return this.httpGetters.get(peer);
        }
        lock.unlock();
        return null;
    }

    //()
    public void serve() throws IOException {
        ServerSocket serverSocket = new ServerSocket(this.port, 50, InetAddress.getByName(this.ip));
        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("[" + this.ip + ":" + this.port + "]");
            System.out.println("connected from " + socket.getRemoteSocketAddress());
            Thread t = new HttpHandler(socket, this.basePath, this.catzCache);
            t.start();
        }
    }

    class HttpHandler extends Thread {
        Socket socket;
        String basePath;
        CatzCache catzCache;

        public HttpHandler(Socket socket, String basePath, CatzCache catzCache) {
            this.socket = socket;
            this.basePath = basePath;
            this.catzCache = catzCache;
        }

        public void run() {
           try(InputStream input = this.socket.getInputStream()) {
                try(OutputStream out = this.socket.getOutputStream()){
                    HttpHandle(input,out);
                }
            }catch (Exception e) {
               try {
                   this.socket.close();
               }catch (IOException i) {
                   i.printStackTrace();
               }
               System.out.println("client disconnected");
           }

        }
    }

    //过滤响应和请求
    private void HttpHandle(InputStream input, OutputStream out) throws IOException{
        System.out.println("Process new http request...");
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        boolean requestOK = false;
        String first = reader.readLine();
        if (first.contains("HTTP/1.") && first.contains(this.basePath)) {
            requestOK = true;
        }
        String[] path = first.split(" ")[1].split("/");
        String groupName = path[2];
        String key = path[3];
        System.out.println(requestOK ? "Response OK" : "Response Error");
        Group group = catzCache.getGroup(groupName);
        byte[] value = null;
        if (group == null) {
            requestOK = false;
        }else {
            value = group.get(key);
            if (value == null){
                requestOK = false;
            }
        }
        if (!requestOK) {
            //错误响应
            writer.write("404 not found\r\n");
            writer.write("Contend-Length\r\n");
            writer.write("\r\n");
        } else {
            //成功响应
            String data = ByteArrayUtil.byte2Obj(value).toString();
            int length = data.getBytes(StandardCharsets.UTF_8).length;
            writer.write("HTTP/1.0 200 OK\r\n");
            writer.write("Connection: close\r\n");
            writer.write("Content-Type: text/html\r\n");
            writer.write("\r\n");
            writer.write(data);
        }
        writer.flush();
    }

}
