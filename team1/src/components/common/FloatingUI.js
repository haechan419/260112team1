import React from "react";
import { useCart } from "../../context/CartContext";
import "../../styles/floating.css"; // 스타일 파일 (3단계에서 만듦)

export default function FloatingUI() {
  const { toastMessage, toggleDrawer, cartItems } = useCart();

  return (
    <>
      {/* 1. 토스트 메시지 (메시지가 있을 때만 뜸) */}
      {toastMessage && <div className="toast-message">{toastMessage}</div>}

      {/* 2. 플로팅 장바구니 버튼 (항상 우측 하단에 떠 있음) */}
      <button className="floating-cart-btn" onClick={toggleDrawer}>
        🛒
        {cartItems.length > 0 && (
          <span className="floating-badge">{cartItems.length}</span>
        )}
      </button>
    </>
  );
}
