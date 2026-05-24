package com.zode.api.video.service;

/**
 * @author zhoump
 * @date 2026/5/24
 * @purpose
 */
public interface VideoService {


    /**
     * 解析b站视频
     * @param bid
     * @return
     */
    String bliVideoParser(String bid);
}
