package com.hrocloud.tiangong.filegw;

import com.hrocloud.apigw.client.utils.AesHelper;
import com.hrocloud.apigw.client.utils.Base64Util;
import com.hrocloud.tiangong.filegw.api.FileTokenService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.net.URLEncoder;
import javax.annotation.Resource;

/**
 * Test for VendorMapper
 *
 * @author Sean Gao
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:META-INF/spring/application-common.xml",
        "classpath:META-INF/spring/application-export.xml",
        "classpath:META-INF/spring/application-service.xml"})
public class FileServiceTest {
    static final Logger Log = LoggerFactory.getLogger(FileServiceTest.class);

    @Resource
    private FileTokenService fileTokenService;

    @Test
    public void testRequestFileToken() throws Exception {
        System.out.println("token="+URLEncoder.encode(fileTokenService.requestFileToken(1000L, 10909L, "0", "M00/01/B5/wKgLPViJ43mAJxryAAAWHGrv0PE47..png", System.currentTimeMillis()+120*1000)));
        System.out.println("=================================");
        System.out.println("token="+URLEncoder.encode(fileTokenService.requestFileToken(1000L, 10909L, "0", "M00/01/B5/wKgLPViC2gyAAOn9AAVzLhEqcT47173923", System.currentTimeMillis()+120*1000)));
//        Thread.sleep(3600000);
        System.out.println("=================================");
    }


    @Test
    public void testEncodeCookie() throws Exception{
        AesHelper aesHelper = new AesHelper(Base64Util.decode("eqHSs48SCL2VoGsW1lWvDWKQ8Vu71UZJyS7Dbf/e4zo="), null);
        System.out.println("=================================");
        String encode = Base64Util.encodeToString(aesHelper.encrypt((1000L + "|" + 10909L + "|0|" + System.currentTimeMillis() + "|"+System.currentTimeMillis()+"||||").getBytes()));
        System.out.println(URLEncoder.encode(encode));
        System.out.println(encode);
        System.out.println("=================================");
        System.out.println(new String(aesHelper.decrypt(Base64Util.decode(encode)),"utf-8"));
    }


    @Test
    public void genTokenAnd_tk() throws Exception {
        String token = "token="+URLEncoder.encode(fileTokenService.requestFileToken(1000L, 11909L, "0", "M00/01/B5/wKgLPViJ8AKAZpbiAAABE4SuQzI15..png", System.currentTimeMillis()+120*1000));
        System.out.println("=================================");
        AesHelper aesHelper = new AesHelper(Base64Util.decode("eqHSs48SCL2VoGsW1lWvDWKQ8Vu71UZJyS7Dbf/e4zo="), null);
       String _tk = "_tk="+URLEncoder.encode(Base64Util.encodeToString(aesHelper.encrypt((1000L + "|" + 11909L + "|0|" + System.currentTimeMillis() + "|||||").getBytes())), "UTF-8");

        System.out.println("http://filegw.hrocloud.com/file?"+token+"&"+_tk);
    }
}
