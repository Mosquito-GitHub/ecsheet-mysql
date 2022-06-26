package com.mars.ecsheet.service;

import com.mars.ecsheet.entity.UserInfoEntity;
import com.mars.ecsheet.repository.UserInfoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Slf4j
@Service
public class UserService {

    @Autowired
    private UserInfoRepository userInfoRepository;

    public boolean checkUserByName(String userName){
        UserInfoEntity userInfoEntity = userInfoRepository.findOneByUserName(userName);
        return ObjectUtils.isEmpty(userInfoEntity);
    }
    public UserInfoEntity findUserByName(String userName){
        return userInfoRepository.findOneByUserName(userName);
    }

    public void addUserInfo(UserInfoEntity userInfoEntity){
        userInfoRepository.save(userInfoEntity);
    }
}
