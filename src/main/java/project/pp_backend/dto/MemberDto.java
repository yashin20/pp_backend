package project.pp_backend.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;
import project.pp_backend.entity.Member;
import project.pp_backend.entity.MemberRole;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class MemberDto {

    @Data
    public static class Response {
        private Long id;
        private String username;
        private String nickname;
        private String email;
        private String createdAt;
        private String updatedAt;
        private String role;

        public Response(Member member) {
            this.id = member.getId();
            this.username = member.getUsername();
            this.nickname = member.getNickname();
            this.email = member.getEmail();
            this.createdAt = formatTime(member.getCreatedAt());
            this.updatedAt = formatTime(member.getUpdatedAt());
            this.role = member.getMemberRole().toString();
        }

        private String formatTime(LocalDateTime dateTime) {
            if (dateTime == null) {
                return null; // 또는 빈 문자열 ""
            }

            //2022-02-22 11:30
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.KOREA);
            return dateTime.format(formatter);
        }
    }

    @Data
    @Builder
    public static class CreateRequest {

        @NotBlank(message = "아이디는 필수 입력 항목입니다.")
        @Size(min = 6, max = 12, message = "아이디는 6~12자리 입니다.")
        @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "아이디는 영문자와 숫자만 가능합니다.")
        private String username;

        @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
        @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하로 입력해주세요.")
        @Pattern(regexp = "^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?~]+$", message = "비밀번호는 알파벳(대소문자), 숫자, 특수문자 만 유효합니다.")
        private String password;

        @NotBlank(message = "닉네임은 필수 입력 항목입니다.")
        @Size(min = 4, max = 12, message = "닉네임은 4자 이상 12자 이하로 입력해주세요.")
        @Pattern(regexp = "^\\S+$", message = "닉네임은 공백을 포함할 수 없습니다.")
        private String nickname;

        @Email(message = "이메일 형식이 올바르지 않습니다.")
        private String email; //비밀번호 복구'에만' 사용

        @NotNull(message = "회원 권한은 필수 선택 항목입니다.")
        private MemberRole role;

        //Create Request DTO -> Entity
        public Member toEntity() {
            return new Member(
                    this.username,
                    this.password,
                    this.nickname,
                    this.email,
                    this.role
            );
        }
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }


    @Data
    public static class UpdateRequest {
        @Size(min = 4, max = 12, message = "닉네임은 4자 이상 12자 이하로 입력해주세요.")
        @Pattern(regexp = "^\\S+$", message = "닉네임은 공백을 포함할 수 없습니다.")
        private String nickname;

        @Email(message = "이메일 형식이 올바르지 않습니다.")
        private String email;
    }

    @Data
    public static class PasswordRequest {
        @NotBlank(message = "기존 비밀번호는 필수 입력 항목입니다.")
        @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하로 입력해주세요.")
        @Pattern(regexp = "^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?~]+$", message = "비밀번호는 알파벳(대소문자), 숫자, 특수문자 만 유효합니다.")
        private String currentPassword;

        @NotBlank(message = "새 비밀번호는 필수 입력 항목입니다.")
        @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하로 입력해주세요.")
        @Pattern(regexp = "^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?~]+$", message = "비밀번호는 알파벳(대소문자), 숫자, 특수문자 만 유효합니다.")
        private String newPassword;

        @NotBlank(message = "새 비밀번호는 필수 입력 항목입니다.")
        @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하로 입력해주세요.")
        @Pattern(regexp = "^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?~]+$", message = "비밀번호는 알파벳(대소문자), 숫자, 특수문자 만 유효합니다.")
        private String repeatPassword;
    }
}
