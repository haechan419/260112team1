import { useEffect, useState } from "react";
import { getReceipt, getReceiptImage } from "../../api/receiptApi";
import useCustomMove from "../../hooks/useCustomMove";
import FetchingModal from "../common/FetchingModal";
import "./ReceiptDetailComponent.css";

const initState = {
    id: 0,
    expenseId: 0,
    fileUrl: "",
    fileHash: "",
    uploadedByName: "",
    createdAt: null,
};

const ReceiptDetailComponent = ({ id }) => {
    const [receipt, setReceipt] = useState(initState);
    const [imageUrl, setImageUrl] = useState(null);
    const [fetching, setFetching] = useState(false);
    const [error, setError] = useState(null);

    const { moveToExpenseDetail, moveToExpenseList } = useCustomMove();

    useEffect(() => {
        // id가 없거나 유효하지 않으면 에러 표시
        if (!id || id === "undefined" || isNaN(parseInt(id))) {
            setError("영수증 ID가 올바르지 않습니다.");
            setFetching(false);
            return;
        }

        setFetching(true);
        setError(null);

        // 영수증 정보 조회
        getReceipt(id)
            .then((data) => {
                setReceipt(data);
                // 이미지 로드
                loadImage(id);
            })
            .catch((err) => {
                console.error("영수증 조회 실패:", err);
                setError("영수증을 불러올 수 없습니다. " + (err.response?.data?.message || err.message || ""));
            })
            .finally(() => {
                setFetching(false);
            });
    }, [id]);

    const loadImage = async (receiptId) => {
        try {
            // ✅ 수정: getReceiptImage가 이미 blob 데이터를 반환하므로 직접 사용
            const blobData = await getReceiptImage(receiptId);
            const url = URL.createObjectURL(blobData);
            setImageUrl(url);
        } catch (error) {
            console.error("영수증 이미지 로드 실패:", error);
        }
    };


    // 에러가 있거나 id가 없으면 에러 메시지 표시
    if (error || !id || id === "undefined") {
        return (
            <div className="border-2 border-sky-200 mt-10 m-2 p-4">
                <div className="text-center p-8">
                    <div className="text-red-500 text-xl font-bold mb-4">오류</div>
                    <p className="text-gray-600 mb-4">{error || "영수증 ID가 필요합니다."}</p>
                    <button
                        type="button"
                        className="rounded p-4 m-2 text-xl w-32 text-white bg-blue-500"
                        onClick={moveToExpenseList}
                    >
                        목록으로
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div style={{ padding: '20px', backgroundColor: '#f5f5f5', minHeight: '100vh' }}>
            {fetching ? <FetchingModal /> : <></>}

            {/* 페이지 헤더 */}
            <div style={{
                backgroundColor: 'white',
                padding: '20px',
                borderRadius: '4px',
                marginBottom: '16px',
                boxShadow: '0 1px 2px rgba(0, 0, 0, 0.1)'
            }}>
                <h1 style={{
                    fontSize: '20px',
                    fontWeight: 600,
                    color: '#1f2937',
                    margin: 0
                }}>
                    영수증 상세
                </h1>
            </div>

            {/* 영수증 이미지 카드 */}
            <div style={{
                backgroundColor: 'white',
                borderRadius: '4px',
                padding: '24px',
                boxShadow: '0 1px 2px rgba(0, 0, 0, 0.1)',
                marginBottom: '16px'
            }}>
                <h2 style={{
                    fontSize: '16px',
                    fontWeight: 600,
                    color: '#1f2937',
                    margin: '0 0 16px 0'
                }}>
                    영수증 이미지
                </h2>
                <div style={{
                    display: 'flex',
                    justifyContent: 'flex-start',
                    marginBottom: '24px'
                }}>
                    <div style={{
                        border: '1px solid #e5e7eb',
                        borderRadius: '4px',
                        padding: '16px',
                        backgroundColor: '#f9fafb',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'flex-start',
                        overflow: 'auto',
                        maxWidth: '100%'
                    }}>
                        {imageUrl ? (
                            <img
                                src={imageUrl}
                                alt="영수증"
                                style={{
                                    maxWidth: '400px',
                                    maxHeight: '600px',
                                    width: 'auto',
                                    height: 'auto',
                                    objectFit: 'contain'
                                }}
                            />
                        ) : (
                            <div style={{ color: '#6b7280', padding: '40px' }}>
                                이미지를 불러오는 중...
                            </div>
                        )}
                    </div>
                </div>

                {/* 영수증 정보 */}
                <div style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(2, 1fr)',
                    gap: '20px',
                    borderTop: '1px solid #e5e7eb',
                    paddingTop: '20px'
                }}>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                        <label style={{
                            fontSize: '12px',
                            fontWeight: 500,
                            color: '#6b7280'
                        }}>
                            업로드 일시
                        </label>
                        <span style={{ fontSize: '14px', color: '#1f2937' }}>
              {receipt.createdAt ? new Date(receipt.createdAt).toLocaleString("ko-KR") : "-"}
            </span>
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                        <label style={{
                            fontSize: '12px',
                            fontWeight: 500,
                            color: '#6b7280'
                        }}>
                            업로드자
                        </label>
                        <span style={{ fontSize: '14px', color: '#1f2937' }}>
              {receipt.uploadedByName || "-"}
            </span>
                    </div>
                    {receipt.fileHash && (
                        <div style={{
                            display: 'flex',
                            flexDirection: 'column',
                            gap: '8px',
                            gridColumn: '1 / -1'
                        }}>
                            <label style={{
                                fontSize: '12px',
                                fontWeight: 500,
                                color: '#6b7280'
                            }}>
                                파일 해시
                            </label>
                            <span style={{
                                fontSize: '12px',
                                color: '#1f2937',
                                fontFamily: 'monospace',
                                backgroundColor: '#f9fafb',
                                padding: '8px',
                                borderRadius: '4px',
                                wordBreak: 'break-all'
                            }}>
                {receipt.fileHash}
              </span>
                        </div>
                    )}
                </div>
            </div>

            {/* 액션 버튼 */}
            <div style={{
                display: 'flex',
                gap: '8px',
                justifyContent: 'flex-end'
            }}>
                {receipt.expenseId > 0 && (
                    <button
                        type="button"
                        onClick={() => moveToExpenseDetail(receipt.expenseId)}
                        style={{
                            padding: '10px 20px',
                            backgroundColor: '#6b7280',
                            color: 'white',
                            border: 'none',
                            borderRadius: '4px',
                            fontSize: '14px',
                            fontWeight: 500,
                            cursor: 'pointer',
                            transition: 'background-color 0.2s'
                        }}
                        onMouseOver={(e) => e.target.style.backgroundColor = '#4b5563'}
                        onMouseOut={(e) => e.target.style.backgroundColor = '#6b7280'}
                    >
                        지출 내역
                    </button>
                )}
                <button
                    type="button"
                    onClick={moveToExpenseList}
                    style={{
                        padding: '10px 20px',
                        backgroundColor: '#6b7280',
                        color: 'white',
                        border: 'none',
                        borderRadius: '4px',
                        fontSize: '14px',
                        fontWeight: 500,
                        cursor: 'pointer',
                        transition: 'background-color 0.2s'
                    }}
                    onMouseOver={(e) => e.target.style.backgroundColor = '#4b5563'}
                    onMouseOut={(e) => e.target.style.backgroundColor = '#6b7280'}
                >
                    목록
                </button>
            </div>
        </div>
    );
};

export default ReceiptDetailComponent;

