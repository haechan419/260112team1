import React, { useEffect, useState, useCallback } from "react";
import AppLayout from "../../components/layout/AppLayout";
import "../../styles/shop.css";
import { useCart } from "../../context/CartContext";
import CartDrawer from "../../components/common/CartDrawer";
import FloatingUI from "../../components/common/FloatingUI";
import { getList, API_SERVER_HOST } from "../../api/productApi";

// ---------------------------------------------------------
// 1. 사이드바 아이템
// ---------------------------------------------------------
const SidebarItem = ({ item, updateQuantity, removeFromCart }) => {
  const [inputValue, setInputValue] = useState(item.quantity);
  useEffect(() => {
    setInputValue(item.quantity);
  }, [item.quantity]);
  const handleChange = (e) => {
    let val = e.target.value;
    if (val.length > 2) val = val.slice(0, 2);
    setInputValue(val);
    const numVal = parseInt(val);
    if (!isNaN(numVal) && numVal >= 1) updateQuantity(item.id, numVal);
  };
  return (
    <div className="sidebar-item">
      <div style={{ flex: 1 }}>
        <div className="sidebar-item-name">{item.name}</div>
        <div style={{ fontSize: "12px", color: "#666" }}>
          {item.price.toLocaleString()}원
        </div>
      </div>
      <div className="qty-control">
        <button
          onClick={() => updateQuantity(item.id, item.quantity - 1)}
          style={{ cursor: "pointer", padding: "2px 6px" }}
        >
          -
        </button>
        <input
          type="number"
          className="qty-input"
          value={inputValue}
          onChange={handleChange}
        />
        <button
          onClick={() => updateQuantity(item.id, item.quantity + 1)}
          style={{ cursor: "pointer", padding: "2px 6px" }}
        >
          +
        </button>
        <button
          onClick={() => removeFromCart(item.id)}
          style={{
            color: "red",
            border: "none",
            background: "none",
            cursor: "pointer",
            marginLeft: "2px",
          }}
        >
          x
        </button>
      </div>
    </div>
  );
};

// ---------------------------------------------------------
// 2. 메인 ShopPage
// ---------------------------------------------------------
export default function ShopPage() {
  const {
    addToCart,
    cartItems,
    updateQuantity,
    removeFromCart,
    totalPrice,
    favorites,
    toggleFavorite,
    currentCategory,
    setCurrentCategory,
    openDrawer,
  } = useCart();

  const [allProducts, setAllProducts] = useState([]);
  const [currentProducts, setCurrentProducts] = useState([]);
  const [loading, setLoading] = useState(false);

  // 페이징 상태
  const [currentPage, setCurrentPage] = useState(1);

  // ✨ [수정 1] 관리자 페이지와 똑같이 15개씩 설정!
  const itemsPerPage = 15;

  // 데이터 가져오기 (Client-Side Pagination)
  const fetchData = useCallback(async (category) => {
    setLoading(true);
    try {
      const reqCategory = category === "Favorites" ? "All" : category;
      // 넉넉하게 100개 가져옴
      const data = await getList({ page: 1, size: 100, category: reqCategory });
      const resultList = data.dtoList || data.content || [];

      setAllProducts(resultList);
      setCurrentPage(1);
    } catch (err) {
      console.error("🚨 로딩 실패:", err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData(currentCategory);
  }, [currentCategory, fetchData]);

  // 페이지 자르기 로직
  useEffect(() => {
    let targetList = allProducts;
    if (currentCategory === "Favorites") {
      targetList = allProducts.filter((p) => favorites.includes(p.pno));
    }

    const indexOfLastItem = currentPage * itemsPerPage;
    const indexOfFirstItem = indexOfLastItem - itemsPerPage;
    const slicedItems = targetList.slice(indexOfFirstItem, indexOfLastItem);

    setCurrentProducts(slicedItems);
  }, [currentPage, allProducts, currentCategory, favorites]);

  // 총 페이지 수 계산
  let targetListLength =
    currentCategory === "Favorites"
      ? allProducts.filter((p) => favorites.includes(p.pno)).length
      : allProducts.length;
  const totalPages = Math.ceil(targetListLength / itemsPerPage);

  const handleAddToCart = (product) => {
    if (product.stockQuantity <= 0) {
      alert("품절된 상품입니다.");
      return;
    }
    const imageUrl =
      product.uploadFileNames && product.uploadFileNames.length > 0
        ? `${API_SERVER_HOST}/api/products/view/s_${product.uploadFileNames[0]}`
        : "https://via.placeholder.com/150";
    addToCart({
      id: product.pno,
      name: product.pname,
      price: product.price,
      img: imageUrl,
      category: product.category,
      quantity: 1,
    });
  };

  const handleCheckout = () => {
    if (cartItems.length === 0) return alert("장바구니가 비어있습니다!");
    openDrawer();
  };

  return (
    <AppLayout>
      <CartDrawer />
      <FloatingUI />

      <div className="page-header">
        <h2 className="page-title">📦 비품 구매</h2>
        <p className="text-gray">원하는 비품을 카테고리별로 확인하세요.</p>
      </div>

      <div className="shop-container">
        <div className="shop-main">
          {/* 카테고리 탭 */}
          <div className="shop-header">
            <div className="shop-filter">
              {[
                "All",
                "Favorites",
                "사무용품",
                "전자기기",
                "탕비실",
                "가구",
              ].map((cat) => (
                <button
                  key={cat}
                  className={`filter-btn ${currentCategory === cat ? "active" : ""
                    }`}
                  onClick={() => setCurrentCategory(cat)}
                  style={
                    cat === "Favorites"
                      ? { color: "#f1c40f", borderColor: "#f1c40f" }
                      : {}
                  }
                >
                  {cat === "Favorites" ? "★ 즐겨찾기" : cat}
                </button>
              ))}
            </div>
          </div>

          {/* 목록 */}
          {loading ? (
            <div
              style={{
                textAlign: "center",
                padding: "80px",
                color: "#666",
                fontSize: "18px",
              }}
            >
              ⏳ 상품을 불러오는 중입니다...
            </div>
          ) : (
            <>
              <div className="product-grid">
                {currentProducts.length === 0 ? (
                  <div
                    style={{
                      gridColumn: "1 / -1",
                      textAlign: "center",
                      padding: "50px",
                      color: "#999",
                    }}
                  >
                    등록된 상품이 없습니다.
                  </div>
                ) : (
                  currentProducts.map((product) => {
                    const isFav = favorites.includes(product.pno);
                    const imageUrl =
                      product.uploadFileNames &&
                        product.uploadFileNames.length > 0
                        ? `${API_SERVER_HOST}/api/products/view/s_${product.uploadFileNames[0]}`
                        : "https://via.placeholder.com/150";

                    return (
                      <div
                        key={product.pno}
                        className="product-card"
                        style={{ position: "relative" }}
                      >
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            toggleFavorite(product.pno);
                          }}
                          style={{
                            position: "absolute",
                            top: "10px",
                            right: "10px",
                            background: "white",
                            border: "1px solid #ddd",
                            borderRadius: "50%",
                            width: "32px",
                            height: "32px",
                            cursor: "pointer",
                            fontSize: "18px",
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            color: isFav ? "#f1c40f" : "#ddd",
                            zIndex: 5,
                          }}
                        >
                          ★
                        </button>
                        <div style={{ position: "relative" }}>
                          <img
                            src={imageUrl}
                            alt={product.pname}
                            className="card-img"
                          />
                          {product.stockQuantity <= 0 && (
                            <div
                              style={{
                                position: "absolute",
                                top: 0,
                                left: 0,
                                width: "100%",
                                height: "100%",
                                backgroundColor: "rgba(0,0,0,0.6)",
                                color: "white",
                                display: "flex",
                                justifyContent: "center",
                                alignItems: "center",
                                fontSize: "18px",
                                fontWeight: "bold",
                              }}
                            >
                              품절
                            </div>
                          )}
                        </div>
                        <div className="card-body">
                          <span className="card-category">
                            {product.category}
                          </span>
                          <div className="card-title">{product.pname}</div>
                          <div className="card-price">
                            {product.price.toLocaleString()}원
                          </div>
                          <div
                            style={{
                              fontSize: "12px",
                              color:
                                product.stockQuantity < 10
                                  ? "#e74c3c"
                                  : "#2ecc71",
                              marginBottom: "10px",
                              fontWeight: "bold",
                            }}
                          >
                            재고: {product.stockQuantity}개
                          </div>
                          <div className="card-footer">
                            <button
                              className="add-cart-btn"
                              onClick={() => handleAddToCart(product)}
                              disabled={product.stockQuantity <= 0}
                              style={{
                                backgroundColor:
                                  product.stockQuantity > 0
                                    ? "#2c3e50"
                                    : "#bdc3c7",
                                cursor:
                                  product.stockQuantity > 0
                                    ? "pointer"
                                    : "not-allowed",
                              }}
                            >
                              {product.stockQuantity > 0 ? "담기" : "품절"}
                            </button>
                          </div>
                        </div>
                      </div>
                    );
                  })
                )}
              </div>

              {/* ✨ [수정 2] 관리자 페이지와 똑같은 동그란 버튼 디자인 적용 */}
              {totalPages > 0 && (
                <div
                  className="pagination-container"
                  style={{
                    display: "flex",
                    justifyContent: "center",
                    gap: "8px",
                    marginTop: "40px",
                    marginBottom: "40px",
                  }}
                >
                  {Array.from({ length: totalPages }, (_, i) => i + 1).map(
                    (pageNum) => (
                      <button
                        key={pageNum}
                        onClick={() => setCurrentPage(pageNum)}
                        style={{
                          width: "40px", // 너비 고정
                          height: "40px", // 높이 고정 (정사각형/원형 유지)
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          border: "none",
                          borderRadius: "50%", // ✨ 완전한 원형 (관리자 페이지 스타일)
                          cursor: "pointer",
                          backgroundColor:
                            currentPage === pageNum ? "#2c3e50" : "white",
                          color: currentPage === pageNum ? "white" : "#333",
                          fontWeight: "bold",
                          fontSize: "14px",
                          boxShadow: "0 2px 5px rgba(0,0,0,0.1)", // 살짝 그림자 추가
                          transition: "all 0.2s",
                        }}
                      >
                        {pageNum}
                      </button>
                    )
                  )}
                </div>
              )}
            </>
          )}
        </div>

        {/* 사이드바 */}
        <aside className="shop-sidebar">
          <div className="sidebar-title">
            장바구니 현황 ({cartItems.length})
          </div>
          <div className="sidebar-list">
            {cartItems.length === 0 ? (
              <div
                style={{
                  color: "#999",
                  textAlign: "center",
                  marginTop: "50px",
                }}
              >
                텅 비었습니다.
                <br />
                왼쪽에서 담아보세요!
              </div>
            ) : (
              cartItems.map((item) => (
                <SidebarItem
                  key={item.id}
                  item={item}
                  updateQuantity={updateQuantity}
                  removeFromCart={removeFromCart}
                />
              ))
            )}
          </div>
          <div className="sidebar-footer">
            <div className="sidebar-total">
              <span>합계</span>
              <span>{totalPrice.toLocaleString()}원</span>
            </div>
            <button className="sidebar-checkout-btn" onClick={handleCheckout}>
              결제 요청하기
            </button>
          </div>
        </aside>
      </div>
    </AppLayout>
  );
}
