package com.demand.module.user.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String realName;
    private String email;
    private String phone;
    private String avatar;
    private Integer status;

    /**
     * 用户拥有的角色标识列表
     */
    private List<String> roles;
}
