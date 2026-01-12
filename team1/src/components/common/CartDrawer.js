import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom"; // 1. 페이지 이동용
import { useCart } from "../../context/CartContext";
import { postRequest } from "../../api/requestApi"; // 2. 백엔드 API 함수 임포트
import "../../styles/cartDrawer.css";
import useCustomLogin from "../../hooks/useCustomLogin";

// [Internal Component] 개별 아이템
const DrawerItem = ({
  item,
  updateQuantity,
  removeFromCart,
  toggleDrawer,
  setCurrentCategory,
}) => {
  const [inputValue, setInputValue] = useState(item.quantity);

  useEffect(() => {
    setInputValue(item.quantity);
  }, [item.quantity]);

  const handleChange = (e) => {
    let val = e.target.value;
    if (val.length > 2) val = val.slice(0, 2);
    setInputValue(val);
    const numVal = parseInt(val);
    if (!isNaN(numVal) && numVal >= 1) {
      updateQuantity(item.id, numVal);
    }
  };

  const handleBlur = () => {
    if (inputValue === "" || parseInt(inputValue) < 1) {
      setInputValue(1);
      updateQuantity(item.id, 1);
    }
  };

  const moveToProduct = () => {
    setCurrentCategory(item.category);
    setTimeout(() => {
      const element = document.getElementById(`product-${item.id}`);
      if (element) {
        element.scrollIntoView({ behavior: "smooth", block: "center" });
        toggleDrawer();
      }
    }, 100);
  };

  return (
    <div className="cart-item">
      <img
        src={item.img}
        alt={item.name}
        className="item-img"
        onClick={moveToProduct}
        style={{ cursor: "pointer" }}
      />
      <div className="item-info">
        <div
          className="item-name"
          onClick={moveToProduct}
          style={{ cursor: "pointer", textDecoration: "underline" }}
        >
          {item.name}
        </div>
        <div className="item-price">{item.price.toLocaleString()}원</div>

        <div className="item-controls">
          <div className="qty-group">
            <button onClick={() => updateQuantity(item.id, item.quantity - 1)}>
              -
            </button>
            <input
              type="number"
              className="qty-input-drawer"
              value={inputValue}
              onChange={handleChange}
              onBlur={handleBlur}
            />
            <button onClick={() => updateQuantity(item.id, item.quantity + 1)}>
              +
            </button>
          </div>
          <button
            className="delete-btn"
            onClick={() => removeFromCart(item.id)}
          >
            삭제
          </button>
        </div>
      </div>
    </div>
  );
};

export default function CartDrawer() {
  const {
    cartItems,
    isDrawerOpen,
    toggleDrawer,
    updateQuantity,
    removeFromCart,
    totalPrice,
    setCurrentCategory,
    // addRequest, // ⚠️ 백엔드 연동 시에는 Context의 addRequest 대신 API를 직접 호출합니다.
  } = useCart();

  const navigate = useNavigate(); // 페이지 이동 훅

  const { loginState } = useCustomLogin(); //로그인정보 가져오기
  const [memo, setMemo] = useState("");
  const BUDGET_LIMIT = 5000000;
  const vat = Math.round(totalPrice * 0.1);
  const finalTotal = totalPrice + vat;
  const usagePercent = Math.min((finalTotal / BUDGET_LIMIT) * 100, 100);

  //결재 상신 핸들러 (API 호출 + 로그 추가)
  const handleCheckout = async () => {
    console.log("👉 1. [CartDrawer] 결재 상신하기 버튼 클릭됨!");

    if (cartItems.length === 0) {
      console.log("⚠️ 장바구니 비어있음");
      return alert("장바구니가 비어있습니다.");
    }

    // 사유 필수 체크 (원하시면 주석 처리 가능)
    if (!memo.trim()) {
      console.log("⚠️ 사유 미입력");
      alert("구매 사유를 입력해주세요!");
      return;
    }

    const msg = `총 ${finalTotal.toLocaleString()}원 (부가세 포함) 결재를 상신하시겠습니까?\n\n📝 사유: ${memo ? memo : "없음"
      }`;

    if (!window.confirm(msg)) {
      console.log("❌ 사용자가 취소함");
      return;
    }

    console.log("👉 2. 데이터 생성 시작");

    // 1. 요청 데이터 생성 (백엔드 RequestDTO 구조와 일치시켜야 함)
    const requestData = {
      requester: loginState.employeeNo,
      reason: memo,
      totalAmount: finalTotal,
      items: cartItems.map((item) => ({
        pno: item.id,
        pname: item.name,
        quantity: item.quantity,
        price: item.price,
      })),
    };

    console.log("👉 3. 서버로 보낼 데이터 확인:", requestData);

    try {
      console.log("🚀 4. API 호출 시도 (postRequest -> /api/request)");

      // 2. 백엔드로 전송!
      const response = await postRequest(requestData);

      console.log("✅ 5. 서버 응답 성공:", response);

      // 3. 장바구니 비우기
      cartItems.forEach((item) => removeFromCart(item.id));

      // 4. 완료 처리
      alert(
        "✅ 결재 승인 요청이 완료되었습니다!\n[내 결재함] 메뉴로 이동합니다."
      );
      setMemo("");
      toggleDrawer(); // 서랍 닫기
      navigate("/history"); // 내역 페이지로 이동
    } catch (error) {
      console.error("🔥 6. 에러 발생:", error);

      if (error.response) {
        // 서버가 응답을 줬으나 에러인 경우 (404, 500 등)
        console.error("응답 상태 코드:", error.response.status);
        console.error("응답 데이터:", error.response.data);
        alert(`서버 에러 (${error.response.status}): 결재에 실패했습니다.`);
      } else if (error.request) {
        // 요청은 갔으나 응답이 없는 경우 (서버 다운, 네트워크 문제)
        console.error("응답 없음 (네트워크 문제 가능성):", error.request);
        alert("서버와 연결할 수 없습니다. 백엔드가 켜져 있나요?");
      } else {
        // 설정 문제 등
        console.error("요청 설정 에러:", error.message);
        alert("요청 중 오류가 발생했습니다.");
      }
    }
  };

  return (
    <>
      {isDrawerOpen && (
        <div className="cart-overlay" onClick={toggleDrawer}></div>
      )}

      <div className={`cart-drawer ${isDrawerOpen ? "open" : ""}`}>
        <div className="drawer-header">
          <h2>📑 결재 기안 확인</h2>
          <button className="close-btn" onClick={toggleDrawer}>
            ×
          </button>
        </div>

        {/* 예산 현황 바 */}
        <div className="budget-section">
          <div className="budget-label">
            <span>부서 예산 현황 (월 500만)</span>
            <span className={usagePercent > 80 ? "warning-text" : ""}>
              {usagePercent.toFixed(1)}% 사용 예상
            </span>
          </div>
          <div className="budget-track">
            <div
              className="budget-fill"
              style={{
                width: `${usagePercent}%`,
                backgroundColor: usagePercent > 90 ? "#e74c3c" : "#4f79df",
              }}
            ></div>
          </div>
          <div className="budget-limit-text">
            결재 후 잔액: {(BUDGET_LIMIT - finalTotal).toLocaleString()}원
          </div>
        </div>

        {/* 장바구니 목록 */}
        <div className="drawer-body">
          {cartItems.length === 0 ? (
            <div className="empty-cart">
              <p>결재할 품목이 없습니다.</p>
              <p
                className="text-sm"
                style={{ marginTop: "5px", color: "#999" }}
              >
                필요한 비품을 담아보세요.
              </p>
            </div>
          ) : (
            cartItems.map((item) => (
              <DrawerItem
                key={item.id}
                item={item}
                updateQuantity={updateQuantity}
                removeFromCart={removeFromCart}
                toggleDrawer={toggleDrawer}
                setCurrentCategory={setCurrentCategory}
              />
            ))
          )}
        </div>

        {/* 하단 푸터 (메모 + 가격요약 + 버튼) */}
        {cartItems.length > 0 && (
          <div className="drawer-footer-complex">
            <div className="memo-section">
              <label>구매 사유 (필수)</label>
              <textarea
                placeholder="예: 신규 입사자 지급용, 부서 비품 교체 등"
                value={memo}
                onChange={(e) => setMemo(e.target.value)}
              ></textarea>
            </div>

            <div className="price-summary">
              <div className="summary-row">
                <span>공급가액</span>
                <span>{totalPrice.toLocaleString()}원</span>
              </div>
              <div className="summary-row">
                <span>부가세 (10%)</span>
                <span>{vat.toLocaleString()}원</span>
              </div>
              <div className="summary-row total">
                <span>최종 결재 금액</span>
                <span className="total-text">
                  {finalTotal.toLocaleString()}원
                </span>
              </div>
            </div>

            <button className="checkout-btn" onClick={handleCheckout}>
              결재 상신하기
            </button>
          </div>
        )}
      </div>
    </>
  );
}
