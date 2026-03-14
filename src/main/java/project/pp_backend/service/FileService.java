package project.pp_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileService {

    //프로필 사진 업로드 주소
    @Value("${file.profile-dir}")
    private String profileDir;

    //채팅 이미지 파일 업로드 주소
    @Value(("${file.chat-dir}"))
    private String chatDir;

    public String storeProfile(MultipartFile file) {
        return storeFile(file, profileDir);
    }

    public String storeChatImage(MultipartFile file) {
        return storeFile(file, chatDir);
    }

    /**
     * 파일을 업로드 하고 고유한 파일 이름을 반환
     * @param file : 업로드할 파일
     * @param targetPath : 저장 주소
     * @return : 생성된 고유 파일명 (DB 저장용)
     */
    public String storeFile(MultipartFile file, String targetPath) {
        //파일명 추출 ("example.png")
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = ""; //확장자(.png, ..)

        //확장자 추출 (".png")
        int i = originalFileName.lastIndexOf(".");
        if (i > 0) {
            extension = originalFileName.substring(i);
        }

        //저장 고유 파일명 생성하기
        String fileName = UUID.randomUUID().toString() + extension;

        try {
            //업로드 경로
            Path uploadPath = Paths.get(targetPath).toAbsolutePath().normalize();

            //폴더가 없으면 새로 만듦
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            /* 저장 경로 + 고유 파일명 ("C:\project\ uploads\profiles\550e8400-e29b-41d4.jpg") */
            Path targetLocation = uploadPath.resolve(fileName);

            /**물리적 복사 수행
             * file.getInputStream() : 사용자가 보낸 파일 데이터 스트림 열기
             * targetLocation : 최종 목적지
             * StandardCopyOption.REPLACE_EXISTING : 같은 이름의 파일이 있으면 덮어씌움
             */
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            //고유 파일명 반환
            return fileName;
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 중 시스템 오류가 발생했습니다.", e);
        }
    }

//    //파일 저장 및 저장된 고유 파일명 반환
//    public String storeFile(MultipartFile file) {
//        //파일명 추출 ("profile.png")
//        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
//        String extension = ""; //확장자(.png, ..)
//
//        //확장자 추출 (".png")
//        int i = originalFileName.lastIndexOf(".");
//        if (i > 0) {
//            extension = originalFileName.substring(i);
//        }
//
//        //저장 고유 파일명 생성하기
//        String fileName = UUID.randomUUID().toString() + extension;
//
//        try {
//            //업로드 경로
//            Path uploadPath = Paths.get(profileDir).toAbsolutePath().normalize();
//
//            //폴더가 없으면 새로 만듦
//            if (!Files.exists(uploadPath)) {
//                Files.createDirectories(uploadPath);
//            }
//
//            /* 저장 경로 + 고유 파일명 ("C:\project\ uploads\profiles\550e8400-e29b-41d4.jpg") */
//            Path targetLocation = uploadPath.resolve(fileName);
//
//            /**물리적 복사 수행
//             * file.getInputStream() : 사용자가 보낸 파일 데이터 스트림 열기
//             * targetLocation : 최종 목적지
//             * StandardCopyOption.REPLACE_EXISTING : 같은 이름의 파일이 있으면 덮어씌움
//             */
//            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
//
//            //고유 파일명 반환
//            return fileName;
//        } catch (IOException e) {
//            throw new RuntimeException("파일 저장 중 시스템 오류가 발생했습니다.", e);
//        }
//    }

    /**
     * 프로필 사진 삭제
     */
    public void deleteProfile(String fileName) {
        deleteFile(fileName, profileDir);
    }

    /**
     * 채팅 이미지 삭제
     */
    public void deleteChatImage(String fileName) {
        deleteFile(fileName, chatDir);
    }

    /**
     * 공통 파일 삭제 로직
     * @param fileName 삭제할 고유 파일명
     * @param targetPath 파일이 저장된 디렉터리 경로 (profileDir 또는 chatDir)
     */
    private void deleteFile(String fileName, String targetPath) {
        // 1. 파일명이 비어있으면 로직을 수행하지 않음
        if (!StringUtils.hasText(fileName)) return;

        try {
            // 2. 해당 폴더의 절대 경로를 찾아 파일 위치 확정
            Path filePath = Paths.get(targetPath).toAbsolutePath().normalize().resolve(fileName);

            // 3. 파일이 존재할 경우에만 삭제 시도
            boolean deleted = Files.deleteIfExists(filePath);

            if (deleted) {
                System.out.println("✅ 파일 삭제 성공: " + filePath);
            } else {
                System.out.println("⚠️ 삭제 실패: 파일이 존재하지 않습니다. (" + filePath + ")");
            }
        } catch (IOException e) {
            // 삭제 중 에러가 발생해도 전체 비즈니스 로직이 멈추지 않도록 예외 처리만 수행
            System.err.println("❌ 파일 삭제 중 오류 발생: " + fileName + " | 에러: " + e.getMessage());
        }
    }

}
