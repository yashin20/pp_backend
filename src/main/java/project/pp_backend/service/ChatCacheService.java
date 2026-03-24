package project.pp_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * [Redis] 채팅방(Room) 참가 목록 관리 로직
 */
@Service
@RequiredArgsConstructor
public class ChatCacheService {

    private final RedisTemplate<String, String> redisTemplate;

    //Redis Key 생성 규칙 (Room Entity Id)
    private String getRoomKey(Long roomId) {
        return "room:" + roomId + ":members";
    }

    //1. 채팅방에 참여자 추가 (SADD)
    public void addRoomMember(Long roomId, String username) {
        redisTemplate.opsForSet().add(getRoomKey(roomId), username);
    }

    //2. 채팅방에서 참여자 제거 (SREM)
    public void removeRoomMember(Long roomId, String username) {
        redisTemplate.opsForSet().remove(getRoomKey(roomId), username);
    }

    //3. 해당 방의 모든 참여자 조회 (SMEMBERS) -> DB 쿼리 대체 지정
    public Set<String> getRoomParticipants(Long roomId) {
        return redisTemplate.opsForSet().members(getRoomKey(roomId));
    }

    //4. 채팅방 참가 정보 삭제 (채팅방 삭제시 데이터 낭비 방지를 위해 구성)
    public void removeRoom(Long roomId) {
        redisTemplate.delete(getRoomKey(roomId));
    }

    //5. 채팅방에 여러 참가자 한 번에 추가 (SADD key val1, val2, ...)
    public void addRoomMembers(Long roomId, List<String> usernames) {
        if (usernames == null || usernames.isEmpty()) return;

        // Redis의 SADD는 가변 인자를 받으므로 배열로 변환하여 전달
        redisTemplate.opsForSet().add(getRoomKey(roomId), usernames.toArray(new String[0]));
    }
}
