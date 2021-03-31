package lru;

import java.util.function.BiConsumer;

/**
 * @description: LRU算法测试
 * @author: zmj
 * @date 2021/3/25 10:48
 */
public class LRUCacheTest {
    public static void main(String[] args) {
        BiConsumer<Integer,Integer> func = new BiConsumer<Integer, Integer>() {
            @Override
            public void accept(Integer key, Integer value) {
                System.out.println("remove--->key:" + key + " | value:" + value);
            }
        };
        LRUCache<Integer,Integer> lru = new LRUCache<>(3,func);
        //测试put
        lru.put(1,1);
        lru.put(2,2);
        lru.put(3,3);
        System.out.println(lru.get(1));
        lru.put(4,4);
        System.out.println(lru.get(3));
        lru.put(5,5);
        System.out.println(lru.get(2));
    }
}
