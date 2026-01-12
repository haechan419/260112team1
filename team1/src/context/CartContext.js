import React, { createContext, useState, useContext } from "react";

const CartContext = createContext();

export const CartProvider = ({ children }) => {
    const [cartItems, setCartItems] = useState([]);
    const [isDrawerOpen, setIsDrawerOpen] = useState(false);
    const [favorites, setFavorites] = useState([]);
    const [currentCategory, setCurrentCategory] = useState("All");

    // 결재 요청 내역 (기존 유지)
    const [requests, setRequests] = useState([
        {
            id: "REQ-001",
            date: "2024-05-20",
            title: "삼성 모니터 외 2건",
            totalAmount: 1500000,
            status: "approved",
            memo: "신규 입사자용",
            rejectReason: "",
        },
        // ... 기존 데이터 유지
    ]);

    const addRequest = (newRequest) => {
        setRequests((prev) => [newRequest, ...prev]);
    };

    // ... (addToCart, updateQuantity 등 기존 함수 유지) ...
    const addToCart = (product) => {
        setCartItems((prev) => {
            const existing = prev.find((item) => item.id === product.id);
            if (existing) {
                return prev.map((item) =>
                    item.id === product.id
                        ? { ...item, quantity: item.quantity + 1 }
                        : item
                );
            }
            return [...prev, { ...product, quantity: 1 }];
        });
    };

    const updateQuantity = (id, newQty) => {
        if (newQty < 1) return;
        setCartItems((prev) =>
            prev.map((item) =>
                item.id === id ? { ...item, quantity: newQty } : item
            )
        );
    };

    const removeFromCart = (id) => {
        setCartItems((prev) => prev.filter((item) => item.id !== id));
    };

    const toggleFavorite = (productId) => {
        setFavorites((prev) =>
            prev.includes(productId)
                ? prev.filter((id) => id !== productId)
                : [...prev, productId]
        );
    };

    // ✨ [핵심 수정] 서랍 제어 함수들 명확하게 분리
    const toggleDrawer = () => setIsDrawerOpen((prev) => !prev); // 기존 토글
    const openDrawer = () => setIsDrawerOpen(true); // 👈 [NEW] 무조건 열기
    const closeDrawer = () => setIsDrawerOpen(false); // 👈 [NEW] 무조건 닫기

    const totalPrice = cartItems.reduce(
        (acc, item) => acc + item.price * item.quantity,
        0
    );

    return (
        <CartContext.Provider
            value={{
                cartItems,
                addToCart,
                updateQuantity,
                removeFromCart,

                // ✨ 서랍 관련 상태 및 함수
                isDrawerOpen,
                toggleDrawer,
                openDrawer, // 👈 내보내기
                closeDrawer, // 👈 내보내기

                totalPrice,
                favorites,
                toggleFavorite,
                currentCategory,
                setCurrentCategory,
                requests,
                addRequest,
            }}
        >
            {children}
        </CartContext.Provider>
    );
};

export const useCart = () => useContext(CartContext);
