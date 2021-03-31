package singleflight;

/**
 * @description: 正在进行中，或已经结束的请求
 * @author: zmj
 * @date 2021/3/25 14:30
 */

import java.util.concurrent.CountDownLatch;

/**
 * 1.缓存雪崩：缓存在同一时刻全部失效。原因有缓存服务器宕机，key设置了相同的过期时间。
 * 2.缓存击穿：key过期的时刻有大量请求请求到该key。
 * 3.缓存穿透：查询一个不存在的数据，瞬间流量过大，穿透DB，导致宕机。
 * */
public class Call {
    private byte[] val;
    private CountDownLatch cld;

    public byte[] getVal() {
        return val;
    }

    public void setVal(byte[] val) {
        this.val = val;
    }

    public void await() {
        try{
            this.cld.await();
        }catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void lock() {
        this.cld = new CountDownLatch(1);
    }

    public void done() {
        this.cld.countDown();
    }
}
