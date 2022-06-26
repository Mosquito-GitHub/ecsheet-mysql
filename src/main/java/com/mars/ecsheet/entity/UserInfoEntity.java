package com.mars.ecsheet.entity;


import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "user_info")
public class UserInfoEntity implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",unique = true)
    private Long id;
    @Column(name = "user_name")
    private String userName;
    @Column(name = "password")
    private String password;
    @Column(name = "email")
    private String email;
    @Column(name = "nick_name")
    private String nickName;
    @Column(name = "status")
    private Integer status;
    @Column(name = "create_time")
    private LocalDateTime createTime;
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    public UserInfoEntity() {
    }

    public UserInfoEntity(String userName, String password, String email, String nickName) {
        this.userName = userName;
        this.password = password;
        this.email = email;
        this.nickName = nickName;
    }
}
