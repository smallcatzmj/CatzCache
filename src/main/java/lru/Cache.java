package lru;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

/**
 * @description: 支持并发读写Cache
 * @author: zmj
 * @date 2021/3/25 11:51
 */
public class Cache<K, V> {
    private LRUCache<K, V> lru;
    private final Lock lock = new ReentrantLock();
    private int size;
    private BiConsumer<K, V> callback;

    public Cache(int size,BiConsumer<K, V> callback){
        this.size = size;
        this.callback = callback;
    }

    //先判断lru是否为空，若为空则创建实例。延迟初始化，提高性能，减少内存要求。
    public void put(K key, V value){
        this.lock.lock();
        if (this.lru == null){
            this.lru = new LRUCache<>(size,callback);
        }
        lru.put(key,value);
        lock.unlock();
    }

    public V get(K key){
        lock.lock();
        if (lru == null){
            lock.unlock();
            return null;
        }
        V v = lru.get(key);
        lock.unlock();
        return v;
    }
}
