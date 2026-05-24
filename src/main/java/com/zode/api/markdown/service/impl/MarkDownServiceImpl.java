package com.zode.api.markdown.service.impl;

import com.common.util.StrKit;
import com.zode.api.markdown.service.MarkDownService;
import com.zode.api.markdown.util.MarkdownToHtmlConverter;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author zhoump
 * @date 2026/5/24
 * @purpose
 */
@Service
public class MarkDownServiceImpl implements MarkDownService {

    @Override
    public String parseMarkdown(Map<String, Object> param) {
        if(!param.containsKey("content") || param.get("content") == null){
            return "参数错误";
        }
        String content = param.get("content").toString();
        try {
            content =  URLDecoder.decode(content, String.valueOf(StandardCharsets.UTF_8));
            content = StrKit.base64Decoder(content);
        }catch (Exception e){
            System.out.println("解码错误 = " + e.getMessage());
        }
        return MarkdownToHtmlConverter.parseMarkdown(content);
    }

    /**
     * html内联样式
     *
     * @param param
     * @return
     */
    @Override
    public String inlineCss(Map<String, Object> param) {
        if(!param.containsKey("content") || param.get("content") == null){
            return "参数错误";
        }
        String content = param.get("content").toString();
        return MarkdownToHtmlConverter.inlineCss(content);
    }
}
