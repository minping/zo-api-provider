package com.zode.api.markdown.controller;

import com.zode.api.handler.PlugInAuth;
import com.zode.api.markdown.service.MarkDownService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @author zhoump
 * @date 2026/5/24
 * @purpose
 */
@RestController
@RequestMapping("/v1/markdown/")
public class MarkDownController {

    @Resource
    MarkDownService markDownService;

    @PlugInAuth
    @PostMapping("parseMarkdown")
    public String parseMarkdown(@RequestBody Map<String,Object> param) {
        return markDownService.parseMarkdown(param);
    }

    @PlugInAuth
    @PostMapping("inlineCss")
    public String inlineCss(@RequestBody Map<String,Object> param) {
        return markDownService.inlineCss(param);
    }

}
