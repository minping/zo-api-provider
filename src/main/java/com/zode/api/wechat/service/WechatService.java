package com.zode.api.wechat.service;

import com.common.util.Result;

import java.util.Map;

/**
 * @author zhoump
 * @date 2026/5/24
 * @purpose
 */
public interface WechatService {

    /**
     * 获取微信公众号Token
     * @param appId
     * @param appSecret
     * @return
     */
    Result getWechatToken(String appId, String appSecret);

    /**
     * 公众号文章上传媒体材料
     * @param accessToken
     * @param filePath
     * @return
     */
    Result uploadImg(String accessToken, String filePath);

    /**
     * 公众号文章上传媒体材料
     * @param accessToken
     * @param filePath
     * @return
     */
    Result addMaterial(String accessToken, String filePath);

    /**
     * 添加草稿
     * @param param
     * @return
     */
    Result addDraft(Map<String,Object> param);

    /**
     * 获取微信文章封面图片
     * @param width
     * @param height
     * @return
     */
    String fetchWeChatArticleCoverImage(String width,String  height);
}
