package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private WeChatProperties weChatProperties;

    @Autowired
    private UserMapper userMapper;

    @Override
    public User login(UserLoginDTO dto) {
        //1.使用dto中的code调用微信接口获取openid
        Map<String, String> param = new HashMap<>();
        param.put("appid", weChatProperties.getAppid());
        param.put("secret",weChatProperties.getSecret());
        param.put("js_code",dto.getCode());
        param.put("grant_type","authorization_code");

        String res = HttpClientUtil.doGet("https://api.weixin.qq.com/sns/jscode2session", param);
        log.info("调用微信接口获取的响应结果：{}",res);
        //2.解析响应结果
        JSONObject jsonObject = JSON.parseObject(res);
        String openid = (String)jsonObject.get("openid");

        if(openid==null){
            throw new LoginFailedException(MessageConstant.USER_NOT_LOGIN);
        }
        //3.通过openid查询数据库，判断用户是否存在
        User user= userMapper.select(openid);

        //4.如果用户不存在，注册新用户
        if(user==null){
            user=new User();
            //补充数据
            user.setOpenid(openid);
            user.setName(openid.substring(0,5));
            user.setCreateTime(LocalDateTime.now());

            //插入数据库
            userMapper.insert(user);
        }

        //5.如果用户存在返回数据
        return user;

    }
}
