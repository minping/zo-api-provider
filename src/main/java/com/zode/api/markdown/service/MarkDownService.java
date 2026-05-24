package com.zode.api.markdown.service;

import java.util.Map;

/**
 * @author zhoump
 * @date 2026/5/24
 * @purpose
 */
public interface MarkDownService {

    /**
     * 解析markdown
     * @param param
     * @return
     */
    String parseMarkdown(Map<String,Object> param);

    /**
     * html内联样式
     * @param param
     * @return
     */
    String inlineCss(Map<String, Object> param);
}
