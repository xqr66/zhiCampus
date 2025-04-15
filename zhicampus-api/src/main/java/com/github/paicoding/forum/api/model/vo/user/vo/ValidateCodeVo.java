package com.github.paicoding.forum.api.model.vo.user.vo;

import lombok.Data;

/**
 * @ClassName ValidateCodeVo
 * @Description
 * @Author xqr
 * @Date 2025/4/7 15:56
 */

@Data
public class ValidateCodeVo {

    // "验证码key"
    private String codeKey ;        // 验证码的key

    // "验证码value"
    private String codeValue ;      // 图片验证码对应的字符串数据

}
