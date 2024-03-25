package com.XduDailyCatalog;

import com.XduDailyCatalog.entity.Shop;
import com.XduDailyCatalog.service.impl.ShopServiceImpl;
import com.XduDailyCatalog.utils.CacheClient;
import com.XduDailyCatalog.utils.RedisidWorker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import java.security.PrivateKey;
import java.util.concurrent.*;

import static com.XduDailyCatalog.utils.RedisConstants.CACHE_SHOP_KEY;

@SpringBootTest
class XduDailyCatalogApplicationTests {

    @Resource
    private ShopServiceImpl shopService;

    @Resource
    private CacheClient cacheClient;

    @Resource
    private RedisidWorker redisidWorker;

    private ExecutorService es = Executors.newFixedThreadPool(500);
    @Test
    void testIdWorker() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);

        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = redisidWorker.nextId("order");
                System.out.println("id = "+ id);
            }
            latch.countDown();
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("time = " + (end - begin));

    }


}
