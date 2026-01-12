import React, { useState, useMemo, useEffect, useRef, useCallback } from "react";
import { useDispatch, useSelector } from "react-redux";
import { updateTodoStatus, deleteTodo } from "../../slices/todoSlice";
import "./TodoList.css";

export default function TodoList() {
    const dispatch = useDispatch();
    const { todos, selectedDate, loading } = useSelector((state) => state.todo);
    const [filter, setFilter] = useState("all"); // all, active, done
    const [displayCount, setDisplayCount] = useState(10); // 초기 표시 개수
    const observerTarget = useRef(null);
    const ITEMS_PER_LOAD = 10; // 한 번에 추가로 로드할 항목 수

    // 선택한 날짜의 Todo 필터링
    const filteredTodos = useMemo(() => {
        let filtered = todos;

        // 날짜 필터링
        if (selectedDate) {
            const selectedDateStr = formatDateToString(selectedDate);
            filtered = filtered.filter((todo) => {
                if (!todo.dueDate) return false;
                const todoDateStr = todo.dueDate.split("T")[0];
                return todoDateStr === selectedDateStr;
            });
        }

        // 상태 필터링
        if (filter === "active") {
            filtered = filtered.filter((todo) => todo.status !== "DONE");
        } else if (filter === "done") {
            filtered = filtered.filter((todo) => todo.status === "DONE");
        }

        // 날짜순 정렬 (마감일이 있는 것 우선, 그 다음 마감일 오름차순)
        // 배열을 복사한 후 정렬 (Redux 상태는 읽기 전용이므로)
        const sorted = [...filtered].sort((a, b) => {
            if (!a.dueDate && !b.dueDate) return 0;
            if (!a.dueDate) return 1;
            if (!b.dueDate) return -1;
            return new Date(a.dueDate) - new Date(b.dueDate);
        });

        return sorted;
    }, [todos, selectedDate, filter]);

    // 날짜 포맷팅 (로컬 타임존 기준)
    function formatDateToString(date) {
        if (!date) return "";
        const d = new Date(date);
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, "0");
        const day = String(d.getDate()).padStart(2, "0");
        return `${year}-${month}-${day}`;
    }

    // 상태 변경 핸들러
    const handleStatusChange = async (e, id) => {
        e.preventDefault();
        e.stopPropagation();
        const newStatus = e.target.value;
        dispatch(updateTodoStatus({ id, status: newStatus }));
    };

    // 삭제 핸들러
    const handleDelete = async (id) => {
        if (window.confirm("정말 삭제하시겠습니까?")) {
            dispatch(deleteTodo(id));
        }
    };

    // 우선순위 색상
    const getPriorityColor = (priority) => {
        switch (priority) {
            case "HIGH":
                return "#ef4444";
            case "MEDIUM":
                return "#f59e0b";
            case "LOW":
                return "#10b981";
            default:
                return "#6b7280";
        }
    };

    // 상태 라벨
    const getStatusLabel = (status) => {
        switch (status) {
            case "TODO":
                return "할 일";
            case "IN_PROGRESS":
                return "진행 중";
            case "DONE":
                return "완료";
            case "CANCELLED":
                return "취소";
            default:
                return status;
        }
    };

    // 표시할 Todo 목록 (무한 스크롤용)
    const displayedTodos = useMemo(() => {
        return filteredTodos.slice(0, displayCount);
    }, [filteredTodos, displayCount]);

    // 더 많은 항목 로드
    const loadMore = useCallback(() => {
        if (displayCount < filteredTodos.length) {
            setDisplayCount((prev) => Math.min(prev + ITEMS_PER_LOAD, filteredTodos.length));
        }
    }, [displayCount, filteredTodos.length]);

    // Intersection Observer 설정
    useEffect(() => {
        const observer = new IntersectionObserver(
            (entries) => {
                if (entries[0].isIntersecting && displayCount < filteredTodos.length) {
                    loadMore();
                }
            },
            { threshold: 0.1 }
        );

        const currentTarget = observerTarget.current;
        if (currentTarget) {
            observer.observe(currentTarget);
        }

        return () => {
            if (currentTarget) {
                observer.unobserve(currentTarget);
            }
        };
    }, [displayCount, filteredTodos.length, loadMore]);

    // 필터나 날짜가 변경되면 표시 개수 리셋
    useEffect(() => {
        setDisplayCount(10);
    }, [filter, selectedDate]);

    if (loading) {
        return <div className="todo-list-loading">로딩 중...</div>;
    }

    return (
        <div className="todo-list-container">
            <div className="todo-list-header">
                <h3>할 일 목록</h3>
                <div className="todo-filter-buttons">
                    <button
                        className={filter === "all" ? "active" : ""}
                        onClick={() => setFilter("all")}
                    >
                        전체
                    </button>
                    <button
                        className={filter === "active" ? "active" : ""}
                        onClick={() => setFilter("active")}
                    >
                        진행 중
                    </button>
                    <button
                        className={filter === "done" ? "active" : ""}
                        onClick={() => setFilter("done")}
                    >
                        완료
                    </button>
                </div>
            </div>

            {selectedDate && (
                <div className="todo-selected-date">
                    선택한 날짜: {formatDateToString(selectedDate)}
                </div>
            )}

            <div className="todo-list-items">
                {filteredTodos.length === 0 ? (
                    <div className="todo-empty">할 일이 없습니다.</div>
                ) : (
                    <>
                        {displayedTodos.map((todo) => (
                            <div
                                key={todo.id}
                                className={`todo-item ${todo.status === "DONE" ? "done" : ""}`}
                            >
                                <div className="todo-item-header">
                                    <div className="todo-priority">
                                        <span
                                            className="priority-dot"
                                            style={{ backgroundColor: getPriorityColor(todo.priority) }}
                                        />
                                        <span className="priority-label">{todo.priority}</span>
                                    </div>
                                    <span className="todo-status">{getStatusLabel(todo.status)}</span>
                                </div>

                                <div className="todo-title">{todo.title}</div>
                                {todo.content && (
                                    <div className="todo-content">{todo.content}</div>
                                )}
                                {todo.dueDate && (
                                    <div className="todo-due-date">
                                        마감일: {formatDateToString(todo.dueDate)}
                                    </div>
                                )}

                                <div className="todo-actions">
                                    <select
                                        className="status-select"
                                        value={todo.status}
                                        onChange={(e) => handleStatusChange(e, todo.id)}
                                    >
                                        <option value="TODO">할 일</option>
                                        <option value="IN_PROGRESS">진행 중</option>
                                        <option value="DONE">완료</option>
                                    </select>
                                    <button
                                        className="btn-delete"
                                        onClick={(e) => {
                                            e.preventDefault();
                                            handleDelete(todo.id);
                                        }}
                                    >
                                        삭제
                                    </button>
                                </div>
                            </div>
                        ))}
                        {displayCount < filteredTodos.length && (
                            <div ref={observerTarget} className="todo-loading-more">
                                로딩 중...
                            </div>
                        )}
                    </>
                )}
            </div>
        </div>
    );
}

