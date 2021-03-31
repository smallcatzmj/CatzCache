import util.ByteArrayUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * @description: 主函数测试
 * @author: zmj
 * @date 2021/3/31 10:44
 */
public class Main {

    public static void main(String[] args) {
        //模拟数据库
        Map<String, Integer> db = new HashMap<>();
        db.put("XiaoMing", 60);
        db.put("XiaoHong", 23);
        db.put("XiaoGang", 987);

        //客户端节点
        String[] peers = new String[]{"localhost:8081", "localhost:8082", "localhost:8083"};
        String groupName = "scores";

        //创建客户端
        for (String peerKey : peers) {
            CatzCache catzCache = new CatzCache();
            catzCache.newGroup(groupName, 3, key -> {
                System.out.println("[SlowDB] search key " + key);
                Integer value = db.get(key);
                if (value != null) {
                    return ByteArrayUtil.obj2Byte(value);
                }
                return null;
            });
            catzCache.startCacheServer(peerKey, peers, groupName);
        }

        //创建服务端
        new Thread( () -> {
            CatzCache catzCache = new CatzCache();
            catzCache.newGroup(groupName, 3, key -> {
                System.out.println("[SlowDB] search key " + key);
               Integer value = db.get(key);
               if (value != null) {
                   return ByteArrayUtil.obj2Byte(value);
               }
               return null;
            });
            catzCache.startAPIServer("localhost:8084", peers, groupName);
        }).start();
    }
}
