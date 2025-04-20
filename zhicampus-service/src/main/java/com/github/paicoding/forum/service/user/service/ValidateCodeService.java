package com.github.paicoding.forum.service.user.service;


import com.github.paicoding.forum.api.model.vo.user.vo.ValidateCodeVo;

/**
 * @author xqr
 * @create 2023-10-24-23:06
 */
public interface ValidateCodeService {

    // 获取验证码图片
    public abstract ValidateCodeVo generateValidateCode();

}
