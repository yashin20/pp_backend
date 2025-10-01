package project.pp_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class TokenDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String grantType; //토큰 유형
        private String accessToken; //실제 인증에 사용되는 토큰
        private String refreshToken; //AccessToken 만료 시 재발급에 사용되는 토큰
        private Long accessTokenExpiresIn; //Access Token의 만료 시간
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        //만료된 Access Token
        private String accessToken;

        //유효성을 검증할 Refresh Token
        private String refreshToken;
    }
}
