package com.hrocloud.tiangong.filegw.util;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by hanzhihua on 2017/2/5.
 */
public class FdfsServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(FdfsServiceTest.class);
    @Test
    public void testFunction() throws Exception {
        FdfsService service = new FdfsService();
        service.setTrackerAddress("privatefile.hrocloud.com:22122");
        service.init();
        FileInputStream fis = new FileInputStream("C:\\Program Files\\Common Files\\microsoft shared\\Stationery\\SoftBlue.jpg");
        byte[] bytes = new byte[fis.available()];
        int fileLength = bytes.length;
        String fileKey = service.save(bytes);
        System.out.println("fileKey:"+fileKey+",fileLength:"+fileLength);
        System.out.println(service.statFile(fileKey));
        Assert.assertTrue(service.download(fileKey).length==fileLength);
    }

    public void testFuncionWithMultiThread() throws Exception{
        final FdfsService service = new FdfsService();
        service.setTrackerAddress("privatefile.hrocloud.com:22122");
        service.init();

        final AtomicInteger count = new AtomicInteger();

        int threads = 15;
        final int repeat = 1;

        final CountDownLatch latch = new CountDownLatch(threads);

        for(int t=0;t<threads;t++){
            new Thread("threads"+t){
                public void run(){
                    for(int c=0;c<repeat;c++) {
                        try {
                            testFuncion(service);
                            count.incrementAndGet();
                        } catch (Exception e) {
                            logger.error(e.getMessage(),e);
                        }
                    }
                    latch.countDown();
                }
            }.start();
        }
        latch.await();
        System.out.println("finished!!!");
        logger.error(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>count:{}",count);



    }

    private void testFuncion(FdfsService service)  throws Exception{
        FileInputStream fis = new FileInputStream("C:\\Program Files\\Common Files\\microsoft shared\\Stationery\\SoftBlue.jpg");
        byte[] bytes = new byte[fis.available()];
        String fileKey = service.save(bytes);
        service.statFile(fileKey);
        service.download(fileKey);
    }

    public static void main(String[] args) throws Exception {
        FdfsServiceTest fst = new FdfsServiceTest();
        fst.testFuncionWithMultiThread();
    }
}
