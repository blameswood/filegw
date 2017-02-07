package com.hrocloud.tiangong.filegw.api;


public interface FileTokenService {

    /**
     *
     * @param domainId 业务域id
     * @param userId 用户id
     * @param group group=0公有云，group=1私有云
     * @param fileKey fdfs key
     * @param expireAt 过期时间
     * @return
     */
    public String requestFileToken(Long domainId, Long userId,String group, String fileKey, long expireAt);
}
