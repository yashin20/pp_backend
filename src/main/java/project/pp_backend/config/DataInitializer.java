package project.pp_backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import project.pp_backend.dto.FriendShipDto;
import project.pp_backend.dto.MemberDto;
import project.pp_backend.dto.MessageDto;
import project.pp_backend.dto.RoomDto;
import project.pp_backend.entity.FriendShipStatus;
import project.pp_backend.entity.MemberRole;
import project.pp_backend.entity.MessageType;
import project.pp_backend.service.*;

import java.util.List;

@Profile("dev")
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final MemberService memberService;
    private final MessageService messageService;
    private final FriendShipService friendShipService;
    private final RoomService roomService;
    private final TestMethodService testMethodService;

    @Override
    public void run(String... args) throws Exception {
        initializeTestData();
    }

    private void initializeTestData() {
        System.out.println("--- [Dev Profile] Initializing Test Member Data ---");

        createMemberIfNotExist("member1", "1q2w3e4r", "maru", "maru@example.com");
        createMemberIfNotExist("member2", "1q2w3e4r", "nickname2", "nickname2@example.com");
        createMemberIfNotExist("member3", "1q2w3e4r", "nickname3", "nickname3@example.com");

        createFriendShipIfNotExist("member1", "member2");
        createFriendShipIfNotExist("member1", "member3");

        Long room1Id = createRoomIfNotExist("room1 - 1", "member1", List.of("member2", "member3"));
//        createRoomIfNotExist("room2 - 1", "member1", List.of("member2"));
//        createRoomIfNotExist("room3 - 3", "member3", List.of("member1"));
//
        createMessageIfNotExist("member1", room1Id, "안녕하세요.", null);
        createMessageIfNotExist("member1", room1Id, "안녕하세요.", null);
        createMessageIfNotExist("member2", room1Id, "안녕하세요2.", null);
        createMessageIfNotExist("member3", room1Id, "안녕하세요3.", null);
        createMessageIfNotExist("member1", room1Id, "안녕하세요1.",null);

        for (int i = 0; i < 20; i++) {
            createMessageIfNotExist("member1", room1Id, String.valueOf(i), null);
        }

        System.out.println("--- Test Member Data Initialization Complete ---");
    }

    private void createMemberIfNotExist(String username, String password, String nickname, String email) {
        try {
            MemberDto.CreateRequest request = MemberDto.CreateRequest.builder()
                    .username(username)
                    .password(password)
                    .nickname(nickname)
                    .email(email)
                    .role(MemberRole.USER)
                    .build();

            memberService.createMember(request);
            System.out.println("✅ Member created: " + username);

        } catch (RuntimeException e) {
            // 사용자 ID가 이미 존재할 경우 (MemberService에서 처리했다고 가정)
            System.out.println("ℹ️ Member already exists: " + username);
        } catch (Exception e) {
            System.err.println("❌ Error creating member " + username + ": " + e.getMessage());
        }
    }

    private void createFriendShipIfNotExist(String ownerUsername, String friendUsername) {
        try {
            testMethodService.testCreateFriendShip(ownerUsername, friendUsername, FriendShipStatus.ACCEPTED);
            System.out.println("✅ FriendShip created: " + ownerUsername + " - " + friendUsername);

        } catch (RuntimeException e) {
            System.out.println("ℹ️ FriendShip already exists: " + ownerUsername + " - " + friendUsername);
        } catch (Exception e) {
            System.err.println("❌ Error creating FriendShip: " + ownerUsername + " - " + friendUsername);
        }
    }

    private Long createRoomIfNotExist(String roomName, String creator, List<String> members) {
        try {
            RoomDto.CreateRequest request = RoomDto.CreateRequest.builder()
                    .name(roomName)
                    .usernames(members)
                    .build();

            RoomDto.Response response = roomService.createRoom(creator, request);
            System.out.println("✅ Room created: " + roomName);
            return response.getId();

        } catch (RuntimeException e) {
            System.out.println("ℹ️ Room already exists: " + roomName);

            return null;
        } catch (Exception e) {
            System.err.println("❌ Error creating Room: " + roomName + ": " + e.getMessage());

            return null;
        }
    }


    private void createMessageIfNotExist(String username, Long roomId, String content, String recipientUsername) {
        try {
            testMethodService.testCreateMessage(username, roomId, content, recipientUsername);
            System.out.println("✅ Message created at roomId: " + roomId);

        } catch (RuntimeException e) {
            System.out.println("ℹ️ Message already exists roomId: " + roomId);
        } catch (Exception e) {
            System.err.println("❌ Error creating Message roomId: " + roomId + ": " + e.getMessage());
        }
    }
}
