package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 通用接口
 */
@RestController
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
@Slf4j
public class CommonController {

    @Value("${media.path}")
    private String mediaPath;

    @Value("${media.url}")
    private String mediaUrl;

    /**
     * 文件上传
     * @param file
     * @return
     */
    @PostMapping("/upload")
    @ApiOperation("文件上传")
    public Result<String> upload(MultipartFile file) {
        log.info("文件上传：{}", file);

        // 原始文件名
        String originalName = file.getOriginalFilename();
        String suffix = originalName.substring(originalName.lastIndexOf("."));

        // 使用UUID重新生成文件名，防止重复造成覆盖
        String fileName = UUID.randomUUID().toString() + suffix;

        try {
            File dir = new File(mediaPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            file.transferTo(new File(mediaPath + fileName));
            log.info("上传到：{}", mediaPath + fileName);

            if (!(mediaUrl.startsWith("http://") || mediaUrl.startsWith("https//"))) {
                mediaUrl = "http://" + mediaUrl;
            }
            log.info("图片地址：{}", mediaUrl + "/img/" + fileName);
            return Result.success(mediaUrl + fileName);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return Result.error(MessageConstant.UPLOAD_FAILED);
    }
}
