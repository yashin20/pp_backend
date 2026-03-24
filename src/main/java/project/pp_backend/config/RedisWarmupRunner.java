package project.pp_backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import project.pp_backend.repository.RoomMemberRepository;
import project.pp_backend.service.ChatCacheService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 서버 시작 시, Redis 에 채팅방 참가 현황을 로드 한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisWarmupRunner implements ApplicationRunner {

    private final RoomMemberRepository roomMemberRepository;
    private final ChatCacheService chatCacheService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("🚀 [Redis Warm-up] 채팅방 참여자 캐싱 시작...");

        try {
            // 1. 모든 방의 (방ID, 유저명) 리스트를 한 번에 조회
            // 이 작업은 서버 시작 시 딱 한 번만 실행되므로 DB 부하가 적습니다.
            List<Object[]> allParticipants = roomMemberRepository.findAllParticipantUsernames();

            // 2. 방 ID별로 유저명 리스트 그룹화 (Java Stream 활용)
            Map<Long, List<String>> roomMap = allParticipants.stream()
                    .collect(Collectors.groupingBy(
                            obj -> (Long) obj[0],
                            Collectors.mapping(obj -> (String) obj[1], Collectors.toList())
                    ));

            // 3. 각 방별로 Redis Set에 한 번에 밀어넣기 (Batch SADD)
            roomMap.forEach((roomId, usernames) -> {
                chatCacheService.addRoomMembers(roomId, usernames);
            });

            log.info("✅ [Redis Warm-up] 완료! 총 {}개 방의 데이터가 캐싱되었습니다.", roomMap.size());
        } catch (Exception e) {
            log.error("❌ [Redis Warm-up] 중 오류 발생: ", e);
        }
    }
}
