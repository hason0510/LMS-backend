package com.example.backend.service.impl;

import com.example.backend.constant.OtpType;
import com.example.backend.entity.Otp;
import com.example.backend.entity.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.repository.OtpRepository;
import com.example.backend.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {
    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRATION_MINUTES = 5;
    private static final int OTP_RESEND_INTERVAL_SECONDS = 30;
    private final JavaMailSender mailSender;
    private final OtpRepository otpRepository;

    @Value("${spring.mail.username}")
    private String fromGmail;
    @Value("${spring.mail.password}")
    private String password;

    @Override
    public void sendOtpEmail(String toGmail, String otpCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromGmail);
        message.setTo(toGmail);
        message.setSubject("Mã OTP của bạn");
        message.setText("Mã OTP của bạn là: " + otpCode + "\nMã sẽ hết hạn trong 5 phút.");
        mailSender.send(message);
    }


    public String generateOtp() {
        Random random = new Random();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    @Override
    public Otp createOtp(User user, OtpType type) {
        cleanUpExpiredOtps(user.getId());
        String code = generateOtp();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(OTP_EXPIRATION_MINUTES);
        Otp otp = new Otp();
        otp.setCode(code);
        otp.setCreatedAt(now);
        otp.setExpiresAt(expiresAt);
        otp.setVerified(false);
        otp.setType(type);
        otp.setUser(user);
        return otpRepository.save(otp);
    }

    @Override
    public boolean validateOtp(User user, String code, OtpType type) {
        LocalDateTime now = LocalDateTime.now();
        Optional<Otp> otpOptional = otpRepository.findByCodeAndUser_IdAndTypeAndVerifiedIsFalseAndExpiresAtAfter(
                code, user.getId(), type, now);
        if (otpOptional.isPresent()) {
            Otp otp = otpOptional.get();
            otp.setVerified(true);
            otpRepository.save(otp);
            return true;
        }
        return false;
    }

    @Override
    public void resendOtp(User user, OtpType type) {
        Optional<Otp> latestOtpOpt =
                otpRepository.findTopByUser_IdAndTypeAndVerifiedFalseOrderByCreatedAtDesc(
                        user.getId(), type
                );
        if (latestOtpOpt.isPresent()) {
            Otp latestOtp = latestOtpOpt.get();
            LocalDateTime allowedTime =
                    latestOtp.getCreatedAt().plusSeconds(OTP_RESEND_INTERVAL_SECONDS);
            if (LocalDateTime.now().isBefore(allowedTime)) {
                long secondsLeft =
                        Duration.between(LocalDateTime.now(), allowedTime).getSeconds();
                throw new BusinessException(
                        "Vui lòng chờ " + secondsLeft + " giây để gửi lại OTP"
                );
            }
        }
        Otp otp = createOtp(user, type);
        sendOtpEmail(user.getGmail(), otp.getCode());
    }


    private void cleanUpExpiredOtps(Integer userId) {
        LocalDateTime now = LocalDateTime.now();
        List<Otp> expiredOtps = otpRepository.findByUser_IdAndVerifiedIsFalseAndExpiresAtBefore(userId, now);
        otpRepository.deleteAll(expiredOtps);
    }
}