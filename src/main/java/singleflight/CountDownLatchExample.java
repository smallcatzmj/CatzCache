package singleflight;

import java.util.concurrent.CountDownLatch;

/**
 * @description: CountDownLatch实例测试
 * @author: zmj
 * @date 2021/3/25 15:31
 */
public class CountDownLatchExample {
    public static void main(String[] args) throws InterruptedException{
        //计数值为5
        CountDownLatch latch = new CountDownLatch(5);
        Service service = new Service(latch);
        Runnable task = () -> service.exec();
        for (int i = 0; i < 5; i++) {
            Thread thread = new Thread(task);
            thread.start();
        }

        System.out.println("main thread wait.");
        latch.await();
        System.out.println("main thread finishes await.");
    }
}
