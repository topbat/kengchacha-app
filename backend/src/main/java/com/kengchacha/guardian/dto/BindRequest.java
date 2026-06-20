package com.kengchacha.guardian.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/** 绑定家人守护关系。 */
public record BindRequest(String ownerName,
                          @NotBlank(message = "请填写家人称呼") String memberName,
                          String relation,
                          String phone,
                          List<String> topics,
                          Boolean voiceFirst) {
}
