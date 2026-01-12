import { createSlice, createAsyncThunk } from "@reduxjs/toolkit";
import { approvalApi } from "../api/approvalApi";

const initialState = {
  requests: [],
  currentRequest: null,
  logs: [],
  loading: false,
  error: null,
  pageResponse: null,
};

export const fetchApprovalRequests = createAsyncThunk(
  "approval/fetchApprovalRequests",
  async (params) => {
    const response = await approvalApi.getApprovalRequests(params);
    return response.data;
  }
);

export const fetchApprovalRequest = createAsyncThunk("approval/fetchApprovalRequest", async (id) => {
  const response = await approvalApi.getApprovalRequest(id);
  return response.data;
});

export const fetchApprovalLogs = createAsyncThunk("approval/fetchApprovalLogs", async (id) => {
  const response = await approvalApi.getApprovalLogs(id);
  return response.data;
});

const approvalSlice = createSlice({
  name: "approval",
  initialState,
  reducers: {
    clearCurrentRequest: (state) => {
      state.currentRequest = null;
      state.logs = [];
    },
    clearError: (state) => {
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchApprovalRequests.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchApprovalRequests.fulfilled, (state, action) => {
        state.loading = false;
        state.requests = action.payload.content;
        state.pageResponse = action.payload;
      })
      .addCase(fetchApprovalRequests.rejected, (state, action) => {
        state.loading = false;
        state.error = action.error.message || "결재 요청 조회에 실패했습니다.";
      })
      .addCase(fetchApprovalRequest.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchApprovalRequest.fulfilled, (state, action) => {
        state.loading = false;
        state.currentRequest = action.payload;
      })
      .addCase(fetchApprovalRequest.rejected, (state, action) => {
        state.loading = false;
        state.error = action.error.message || "결재 요청 조회에 실패했습니다.";
      })
      .addCase(fetchApprovalLogs.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchApprovalLogs.fulfilled, (state, action) => {
        state.loading = false;
        state.logs = action.payload;
      })
      .addCase(fetchApprovalLogs.rejected, (state, action) => {
        state.loading = false;
        state.error = action.error.message || "결재 로그 조회에 실패했습니다.";
      });
  },
});

export const { clearCurrentRequest, clearError } = approvalSlice.actions;
export default approvalSlice.reducer;

