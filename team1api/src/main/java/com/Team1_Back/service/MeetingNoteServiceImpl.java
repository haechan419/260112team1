package com.Team1_Back.service;

import com.Team1_Back.ai.service.AiService;
import com.Team1_Back.domain.MeetingNote;
import com.Team1_Back.domain.Todo;
import com.Team1_Back.domain.User;
import com.Team1_Back.domain.enums.TodoPriority;
import com.Team1_Back.domain.enums.TodoStatus;
import com.Team1_Back.dto.MeetingNoteDTO;
import com.Team1_Back.dto.TodoDTO;
import com.Team1_Back.repository.MeetingNoteRepository;
import com.Team1_Back.repository.TodoRepository;
import com.Team1_Back.repository.UserRepository;
import com.Team1_Back.util.CustomFileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class MeetingNoteServiceImpl implements MeetingNoteService {

    private final MeetingNoteRepository meetingNoteRepository;
    private final UserRepository userRepository;
    private final TodoRepository todoRepository;
    private final CustomFileUtil customFileUtil;
    private final ModelMapper modelMapper;
    private final AiService aiService;

    @Override
    public MeetingNoteDTO upload(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 파일 유효성 검증
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("파일이 비어 있습니다.");
        }

        // 파일 타입 검증 (TXT만 허용)
        String contentType = file.getContentType();
        String originalFileName = file.getOriginalFilename();
        boolean isTxtFile = (contentType != null && contentType.equals("text/plain")) ||
                (originalFileName != null && originalFileName.toLowerCase().endsWith(".txt"));

        if (!isTxtFile) {
            throw new RuntimeException("TXT 파일만 업로드할 수 있습니다.");
        }

        if (file.getSize() > 30 * 1024 * 1024) { // 30MB
            throw new RuntimeException("파일 크기가 너무 큽니다. 30MB 이하만 업로드 가능합니다.");
        }

        // 파일 저장
        String filePath = customFileUtil.saveFile(file, "meeting_notes");
        Path path = Paths.get(filePath);
        String fileName = path.getFileName().toString();

        // MeetingNote 저장
        MeetingNote meetingNote = MeetingNote.builder()
                .user(user)
                .originalFileName(file.getOriginalFilename())
                .fileName(fileName)
                .filePath(filePath)
                .fileType(contentType)
                .fileSize(file.getSize())
                .analyzed(false)
                .uploadDate(LocalDateTime.now())
                .build();

        MeetingNote saved = meetingNoteRepository.save(meetingNote);
        log.info("회의록 업로드 완료: ID={}, 파일명={}", saved.getId(), saved.getOriginalFileName());

        return entityToDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MeetingNoteDTO get(Long id, Long userId) {
        MeetingNote meetingNote = meetingNoteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("회의록을 찾을 수 없습니다."));

        // 권한 확인: 본인의 회의록인지 확인
        if (!meetingNote.getUser().getId().equals(userId)) {
            throw new RuntimeException("권한이 없습니다.");
        }

        return entityToDTO(meetingNote);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource getFile(Long id, Long userId) {
        MeetingNote meetingNote = meetingNoteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("회의록을 찾을 수 없습니다."));

        // 권한 확인
        if (!meetingNote.getUser().getId().equals(userId)) {
            throw new RuntimeException("권한이 없습니다.");
        }

        Path filePath = Paths.get(meetingNote.getFilePath());
        return customFileUtil.getFileAsResource(filePath);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeetingNoteDTO> getList(Long userId) {
        List<MeetingNote> meetingNotes = meetingNoteRepository.findByUserIdOrderByUploadDateDesc(userId);
        return meetingNotes.stream()
                .map(this::entityToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeetingNoteDTO> getUnanalyzedList(Long userId) {
        List<MeetingNote> meetingNotes = meetingNoteRepository.findUnanalyzedByUserId(userId);
        return meetingNotes.stream()
                .map(this::entityToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void remove(Long id, Long userId) {
        MeetingNote meetingNote = meetingNoteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("회의록을 찾을 수 없습니다."));

        // 권한 확인
        if (!meetingNote.getUser().getId().equals(userId)) {
            throw new RuntimeException("권한이 없습니다.");
        }

        // 파일 삭제
        Path filePath = Paths.get(meetingNote.getFilePath());
        customFileUtil.deleteFile(filePath);

        // MeetingNote 삭제
        meetingNoteRepository.delete(meetingNote);
        log.info("회의록 삭제 완료: ID={}", id);
    }

    @Override
    public int analyzeAndCreateTodos(Long id, Long userId) {
        MeetingNote meetingNote = meetingNoteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("회의록을 찾을 수 없습니다."));

        // 권한 확인
        if (!meetingNote.getUser().getId().equals(userId)) {
            throw new RuntimeException("권한이 없습니다.");
        }

        // 이미 분석된 경우
        if (meetingNote.isAnalyzed()) {
            log.info("이미 분석된 회의록입니다: ID={}", id);
            return 0;
        }

        try {
            // 파일 내용 읽기
            String fileContent = readFileContent(meetingNote);
            if (fileContent == null || fileContent.isBlank()) {
                throw new RuntimeException("파일 내용을 읽을 수 없습니다.");
            }

            // 회의록에서 날짜 추출 (기본 마감일로 사용)
            java.time.LocalDate defaultDueDate = extractMeetingDateFromContent(fileContent);
            if (defaultDueDate == null) {
                // 날짜를 추출하지 못한 경우 회의록 업로드 날짜 사용
                defaultDueDate = meetingNote.getUploadDate().toLocalDate();
            }
            log.info("[회의록 분석] 기본 마감일: {}", defaultDueDate);

            // AI 분석
            List<TodoDTO> todos = aiService.analyzeMeetingNote(fileContent);

            // Todo 생성
            User user = meetingNote.getUser();
            int createdCount = 0;
            for (TodoDTO todoDTO : todos) {
                // AI가 날짜를 제공하지 않은 경우 회의록 날짜를 기본값으로 사용
                java.time.LocalDate dueDate = todoDTO.getDueDate() != null
                        ? todoDTO.getDueDate()
                        : defaultDueDate;

                log.info("[회의록 분석] Todo 생성: 제목={}, 마감일={}", todoDTO.getTitle(), dueDate);

                Todo todo = Todo.builder()
                        .user(user)
                        .title(todoDTO.getTitle())
                        .content(todoDTO.getContent())
                        .dueDate(dueDate)
                        .status(TodoStatus.valueOf(todoDTO.getStatus() != null ? todoDTO.getStatus() : "TODO"))
                        .priority(
                                TodoPriority.valueOf(todoDTO.getPriority() != null ? todoDTO.getPriority() : "MEDIUM"))
                        .meetingNote(meetingNote)
                        .build();

                todoRepository.save(todo);
                createdCount++;
            }

            // 분석 완료 표시
            meetingNote.setAnalyzed(true);
            meetingNoteRepository.save(meetingNote);

            log.info("회의록 분석 완료: ID={}, 생성된 Todo 개수={}", id, createdCount);
            return createdCount;

        } catch (Exception e) {
            log.error("회의록 분석 실패: ID={}, 오류={}", id, e.getMessage(), e);
            throw new RuntimeException("회의록 분석에 실패했습니다: " + e.getMessage(), e);
        }
    }

    // 회의록 내용에서 날짜 추출
    private java.time.LocalDate extractMeetingDateFromContent(String fileContent) {
        if (fileContent == null || fileContent.isBlank()) {
            return null;
        }

        // 다양한 날짜 형식 패턴 매칭
        // "2026년 01월 06일", "2026-01-06", "2026/01/06" 등
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(\\d{4})[년\\s-/.]+(\\d{1,2})[월\\s-/.]+(\\d{1,2})[일]?");
        java.util.regex.Matcher matcher = pattern.matcher(fileContent);

        if (matcher.find()) {
            try {
                int year = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int day = Integer.parseInt(matcher.group(3));

                // 유효한 날짜인지 확인
                java.time.LocalDate date = java.time.LocalDate.of(year, month, day);
                log.info("[회의록 분석] 날짜 추출 성공: {}", date);
                return date;
            } catch (Exception e) {
                log.warn("[회의록 분석] 날짜 추출 실패: {}", e.getMessage());
            }
        }

        return null;
    }

    // 파일 내용을 읽어서 텍스트로 반환 (현재는 TXT 파일만 지원, PDF/DOCX는 추후 확장)
    private String readFileContent(MeetingNote meetingNote) {
        try {
            Path filePath = Paths.get(meetingNote.getFilePath());

            // 파일이 존재하는지 확인
            if (!Files.exists(filePath)) {
                throw new RuntimeException("파일을 찾을 수 없습니다: " + filePath);
            }

            String fileType = meetingNote.getFileType();
            String fileName = meetingNote.getOriginalFileName();

            // TXT 파일인 경우 (contentType이 "text/plain"으로 시작하거나, 확장자가 .txt인 경우)
            boolean isTxtFile = (fileType != null && fileType.startsWith("text/plain")) ||
                    (fileName != null && fileName.toLowerCase().endsWith(".txt"));

            if (isTxtFile) {
                return Files.readString(filePath);
            }

            // PDF, DOCX는 추후 확장
            // TODO: Apache PDFBox나 Apache POI를 사용하여 PDF/DOCX 텍스트 추출
            throw new RuntimeException("현재는 TXT 파일만 지원합니다. PDF, DOCX는 추후 지원 예정입니다.");

        } catch (RuntimeException e) {
            // RuntimeException은 그대로 재던지기
            throw e;
        } catch (Exception e) {
            log.error("파일 읽기 실패: {}", e.getMessage(), e);
            throw new RuntimeException("파일을 읽을 수 없습니다: " + e.getMessage(), e);
        }
    }

    // MeetingNote 엔티티를 MeetingNoteDTO로 변환
    private MeetingNoteDTO entityToDTO(MeetingNote entity) {
        MeetingNoteDTO dto = modelMapper.map(entity, MeetingNoteDTO.class);

        if (entity.getUser() != null) {
            dto.setUserId(entity.getUser().getId());
            dto.setUserName(entity.getUser().getName());
        }

        return dto;
    }
}
