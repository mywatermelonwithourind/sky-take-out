package com.sky.config;

import com.sky.properties.AliOssProperties;
import com.sky.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * 阿里Oss的配置类用来生成阿里oss的对象并交由容器管理
 */
@Configuration
@Slf4j
public class OssConfiguration {

    @Bean
    public AliOssUtil aliOssUtil(AliOssProperties aliOssProperties){
        log.info("初始化aliOssUtil对象");
        AliOssUtil aliOssUtil = new AliOssUtil(aliOssProperties.getEndpoint(),aliOssProperties.getAccessKeyId(),
                aliOssProperties.getAccessKeySecret(),aliOssProperties.getBucketName());
        return  aliOssUtil;
    }
}
