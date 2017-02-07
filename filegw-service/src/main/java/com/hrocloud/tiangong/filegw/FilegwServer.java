package com.hrocloud.tiangong.filegw;

import org.springframework.util.ClassUtils;

public class FilegwServer {
    public static void main(String[] args) {
        System.getProperties().list(System.out);
        System.out.println("new SecurityInit().init()");
        System.out.println(FilegwServer.class.getClassLoader().getResource("javax/crypto/KeyGenerator.class"));
//        System.out.println(Class.forName("javax.crypto.KeyGenerator").get);
        new SecurityInit().init();
         com.alibaba.dubbo.container.Main.main(args);
    }
}
