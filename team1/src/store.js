import { configureStore } from "@reduxjs/toolkit";
import loginSlice from "./slices/loginSlice";
import expenseSlice from "./slices/expenseSlice";
import approvalSlice from "./slices/approvalSlice";
import receiptSlice from "./slices/receiptSlice";
import userSlice from "./slices/userSlice";
import todoSlice from "./slices/todoSlice";
import notificationReducer from "./slices/notificationSlice";

const store = configureStore({
    reducer: {
        loginSlice: loginSlice, // state.loginSlice
        expense: expenseSlice, // state.expense
        approval: approvalSlice, // state.approval
        receipt: receiptSlice, // state.receipt
        user: userSlice, // state.user
        todo: todoSlice, // state.todo
        notification: notificationReducer, // state.notification
    },
});

export default store;
