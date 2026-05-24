package com.zode.api.video.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zode.api.video.dto.VideoInfo;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.concurrent.TimeUnit;

/**
 * @author zhoump
 * @date 2026/4/14
 * @purpose
 */
@Component
public class BilibiliAudioDownloader {

    @Value("${zode.file.path}")
    private String path;


    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)  // 增加超时时间，音频文件可能较大
            .build();

    private static final ObjectMapper mapper = new ObjectMapper();


    // 1. 获取视频基本信息
    public VideoInfo getVideoInfo(String bvid) throws IOException {
        String url = "https://api.bilibili.com/x/web-interface/view?bvid=" + bvid;

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Referer", "https://www.bilibili.com")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            JsonNode root = mapper.readTree(responseBody);

            if (root.get("code").asInt() != 0) {
                throw new IOException("获取视频信息失败: " + root.get("message").asText());
            }

            JsonNode data = root.get("data");
            VideoInfo info = new VideoInfo();
            info.setBvid(bvid);
            info.setCid(data.get("cid").asLong());
            info.setTitle(cleanFileName(data.get("title").asText()));
            info.setAuthor(data.get("owner").get("name").asText());

            return info;
        }
    }

    // 2. 获取音频URL（优化：只获取音频流）
    public String getAudioUrl(String bvid, long cid) throws IOException {
        String url = "https://api.bilibili.com/x/player/playurl";

        HttpUrl httpUrl = HttpUrl.parse(url).newBuilder()
                .addQueryParameter("bvid", bvid)
                .addQueryParameter("cid", String.valueOf(cid))
                .addQueryParameter("qn", "16")  // 最低画质，减少数据量
                .addQueryParameter("fnval", "16")  // DASH格式
                .addQueryParameter("fourk", "0")
                .build();

        Request request = new Request.Builder()
                .url(httpUrl)
                .header("User-Agent", "Mozilla/5.0")
                .header("Referer", "https://www.bilibili.com")
                .build();

        try (Response response = client.newCall(request).execute()) {
            JsonNode root = null;
            if (response.body() != null) {
                root = mapper.readTree(response.body().string());
            }
            if (root == null){
                throw new IOException("请求失败: " + response.code());
            }
            if (root.get("code").asInt() != 0) {
                throw new IOException("获取播放地址失败: " + root.get("message").asText());
            }

            // 直接获取音频流URL
            JsonNode dash = root.get("data").get("dash");
            JsonNode audio = dash.get("audio").get(0);
            return audio.get("baseUrl").asText();
        }
    }

    // 3. 下载音频流（直接保存为临时文件）
    public File downloadAudio(String audioUrl, String tempDir) throws IOException {
        Request request = new Request.Builder()
                .url(audioUrl)
                .header("User-Agent", "Mozilla/5.0")
                .header("Referer", "https://www.bilibili.com")
                .header("Range", "bytes=0-")  // 支持断点续传
                .build();

        File tempDirFile = new File(tempDir);
        if (!tempDirFile.exists()) {
            tempDirFile.mkdirs();
        }

        File tempFile = new File(tempDir, "audio_temp.m4s");

        try (Response response = client.newCall(request).execute();
             InputStream is = response.body().byteStream();
             FileOutputStream fos = new FileOutputStream(tempFile)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = 0;

            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;

                // 显示下载进度
                if (totalBytes % (1024 * 1024) == 0) { // 每MB显示一次
                    System.out.printf("已下载: %.2f MB\r", totalBytes / (1024.0 * 1024.0));
                }
            }

            System.out.println("\n音频下载完成: " + tempFile.getAbsolutePath());
            System.out.println("文件大小: " + (totalBytes / (1024 * 1024)) + " MB");

            return tempFile;
        }
    }

    // 4. 转换为MP3（核心方法）
    public File convertToMP3(File inputFile, String outputPath, String title) throws IOException, InterruptedException {
        // 清理文件名中的非法字符
        String safeTitle = cleanFileName(title);
        System.out.println("文件放置目录outputPath = " + outputPath);
        File outputFile = new File(outputPath, safeTitle + ".mp3");

        // 如果已存在，先删除
        if (outputFile.exists()) {
            outputFile.delete();
        }

        // FFmpeg转换命令
        // -i 输入文件
        // -vn 不要视频流
        // -ar 44100 采样率
        // -ac 2 立体声
        // -b:a 192k 比特率（192kbps，高质量）
        // -y 覆盖输出文件
        String[] ffmpegCmd = {
                "ffmpeg",
                "-i", inputFile.getAbsolutePath(),
                "-vn",                    // 不要视频
                "-ar", "44100",           // 采样率
                "-ac", "2",               // 立体声
                "-b:a", "192k",           // 音频比特率
                "-f", "mp3",              // 输出格式
                "-y",                     // 覆盖已存在文件
                outputFile.getAbsolutePath()
        };

        System.out.println("正在转换为MP3...");
        System.out.println("转换命令: " + String.join(" ", ffmpegCmd));

        ProcessBuilder pb = new ProcessBuilder(ffmpegCmd);
        pb.redirectErrorStream(true);  // 合并错误流和输出流

        Process process = pb.start();

        // 读取FFmpeg输出
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("time=")) {
                    System.out.println("转换进度: " + line);
                }
            }
        }

        int exitCode = process.waitFor();

        if (exitCode == 0) {
            System.out.println("转换成功！MP3文件: " + outputFile.getAbsolutePath());

            // 删除临时文件
            if (inputFile.delete()) {
                System.out.println("已清理临时文件");
            }

            return outputFile;
        } else {
            throw new IOException("FFmpeg转换失败，退出码: " + exitCode);
        }
    }

    // 5. 清理文件名（移除Windows/文件系统不允许的字符）
    private  String cleanFileName(String fileName) {
        if (fileName == null) return "audio";
        // 移除非法字符
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", " ")
                .trim();
    }

    // 6. 主流程
    public File downloadBilibiliAudioAsMP3(String bvid) {
        String outputDir = path;

        try {
            System.out.println("=== B站音频下载器 ===");
            System.out.println("视频BV号: " + bvid);

            // 1. 获取视频信息
            System.out.println("\n[1/4] 获取视频信息...");
            VideoInfo info = getVideoInfo(bvid);
            System.out.println("标题: " + info.getTitle());
            System.out.println("UP主: " + info.getAuthor());
            System.out.println("CID: " + info.getCid());

            // 2. 获取音频URL
            System.out.println("\n[2/4] 获取音频地址...");
            String audioUrl = getAudioUrl(info.getBvid(), info.getCid());
            System.out.println("音频流获取成功");

            // 3. 下载音频
            System.out.println("\n[3/4] 下载音频流...");
            File tempAudio = downloadAudio(audioUrl, outputDir + "temp/");

            // 4. 转换为MP3
            System.out.println("\n[4/4] 转换为MP3格式...");
            File mp3File = convertToMP3(tempAudio, outputDir, bvid);

            System.out.println("\n✅ 下载完成！");
            System.out.println("文件位置: " + mp3File.getAbsolutePath());
            System.out.println("文件大小: " + (mp3File.length() / (1024 * 1024)) + " MB");

            return mp3File;

        } catch (Exception e) {
            System.err.println("❌ 下载失败: " + e.getMessage());
        }
        return null;
    }
}
