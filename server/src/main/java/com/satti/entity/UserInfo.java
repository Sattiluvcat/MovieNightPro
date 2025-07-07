package com.satti.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.MessageDigest;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserInfo {
    private String nickname;
    private String title;
    // 联系方式
    private String contact;
    // 密码
    private String password;

    // 添加密码加密方法
    public void encryptPassword(PasswordEncoder encoder) {
        // 加盐MD5加密
        if (this.password != null) {
            this.password=hashPassword(this.password);
        }
    }

    // 生成随机盐值
//    private String generateSalt() {
//        SecureRandom random = new SecureRandom();
//        byte[] saltBytes = new byte[16];
//        random.nextBytes(saltBytes);
//        return Base64.getEncoder().encodeToString(saltBytes);
//    }

    // 加盐(bu)哈希处理
    private String hashPassword(String password) {
        try {
            // 创建MD5哈希实例
            MessageDigest md = MessageDigest.getInstance("MD5");

            // 组合密码和盐值
//            String saltedPassword = password + salt;

            // 可以多次迭代增加安全性，此处不迭代了
            byte[] hashedBytes = password.getBytes();
            hashedBytes = md.digest(hashedBytes);

            // 转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashedBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("密码加密失败", e);
        }
    }
}
