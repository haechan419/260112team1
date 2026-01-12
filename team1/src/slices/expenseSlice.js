import { createSlice, createAsyncThunk } from "@reduxjs/toolkit";
import { expenseApi } from "../api/expenseApi";

const initialState = {
    expenses: [],
    currentExpense: null,
    loading: false,
    error: null,
    pageResponse: null,
};

/**
 * 지출 내역 목록 조회 비동기 액션
 *
 * @param {Object} params - 조회 파라미터 (page, size, status, startDate, endDate)
 */
export const fetchExpenses = createAsyncThunk(
    "expense/fetchExpenses",
    async (params) => {
        const response = await expenseApi.getExpenses(params);
        return response.data;
    }
);

/**
 * 지출 내역 상세 조회 비동기 액션
 *
 * @param {number} id - 지출 내역 ID
 */
export const fetchExpense = createAsyncThunk("expense/fetchExpense", async (id) => {
    const response = await expenseApi.getExpense(id);
    return response.data;
});

/**
 * 지출 내역 생성 비동기 액션
 *
 * @param {Object} data - 지출 내역 데이터
 */
export const createExpense = createAsyncThunk("expense/createExpense", async (data) => {
    const response = await expenseApi.createExpense(data);
    return response.data;
});

/**
 * 지출 내역 수정 비동기 액션
 *
 * @param {Object} payload - 수정 정보 (id, data)
 */
export const updateExpense = createAsyncThunk(
    "expense/updateExpense",
    async ({ id, data }) => {
        const response = await expenseApi.updateExpense(id, data);
        return response.data;
    }
);

/**
 * 지출 내역 삭제 비동기 액션
 *
 * @param {number} id - 지출 내역 ID
 */
export const deleteExpense = createAsyncThunk("expense/deleteExpense", async (id) => {
    await expenseApi.deleteExpense(id);
    return id;
});

/**
 * 지출 내역 제출 비동기 액션
 *
 * @param {Object} payload - 제출 정보 (id, data)
 */
export const submitExpense = createAsyncThunk(
    "expense/submitExpense",
    async ({ id, data }) => {
        const response = await expenseApi.submitExpense(id, data);
        return response.data;
    }
);

const expenseSlice = createSlice({
    name: "expense",
    initialState,
    reducers: {
        clearCurrentExpense: (state) => {
            state.currentExpense = null;
        },
        clearError: (state) => {
            state.error = null;
        },
    },
    extraReducers: (builder) => {
        builder
            .addCase(fetchExpenses.pending, (state) => {
                state.loading = true;
                state.error = null;
            })
            .addCase(fetchExpenses.fulfilled, (state, action) => {
                state.loading = false;
                state.expenses = action.payload.content || [];
                state.pageResponse = action.payload;
            })
            .addCase(fetchExpenses.rejected, (state, action) => {
                state.loading = false;
                state.error = action.error.message || "지출 내역 조회에 실패했습니다.";
            })
            .addCase(fetchExpense.pending, (state) => {
                state.loading = true;
                state.error = null;
            })
            .addCase(fetchExpense.fulfilled, (state, action) => {
                state.loading = false;
                state.currentExpense = action.payload;
            })
            .addCase(fetchExpense.rejected, (state, action) => {
                state.loading = false;
                state.error = action.error.message || "지출 내역 조회에 실패했습니다.";
            })
            .addCase(createExpense.fulfilled, (state, action) => {
                state.expenses.unshift(action.payload);
                state.currentExpense = action.payload;
            })
            .addCase(updateExpense.fulfilled, (state, action) => {
                const index = state.expenses.findIndex((e) => e.id === action.payload.id);
                if (index !== -1) {
                    state.expenses[index] = action.payload;
                }
                if (state.currentExpense?.id === action.payload.id) {
                    state.currentExpense = action.payload;
                }
            })
            .addCase(deleteExpense.fulfilled, (state, action) => {
                state.expenses = state.expenses.filter((e) => e.id !== action.payload);
                if (state.currentExpense?.id === action.payload) {
                    state.currentExpense = null;
                }
            })
            .addCase(submitExpense.fulfilled, (state, action) => {
                const index = state.expenses.findIndex((e) => e.id === action.payload.id);
                if (index !== -1) {
                    state.expenses[index] = action.payload;
                }
                if (state.currentExpense?.id === action.payload.id) {
                    state.currentExpense = action.payload;
                }
            });
    },
});

export const { clearCurrentExpense, clearError } = expenseSlice.actions;
export default expenseSlice.reducer;

