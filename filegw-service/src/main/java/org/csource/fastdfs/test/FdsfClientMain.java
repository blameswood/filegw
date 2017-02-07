package org.csource.fastdfs.test;

import org.csource.common.NameValuePair;
import org.csource.fastdfs.*;

import java.net.InetSocketAddress;
import java.util.Arrays;

/**
 * Created by hanzhihua on 2017/1/2.
 */
public class FdsfClientMain {

    public static void main(String[] args) throws Exception{

        InetSocketAddress[] tracker_servers =new InetSocketAddress[1];
        tracker_servers[0] = new InetSocketAddress("tiangong61",22122);
        TrackerGroup g_tracker_group = new TrackerGroup(tracker_servers);
        ClientGlobal.setG_charset("utf-8");
        ClientGlobal.setG_tracker_group(g_tracker_group);

        TrackerClient tracker = new TrackerClient();
        TrackerServer trackerServer = tracker.getConnection();
        StorageClient client = new StorageClient(trackerServer, null);
        byte[] file_buff = "this is a test1111111111111111111111111111111111111111111".getBytes();

        StorageServer[] storageServers = tracker.getStoreStorages(trackerServer,"group1");
        if (storageServers == null)
        {
            System.err.println("get store storage servers fail, error code: " + tracker.getErrorCode());
        }
        else
        {
            System.err.println("store storage servers count: " + storageServers.length);
            for (int k=0; k<storageServers.length; k++)
            {
                System.err.println((k+1) + ". " + storageServers[k].getInetSocketAddress().getAddress().getHostAddress() + ":" + storageServers[k].getInetSocketAddress().getPort());
            }
            System.err.println("");
        }

        String[] results = client.upload_file(file_buff, "txt", null);
        if(results != null){
            System.out.println("result:"+ Arrays.toString(results));
        }
    }
}
