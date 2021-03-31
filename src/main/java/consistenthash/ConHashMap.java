package consistenthash;

import java.util.*;
import java.util.zip.CRC32;
import util.ByteArrayUtil;

/**
 * @description: 一致性哈希,单节点走向分布式节点
 * @author: zmj
 * @date 2021/3/26 14:30
 */
/*
* 1.问题1：访问节点随机，假设有10个节点，第一次随机访问节点1，获取数据并缓存。后续再次访问有1/10的概率访问节点1，
*   9/10的概率会访问到其他的节点。这时需要重新获取并且缓存，非常耗时，且存储相同的数据浪费大量的存储空间。
*    解决：可以使用hash算法对数据的key的字符ASCII码取模，保证每次访问都是同一个节点。
* 2.问题2：节点数量变化，假设减少一台机器，hash(key) % 10变成hash(key) % 9，缓存值对应的节点都发生变化。
*   造成缓存雪崩，同一时刻缓存全部失效，造成瞬时大量请求打到DB，造成缓存服务器宕机。
*    解决：一致性hash将key映射到2^32个空间，将数字收尾相连，形成一个环。
*       a.计算节点的hash值放在环上
*       b.计算key的hash值放在环上，顺时针遇到的第一个节点为选取的节点
* 3.问题3：数据倾斜，服务器节点过少，key节点映射集中在某一部分。
*    解决：引入虚拟节点，一个节点分为几个不同的虚拟节点，添加编号来区分，虚拟节点重新计算hash值。
*       a.计算虚拟节点的hash值放在环上
*       b.计算key的hash值放在环上，顺时针选取相应的虚拟节点，对应真实节点，增加一个字典(map)维护真实节点与虚拟节点的映射
* */
public class ConHashMap {
    //虚拟节点倍数
    private final int replicas;
    //哈希环
    private final List<Long> keys;
    //虚拟节点与真实节点的映射表，键为虚拟节点的hash值，值为真实节点名称
    private final Map<Long,String> hashMap;
    //hash函数
    private static long hash(byte[] date) {
        CRC32 crc32 = new CRC32(); //CRC循环冗余校验，用于计算数据流
        crc32.update(date); //用指定的字节数组更新CRC-32的校验和
        return crc32.getValue(); //返回CRC-32的值
    }

    public ConHashMap(int replicas) {
        this.replicas = replicas;
        hashMap = new HashMap<>();
        keys = new ArrayList<>();
    }

    //添加新节点
    public void add(String ... keys) {
        for (String key : keys) {
            for (int i = 0; i < replicas; i++) {
                //真实节点的虚拟节点
                byte[] virtualValue = ByteArrayUtil.obj2Byte(i + key);
                long hashValue = hash(virtualValue);
                this.keys.add(hashValue); //hash环添加节点
                hashMap.put(hashValue,key);
            }

        }
        Collections.sort(this.keys); //hash值排序
    }

    //选择节点
    public String get(String key){
        if (key.length() == 0){
            return "";
        }
        long hashedKey = hash(ByteArrayUtil.obj2Byte(key));
        int idx = 0; //保存匹配的第一个虚拟节点的下标
        for (int i = 0; i < keys.size(); i++) {
            if (keys.get(i) >= hashedKey){
                idx = i;
                break;
            }
        }
        //若idx==keys.size()应该选择keys[0],所以取余操作
        long peerKey = keys.get(idx % keys.size());
        return hashMap.get(peerKey); //返回真实节点
    }
}
