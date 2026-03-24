package project.pp_backend.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.security.jackson2.SecurityJackson2Modules;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    /**
     * Lettuce를 사용하여 RedisConnectionFactory를 생성
     * 이 팩토리는 Spring Data Redis가 Redis 서버와 연결을 관리하는데 사용
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(redisHost, redisPort);
    }

    /**
     * RedisTemplate을 설정하고 빈으로 등록
     * Refresh Token을 Key-Value 형태로 저장할 때 사용
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate() {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }

//    @Bean
//    public RedisTemplate<String, String> redisTemplate() {
//        //** redisTemplate -> Refresh Token 관리
//        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
//
//        //연결 팩토리 설정
//        redisTemplate.setConnectionFactory(redisConnectionFactory());
//
//        // Key 직렬화 방식 설정: Key는 문자열로 저장합니다.
//        // Refresh Token의 Key는 사용자 Username(String)이므로 StringRedisSerializer가 적합합니다.
//        redisTemplate.setKeySerializer(new StringRedisSerializer());
//        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
//
//        // Value 직렬화 방식 설정: Value도 문자열(Refresh Token 값)로 저장합니다.
//        redisTemplate.setValueSerializer(new StringRedisSerializer());
//        redisTemplate.setHashValueSerializer(new StringRedisSerializer());
//
//        // 트랜잭션 옵션을 비활성화합니다 (기본 설정)
//        redisTemplate.setEnableTransactionSupport(false);
//
//        return redisTemplate;
//    }

    //유저 정보 캐싱용
    @Bean
    public RedisTemplate<String, Object> userRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(new StringRedisSerializer());

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModules(SecurityJackson2Modules.getModules(getClass().getClassLoader()));
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        return template;
    }
}
