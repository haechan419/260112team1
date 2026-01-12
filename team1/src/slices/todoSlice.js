import { createSlice, createAsyncThunk } from "@reduxjs/toolkit";
import * as todoApi from "../api/todoApi";

// 비동기 액션: Todo 목록 조회
export const fetchTodos = createAsyncThunk(
    "todo/fetchTodos",
    async (_, { rejectWithValue }) => {
        try {
            const data = await todoApi.getTodoList();
            return data;
        } catch (error) {
            return rejectWithValue(error.response?.data || error.message);
        }
    }
);

// 비동기 액션: 활성 Todo 목록 조회
export const fetchActiveTodos = createAsyncThunk(
    "todo/fetchActiveTodos",
    async (_, { rejectWithValue }) => {
        try {
            const data = await todoApi.getActiveTodoList();
            return data;
        } catch (error) {
            return rejectWithValue(error.response?.data || error.message);
        }
    }
);

// 비동기 액션: 마감일 지난 Todo 목록 조회
export const fetchOverdueTodos = createAsyncThunk(
    "todo/fetchOverdueTodos",
    async (_, { rejectWithValue }) => {
        try {
            const data = await todoApi.getOverdueTodoList();
            return data;
        } catch (error) {
            return rejectWithValue(error.response?.data || error.message);
        }
    }
);

// 비동기 액션: 날짜 범위로 Todo 조회
export const fetchTodosByDateRange = createAsyncThunk(
    "todo/fetchTodosByDateRange",
    async ({ startDate, endDate }, { rejectWithValue }) => {
        try {
            const data = await todoApi.getTodoListByDateRange(startDate, endDate);
            return data;
        } catch (error) {
            return rejectWithValue(error.response?.data || error.message);
        }
    }
);

// 비동기 액션: Todo 생성
export const createTodo = createAsyncThunk(
    "todo/createTodo",
    async (todoData, { rejectWithValue }) => {
        try {
            const response = await todoApi.createTodo(todoData);
            const newTodo = await todoApi.getTodo(response.id);
            return newTodo;
        } catch (error) {
            return rejectWithValue(error.response?.data || error.message);
        }
    }
);

// 비동기 액션: Todo 수정
export const updateTodo = createAsyncThunk(
    "todo/updateTodo",
    async ({ id, todoData }, { rejectWithValue }) => {
        try {
            await todoApi.updateTodo(id, todoData);
            const updatedTodo = await todoApi.getTodo(id);
            return updatedTodo;
        } catch (error) {
            return rejectWithValue(error.response?.data || error.message);
        }
    }
);

// 비동기 액션: Todo 상태 변경
export const updateTodoStatus = createAsyncThunk(
    "todo/updateTodoStatus",
    async ({ id, status }, { rejectWithValue }) => {
        try {
            await todoApi.updateTodoStatus(id, status);
            return { id, status };
        } catch (error) {
            return rejectWithValue(error.response?.data || error.message);
        }
    }
);

// 비동기 액션: Todo 삭제
export const deleteTodo = createAsyncThunk(
    "todo/deleteTodo",
    async (id, { rejectWithValue }) => {
        try {
            await todoApi.deleteTodo(id);
            return id;
        } catch (error) {
            return rejectWithValue(error.response?.data || error.message);
        }
    }
);

const initialState = {
    todos: [],
    activeTodos: [],
    overdueTodos: [],
    selectedDate: null, // 캘린더에서 선택한 날짜
    loading: false,
    error: null,
};

const todoSlice = createSlice({
    name: "todo",
    initialState,
    reducers: {
        // 선택한 날짜 설정
        setSelectedDate: (state, action) => {
            state.selectedDate = action.payload;
        },
        // 에러 초기화
        clearError: (state) => {
            state.error = null;
        },
    },
    extraReducers: (builder) => {
        // fetchTodos
        builder
            .addCase(fetchTodos.pending, (state) => {
                state.loading = true;
                state.error = null;
            })
            .addCase(fetchTodos.fulfilled, (state, action) => {
                state.loading = false;
                state.todos = action.payload;
            })
            .addCase(fetchTodos.rejected, (state, action) => {
                state.loading = false;
                state.error = action.payload;
            });

        // fetchActiveTodos
        builder
            .addCase(fetchActiveTodos.pending, (state) => {
                state.loading = true;
                state.error = null;
            })
            .addCase(fetchActiveTodos.fulfilled, (state, action) => {
                state.loading = false;
                state.activeTodos = action.payload;
            })
            .addCase(fetchActiveTodos.rejected, (state, action) => {
                state.loading = false;
                state.error = action.payload;
            });

        // fetchOverdueTodos
        builder
            .addCase(fetchOverdueTodos.pending, (state) => {
                state.loading = true;
                state.error = null;
            })
            .addCase(fetchOverdueTodos.fulfilled, (state, action) => {
                state.loading = false;
                state.overdueTodos = action.payload;
            })
            .addCase(fetchOverdueTodos.rejected, (state, action) => {
                state.loading = false;
                state.error = action.payload;
            });

        // fetchTodosByDateRange
        builder
            .addCase(fetchTodosByDateRange.pending, (state) => {
                state.loading = true;
                state.error = null;
            })
            .addCase(fetchTodosByDateRange.fulfilled, (state, action) => {
                state.loading = false;
                state.todos = action.payload;
            })
            .addCase(fetchTodosByDateRange.rejected, (state, action) => {
                state.loading = false;
                state.error = action.payload;
            });

        // createTodo
        builder
            .addCase(createTodo.pending, (state) => {
                state.loading = true;
                state.error = null;
            })
            .addCase(createTodo.fulfilled, (state, action) => {
                state.loading = false;
                state.todos.push(action.payload);
            })
            .addCase(createTodo.rejected, (state, action) => {
                state.loading = false;
                state.error = action.payload;
            });

        // updateTodo
        builder
            .addCase(updateTodo.pending, (state) => {
                state.loading = true;
                state.error = null;
            })
            .addCase(updateTodo.fulfilled, (state, action) => {
                state.loading = false;
                const index = state.todos.findIndex((t) => t.id === action.payload.id);
                if (index !== -1) {
                    state.todos[index] = action.payload;
                }
            })
            .addCase(updateTodo.rejected, (state, action) => {
                state.loading = false;
                state.error = action.payload;
            });

        // updateTodoStatus
        builder
            .addCase(updateTodoStatus.pending, (state) => {
                // 상태 변경 시에는 loading을 변경하지 않음 (깜빡임 방지)
                state.error = null;
            })
            .addCase(updateTodoStatus.fulfilled, (state, action) => {
                const index = state.todos.findIndex((t) => t.id === action.payload.id);
                if (index !== -1) {
                    state.todos[index].status = action.payload.status;
                }
            })
            .addCase(updateTodoStatus.rejected, (state, action) => {
                state.error = action.payload;
            });

        // deleteTodo
        builder
            .addCase(deleteTodo.pending, (state) => {
                // 삭제 시에도 loading을 변경하지 않음 (깜빡임 방지)
                state.error = null;
            })
            .addCase(deleteTodo.fulfilled, (state, action) => {
                state.todos = state.todos.filter((t) => t.id !== action.payload);
            })
            .addCase(deleteTodo.rejected, (state, action) => {
                state.error = action.payload;
            });
    },
});

export const { setSelectedDate, clearError } = todoSlice.actions;
export default todoSlice.reducer;

