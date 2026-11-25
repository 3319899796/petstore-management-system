package com.ningcong.petstore.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {

    private Integer userId;
    private String username;
    private Integer roleId;
    private String roleName;

    /** 脱敏后的手机号 */
    private String phone;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
