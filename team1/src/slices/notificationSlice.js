import { createSlice, createAsyncThunk } from "@reduxjs/toolkit";
import {
    getMyExpenseNotifications,
    getMyOrderNotifications,
} from "../api/notificationApi";

const STORAGE_KEY = "read_notifications_sorted_final_v3";

const getReadList = () => {
    try {
        const stored = localStorage.getItem(STORAGE_KEY);
        return stored ? JSON.parse(stored) : [];
    } catch (e) {
        return [];
    }
};

export const checkMyNotification = createAsyncThunk(
    "notification/checkMyNotification",
    async (_, { getState, rejectWithValue }) => {
        try {
            // 1. 현재 로그인 사용자 정보 (소문자로 변환하여 저장)
            const state = getState();
            const rawUser = state.loginSlice?.email || "";
            const currentUser = rawUser.toLowerCase().trim();
            // console.log("현재 로그인 유저:", currentUser);

            const [expenseRes, orderRes] = await Promise.all([
                getMyExpenseNotifications(),
                getMyOrderNotifications(),
            ]);

            const readList = getReadList();

            const createNotificationItem = (item, forcedType) => {
                // 데이터 안전장치
                if (!item) return null;

                // 신청자 정보 (DB 데이터)
                const owner = item.requester || item.writer || "";

                const status = item.status || "";
                // 대기/신청 상태는 알림에서 제외
                if (["PENDING", "WAITING", "REQUEST", "신청", "대기"].includes(status))
                    return null;

                // 제목 및 유효성 검사
                let title = "";
                const label = forcedType === "EXPENSE" ? "[📄지출]" : "[📦비품]";
                const reason = item.rejectReason || item.reason || "";
                let isValid = false;

                // 반려
                if (
                    ["REJECTED", "RETURN", "반려", "거절"].some((s) => status.includes(s))
                ) {
                    title = `${label} 반려: ${reason ? reason : "사유 확인"}`;
                    isValid = true;
                }
                // 보완
                else if (
                    ["REQUEST_MORE", "SUPPLEMENT", "보완", "보류"].some((s) =>
                        status.includes(s)
                    )
                ) {
                    title = `${label} 보완요청: ${reason ? reason : "내용 확인"}`;
                    isValid = true;
                }
                // 승인
                else if (
                    [
                        "APPROVED",
                        "CONFIRMED",
                        "COMPLETE",
                        "승인",
                        "결재",
                        "결제",
                        "완료",
                    ].some((s) => status.includes(s))
                ) {
                    const name =
                        item.title ||
                        item.pname ||
                        (item.items && item.items[0]?.pname) ||
                        "상세 내역";
                    title = `${label} 승인완료: ${name}`;
                    isValid = true;
                }

                if (!isValid) return null;

                // ID 생성
                let id;
                if (forcedType === "EXPENSE") {
                    id = item.eno || item.expenseId || item.id; // 영수증 ID
                } else {
                    id = item.rno || item.pno || item.id; // 비품 ID
                }
                if (!id) return null;

                const targetDate =
                    item.modDate || item.uptDate || item.updatedAt || item.regDate || "";
                const idKey = `${forcedType}_${id}_${status}_${targetDate}`;

                if (readList.includes(idKey)) return null;

                return {
                    ...item,
                    notiType: forcedType,
                    id: idKey,
                    targetId: id,
                    displayDate: targetDate || new Date().toISOString(),
                    title: title,
                };
            };

            // 목록 합치기
            const expenses = (Array.isArray(expenseRes) ? expenseRes : [])
                .map((item) => createNotificationItem(item, "EXPENSE"))
                .filter((item) => item !== null);
            const orders = (Array.isArray(orderRes) ? orderRes : [])
                .map((item) => createNotificationItem(item, "ORDER"))
                .filter((item) => item !== null);

            const combinedList = [...expenses, ...orders];
            combinedList.sort(
                (a, b) =>
                    new Date(b.displayDate).getTime() - new Date(a.displayDate).getTime()
            );

            return combinedList;
        } catch (error) {
            return rejectWithValue(error);
        }
    }
);

// 리듀서는 기존과 동일
const notificationSlice = createSlice({
    name: "notification",
    initialState: { items: [], count: 0 },
    reducers: {
        removeNotification: (state, action) => {
            const uniqueId = action.payload;
            state.items = state.items.filter((item) => item.id !== uniqueId);
            state.count = state.items.length;
            const currentReadList = getReadList();
            if (!currentReadList.includes(uniqueId)) {
                currentReadList.push(uniqueId);
                localStorage.setItem(STORAGE_KEY, JSON.stringify(currentReadList));
            }
        },
        //  모두 읽음 처리
        markAllRead: (state) => {
            // 현재 화면에 떠있는 모든 알림의 ID를 가져옵니다.
            const currentIds = state.items.map((item) => item.id);

            // 기존에 읽었던 목록을 가져옵니다.
            const prevReadList = getReadList();

            // 기존 목록 + 새 목록 합치기 (중복 제거)
            // Set을 이용해 중복을 없애고 다시 배열로 만듭니다.
            const newReadList = [...new Set([...prevReadList, ...currentIds])];

            // 로컬 스토리지에 저장 (영구 저장)
            localStorage.setItem(STORAGE_KEY, JSON.stringify(newReadList));

            // 화면의 알림 목록과 카운트를 0으로 초기화
            state.items = [];
            state.count = 0;
        },
    },
    extraReducers: (builder) => {
        builder.addCase(checkMyNotification.fulfilled, (state, action) => {
            const readList = getReadList();
            state.items = action.payload.filter(
                (item) => !readList.includes(item.id)
            );
            state.count = state.items.length;
        });
    },
});

export const { removeNotification, markAllRead } = notificationSlice.actions;
export default notificationSlice.reducer;
