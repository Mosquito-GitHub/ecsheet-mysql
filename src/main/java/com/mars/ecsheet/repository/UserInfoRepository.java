package com.mars.ecsheet.repository;

import com.mars.ecsheet.entity.UserInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserInfoRepository extends JpaRepository<UserInfoEntity,Long> {

    UserInfoEntity findOneByUserName(String userName);
}
