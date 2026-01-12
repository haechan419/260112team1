import jwtAxios from "../util/jwtUtil";

// Todo 생성
export const createTodo = async (todoData) => {
    const response = await jwtAxios.post("/todos", todoData);
    return response.data;
};

// Todo 조회
export const getTodo = async (id) => {
    const response = await jwtAxios.get(`/todos/${id}`);
    return response.data;
};

// Todo 목록 조회
export const getTodoList = async () => {
    const response = await jwtAxios.get("/todos/list");
    return response.data;
};

// 활성 Todo 목록 조회 (미완료)
export const getActiveTodoList = async () => {
    const response = await jwtAxios.get("/todos/active");
    return response.data;
};

// 마감일 지난 Todo 목록 조회
export const getOverdueTodoList = async () => {
    const response = await jwtAxios.get("/todos/overdue");
    return response.data;
};

// 날짜 범위로 Todo 조회
export const getTodoListByDateRange = async (startDate, endDate) => {
    const response = await jwtAxios.get("/todos/range", {
        params: {
            startDate,
            endDate,
        },
    });
    return response.data;
};

// Todo 수정
export const updateTodo = async (id, todoData) => {
    const response = await jwtAxios.put(`/todos/${id}`, todoData);
    return response.data;
};

// Todo 상태 변경
export const updateTodoStatus = async (id, status) => {
    const response = await jwtAxios.patch(`/todos/${id}/status`, null, {
        params: { status },
    });
    return response.data;
};

// Todo 삭제
export const deleteTodo = async (id) => {
    const response = await jwtAxios.delete(`/todos/${id}`);
    return response.data;
};

