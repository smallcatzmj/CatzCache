package singleflight;

import util.ByteArrayUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * @description: singleflight 的主数据结构，管理不同 key 的请求(call)
 * @author: zmj
 * @date 2021/3/29 18:18
 */
public class CallManage {
    private final Lock lock = new ReentrantLock();
    private Map<String, Call> callMap;

    public byte[] run(String key, Supplier<byte[]> func){
        this.lock.lock();
        if (this.callMap == null) {
            this.callMap = new HashMap<>();
        }

        Call call = this.callMap.get(key);
        if (call != null) {
            this.lock.unlock();
            call.await();  //有请求正在进行，则等待
            return call.getVal(); //相同的key,直接返回结果
        }

        call = new Call();
        call.lock(); //发请求前加锁（创建CountDownLatch对象）
        this.callMap.put(key,call); //map中有key表示请求正在处理
        this.lock.unlock();

        call.setVal(func.get());
        call.done(); //请求结束，计数-1

        this.lock.lock();
        this.callMap.remove(key); //删除键值映射，释放内存
        this.lock.unlock();

        return call.getVal(); //返回结果
    }

    //测试相同的请求只会执行一次
    public static void main(String[] args) {
        CallManage callManage = new CallManage();
        int count = 10; //访问次数
        CountDownLatch latch = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            new Thread( () ->
            {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            byte[] res = callManage.run("key", () ->{
                        System.out.println("fun");
                        return ByteArrayUtil.obj2Byte("bar");
                    });
                System.out.println(ByteArrayUtil.byte2Obj(res).toString());
            }).start();
            latch.countDown();
        }
    }
}
