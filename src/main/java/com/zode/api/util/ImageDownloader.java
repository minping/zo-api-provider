package com.zode.api.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * @author zhoump
 * @date 2026/5/11
 * @purpose
 */
public class ImageDownloader {

    /**
     * 方法2：使用 Java 原生 API
     */
    public static File downloadWithJavaNIO(String imageUrl, String savePath) throws IOException {
        URL url = new URL(imageUrl);

        // 确定保存路径
        String fileName = extractFileName(imageUrl);
        File outputFile;

        if (new File(savePath).isDirectory()) {
            outputFile = new File(savePath, fileName);
        } else {
            outputFile = new File(savePath);
        }

        // 确保目录存在
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // 使用 NIO 下载文件
        try (InputStream in = url.openStream()) {
            Files.copy(in, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        System.out.println("图片下载成功: " + outputFile.getAbsolutePath());
        return outputFile;
    }

    /**
     * 从URL中提取文件名
     */
    private static String extractFileName(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            String path = url.getPath();
            String fileName = path.substring(path.lastIndexOf('/') + 1);

            if (fileName.isEmpty()) {
                // 从Content-Disposition头获取文件名（需要连接后才能获取）
                return "image_" + System.currentTimeMillis() + ".jpg";
            }

            return fileName;
        } catch (Exception e) {
            return "image_" + System.currentTimeMillis() + ".jpg";
        }
    }

}
