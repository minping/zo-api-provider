package com.zode.api.video.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zode.api.video.dto.PlayUrl;
import com.zode.api.video.dto.VideoInfo;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * @author zhoump
 * @date 2026/4/14
 * @purpose
 */
public class BilibiliVideoParser {

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    private static final ObjectMapper mapper = new ObjectMapper();

    // 1. 获取视频信息（包括CID）
    public static VideoInfo getVideoInfo(String bvid) throws IOException {
        String url = "https://api.bilibili.com/x/web-interface/view?bvid=" + bvid;

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .header("Referer", "https://www.bilibili.com")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response.code());
            }

            JsonNode root = mapper.readTree(response.body().string());
            int code = root.get("code").asInt();

            if (code != 0) {
                throw new IOException("API错误: " + root.get("message").asText());
            }

            JsonNode data = root.get("data");
            VideoInfo info = new VideoInfo();
            info.setBvid(bvid);
            info.setAid(data.get("aid").asLong());
            info.setCid(data.get("cid").asLong());
            info.setTitle(data.get("title").asText());
            info.setAuthor(data.get("owner").get("name").asText());
            info.setDuration(data.get("duration").asInt());

            return info;
        }
    }

    // 2. 获取播放地址（支持不同画质）
    public static PlayUrl getPlayUrl(String bvid, long cid, int quality) throws IOException {
        String url = "https://api.bilibili.com/x/player/playurl";

        HttpUrl httpUrl = HttpUrl.parse(url).newBuilder()
                .addQueryParameter("bvid", bvid)
                .addQueryParameter("cid", String.valueOf(cid))
                .addQueryParameter("qn", String.valueOf(quality))  // 画质代码
                .addQueryParameter("fnval", "16")  // 16表示DASH格式
                .addQueryParameter("fourk", "1")   // 支持4K
                .build();

        Request request = new Request.Builder()
                .url(httpUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .header("Referer", "https://www.bilibili.com")
                .header("Origin", "https://www.bilibili.com")
                .build();

        try (Response response = client.newCall(request).execute()) {
            JsonNode root = mapper.readTree(response.body().string());

            if (root.get("code").asInt() != 0) {
                throw new IOException("获取播放地址失败: " + root.get("message").asText());
            }

            JsonNode dash = root.get("data").get("dash");
            PlayUrl playUrl = new PlayUrl();

            // 获取视频流（取第一个）
            JsonNode video = dash.get("video").get(0);
            playUrl.setVideoUrl(video.get("baseUrl").asText());
            playUrl.setVideoCodec(video.get("codecid").asInt());
            playUrl.setVideoQuality(video.get("id").asInt());

            // 获取音频流（取第一个）
            JsonNode audio = dash.get("audio").get(0);
            playUrl.setAudioUrl(audio.get("baseUrl").asText());
            playUrl.setAudioCodec(audio.get("codecid").asInt());

            return playUrl;
        }
    }

    // 3. 下载文件
    public static void downloadFile(String url, String savePath) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .header("Referer", "https://www.bilibili.com")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("下载失败: " + response.code());
            }

            try (InputStream is = response.body().byteStream();
                 FileOutputStream fos = new FileOutputStream(savePath)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;

                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }
                System.out.println("下载完成: " + savePath + " (" + totalBytes + " bytes)");
            }
        }
    }

    // 4. 主程序示例
    public static void main(String[] args) {
        try {
            // 示例：解析 BV1xx411c7mD
            String bvid = "BV1GXQgBEE2x";  // 替换为你要解析的BV号

            System.out.println("正在获取视频信息...");
            VideoInfo info = getVideoInfo(bvid);
            System.out.println("标题: " + info.getTitle());
            System.out.println("作者: " + info.getAuthor());
            System.out.println("时长: " + info.getDuration() + "秒");
            System.out.println("CID: " + info.getCid());

            System.out.println("\n正在获取播放地址...");
            // 画质代码映射：16=360P, 32=480P, 64=720P, 80=1080P, 112=1080P+, 116=4K
            PlayUrl playUrl = getPlayUrl(bvid, info.getCid(), 80);
            System.out.println("视频流: " + playUrl.getVideoUrl().substring(0, 100) + "...");
            System.out.println("音频流: " + playUrl.getAudioUrl().substring(0, 100) + "...");

            // 下载文件
            String basePath = "D:/java/Project/zode/zo-account/files/";

            System.out.println("\n开始下载视频流...");
            downloadFile(playUrl.getVideoUrl(), basePath + "video.m4s");

            System.out.println("开始下载音频流...");
            downloadFile(playUrl.getAudioUrl(), basePath + "audio.m4s");

            System.out.println("\n下载完成！请使用以下FFmpeg命令合并：");
            System.out.println("ffmpeg -i video.m4s -i audio.m4s -c:v copy -c:a copy output.mp4");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
