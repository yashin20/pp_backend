package project.pp_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.profile-dir}")
    private String profileDir;

    @Value("${file.chat-dir}")
    private String chatDir;

    @Value("${file.url-prefix.profile}")
    private String profileUrlPrefix; // "/uploads/profiles/"

    @Value("${file.url-prefix.chat}")
    private String chatUrlPrefix;    // "/uploads/chats/"

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String profilePath = profileDir.endsWith("/") ? profileDir : profileDir + "/";
        String chatPath = chatDir.endsWith("/") ? chatDir : chatDir + "/";

        // 1. 프로필 이미지 핸들러
        // "/uploads/profiles/**" 와 같은 의미가 됩니다.
        registry.addResourceHandler(profileUrlPrefix + "**")
                .addResourceLocations("file:///" + profilePath);

        // 2. 채팅 이미지 핸들러
        // "/uploads/chats/**" 와 같은 의미가 됩니다.
        registry.addResourceHandler(chatUrlPrefix + "**")
                .addResourceLocations("file:///" + chatPath);
    }
}
