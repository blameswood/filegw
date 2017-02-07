package com.hrocloud.tiangong.filegw.util;

import org.csource.fastdfs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;

/**
 * Created by hanzhihua on 2017/1/2.
 */
public class FdfsService {

    private String trackerAddress;

    //    private StorageClient client;
    private StorageClientPool storageClientPool;

    private final static String DEFAULT_GROUP = "group1";

    private static final Logger logger = LoggerFactory.getLogger(FdfsService.class);

    public String getTrackerAddress() {
        return trackerAddress;
    }

    public void setTrackerAddress(String trackerAddress) {
        this.trackerAddress = trackerAddress;
    }

    public void init() {
        storageClientPool = new StorageClientPool(trackerAddress, DEFAULT_GROUP);
    }

    public String saveFile(BufferedImage bufferedImage) throws Exception {
        int retry = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "jpg", baos);
        byte[] content = baos.toByteArray();
        return save(content);
    }

    public String save(byte[] content) throws Exception {
        StorageClient storageClient = storageClientPool.getResource();
        try {
            String[] results = storageClient.upload_file(content, "jpg", null);
            return results[1];
        } finally {
            storageClientPool.returnResource(storageClient);
        }
    }

    public String save(byte[] content, String ext) throws Exception {
        StorageClient storageClient = storageClientPool.getResource();
        try {
            String[] results = storageClient.upload_file(content, ext, null);
            return results[1];
        } finally {
            storageClientPool.returnResource(storageClient);
        }
    }

    public byte[] download(String fileKey) throws Exception {
        StorageClient storageClient = storageClientPool.getResource();
        try {
            return storageClient.download_file(FdfsService.DEFAULT_GROUP, fileKey);
        } finally {
            storageClientPool.returnResource(storageClient);
        }
    }

    public FileInfo statFile(String fileKey) throws Exception {
        StorageClient storageClient = storageClientPool.getResource();
        try {
            return storageClient.get_file_info(FdfsService.DEFAULT_GROUP, fileKey);
        } finally {
            storageClientPool.returnResource(storageClient);
        }
    }
}
