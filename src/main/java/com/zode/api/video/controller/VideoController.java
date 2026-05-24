package com.zode.api.video.controller;

import com.common.base.BaseController;
import com.zode.api.handler.PlugInAuth;
import com.zode.api.video.service.VideoService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @author zhoump
 * @date 2026/5/24
 * @purpose
 */
@RestController
@RequestMapping("/v1/video/")
public class VideoController extends BaseController {

    @Resource
    VideoService videoService;

    @PlugInAuth
    @GetMapping("bliVideoParser")
    public String bliVideoParser(String bid){
        return videoService.bliVideoParser(bid);
    }

}
