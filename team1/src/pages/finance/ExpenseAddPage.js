import React, {useEffect, useState} from "react";
import {useDispatch, useSelector} from "react-redux";
import {useNavigate, useParams, useSearchParams} from "react-router-dom";
import {createExpense, updateExpense, fetchExpense} from "../../slices/expenseSlice";
import ExpenseForm from "../../components/finance/ExpenseForm";
import "./ExpenseAddPage.css";
import AppLayout from "../../components/layout/AppLayout";

const ExpenseAddPage = () => {
    const dispatch = useDispatch();
    const navigate = useNavigate();
    const {id} = useParams();
    const [searchParams] = useSearchParams();
    const isEditMode = !!id;

    const [loading, setLoading] = useState(isEditMode);
    const currentExpense = useSelector((state) => state.expense.currentExpense);

    // 수정 모드일 때 기존 데이터 로드
    useEffect(() => {
        if (isEditMode && id) {
            dispatch(fetchExpense(id))
                .unwrap()
                .then(() => {
                    setLoading(false);
                })
                .catch((error) => {
                    console.error("지출 내역 조회 실패:", error);
                    alert("지출 내역을 불러올 수 없습니다.");
                    navigate("/receipt/expenses");
                });
        }
    }, [id, isEditMode, dispatch, navigate]);

    const handleSubmit = async (data, tempExpenseId = null) => {
        try {
            // ✅ 수정: tempExpenseId가 있으면 기존 임시 지출 내역 업데이트
            const expenseIdToUpdate = tempExpenseId || (isEditMode ? id : null);

            if (expenseIdToUpdate) {
                // 임시 지출 내역 업데이트 또는 수정 모드
                const result = await dispatch(updateExpense({id: expenseIdToUpdate, data})).unwrap();
                return result;
            } else {
                // 등록 모드 + 임시 지출 내역 없음: 새로 생성
                const result = await dispatch(createExpense(data)).unwrap();
                return result;
            }
        } catch (error) {
            console.error(isEditMode ? "지출 수정 실패:" : "지출 등록 실패:", error);
            alert(isEditMode ? "지출 수정에 실패했습니다." : "지출 등록에 실패했습니다.");
            throw error;
        }
    };

    const handleSubmitComplete = () => {
        const queryString = searchParams.toString();
        navigate(`/receipt/expenses${queryString ? `?${queryString}` : ""}`);
        // 목록 페이지가 마운트되면 자동으로 fetchExpenses가 호출됨
    };

    const handleCancel = () => {
        const queryString = searchParams.toString();
        // 수정 모드일 때는 상세 페이지로, 등록 모드일 때는 목록으로
        if (isEditMode && id) {
            navigate(`/receipt/expenses/${id}${queryString ? `?${queryString}` : ""}`);
        } else {
            navigate(`/receipt/expenses${queryString ? `?${queryString}` : ""}`);
        }
    };

    if (loading) {
        return (
            <div className="expense-add-page">
                <div className="loading">로딩 중...</div>
            </div>
        );
    }

    return (
        <AppLayout>
            <div className="expense-add-page">
                <div className="page-header-with-tab">
                    <div className="page-title-section">
                        <h1 className="page-title">{isEditMode ? "지출 내역 수정" : "새 지출 등록"}</h1>
                        <button className="close-tab-btn" onClick={handleCancel}>
                            ×
                        </button>
                    </div>
                    <p className="page-description">
                        {isEditMode
                            ? "지출 내역을 수정하고 영수증을 업로드할 수 있습니다."
                            : "지출 내역을 등록하고 영수증을 업로드할 수 있습니다."}
                    </p>
                </div>

                <div className="form-card">
                    <ExpenseForm
                        expense={isEditMode ? currentExpense : null}
                        onSubmit={handleSubmit}
                        onCancel={handleCancel}
                        onSubmitComplete={handleSubmitComplete}
                    />
                </div>
            </div>
        </AppLayout>
    );
};

export default ExpenseAddPage;
