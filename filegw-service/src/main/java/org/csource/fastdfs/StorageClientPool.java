package org.csource.fastdfs;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * Created by hanzhihua on 2017/2/5.
 */
public class StorageClientPool extends Pool<StorageClient> {

    public StorageClientPool(String trackerAddress,String group){
        super(new GenericObjectPoolConfig(),new StorageClientFactory(trackerAddress,group));
    }
}
