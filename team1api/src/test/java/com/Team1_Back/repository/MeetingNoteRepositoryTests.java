package com.Team1_Back.repository;

import com.Team1_Back.domain.MeetingNote;
import com.Team1_Back.domain.User;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Log4j2
public class MeetingNoteRepositoryTests {

    @Autowired
    private MeetingNoteRepository meetingNoteRepository;

    @Autowired
    private UserRepository userRepository;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Create - 생성
    @Test
    @Transactional
    public void testCreate() {
        // given
        User testUser = userRepository.findByEmployeeNo("20250001")
                .orElseThrow(() -> new RuntimeException("테스트 사용자를 찾을 수 없습니다."));

        MeetingNote meetingNote = MeetingNote.builder()
                .user(testUser)
                .originalFileName("회의록_20250106.pdf")
                .fileName("meeting_note_20250106_123456.pdf")
                .filePath("/upload/meeting_notes/2025/01/meeting_note_20250106_123456.pdf")
                .fileType("application/pdf")
                .fileSize(1024000L)
                .analyzed(false)
                .uploadDate(LocalDateTime.now())
                .build();

        // when
        MeetingNote saved = meetingNoteRepository.save(meetingNote);

        // then
        assertNotNull(saved.getId());
        assertEquals("회의록_20250106.pdf", saved.getOriginalFileName());
        assertEquals("meeting_note_20250106_123456.pdf", saved.getFileName());
        assertFalse(saved.isAnalyzed());
        log.info("생성된 MeetingNote ID: {}", saved.getId());
    }

    // Read - 조회
    @Test
    @Transactional
    public void testRead() {
        // given
        User testUser = userRepository.findByEmployeeNo("20250001")
                .orElseThrow(() -> new RuntimeException("테스트 사용자를 찾을 수 없습니다."));

        MeetingNote meetingNote = MeetingNote.builder()
                .user(testUser)
                .originalFileName("조회_테스트_회의록.pdf")
                .fileName("test_meeting_note.pdf")
                .filePath("/upload/meeting_notes/test_meeting_note.pdf")
                .fileType("application/pdf")
                .fileSize(512000L)
                .analyzed(false)
                .uploadDate(LocalDateTime.now())
                .build();
        MeetingNote saved = meetingNoteRepository.save(meetingNote);

        // when
        Optional<MeetingNote> found = meetingNoteRepository.findById(saved.getId());

        // then
        assertTrue(found.isPresent());
        assertEquals("조회_테스트_회의록.pdf", found.get().getOriginalFileName());
        log.info("조회된 MeetingNote: {}", found.get().getOriginalFileName());
    }

    // Update - 수정
    @Test
    @Transactional
    public void testUpdate() {
        // given
        User testUser = userRepository.findByEmployeeNo("20250001")
                .orElseThrow(() -> new RuntimeException("테스트 사용자를 찾을 수 없습니다."));

        MeetingNote meetingNote = MeetingNote.builder()
                .user(testUser)
                .originalFileName("수정_전_회의록.pdf")
                .fileName("before_update.pdf")
                .filePath("/upload/before_update.pdf")
                .fileType("application/pdf")
                .fileSize(256000L)
                .analyzed(false)
                .uploadDate(LocalDateTime.now())
                .build();
        MeetingNote saved = meetingNoteRepository.save(meetingNote);

        // when
        saved.setAnalyzed(true);
        MeetingNote updated = meetingNoteRepository.save(saved);

        // then
        assertTrue(updated.isAnalyzed());
        log.info("수정된 MeetingNote: {}, 분석 완료: {}", updated.getOriginalFileName(), updated.isAnalyzed());
    }

    // Delete - 삭제
    @Test
    @Transactional
    public void testDelete() {
        // given
        User testUser = userRepository.findByEmployeeNo("20250001")
                .orElseThrow(() -> new RuntimeException("테스트 사용자를 찾을 수 없습니다."));

        MeetingNote meetingNote = MeetingNote.builder()
                .user(testUser)
                .originalFileName("삭제_테스트_회의록.pdf")
                .fileName("delete_test.pdf")
                .filePath("/upload/delete_test.pdf")
                .fileType("application/pdf")
                .fileSize(128000L)
                .analyzed(false)
                .uploadDate(LocalDateTime.now())
                .build();
        MeetingNote saved = meetingNoteRepository.save(meetingNote);
        Long meetingNoteId = saved.getId();

        // when
        meetingNoteRepository.deleteById(meetingNoteId);

        // then
        Optional<MeetingNote> deleted = meetingNoteRepository.findById(meetingNoteId);
        assertFalse(deleted.isPresent());
        log.info("MeetingNote 삭제 완료: ID {}", meetingNoteId);
    }

    // 사용자별 조회 테스트
    @Test
    @Transactional
    public void testFindByUserIdOrderByUploadDateDesc() {
        // given
        User testUser = userRepository.findByEmployeeNo("20250001")
                .orElseThrow(() -> new RuntimeException("테스트 사용자를 찾을 수 없습니다."));

        // when
        List<MeetingNote> result = meetingNoteRepository.findByUserIdOrderByUploadDateDesc(testUser.getId());

        // then
        assertNotNull(result);
        log.info("사용자 {}의 회의록 개수: {}", testUser.getId(), result.size());
        result.forEach(note -> log.info("회의록: {} - 업로드일: {}",
                note.getOriginalFileName(), note.getUploadDate().format(formatter)));
    }

    // 분석 완료 여부별 조회 테스트
    @Test
    @Transactional
    public void testFindByUserIdAndAnalyzed() {
        // given
        User testUser = userRepository.findByEmployeeNo("20250001")
                .orElseThrow(() -> new RuntimeException("테스트 사용자를 찾을 수 없습니다."));

        // when
        List<MeetingNote> analyzedList = meetingNoteRepository.findByUserIdAndAnalyzed(testUser.getId(), true);
        List<MeetingNote> unanalyzedList = meetingNoteRepository.findByUserIdAndAnalyzed(testUser.getId(), false);

        // then
        assertNotNull(analyzedList);
        assertNotNull(unanalyzedList);
        log.info("분석 완료: {}개, 미분석: {}개", analyzedList.size(), unanalyzedList.size());

        analyzedList.forEach(note -> {
            assertTrue(note.isAnalyzed());
            log.info("분석 완료 회의록: {}", note.getOriginalFileName());
        });

        unanalyzedList.forEach(note -> {
            assertFalse(note.isAnalyzed());
            log.info("미분석 회의록: {}", note.getOriginalFileName());
        });
    }

    // 미분석 회의록 조회 테스트
    @Test
    @Transactional
    public void testFindUnanalyzedByUserId() {
        // given
        User testUser = userRepository.findByEmployeeNo("20250001")
                .orElseThrow(() -> new RuntimeException("테스트 사용자를 찾을 수 없습니다."));

        // when
        List<MeetingNote> result = meetingNoteRepository.findUnanalyzedByUserId(testUser.getId());

        // then
        assertNotNull(result);
        log.info("미분석 회의록 개수: {}", result.size());
        result.forEach(note -> {
            assertFalse(note.isAnalyzed());
            log.info("미분석 회의록: {} - 업로드일: {}",
                    note.getOriginalFileName(), note.getUploadDate().format(formatter));
        });
    }

    // 파일명으로 조회 테스트
    @Test
    @Transactional
    public void testFindByFileName() {
        // given
        User testUser = userRepository.findByEmployeeNo("20250001")
                .orElseThrow(() -> new RuntimeException("테스트 사용자를 찾을 수 없습니다."));

        String fileName = "test_file_name_" + System.currentTimeMillis() + ".pdf";
        MeetingNote meetingNote = MeetingNote.builder()
                .user(testUser)
                .originalFileName("원본_파일명.pdf")
                .fileName(fileName)
                .filePath("/upload/" + fileName)
                .fileType("application/pdf")
                .fileSize(1024000L)
                .analyzed(false)
                .uploadDate(LocalDateTime.now())
                .build();
        meetingNoteRepository.save(meetingNote);

        // when
        Optional<MeetingNote> found = meetingNoteRepository.findByFileName(fileName);

        // then
        assertTrue(found.isPresent());
        assertEquals(fileName, found.get().getFileName());
        log.info("파일명으로 조회된 회의록: {}", found.get().getOriginalFileName());
    }

    // 사용자 ID와 파일명으로 조회 테스트
    @Test
    @Transactional
    public void testFindByUserIdAndFileName() {
        // given
        User testUser = userRepository.findByEmployeeNo("20250001")
                .orElseThrow(() -> new RuntimeException("테스트 사용자를 찾을 수 없습니다."));

        String fileName = "user_specific_file_" + System.currentTimeMillis() + ".pdf";
        MeetingNote meetingNote = MeetingNote.builder()
                .user(testUser)
                .originalFileName("사용자별_파일.pdf")
                .fileName(fileName)
                .filePath("/upload/" + fileName)
                .fileType("application/pdf")
                .fileSize(512000L)
                .analyzed(false)
                .uploadDate(LocalDateTime.now())
                .build();
        meetingNoteRepository.save(meetingNote);

        // when
        Optional<MeetingNote> found = meetingNoteRepository.findByUserIdAndFileName(testUser.getId(), fileName);

        // then
        assertTrue(found.isPresent());
        assertEquals(fileName, found.get().getFileName());
        assertEquals(testUser.getId(), found.get().getUser().getId());
        log.info("사용자 ID와 파일명으로 조회된 회의록: {}", found.get().getOriginalFileName());
    }

    // 분석 완료 여부 확인 메서드 테스트
    @Test
    @Transactional
    public void testIsAnalyzed() {
        // given
        User testUser = userRepository.findByEmployeeNo("20250001")
                .orElseThrow(() -> new RuntimeException("테스트 사용자를 찾을 수 없습니다."));

        MeetingNote analyzedNote = MeetingNote.builder()
                .user(testUser)
                .originalFileName("분석_완료_회의록.pdf")
                .fileName("analyzed_note.pdf")
                .filePath("/upload/analyzed_note.pdf")
                .fileType("application/pdf")
                .fileSize(256000L)
                .analyzed(true)
                .uploadDate(LocalDateTime.now())
                .build();

        MeetingNote unanalyzedNote = MeetingNote.builder()
                .user(testUser)
                .originalFileName("미분석_회의록.pdf")
                .fileName("unanalyzed_note.pdf")
                .filePath("/upload/unanalyzed_note.pdf")
                .fileType("application/pdf")
                .fileSize(128000L)
                .analyzed(false)
                .uploadDate(LocalDateTime.now())
                .build();

        // when
        boolean analyzedResult = analyzedNote.isAnalyzed();
        boolean unanalyzedResult = unanalyzedNote.isAnalyzed();

        // then
        assertTrue(analyzedResult);
        assertFalse(unanalyzedResult);
        log.info("분석 완료 회의록: {}, 미분석 회의록: {}", analyzedResult, unanalyzedResult);
    }

    // 더미 데이터 30개 생성
    @Test
    @Transactional
    public void testCreateDummyData() {
        // given
        User testUser = userRepository.findByEmployeeNo("20250001")
                .orElseThrow(() -> new RuntimeException("테스트 사용자를 찾을 수 없습니다."));

        String[] fileTypes = { "application/pdf",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/plain" };
        String[] prefixes = { "회의록", "미팅_노트", "회의_기록", "프로젝트_회의", "팀_미팅" };
        java.util.Random random = new java.util.Random();
        java.util.List<MeetingNote> savedNotes = new java.util.ArrayList<>();

        // when - 30개의 더미 데이터 생성
        for (int i = 0; i < 30; i++) {
            String prefix = prefixes[random.nextInt(prefixes.length)];
            String fileType = fileTypes[random.nextInt(fileTypes.length)];
            String extension = fileType.contains("pdf") ? "pdf" : (fileType.contains("word") ? "docx" : "txt");
            String originalFileName = prefix + "_" + (i + 1) + "." + extension;
            String fileName = "meeting_note_" + System.currentTimeMillis() + "_" + i + "." + extension;
            String filePath = "/upload/meeting_notes/2025/01/" + fileName;
            long fileSize = (random.nextInt(5) + 1) * 256000L; // 256KB ~ 1.28MB
            boolean analyzed = random.nextBoolean();
            LocalDateTime uploadDate = LocalDateTime.now().minusDays(random.nextInt(60));

            MeetingNote meetingNote = MeetingNote.builder()
                    .user(testUser)
                    .originalFileName(originalFileName)
                    .fileName(fileName)
                    .filePath(filePath)
                    .fileType(fileType)
                    .fileSize(fileSize)
                    .analyzed(analyzed)
                    .uploadDate(uploadDate)
                    .build();

            MeetingNote saved = meetingNoteRepository.save(meetingNote);
            savedNotes.add(saved);
        }

        // then
        assertEquals(30, savedNotes.size());
        log.info("더미 데이터 30개 생성 완료");

        // 분석 완료 여부별 통계 출력
        long analyzedCount = savedNotes.stream().filter(MeetingNote::isAnalyzed).count();
        long unanalyzedCount = savedNotes.stream().filter(note -> !note.isAnalyzed()).count();

        log.info("분석 완료: {}개, 미분석: {}개", analyzedCount, unanalyzedCount);

        // 파일 타입별 통계
        long pdfCount = savedNotes.stream().filter(note -> note.getFileType().contains("pdf")).count();
        long docxCount = savedNotes.stream().filter(note -> note.getFileType().contains("word")).count();
        long txtCount = savedNotes.stream().filter(note -> note.getFileType().contains("text")).count();

        log.info("파일 타입별 통계 - PDF: {}개, DOCX: {}개, TXT: {}개", pdfCount, docxCount, txtCount);
    }
}
