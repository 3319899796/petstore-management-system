package com.ningcong.petstore.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class NoticeDTO {

    /** 修改时必填 */
    private Integer noticeId;

    @NotBlank(message = "请填写公告标题")
    @Size(max = 100, message = "标题不能超过 100 个字")
    private String title;

    @Size(max = 5000, message = "正文不能超过 5000 个字")
    private String content;
}
