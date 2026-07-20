package com.zode.api.video.service.impl;

import com.common.util.StrKit;
import com.file.util.FileUtils;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.zode.api.video.service.VideoService;
import com.zode.api.video.util.BilibiliAudioDownloader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;

/**
 * @author zhoump
 * @date 2026/5/24
 * @purpose
 */
@Service
public class VideoServiceImpl implements VideoService {

    @Value("${zode.server.path}")
    private String serverPath;

    @Resource
    BilibiliAudioDownloader bilibiliAudioDownloader;
    @Resource
    private FileUtils fileUtils;

    /**
     * 解析b站视频
     *
     * @param bid
     * @return
     */
    @Override
    public String bliVideoParser(String bid) {
        if (bid == null || bid.isEmpty()) {
            return "参数错误";
        }
        Record ex = Db.findById("zo_file", "original_name", bid + ".mp3");
        if (ex != null){
            return serverPath+"file/download/audio/"+ex.getStr("original_name");
        }
        File audio = bilibiliAudioDownloader.downloadBilibiliAudioAsMP3(bid);
        // 生成存储文件名
        String originalFilename = audio.getName();
        String fileExtension =  fileUtils.getFileExtension(originalFilename);
        String storageName =  fileUtils.generateStorageName(fileExtension);
        // 计算MD5
        String md5Hash = fileUtils.calculateMd5(audio.getPath());
        // 保存元数据
        Record zoFile = new Record();
        zoFile.set("zo_file_id", StrKit.uuid());
        zoFile.set("created_time",new Date());
        zoFile.set("updated_time",new Date());
        zoFile.set("upload_time",new Date());
        zoFile.set("original_name",audio.getName());
        zoFile.set("storage_name",storageName);
        zoFile.set("file_path",audio.getAbsolutePath());
        zoFile.set("file_size",audio.length());
        zoFile.set("file_type","audio");
        zoFile.set("file_extension",fileExtension);
        zoFile.set("md5_hash",md5Hash);
        Db.save("zo_file",zoFile);
        return serverPath+"file/download/audio/"+zoFile.getStr("original_name");
    }

}
