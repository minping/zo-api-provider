package com.zode.api.video.dto;

import lombok.Data;

/**
 * @author zhoump
 * @date 2026/4/14
 * @purpose
 */
@Data
public class VideoInfo {
    private String bvid;
    private long aid;
    private long cid;
    private String title;
    private String author;
    private int duration;
}
