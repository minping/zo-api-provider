package com.zode.api.util;

import lombok.Data;

import java.util.List;

/**
 * @author zhoump
 * @date 2026/4/13
 * @purpose
 */
@Data
public class RedirectChainResult {
    List<String> redirectChain;
    String finalContent;

    public RedirectChainResult(List<String> redirectChain, String finalContent) {
        this.redirectChain = redirectChain;
        this.finalContent = finalContent;
    }
}
