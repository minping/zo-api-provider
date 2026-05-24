package com.zode.api.video.dto;

import lombok.Data;

/**
 * @author zhoump
 * @date 2026/4/14
 * @purpose
 */
@Data
public class PlayUrl {
    private String videoUrl;
    private String audioUrl;
    private int videoCodec;
    private int audioCodec;
    private int videoQuality;
}
