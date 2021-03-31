import lru.Cache;
import singleflight.CallManage;
import util.ByteArrayUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @description: Group 是 CatzCache 最核心的数据结构，负责与用户的交互，并且控制缓存值存储和获取的流程。
 * //                            是
 * //接收 key --> 检查是否被缓存 -----> 返回缓存值 (1)
 * //                |  否                         是
 * //                |-----> 是否应当从远程节点获取 -----> 与远程节点交互 --> 返回缓存值 (2)
 * //                            |  否
 * //                            |-----> 调用`回调函数`，获取值并添加到缓存 --> 返回缓存值 (3)
 * //
 * //细化流程（2）：
 * //使用一致性哈希选择节点        是                                    是
 * //    |-----> 是否是远程节点 -----> HTTP 客户端访问远程节点 --> 成功？-----> 服务端返回返回值
 * //                    |  否                                    ↓  否
 * //                    |----------------------------> 回退到本地节点处理。
 * @author: zmj
 * @date 2021/3/25 16:23
 */
public class Group {
    private final String name; //缓存的命名空间，唯一的名称
    private final Cache<String, byte[]> mainCache; //缓存队列
    private final Function<String, byte[]> getter; //缓存未命中时的回调，获取数据源
    private HttpPool peers; //http服务端
    private CallManage loader;

    public Group(String name, Function<String, byte[]> getter, int size) {
        this.name = name;
        this.mainCache = new Cache<>(size,null);
        this.getter = getter;
    }

    //流程（1）：从maincache中查找缓存，若存在则返回
    //流程（3）：缓存不存在，调用load方法，load调用getLocally(分布式场景调用getFromPeer从其他节点获取)
    //         ，getLocally调用回调函数getter.apply()获取数据源，通过populateCache方法加入mainCache
    public byte[] get(String key){
        if (key == null){
            return null;
        }
        byte[] value = mainCache.get(key);
        if (value != null){
            System.out.println("[CatzCache] hit");
            return value;
        }
        return load(key);
    }

    //httpPool对象注入
    public void registerPeers(HttpPool peers){
        if (peers != null) {
            System.out.println("RegisterPeerPicker called more than once");
        }
        this.peers = peers;
    }

    //从远程节点获取缓存，若没有从本地获取
    private byte[] load(String key){
        //loader保证并发场景下相同的key,load只会调用一次
        return this.loader.run(key, () -> {
            if (this.peers != null) {
                HttpGetter peer = this.peers.pickPeer(key);
                if (peer != null) {
                    byte[] value = this.getFromPeer(peer,key);
                    if (value != null) {
                        return value;
                    } else {
                        System.out.println("[catzCache] Failed to get from peer cache");
                    }
                }
            }
            return this.getLocally(key);
        });

    }

    //访问远程节点，获取缓存值
    private byte[] getFromPeer(HttpGetter peer, String key){
        String res = peer.get(this.name, key);
        if (res == null) {
            return null;
        }
        return ByteArrayUtil.obj2Byte(res);
    }

    //本地获取缓存值
    private byte[] getLocally(String key) {
        byte[] value = getter.apply(key);
        if (value == null){
            return null;
        }
        populateCache(key,value);
        return value;
    }

    //将回调得到的缓存值加入缓存队列
    private void populateCache(String key,byte[] value){
        mainCache.put(key,value);
    }

    public static void main(String[] args) {
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("Tom","1");
        hashMap.put("Jack","2");
        hashMap.put("Timi","3");
        Map<String,Integer> map = new HashMap<>();
        //1.测试缓存为空时，是否通过回调函数获取数据源
        //2.map用来记录回调次数，若次数>1表示多次调用回调函数，没有缓存
        Function<String, byte[]> getter = key -> {
            if (map.get(key) == null) {
                map.put(key,0);
            }
            int num = map.get(key);
            map.put(key,num++);
            return ByteArrayUtil.obj2Byte(hashMap.get(key));
        };
        Group group = new Group("number",getter,10);
        for (String key : hashMap.keySet()) {
            System.out.println("[SlowDB] search key " + key);
            group.get(key);
            group.get(key);
           if (map.get(key) > 1 || map.get(key) == null){
               System.out.println("cache " + key + " miss");
           }
        }
        group.get("Tom");
        group.get("unKnow");
    }
}
