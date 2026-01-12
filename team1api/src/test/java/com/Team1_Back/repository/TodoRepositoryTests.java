package com.Team1_Back.repository;

import com.Team1_Back.domain.Todo;
import com.Team1_Back.domain.User;
import com.Team1_Back.domain.enums.TodoPriority;
import com.Team1_Back.domain.enums.TodoStatus;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Log4j2
public class TodoRepositoryTests {

        @Autowired
        private TodoRepository todoRepository;

        @Autowired
        private UserRepository userRepository;

        // Create - 생성
        @Test
        @Transactional
        public void testCreate() {
                // given
                User testUser = userRepository.findByEmployeeNo("20250001")
                                .orElseThrow(() -> new RuntimeException("테스트 사용자를 찾을 수 없습니다."));

                Todo todo = Todo.builder()
                                .user(testUser)
                                .title("테스트 Todo")
                                .content("테스트 설명")
                                .dueDate(LocalDate.now().plusDays(3))
                                .status(TodoStatus.TODO)
                                .priority(TodoPriority.MEDIUM)
                                .build();

                // when
                Todo saved = todoRepository.save(todo);

                // then
                assertNotNull(saved.getId());
                assertEquals("테스트 Todo", saved.getTitle());
                assertEquals(TodoStatus.TODO, saved.getStatus());
                assertEquals(TodoPriority.MEDIUM, saved.getPriority());
                log.info("생성된 Todo ID: {}", saved.getId());
        }

        // Read - 조회
        @Test
        @Transactional
        public void testRead() {
                // given
                User testUser = userRepository.findByEmployeeNo("20250001")
                                .orElseThrow(() -> new RuntimeException("테스트 사용자를 찾을 수 없습니다."));

                Todo todo = Todo.builder()
                                .user(testUser)
                                .title("조회 테스트 Todo")
                                .content("조회 테스트 설명")
                                .dueDate(LocalDate.now().plusDays(5))
                                .status(TodoStatus.TODO)
                                .priority(TodoPriority.HIGH)
                                .build();
                Todo saved = todoRepository.save(todo);

                // when
                Optional<Todo> found = todoRepository.findById(saved.getId());

                // then
                assertTrue(found.isPresent());
                assertEquals("조회 테스트 Todo", found.get().getTitle());
                log.info("조회된 Todo: {}", found.get().getTitle());
        }

        // Update - 수정
        @Test
        @Transactional
        public void testUpdate() {
                // given
                User testUser = userRepository.findByEmployeeNo("20250001")
                                .orElseThrow(() -> new RuntimeException("테스트 사용자를 찾을 수 없습니다."));

                Todo todo = Todo.builder()
                                .user(testUser)
                                .title("수정 전 제목")
                                .content("수정 전 내용")
                                .dueDate(LocalDate.now().plusDays(7))
                                .status(TodoStatus.TODO)
                                .priority(TodoPriority.LOW)
                                .build();
                Todo saved = todoRepository.save(todo);

                // when
                saved.setTitle("수정 후 제목");
                saved.setContent("수정 후 내용");
                saved.setStatus(TodoStatus.IN_PROGRESS);
                saved.setPriority(TodoPriority.HIGH);
                Todo updated = todoRepository.save(saved);

                // then
                assertEquals("수정 후 제목", updated.getTitle());
                assertEquals("수정 후 내용", updated.getContent());
                assertEquals(TodoStatus.IN_PROGRESS, updated.getStatus());
                assertEquals(TodoPriority.HIGH, updated.getPriority());
                log.info("수정된 Todo: {}", updated.getTitle());
        }

        // Delete - 삭제
        @Test
        @Transactional
        public void testDelete() {
                // given
                User testUser = userRepository.findByEmployeeNo("20250001")
                                .orElseThrow(() -> new RuntimeException("테스트 사용자를 찾을 수 없습니다."));

                Todo todo = Todo.builder()
                                .user(testUser)
                                .title("삭제 테스트 Todo")
                                .content("삭제 테스트 설명")
                                .dueDate(LocalDate.now().plusDays(10))
                                .status(TodoStatus.TODO)
                                .priority(TodoPriority.MEDIUM)
                                .build();
                Todo saved = todoRepository.save(todo);
                Long todoId = saved.getId();

                // when
                todoRepository.deleteById(todoId);

                // then
                Optional<Todo> deleted = todoRepository.findById(todoId);
                assertFalse(deleted.isPresent());
                log.info("Todo 삭제 완료: ID {}", todoId);
        }

        // 사용자별 조회 테스트
        @Test
        @Transactional
        public void testFindByUserId() {
                // given
                User testUser = userRepository.findByEmployeeNo("20250001")
                                .orElseThrow(() -> new RuntimeException("테스트 사용자를 찾을 수 없습니다."));

                // when
                List<Todo> result = todoRepository.findByUserIdOrderByStatusAscDueDateAsc(testUser.getId());

                // then
                assertNotNull(result);
                log.info("사용자 {}의 Todo 개수: {}", testUser.getId(), result.size());
                result.forEach(todo -> log.info("Todo: {} - 상태: {}, 마감일: {}",
                                todo.getTitle(), todo.getStatus(), todo.getDueDate()));
        }

        // 상태별 조회 테스트
        @Test
        @Transactional
        public void testFindByUserIdAndStatus() {
                // given
                User testUser = userRepository.findByEmployeeNo("20250001")
                                .orElseThrow(() -> new RuntimeException("테스트 사용자를 찾을 수 없습니다."));

                // when
                List<Todo> todoList = todoRepository.findByUserIdAndStatus(testUser.getId(), TodoStatus.TODO);
                List<Todo> inProgressList = todoRepository.findByUserIdAndStatus(testUser.getId(),
                                TodoStatus.IN_PROGRESS);
                List<Todo> doneList = todoRepository.findByUserIdAndStatus(testUser.getId(), TodoStatus.DONE);

                // then
                assertNotNull(todoList);
                assertNotNull(inProgressList);
                assertNotNull(doneList);
                log.info("TODO 상태: {}개, IN_PROGRESS 상태: {}개, DONE 상태: {}개",
                                todoList.size(), inProgressList.size(), doneList.size());
        }

        // 마감일 범위 조회 테스트
        @Test
        @Transactional
        public void testFindByUserIdAndDueDateBetween() {
                // given
                User testUser = userRepository.findByEmployeeNo("20250001")
                                .orElseThrow(() -> new RuntimeException("테스트 사용자를 찾을 수 없습니다."));

                LocalDate startDate = LocalDate.now();
                LocalDate endDate = LocalDate.now().plusDays(30);

                // when
                List<Todo> result = todoRepository.findByUserIdAndDueDateBetween(testUser.getId(), startDate, endDate);

                // then
                assertNotNull(result);
                log.info("다음 30일 내 마감일 Todo 개수: {}", result.size());
                result.forEach(todo -> log.info("Todo: {} - 마감일: {}", todo.getTitle(), todo.getDueDate()));
        }

        // 미완료 Todo 조회 테스트
        @Test
        @Transactional
        public void testFindActiveTodosByUserId() {
                // given
                User testUser = userRepository.findByEmployeeNo("20250001")
                                .orElseThrow(() -> new RuntimeException("테스트 사용자를 찾을 수 없습니다."));

                // when
                List<Todo> result = todoRepository.findActiveTodosByUserId(testUser.getId());

                // then
                assertNotNull(result);
                log.info("활성 Todo 개수: {}", result.size());
                result.forEach(todo -> {
                        assertNotEquals(TodoStatus.DONE, todo.getStatus());
                        assertNotEquals(TodoStatus.CANCELLED, todo.getStatus());
                        log.info("활성 Todo: {} - 상태: {}, 마감일: {}, 우선순위: {}",
                                        todo.getTitle(), todo.getStatus(), todo.getDueDate(), todo.getPriority());
                });
        }

        // 마감일 지난 Todo 조회 테스트
        @Test
        @Transactional
        public void testFindOverdueTodosByUserId() {
                // given
                User testUser = userRepository.findByEmployeeNo("20250001")
                                .orElseThrow(() -> new RuntimeException("테스트 사용자를 찾을 수 없습니다."));

                LocalDate today = LocalDate.now();

                // when
                List<Todo> result = todoRepository.findOverdueTodosByUserId(testUser.getId(), today);

                // then
                assertNotNull(result);
                log.info("마감일 지난 미완료 Todo 개수: {}", result.size());
                result.forEach(todo -> {
                        assertTrue(todo.getDueDate().isBefore(today));
                        assertNotEquals(TodoStatus.DONE, todo.getStatus());
                        assertNotEquals(TodoStatus.CANCELLED, todo.getStatus());
                        log.info("지난 Todo: {} - 마감일: {}, 상태: {}",
                                        todo.getTitle(), todo.getDueDate(), todo.getStatus());
                });
        }

        // Todo 상태 변경 메서드 테스트
        @Test
        @Transactional
        public void testTodoStatusMethods() {
                // given
                User testUser = userRepository.findByEmployeeNo("20250001")
                                .orElseThrow(() -> new RuntimeException("테스트 사용자를 찾을 수 없습니다."));

                Todo todo = Todo.builder()
                                .user(testUser)
                                .title("상태 변경 테스트")
                                .content("상태 변경 테스트 설명")
                                .dueDate(LocalDate.now().plusDays(5))
                                .status(TodoStatus.TODO)
                                .priority(TodoPriority.MEDIUM)
                                .build();
                Todo saved = todoRepository.save(todo);

                // when - 진행 중으로 변경
                saved.start();
                todoRepository.save(saved);

                // then
                assertEquals(TodoStatus.IN_PROGRESS, saved.getStatus());
                assertFalse(saved.isCompleted());
                assertFalse(saved.isCancelled());

                // when - 완료로 변경
                saved.complete();
                todoRepository.save(saved);

                // then
                assertEquals(TodoStatus.DONE, saved.getStatus());
                assertTrue(saved.isCompleted());
                assertNotNull(saved.getCompletedAt());
                log.info("완료된 Todo: {}, 완료 시간: {}", saved.getTitle(), saved.getCompletedAt());

                // when - 취소로 변경
                saved.cancel();
                todoRepository.save(saved);

                // then
                assertEquals(TodoStatus.CANCELLED, saved.getStatus());
                assertTrue(saved.isCancelled());
                log.info("취소된 Todo: {}", saved.getTitle());
        }

        // 마감일 지났는지 확인 테스트
        @Test
        @Transactional
        public void testIsOverdue() {
                // given
                User testUser = userRepository.findByEmployeeNo("20250001")
                                .orElseThrow(() -> new RuntimeException("테스트 사용자를 찾을 수 없습니다."));

                Todo pastTodo = Todo.builder()
                                .user(testUser)
                                .title("과거 마감일 Todo")
                                .content("과거 마감일")
                                .dueDate(LocalDate.now().minusDays(5))
                                .status(TodoStatus.TODO)
                                .priority(TodoPriority.HIGH)
                                .build();

                Todo futureTodo = Todo.builder()
                                .user(testUser)
                                .title("미래 마감일 Todo")
                                .content("미래 마감일")
                                .dueDate(LocalDate.now().plusDays(5))
                                .status(TodoStatus.TODO)
                                .priority(TodoPriority.HIGH)
                                .build();

                // when
                boolean pastOverdue = pastTodo.isOverdue();
                boolean futureOverdue = futureTodo.isOverdue();

                // then
                assertTrue(pastOverdue);
                assertFalse(futureOverdue);
                log.info("과거 마감일 Todo 지남 여부: {}, 미래 마감일 Todo 지남 여부: {}", pastOverdue, futureOverdue);
        }

        // 더미 데이터 50개 생성
        @Test
        // @Transactional
        public void testCreateDummyData() {
                // given
                User testUser = userRepository.findByEmployeeNo("20250001")
                                .orElseThrow(() -> new RuntimeException("테스트 사용자를 찾을 수 없습니다."));

                String[] titles = {
                                "프로젝트 기획서 작성", "코드 리뷰 진행", "데이터베이스 설계", "API 문서 작성",
                                "테스트 케이스 작성", "버그 수정", "성능 최적화", "보안 점검",
                                "배포 준비", "문서화 작업", "회의록 정리", "프레젠테이션 준비",
                                "사용자 피드백 수집", "디자인 리뷰", "인프라 구축", "모니터링 설정",
                                "로깅 시스템 구축", "에러 핸들링 개선", "코드 리팩토링", "단위 테스트 작성",
                                "통합 테스트 수행", "사용자 매뉴얼 작성", "온보딩 자료 준비", "팀 미팅 준비",
                                "스프린트 계획 수립", "백로그 정리", "이슈 트래킹", "버전 관리",
                                "의존성 업데이트", "라이브러리 마이그레이션", "환경 설정", "CI/CD 파이프라인 구축",
                                "성능 테스트", "부하 테스트", "보안 스캔", "코드 커버리지 측정",
                                "문서 번역", "국제화 작업", "접근성 개선", "모바일 반응형 작업",
                                "데이터 마이그레이션", "백업 전략 수립", "재해 복구 계획", "모니터링 대시보드 구성",
                                "알림 시스템 구축", "리포트 생성", "데이터 분석", "통계 수집"
                };

                String[] contents = {
                                "상세한 기획서를 작성하고 팀원들과 공유해야 합니다.",
                                "코드 품질을 향상시키기 위한 리뷰를 진행합니다.",
                                "효율적인 데이터베이스 구조를 설계합니다.",
                                "개발자들이 쉽게 이해할 수 있는 API 문서를 작성합니다.",
                                "모든 기능에 대한 테스트 케이스를 작성합니다.",
                                "발견된 버그를 수정하고 재테스트를 진행합니다.",
                                "애플리케이션의 성능을 개선합니다.",
                                "보안 취약점을 점검하고 수정합니다.",
                                "프로덕션 환경 배포를 준비합니다.",
                                "프로젝트 관련 문서를 정리하고 업데이트합니다.",
                                "회의 내용을 정리하고 액션 아이템을 추출합니다.",
                                "프레젠테이션 자료를 준비하고 리허설을 진행합니다.",
                                "사용자들의 의견을 수집하고 분석합니다.",
                                "디자인을 검토하고 개선 사항을 도출합니다.",
                                "서버 인프라를 구축하고 설정합니다.",
                                "시스템 모니터링 도구를 설정합니다.",
                                "효과적인 로깅 시스템을 구축합니다.",
                                "에러 처리 로직을 개선합니다.",
                                "코드 구조를 개선하고 가독성을 높입니다.",
                                "각 모듈에 대한 단위 테스트를 작성합니다.",
                                "시스템 전체의 통합 테스트를 수행합니다.",
                                "사용자를 위한 매뉴얼을 작성합니다.",
                                "신규 입사자를 위한 온보딩 자료를 준비합니다.",
                                "팀 미팅을 위한 자료를 준비합니다.",
                                "다음 스프린트 계획을 수립합니다.",
                                "프로젝트 백로그를 정리합니다.",
                                "이슈를 추적하고 관리합니다.",
                                "버전 관리를 체계적으로 수행합니다.",
                                "프로젝트 의존성을 업데이트합니다.",
                                "구버전 라이브러리를 최신 버전으로 마이그레이션합니다.",
                                "개발 환경을 설정합니다.",
                                "지속적 통합/배포 파이프라인을 구축합니다.",
                                "시스템 성능을 테스트합니다.",
                                "높은 부하 상황에서의 시스템 동작을 테스트합니다.",
                                "보안 취약점을 스캔합니다.",
                                "코드 커버리지를 측정하고 개선합니다.",
                                "문서를 다른 언어로 번역합니다.",
                                "다국어 지원을 위한 국제화 작업을 진행합니다.",
                                "접근성을 개선합니다.",
                                "모바일 환경에 최적화합니다.",
                                "데이터를 새로운 시스템으로 마이그레이션합니다.",
                                "효과적인 백업 전략을 수립합니다.",
                                "재해 복구 계획을 수립합니다.",
                                "모니터링 대시보드를 구성합니다.",
                                "알림 시스템을 구축합니다.",
                                "정기 리포트를 생성합니다.",
                                "데이터를 분석하고 인사이트를 도출합니다.",
                                "시스템 통계를 수집합니다."
                };

                TodoStatus[] statuses = TodoStatus.values();
                TodoPriority[] priorities = TodoPriority.values();
                java.util.Random random = new java.util.Random();
                LocalDate today = LocalDate.now();
                java.util.List<Todo> savedTodos = new java.util.ArrayList<>();

                // when - 50개의 더미 데이터 생성
                for (int i = 0; i < 50; i++) {
                        int titleIndex = i % titles.length;
                        int contentIndex = i % contents.length;
                        TodoStatus status = statuses[random.nextInt(statuses.length)];
                        TodoPriority priority = priorities[random.nextInt(priorities.length)];

                        // 마감일: 과거(-30 ~ -1), 오늘(0), 미래(1 ~ 30) 중 랜덤
                        int daysOffset = random.nextInt(61) - 30; // -30 ~ 30
                        LocalDate dueDate = today.plusDays(daysOffset);

                        Todo todo = Todo.builder()
                                        .user(testUser)
                                        .title(titles[titleIndex] + " #" + (i + 1))
                                        .content(contents[contentIndex])
                                        .dueDate(dueDate)
                                        .status(status)
                                        .priority(priority)
                                        .build();

                        Todo saved = todoRepository.save(todo);
                        savedTodos.add(saved);
                }

                // then
                assertEquals(50, savedTodos.size());
                log.info("더미 데이터 50개 생성 완료");

                // 상태별 통계 출력
                long todoCount = savedTodos.stream().filter(t -> t.getStatus() == TodoStatus.TODO).count();
                long inProgressCount = savedTodos.stream().filter(t -> t.getStatus() == TodoStatus.IN_PROGRESS).count();
                long doneCount = savedTodos.stream().filter(t -> t.getStatus() == TodoStatus.DONE).count();
                long cancelledCount = savedTodos.stream().filter(t -> t.getStatus() == TodoStatus.CANCELLED).count();

                log.info("상태별 통계 - TODO: {}, IN_PROGRESS: {}, DONE: {}, CANCELLED: {}",
                                todoCount, inProgressCount, doneCount, cancelledCount);
        }


}
