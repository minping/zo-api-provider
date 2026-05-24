package com.zode.api.wechat.controller;

import com.common.base.BaseController;
import com.common.util.Result;
import com.zode.api.handler.PlugInAuth;
import com.zode.api.wechat.service.WechatService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @author zhoump
 * @date 2026/5/24
 * @purpose
 */
@RestController
@RequestMapping("/v1/wechat/")
public class WechatController extends BaseController {

    @Resource
    WechatService wechatService;

    @PlugInAuth
    @GetMapping("getToken")
    public Result getToken(String appId, String appSecret){
        return wechatService.getWechatToken(appId,appSecret);
    }

    @PlugInAuth
    @GetMapping("uploadImg")
    public Result uploadImg(String accessToken,String filePath){
        return wechatService.uploadImg(accessToken,filePath);
    }

    @PlugInAuth
    @GetMapping("addMaterial")
    public Result addMaterial(String accessToken,String filePath){
        return wechatService.addMaterial(accessToken,filePath);
    }

    @PlugInAuth
    @PostMapping("addDraft")
    public Result addDraft(@RequestBody Map<String,Object> param){
        return wechatService.addDraft(param);
    }

    @PlugInAuth
    @GetMapping("fetchWeChatArticleCoverImage")
    public String fetchWeChatArticleCoverImage(String width,String  height){
        return wechatService.fetchWeChatArticleCoverImage(width, height);
    }


}
