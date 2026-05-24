package com.zode.api.wechat.service.impl;

import com.common.util.Result;
import com.common.util.StrKit;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zode.api.util.HttpClientRedirect;
import com.zode.api.util.ImageDownloader;
import com.zode.api.util.RedirectChainResult;
import com.zode.api.wechat.service.WechatService;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author zhoump
 * @date 2026/5/24
 * @purpose
 */
@Service
public class WechatServiceImpl implements WechatService {

    // 接口基础地址
    public static final String BASE_URL = "https://api.weixin.qq.com/cgi-bin";

    @Value("${zode.file.path}")
    private String baseDir;

    @Resource
    StringRedisTemplate stringRedisTemplate;

    // 1. 创建 ObjectMapper 实例（通常全局共享一个）
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * 获取微信公众号Token
     *
     * @param appId
     * @param appSecret
     * @return
     */
    @Override
    public Result getWechatToken(String appId, String appSecret) {
        if (StrKit.isBlank(appId) || StrKit.isBlank(appSecret)){
            return Result.me().error().setMessage("缺少参数");
        }
        String token = stringRedisTemplate.opsForValue().get("wechat:token:" + appId + appSecret);
        if (StrKit.isNotBlank(token)){
            return Result.me().success().setData(token);
        }
        String url = BASE_URL + "/token?grant_type=client_credential" +
                "&appid=" + appId +
                "&secret=" + appSecret;
        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(new HttpGet(url))) {

            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity);
            Map<String, Object> map = mapper.readValue(result, Map.class);
            if (map.containsKey("access_token")) {
                stringRedisTemplate.opsForValue().set("wechat:token:" + appId + appSecret,map.get("access_token").toString(),7200, TimeUnit.SECONDS);
                // 将获取token存储
                return Result.me().success().setData(map.get("access_token").toString());
            } else {
                return Result.me().error().setMessage("获取Token失败: " + map.toString());
            }
        } catch (Exception e) {
            return Result.me().error().setMessage("获取Token失败: " + e.getMessage());
        }
    }

    /**
     * 公众号文章上传媒体材料
     *
     * @param accessToken
     * @param filePath
     * @return
     */
    @Override
    public Result uploadImg(String accessToken, String filePath) {
        String url = BASE_URL + "/material/add_material?access_token=" + accessToken+"&type=image";

        HttpPost post = new HttpPost(url);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        // 注意：参数名必须是 "media"
        try {
            File imageFile = ImageDownloader.downloadWithJavaNIO(filePath, baseDir);
            builder.addBinaryBody("media", imageFile,
                    ContentType.IMAGE_JPEG,
                    imageFile.getName());
        }catch (Exception e){
            return Result.me().error().setMessage("图片下载失败");
        }
        post.setEntity(builder.build());

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(post)) {

            String result = EntityUtils.toString(response.getEntity());
            Map<String, Object> map = mapper.readValue(result, Map.class);
            if (map.containsKey("url")) {
                return Result.me().success().setData(map.get("url").toString());
            }
            throw new RuntimeException("上传图片失败: " + result);
        }catch (Exception e){
            return Result.me().error().setData("").setMessage("图片上传失败"+e.getMessage());
        }
    }

    /**
     * 公众号文章上传媒体材料
     *
     * @param accessToken
     * @param filePath
     * @return
     */
    @Override
    public Result addMaterial(String accessToken, String filePath) {
        String url = BASE_URL + "/material/add_material?access_token="
                + accessToken + "&type=image";

        HttpPost post = new HttpPost(url);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        try {
            File imageFile = ImageDownloader.downloadWithJavaNIO(filePath, baseDir);
            builder.addBinaryBody("media", imageFile,
                    ContentType.IMAGE_JPEG,
                    imageFile.getName());
        }catch (Exception e){
            return Result.me().error().setData("").setMessage("图片下载失败");
        }
        // 如果是视频，需要额外传 description 参数（此处省略）
        post.setEntity(builder.build());

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(post)) {
            String result = EntityUtils.toString(response.getEntity());
            Map<String, Object> map = mapper.readValue(result, Map.class);
            if (map.containsKey("media_id")) {
                return Result.me().success().setData(map.get("media_id").toString());
            }
            return Result.me().error().setData("").setMessage("上传永久素材失败: "+result);
        }catch (Exception e){
            return Result.me().error().setData("").setMessage("上传永久素材失败: "+e.getMessage());
        }
    }

    /**
     * 添加草稿
     *
     * @param param
     * @return
     */
    @Override
    public Result addDraft(Map<String,Object> param) {
        String accessToken = param.get("accessToken").toString();
        String title = param.get("title").toString();
        String mediaId = param.get("mediaId").toString();
        String content = param.get("content").toString();
        String author = param.get("author").toString();
        String url = BASE_URL + "/draft/add?access_token=" + accessToken;

        // 构建图文消息结构
        String jsonBody = String.format(
                "{\"articles\": [{" +
                        "\"title\": \"%s\"," +
                        "\"thumb_media_id\": \"%s\"," +
                        "\"content\": \"%s\"," +
                        "\"author\": \"%s\"," +
                        "\"content_source_url\": \"\"," + // 阅读原文链接（可选）
                        "\"show_cover_pic\": 1" +        // 是否显示封面
                        "}]}",
                title, mediaId, content.replace("\"", "\\\""),author); // 转义双引号

        HttpPost post = new HttpPost(url);
        post.setHeader("Content-Type", "application/json");
        post.setEntity(new StringEntity(jsonBody, "UTF-8"));

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(post)) {

            String result = EntityUtils.toString(response.getEntity());
            Map<String, Object> map = mapper.readValue(result, Map.class);
            if (map.containsKey("media_id")) {
                return Result.me().success().setData(map.get("media_id").toString());
            }
            throw new RuntimeException("创建草稿失败: " + result);
        }catch (Exception e){
            return Result.me().error().setData("").setMessage("创建草稿失败: "+ e.getMessage());
        }
    }

    @Override
    public String fetchWeChatArticleCoverImage(String width, String height) {
        String apiUrl = StrKit.format("https://picsum.photos/{}/{}",width,height);
        HttpClientRedirect httpClientRedirect = new HttpClientRedirect();
        try{
            RedirectChainResult redirectChainResult = httpClientRedirect.fetchWithRedirectChain(apiUrl);
            List<String> redirectChain = redirectChainResult.getRedirectChain();
            if (redirectChain.isEmpty()){
                return "未获取到图片链接";
            }
            return redirectChain.get(redirectChain.size() - 1);
        }catch (Exception e){
            return e.getMessage();
        }
    }
}
