import { createSlice, createAsyncThunk } from "@reduxjs/toolkit";
import { receiptApi } from "../api/receiptApi";

const initialState = {
  currentReceipt: null,
  extraction: null,
  loading: false,
  error: null,
};

export const uploadReceipt = createAsyncThunk(
  "receipt/uploadReceipt",
  async ({ expenseId, file }) => {
    const response = await receiptApi.uploadReceipt(expenseId, file);
    return response.data;
  }
);

export const fetchReceipt = createAsyncThunk("receipt/fetchReceipt", async (id) => {
  const response = await receiptApi.getReceipt(id);
  return response.data;
});

export const fetchExtraction = createAsyncThunk("receipt/fetchExtraction", async (id) => {
  const response = await receiptApi.getExtraction(id);
  return response.data;
});

export const deleteReceipt = createAsyncThunk("receipt/deleteReceipt", async (id) => {
  await receiptApi.deleteReceipt(id);
  return id;
});

const receiptSlice = createSlice({
  name: "receipt",
  initialState,
  reducers: {
    clearCurrentReceipt: (state) => {
      state.currentReceipt = null;
      state.extraction = null;
    },
    clearError: (state) => {
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(uploadReceipt.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(uploadReceipt.fulfilled, (state, action) => {
        state.loading = false;
        state.currentReceipt = action.payload;
      })
      .addCase(uploadReceipt.rejected, (state, action) => {
        state.loading = false;
        state.error = action.error.message || "영수증 업로드에 실패했습니다.";
      })
      .addCase(fetchReceipt.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchReceipt.fulfilled, (state, action) => {
        state.loading = false;
        state.currentReceipt = action.payload;
      })
      .addCase(fetchReceipt.rejected, (state, action) => {
        state.loading = false;
        state.error = action.error.message || "영수증 조회에 실패했습니다.";
      })
      .addCase(fetchExtraction.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchExtraction.fulfilled, (state, action) => {
        state.loading = false;
        state.extraction = action.payload;
      })
      .addCase(fetchExtraction.rejected, (state, action) => {
        state.loading = false;
        state.error = action.error.message || "AI 추출 결과 조회에 실패했습니다.";
      })
      .addCase(deleteReceipt.fulfilled, (state) => {
        state.currentReceipt = null;
        state.extraction = null;
      });
  },
});

export const { clearCurrentReceipt, clearError } = receiptSlice.actions;
export default receiptSlice.reducer;

