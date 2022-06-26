package com.mars.ecsheet.controller;

import com.mars.ecsheet.common.CommonResVo;
import com.mars.ecsheet.entity.UserInfoEntity;
import com.mars.ecsheet.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;

@Slf4j
@RestController //定义为rest类型的控制器
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/user/register") //定义post请求
    public CommonResVo userRegister(String userName,String password,String email,String nickName) throws Exception {

        log.info("register, userName:{}, psd:{}, email:{}, nickName:{}",userName,password,email,nickName);
        if(!StringUtils.hasText(userName)||!StringUtils.hasText(password)){
            return CommonResVo.fail("PARAM_INVALID","Username or password could not be empty!");
        }
        //验证用户名是否已存在
        if (!userService.checkUserByName(userName)) {
            return CommonResVo.fail("DATA_EXIST","Username is already exist!");
        }
        //用户密码存表时，要做加密处理，可以直接使用spring提供的DigestUtils工具类生成32位MD5字符串
        String passwordMd5 = DigestUtils.md5DigestAsHex(password.getBytes());
        LocalDateTime now = LocalDateTime.now();
        UserInfoEntity userInfo = new UserInfoEntity(userName,passwordMd5,email,nickName);
        userInfo.setStatus(0);
        userInfo.setCreateTime(now);
        userInfo.setUpdateTime(now);
        userService.addUserInfo(userInfo);
        return CommonResVo.success();
    }

    @PostMapping("/user/login") //定义post请求
    public CommonResVo userLogin(String userName, String password, HttpServletRequest request, HttpServletResponse response) throws Exception {
        //验证用户名是否已存在
        log.info("login, userName:{},psd:{}",userName,password);
        if (!StringUtils.hasText(userName) || !StringUtils.hasText(password)) {
            log.warn("username or password could not be empty!");
            return CommonResVo.fail("PARAM_INVALID","Username or password could not be empty!");
        }
        UserInfoEntity userInfoEntity = userService.findUserByName(userName);
        if(ObjectUtils.isEmpty(userInfoEntity)){
            log.warn("Username is not exit!");
            return CommonResVo.fail("DATA_NOT_EXIST","Username is not exit!");
        }
        //用户密码存表时，要做加密处理，可以直接使用spring提供的DigestUtils工具类生成32位MD5字符串
        String passwordMd5 = DigestUtils.md5DigestAsHex(password.getBytes());
        if(!passwordMd5.equals(userInfoEntity.getPassword())){
            log.warn("password is invalid!");
            return CommonResVo.fail("PARAM_INVALID","password is invalid!");
        }

        if(1==userInfoEntity.getStatus()){
            request.getSession().setAttribute("userInfo",userInfoEntity);
            return CommonResVo.success();
        }else {
            return CommonResVo.fail("LIMITED_PERMISSIONS","Permissions are limited, please contact the administrator!");
        }
    }

    @GetMapping("login")
    public ModelAndView login() {

        return new ModelAndView("login", "login", null);
    }

    @GetMapping("register")
    public ModelAndView register() {

        return new ModelAndView("register", "register", null);
    }
}
