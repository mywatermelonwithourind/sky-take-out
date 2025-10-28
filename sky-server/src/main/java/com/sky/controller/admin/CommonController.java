package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@RequestMapping("/admin/common")
@RestController
public class CommonController {

    @Autowired
    private AliOssUtil aliOssUtil;

    //使用阿里云oss做文件上传
    @PostMapping("upload")
    public Result<String> upload(MultipartFile file)  {
        //1.调用工具类的文件上传方法
        String originalFilename = file.getOriginalFilename();
        log.info("文件上传,原始文件名：{}",originalFilename);
        String substring = originalFilename.substring(originalFilename.lastIndexOf("."));//获取文件后缀

        String url = null;
        try {
            String  objectName= UUID.randomUUID().toString()+substring;
            url = aliOssUtil.upload(file.getBytes(), objectName);
        } catch (IOException e) {
            log.info("文件上传失败，原因：{}",e.getMessage());
            return Result.error("文件上传失败");
        }

        //2.返回结果


        return Result.success(url);
    }
}
