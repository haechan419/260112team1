// src/data/mockData.js

// 기본 템플릿 데이터
const baseProducts = [
    {
        name: "삼성 27인치 모니터",
        price: 250000,
        category: "전자기기",
        img: "https://placehold.co/300x200/2c3e50/ffffff?text=Monitor",
    },
    {
        name: "로지텍 MX Master 3S",
        price: 129000,
        category: "전자기기",
        img: "https://placehold.co/300x200/e74c3c/ffffff?text=Mouse",
    },
    {
        name: "Double A A4 용지",
        price: 28000,
        category: "사무용품",
        img: "https://placehold.co/300x200/f1c40f/333333?text=Paper",
    },
    {
        name: "시디즈 T50 의자",
        price: 340000,
        category: "가구",
        img: "https://placehold.co/300x200/3498db/ffffff?text=Chair",
    },
    {
        name: "맥심 모카골드",
        price: 25000,
        category: "탕비실",
        img: "https://placehold.co/300x200/e67e22/ffffff?text=Coffee",
    },
    {
        name: "3M 포스트잇",
        price: 5000,
        category: "사무용품",
        img: "https://placehold.co/300x200/2ecc71/ffffff?text=Post-it",
    },
];

// 100개 데이터 자동 생성기
export const mockProducts = Array.from({ length: 100 }).map((_, index) => {
    const base = baseProducts[index % baseProducts.length]; // 6개를 번갈아가며 사용
    return {
        id: index + 1,
        name: `${base.name} (${index + 1}호)`, // 이름 뒤에 번호 붙임
        price: base.price,
        category: base.category,
        stock: Math.floor(Math.random() * 50), // 재고 랜덤 (0~49)
        img: base.img,
    };
});
