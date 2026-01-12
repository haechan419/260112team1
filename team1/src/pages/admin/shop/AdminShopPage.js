import React, { useEffect, useState, useRef, useCallback } from "react";
import AppLayout from "../../../components/layout/AppLayout";
import {
  getList,
  postAdd,
  putOne,
  deleteOne,
  putOrder,
  API_SERVER_HOST,
} from "../../../api/productApi";

//  초기 상태에 status 추가 (기본값 true: 판매중)
const productInitState = {
  pname: "",
  price: 0,
  pdesc: "",
  category: "사무용품",
  stockQuantity: 100,
  status: true, // 판매 상태 추가
  files: [],
};

const CATEGORIES = ["All", "사무용품", "전자기기", "탕비실", "가구"];

const AdminShopPage = () => {
  const [allProducts, setAllProducts] = useState([]);
  const [currentCategory, setCurrentCategory] = useState("All");

  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 15;

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [currentProduct, setCurrentProduct] = useState({ ...productInitState });
  const [mode, setMode] = useState("ADD");

  const dragItem = useRef();
  const dragOverItem = useRef();
  const [isOrderChanged, setIsOrderChanged] = useState(false);
  const [selectedIds, setSelectedIds] = useState([]);

  const uploadRef = useRef();

  const fetchData = useCallback((category) => {
    // 혹시 모르니 size를 50으로 살짝 줄여서 요청
    getList({ page: 1, size: 50, category: category })
      .then((data) => {
        console.log("🔥 관리자 페이지 데이터 도착:", data);

        const resultList = data.dtoList || data.content || [];

        if (resultList.length === 0) {
          console.warn(
            "⚠️ 데이터 배열이 비어있습니다! (DB에 데이터가 없거나, 페이지 번호 문제)"
          );
        }

        setAllProducts(resultList);
        setIsOrderChanged(false);
        setSelectedIds([]);
        setCurrentPage(1);
      })
      .catch((err) => {
        console.error("🚨 데이터 가져오기 실패:", err);
      });
  }, []);

  useEffect(() => {
    fetchData(currentCategory);
  }, [currentCategory, fetchData]);

  const indexOfLastItem = currentPage * itemsPerPage;
  const indexOfFirstItem = indexOfLastItem - itemsPerPage;
  const currentItems = allProducts.slice(indexOfFirstItem, indexOfLastItem);
  const totalPages = Math.ceil(allProducts.length / itemsPerPage);

  const handlePageChange = (pageNum) => {
    setCurrentPage(pageNum);
  };

  const dragStart = (e, index) => {
    const globalIndex = indexOfFirstItem + index;
    dragItem.current = globalIndex;
    e.target.style.opacity = "0.4";
  };
  const dragEnter = (e, index) => {
    const globalIndex = indexOfFirstItem + index;
    dragOverItem.current = globalIndex;
  };
  const dragEnd = (e) => {
    e.target.style.opacity = "1";

    // 1. 시작점이나 도착점이 없으면 그냥 취소 (원위치)
    if (dragItem.current === null || dragOverItem.current === null) {
      return;
    }

    // 2. 제자리에 놓았으면 취소
    if (dragItem.current === dragOverItem.current) {
      return;
    }

    // 3. 배열 재정렬 로직
    const copyList = [...allProducts];

    // 이동할 아이템 내용 꺼내기
    const dragItemContent = copyList[dragItem.current];

    // 기존 위치에서 삭제
    copyList.splice(dragItem.current, 1);

    // 새 위치에 삽입
    copyList.splice(dragOverItem.current, 0, dragItemContent);

    // 참조값 초기화
    dragItem.current = null;
    dragOverItem.current = null;

    // ✨ 화면 업데이트 (이게 되어야 안 튕김)
    setAllProducts(copyList);

    // "저장 버튼" 활성화
    setIsOrderChanged(true);
  };

  const handleApplyOrder = () => {
    if (!isOrderChanged) return;
    const pnoList = allProducts.map((p) => p.pno);

    putOrder(pnoList)
      .then(() => {
        alert("✅ 순서가 저장되었습니다!");
        setIsOrderChanged(false);
        fetchData(currentCategory);
      })
      .catch(() => alert("순서 저장 실패"));
  };

  // 입력 핸들러 (checkbox 처리 추가)
  const handleChange = (e) => {
    const value =
      e.target.type === "checkbox" ? e.target.checked : e.target.value;
    setCurrentProduct({ ...currentProduct, [e.target.name]: value });
  };

  const handleSave = () => {
    const formData = new FormData();
    formData.append("pname", currentProduct.pname);
    formData.append("pdesc", currentProduct.pdesc);
    formData.append("price", currentProduct.price);
    formData.append("category", currentProduct.category);
    formData.append("stockQuantity", currentProduct.stockQuantity);

    // status 값 전송 (boolean -> String 변환 필요할 수 있음)
    formData.append("status", currentProduct.status);

    if (uploadRef.current?.files.length > 0) {
      for (let i = 0; i < uploadRef.current.files.length; i++)
        formData.append("files", uploadRef.current.files[i]);
    }

    const apiCall =
      mode === "ADD" ? postAdd(formData) : putOne(currentProduct.pno, formData);
    apiCall.then(() => {
      alert("저장 완료");
      setIsModalOpen(false);
      fetchData(currentCategory);
    });
  };

  const handleDelete = (pno) => {
    if (window.confirm("삭제하시겠습니까?")) {
      deleteOne(pno).then(() => {
        fetchData(currentCategory);
      });
    }
  };

  const openModal = (product = null) => {
    if (product) {
      setMode("EDIT");
      // status가 없는 경우 기본값 true 처리
      setCurrentProduct({ ...product, status: product.status !== false });
    } else {
      setMode("ADD");
      setCurrentProduct({ ...productInitState });
    }
    setIsModalOpen(true);
  };

  const toggleSelect = (pno) => {
    if (selectedIds.includes(pno))
      setSelectedIds(selectedIds.filter((id) => id !== pno));
    else setSelectedIds([...selectedIds, pno]);
  };

  const handleBatchDelete = () => {
    if (window.confirm(`${selectedIds.length}개 삭제?`)) {
      Promise.all(selectedIds.map((pno) => deleteOne(pno))).then(() => {
        alert("삭제 완료");
        fetchData(currentCategory);
      });
    }
  };

  return (
    <AppLayout>
      <div style={{ padding: "30px", maxWidth: "1600px", margin: "0 auto" }}>
        <div style={headerContainerStyle}>
          <div>
            <h2
              style={{
                fontSize: "26px",
                fontWeight: "800",
                margin: 0,
                color: "#2c3e50",
              }}
            >
              🎨 상품 진열 관리
            </h2>
            <div style={{ marginTop: "15px", display: "flex", gap: "10px" }}>
              {CATEGORIES.map((cat) => (
                <button
                  key={cat}
                  onClick={() => setCurrentCategory(cat)}
                  style={{
                    ...tabStyle,
                    backgroundColor: currentCategory === cat ? "#333" : "#eee",
                    color: currentCategory === cat ? "white" : "#333",
                  }}
                >
                  {cat}
                </button>
              ))}
            </div>
          </div>

          <div style={{ display: "flex", gap: "12px", alignItems: "flex-end" }}>
            <button
              onClick={handleApplyOrder}
              disabled={!isOrderChanged}
              style={
                isOrderChanged ? btnStyle.applyActive : btnStyle.applyDisabled
              }
            >
              {isOrderChanged ? "💾 순서 DB 저장" : "순서 변경 없음"}
            </button>
            <button onClick={() => openModal(null)} style={btnStyle.add}>
              + 상품 등록
            </button>
          </div>
        </div>

        <div style={gridContainerStyle}>
          {currentItems.map((product, index) => (
            <div
              key={product.pno}
              draggable
              onDragStart={(e) => dragStart(e, index)}
              onDragEnter={(e) => dragEnter(e, index)}
              onDragOver={(e) => {
                e.preventDefault();
                dragEnter(e, index);
              }}
              onDragEnd={dragEnd}
              style={{
                ...cardStyle,
                border: selectedIds.includes(product.pno)
                  ? "2px solid #3498db"
                  : "1px solid #eee",
                backgroundColor: selectedIds.includes(product.pno)
                  ? "#fbfdff"
                  : "white",
                // 판매 중지된 상품 흐리게 표시
                opacity: product.status ? 1 : 0.6,
              }}
            >
              <div style={imageContainerStyle}>
                {/* 안전한 이미지 접근 (?. 사용) */}
                {product.uploadFileNames &&
                  product.uploadFileNames.length > 0 ? (
                  <img
                    src={`${API_SERVER_HOST}/api/products/view/s_${product.uploadFileNames[0]}`}
                    alt={product.pname}
                    style={imageStyle}
                  />
                ) : (
                  <div style={noImageStyle}>No Image</div>
                )}
              </div>

              <div style={infoContainerStyle}>
                <div style={categoryBadgeStyle}>
                  {product.category}
                  {/* ✨ [수정 6] 상태 뱃지 표시 */}
                  {!product.status && (
                    <span style={{ color: "red", marginLeft: "5px" }}>
                      (판매중지)
                    </span>
                  )}
                </div>
                <div style={productNameStyle}>{product.pname}</div>
                <div style={priceRowStyle}>
                  <span style={priceStyle}>
                    {product.price.toLocaleString()}원
                  </span>
                  <span
                    style={{
                      fontSize: "12px",
                      color: product.stockQuantity < 10 ? "#e74c3c" : "#2ecc71",
                    }}
                  >
                    재고 {product.stockQuantity}
                  </span>
                </div>
              </div>

              <div style={actionBarContainerStyle}>
                <button
                  onClick={() => openModal(product)}
                  style={actionBtnStyle.edit}
                >
                  ✏️ 수정
                </button>
                <div
                  style={{ width: "1px", height: "20px", background: "#eee" }}
                ></div>
                <button
                  onClick={() => handleDelete(product.pno)}
                  style={actionBtnStyle.delete}
                >
                  🗑️ 삭제
                </button>
              </div>
            </div>
          ))}

          {allProducts.length === 0 && (
            <div
              style={{
                gridColumn: "1 / -1",
                textAlign: "center",
                padding: "50px",
                color: "#aaa",
              }}
            >
              등록된 상품이 없습니다.
            </div>
          )}
        </div>

        {totalPages > 0 && (
          <div
            style={{
              display: "flex",
              justifyContent: "center",
              marginTop: "40px",
              gap: "5px",
            }}
          >
            {Array.from({ length: totalPages }, (_, i) => i + 1).map(
              (pageNum) => (
                <button
                  key={pageNum}
                  onClick={() => handlePageChange(pageNum)}
                  style={{
                    padding: "10px 16px",
                    border: "none",
                    borderRadius: "50%",
                    cursor: "pointer",
                    backgroundColor:
                      currentPage === pageNum ? "#2c3e50" : "white",
                    color: currentPage === pageNum ? "white" : "#333",
                    fontWeight: "bold",
                    boxShadow: "0 2px 5px rgba(0,0,0,0.1)",
                    transition: "all 0.2s",
                  }}
                >
                  {pageNum}
                </button>
              )
            )}
          </div>
        )}

        {isModalOpen && (
          <div style={modalOverlayStyle}>
            <div style={modalContentStyle}>
              <h3>{mode === "ADD" ? "상품 등록" : "상품 수정"}</h3>

              {/*  판매 상태 체크박스 추가 */}
              <div
                style={{
                  ...inputGroupStyle,
                  display: "flex",
                  alignItems: "center",
                  gap: "10px",
                }}
              >
                <label style={{ margin: 0 }}>판매 상태:</label>
                <label
                  style={{
                    cursor: "pointer",
                    display: "flex",
                    alignItems: "center",
                  }}
                >
                  <input
                    type="checkbox"
                    name="status"
                    checked={currentProduct.status}
                    onChange={handleChange}
                    style={{
                      width: "20px",
                      height: "20px",
                      marginRight: "5px",
                    }}
                  />
                  {currentProduct.status ? "판매 중" : "판매 중지"}
                </label>
              </div>

              <div style={inputGroupStyle}>
                <label>카테고리</label>
                <select
                  name="category"
                  value={currentProduct.category}
                  onChange={handleChange}
                  style={inputStyle}
                >
                  {CATEGORIES.filter((c) => c !== "All").map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                </select>
              </div>
              <div style={inputGroupStyle}>
                <label>상품명</label>
                <input
                  name="pname"
                  value={currentProduct.pname}
                  onChange={handleChange}
                  style={inputStyle}
                />
              </div>
              <div style={{ display: "flex", gap: "10px" }}>
                <div style={{ flex: 1 }}>
                  <label>가격</label>
                  <input
                    type="number"
                    name="price"
                    value={currentProduct.price}
                    onChange={handleChange}
                    style={inputStyle}
                  />
                </div>
                <div style={{ flex: 1 }}>
                  <label>재고</label>
                  <input
                    type="number"
                    name="stockQuantity"
                    value={currentProduct.stockQuantity}
                    onChange={handleChange}
                    style={inputStyle}
                  />
                </div>
              </div>
              <div style={inputGroupStyle}>
                <label>설명</label>
                <textarea
                  name="pdesc"
                  value={currentProduct.pdesc}
                  onChange={handleChange}
                  style={inputStyle}
                />
              </div>
              <div style={inputGroupStyle}>
                <label>이미지</label>
                <input type="file" ref={uploadRef} multiple />
              </div>
              <div
                style={{
                  display: "flex",
                  justifyContent: "flex-end",
                  gap: "10px",
                  marginTop: "20px",
                }}
              >
                <button
                  onClick={() => setIsModalOpen(false)}
                  style={btnStyle.cancel}
                >
                  취소
                </button>
                <button onClick={handleSave} style={btnStyle.save}>
                  저장
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </AppLayout>
  );
};

// 스타일 (기존과 동일)
const headerContainerStyle = {
  display: "flex",
  justifyContent: "space-between",
  marginBottom: "30px",
  alignItems: "flex-end",
  paddingBottom: "20px",
  borderBottom: "1px solid #eee",
};
const tabStyle = {
  padding: "8px 16px",
  borderRadius: "20px",
  border: "none",
  cursor: "pointer",
  fontWeight: "bold",
  fontSize: "14px",
};
const gridContainerStyle = {
  display: "grid",
  gridTemplateColumns: "repeat(auto-fill, minmax(240px, 1fr))",
  gap: "25px",
};
const cardStyle = {
  backgroundColor: "white",
  borderRadius: "12px",
  boxShadow: "0 4px 12px rgba(0,0,0,0.05)",
  overflow: "hidden",
  cursor: "grab",
  transition: "transform 0.2s",
  display: "flex",
  flexDirection: "column",
  justifyContent: "space-between",
};
const imageContainerStyle = {
  width: "100%",
  height: "180px",
  backgroundColor: "#f8f9fa",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  overflow: "hidden",
};
const imageStyle = { width: "100%", height: "100%", objectFit: "cover" };
const noImageStyle = { color: "#ccc" };
const infoContainerStyle = { padding: "15px", flex: 1 };
const categoryBadgeStyle = {
  fontSize: "11px",
  color: "#888",
  textTransform: "uppercase",
};
const productNameStyle = {
  fontSize: "16px",
  fontWeight: "bold",
  color: "#333",
  marginBottom: "5px",
  overflow: "hidden",
  textOverflow: "ellipsis",
  whiteSpace: "nowrap",
};
const priceRowStyle = {
  display: "flex",
  justifyContent: "space-between",
  alignItems: "center",
  marginTop: "10px",
};
const priceStyle = { fontSize: "18px", fontWeight: "800", color: "#2c3e50" };
const actionBarContainerStyle = {
  display: "flex",
  borderTop: "1px solid #f0f0f0",
  backgroundColor: "#fff",
};
const actionBtnStyle = {
  edit: {
    flex: 1,
    padding: "12px",
    border: "none",
    background: "transparent",
    cursor: "pointer",
    color: "#555",
  },
  delete: {
    flex: 1,
    padding: "12px",
    border: "none",
    background: "transparent",
    cursor: "pointer",
    color: "#e74c3c",
  },
};
const btnStyle = {
  add: {
    padding: "10px 20px",
    background: "#2c3e50",
    color: "white",
    border: "none",
    borderRadius: "8px",
    cursor: "pointer",
  },
  applyActive: {
    padding: "10px 20px",
    background: "#3498db",
    color: "white",
    border: "none",
    borderRadius: "8px",
    cursor: "pointer",
  },
  applyDisabled: {
    padding: "10px 20px",
    background: "#ecf0f1",
    color: "#bdc3c7",
    border: "none",
    borderRadius: "8px",
    cursor: "default",
  },
  save: {
    padding: "10px 20px",
    background: "#2ecc71",
    color: "white",
    border: "none",
    borderRadius: "5px",
  },
  cancel: {
    padding: "10px 20px",
    background: "#eee",
    border: "none",
    borderRadius: "5px",
  },
};
const inputGroupStyle = { marginBottom: "15px" };
const inputStyle = {
  width: "100%",
  padding: "8px",
  border: "1px solid #ddd",
  borderRadius: "5px",
};
const modalOverlayStyle = {
  position: "fixed",
  top: 0,
  left: 0,
  width: "100%",
  height: "100%",
  background: "rgba(0,0,0,0.5)",
  display: "flex",
  justifyContent: "center",
  alignItems: "center",
  zIndex: 1200,
};
const modalContentStyle = {
  background: "white",
  padding: "30px",
  borderRadius: "12px",
  width: "450px",
};

export default AdminShopPage;
