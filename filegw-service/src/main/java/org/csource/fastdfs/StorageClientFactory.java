package org.csource.fastdfs;

import com.hrocloud.tiangong.filegw.util.FdfsService;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * Created by hanzhihua on 2017/2/5.
 */
public class StorageClientFactory  implements PooledObjectFactory<StorageClient> {

    private String trackerAddress;
    private String group;

    private final static Logger logger = LoggerFactory.getLogger(StorageClientFactory.class);

    public StorageClientFactory(String trackerAddress,String group){
        this.trackerAddress = trackerAddress;
        this.group = group;
    }

    @Override
    public PooledObject<StorageClient> makeObject() throws Exception {

        String[] parts = trackerAddress.split("\\:", 2);
        InetSocketAddress[] tracker_servers = new InetSocketAddress[1];
        tracker_servers[0] = new InetSocketAddress(parts[0].trim(), Integer.parseInt(parts[1].trim()));
        TrackerGroup g_tracker_group = new TrackerGroup(tracker_servers);
        ClientGlobal.setG_charset("utf-8");
        ClientGlobal.setG_tracker_group(g_tracker_group);

        TrackerClient tracker = new TrackerClient();
        TrackerServer trackerServer = tracker.getConnection();
        StorageServer storageServer = tracker.getStoreStorage(trackerServer, group);
        if (storageServer == null) {
            logger.error("storageServers is empty list!");
        }
        StorageClient storageClient =  new StorageClient(trackerServer, storageServer);

        return new DefaultPooledObject<StorageClient>(storageClient);
    }

    @Override
    public void destroyObject(PooledObject<StorageClient> pooledObject) throws Exception {
        StorageClient storageClient = pooledObject.getObject();
        storageClient.close();
    }

    @Override
    public boolean validateObject(PooledObject<StorageClient> pooledObject) {
        StorageClient storageClient = pooledObject.getObject();

        try {
            return storageClient.validate();
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
            return false;
        }
    }

    @Override
    public void activateObject(PooledObject<StorageClient> pooledObject) throws Exception {

    }

    @Override
    public void passivateObject(PooledObject<StorageClient> pooledObject) throws Exception {

    }
}
