package org.csource.fastdfs;

/**
 * Created by hanzhihua on 2017/2/5.
 */
public class FdfsException extends RuntimeException {
    private static final long serialVersionUID = -2946266495682282677L;

    public FdfsException(String message) {
        super(message);
    }

    public FdfsException(Throwable e) {
        super(e);
    }

    public FdfsException(String message, Throwable cause) {
        super(message, cause);
    }
}
