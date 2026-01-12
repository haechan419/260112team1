import { createAsyncThunk, createSlice } from "@reduxjs/toolkit";
import { getCookie, removeCookie, setCookie } from "../util/cookieUtil";
import { login } from "../api/authApi";

// ═══════════════════════════════════════════════════════════════
// 초기 상태
// ═══════════════════════════════════════════════════════════════
const initState = {
  employeeNo: "",
};

/**
 * 쿠키에서 로그인 정보 로딩
 * - 새로고침해도 로그인 상태 유지
 */
const loadMemberCookie = () => {
  const memberInfo = getCookie("member");

  // 닉네임에 특수문자가 있을 경우 디코딩
  if (memberInfo && memberInfo.name) {
    memberInfo.name = decodeURIComponent(memberInfo.name);
  }

  return memberInfo;
};

// ═══════════════════════════════════════════════════════════════
// 비동기 Thunk: 로그인 API 호출
// ═══════════════════════════════════════════════════════════════
export const loginPostAsync = createAsyncThunk(
  "loginPostAsync",
  async (loginParam, { rejectWithValue }) => {
    try {
      const response = await login(loginParam.employeeNo, loginParam.password);
      return response.data;
    } catch (error) {
      // ⭐ axios 에러 응답은 error.response.data에 있음
      if (error.response && error.response.data) {
        return rejectWithValue(error.response.data);
      }
      return rejectWithValue({ error: "LOGIN_FAILED", message: "로그인에 실패했습니다." });
    }
  }
);

// ═══════════════════════════════════════════════════════════════
// Slice 생성
// ═══════════════════════════════════════════════════════════════
const loginSlice = createSlice({
  name: "loginSlice",

  // 초기값: 쿠키에 저장된 값이 있으면 사용, 없으면 initState
  initialState: loadMemberCookie() || initState,

  // ═══════════════════════════════════════════════════════════════
  // 동기 리듀서 (reducers)
  // ═══════════════════════════════════════════════════════════════
  reducers: {
    // 로그인 (소셜 로그인 등에서 직접 사용)
    login: (state, action) => {
      console.log("login reducer 호출");
      const payload = action.payload;
      setCookie("member", JSON.stringify(payload), 1); // 1일
      return payload;
    },

    // 로그아웃
    logout: (state, action) => {
      console.log("logout reducer 호출");
      removeCookie("member");
      return { ...initState };
    },
  },

  // ═══════════════════════════════════════════════════════════════
  // 비동기 리듀서 (extraReducers) - createAsyncThunk 결과 처리
  // ═══════════════════════════════════════════════════════════════
  extraReducers: (builder) => {
    builder
      // 로그인 성공 (fulfilled)
      .addCase(loginPostAsync.fulfilled, (state, action) => {
        console.log("로그인 성공 (fulfilled)");
        const payload = action.payload;

        // 에러가 없으면 쿠키에 저장
        if (!payload.error) {
          setCookie("member", JSON.stringify(payload), 1); // 1일
        }

        return payload;
      })
      // 로그인 진행 중 (pending)
      .addCase(loginPostAsync.pending, (state, action) => {
        console.log("로그인 중 (pending)");
      })
      // 로그인 실패 (rejected)
      .addCase(loginPostAsync.rejected, (state, action) => {
        console.log("로그인 실패 (rejected):", action.payload);
        // ⭐ 에러 정보를 상태에 저장
        return { ...initState, error: action.payload?.error, message: action.payload?.message };
      });
  },
});

// 액션과 리듀서 내보내기
export const { login: loginAction, logout } = loginSlice.actions;
export default loginSlice.reducer;
