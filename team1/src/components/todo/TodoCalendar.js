import React, { useState, useMemo, useCallback } from "react";
import Calendar from "react-calendar";
import { useDispatch, useSelector } from "react-redux";
import { setSelectedDate } from "../../slices/todoSlice";
import "react-calendar/dist/Calendar.css";
import "./TodoCalendar.css";

// 로컬 시간 기준으로 날짜를 YYYY-MM-DD 형식으로 포맷
const formatLocalDate = (date) => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
};

export default function TodoCalendar() {
    const dispatch = useDispatch();
    const { todos, selectedDate } = useSelector((state) => state.todo);

    const [viewDate, setViewDate] = useState(new Date());

    // 날짜별 Todo 개수 계산
    const todosByDate = useMemo(() => {
        const map = new Map();
        todos.forEach((todo) => {
            if (todo.dueDate) {
                const dateStr = todo.dueDate.split("T")[0];
                map.set(dateStr, (map.get(dateStr) || 0) + 1);
            }
        });
        return map;
    }, [todos]);

    // 날짜 변경 핸들러
    const handleDateChange = useCallback((date) => {
        if (date) {
            const dateStr = formatLocalDate(date);
            dispatch(setSelectedDate(dateStr));
        }
    }, [dispatch]);

    // 오늘로 이동 핸들러
    const handleGoToday = useCallback(() => {
        const now = new Date();
        setViewDate(now);
        handleDateChange(now);
    }, [handleDateChange]);

    // 월 변경 핸들러
    const handleMonthChange = useCallback(({ activeStartDate }) => {
        setViewDate(activeStartDate);
    }, []);

    // 타일 클래스 커스터마이징 (Todo 개수에 따라)
    const tileClassName = useCallback(
        ({ date }) => {
            const dateStr = formatLocalDate(date);
            const todoCount = todosByDate.get(dateStr);
            if (todoCount && todoCount > 0) {
                return "has-todos";
            }
            return null;
        },
        [todosByDate]
    );

    // 타일 컨텐츠 커스터마이징 (Todo 개수 표시)
    const tileContent = useCallback(
        ({ date }) => {
            const dateStr = formatLocalDate(date);
            const todoCount = todosByDate.get(dateStr);
            if (todoCount && todoCount > 0) {
                return (
                    <div className="todo-count-indicator">
                        <span>{todoCount}</span>
                    </div>
                );
            }
            return null;
        },
        [todosByDate]
    );

    return (
        <div className="todo-calendar-container">
            <div className="calendar-header">
                <h3>Todo 캘린더</h3>
                <button className="today-btn" onClick={handleGoToday}>
                    오늘
                </button>
            </div>
            <Calendar
                onChange={handleDateChange}
                value={selectedDate ? new Date(selectedDate) : null}
                locale="ko-KR"
                calendarType="gregory"
                tileClassName={tileClassName}
                tileContent={tileContent}
                onActiveStartDateChange={handleMonthChange}
                activeStartDate={viewDate}
            />
            {selectedDate && (
                <div className="calendar-selected-info">
                    선택한 날짜: {selectedDate}
                </div>
            )}
        </div>
    );
}
