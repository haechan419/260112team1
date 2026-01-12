import React, { useState, useEffect } from "react";

const OcrResultModal = ({ isOpen, onClose, extraction, onApply, onCancel }) => {
    const [editableData, setEditableData] = useState({
        extractedDate: "",
        extractedMerchant: "",
        extractedAmount: "",
        extractedCategory: "",
        extractedDescription: "",
    });

    useEffect(() => {
        if (extraction) {
            setEditableData({
                extractedDate: extraction.extractedDate || "",
                extractedMerchant: extraction.extractedMerchant || "",
                extractedAmount: extraction.extractedAmount || "",
                extractedCategory: extraction.extractedCategory || "",
                extractedDescription: extraction.extractedDescription || "",
            });
        }
    }, [extraction]);

    if (!isOpen) return null;

    const handleChange = (field, value) => {
        setEditableData({
            ...editableData,
            [field]: value,
        });
    };

    const handleApply = () => {
        if (onApply) {
            // 수정된 데이터를 포함한 extraction 객체 전달
            const modifiedExtraction = {
                ...extraction,
                ...editableData,
                extractedAmount: editableData.extractedAmount ? parseInt(editableData.extractedAmount) : null,
                extractedDescription: editableData.extractedDescription || null,
            };
            onApply(modifiedExtraction);
        }
        onClose();
    };

    const handleCancel = () => {
        if (onCancel) {
            onCancel();
        }
        onClose();
    };

    return (
        <div
            className="fixed top-0 left-0 z-[1055] flex h-full w-full justify-center bg-black bg-opacity-20"
            onClick={onClose}
        >
            <div
                className="absolute bg-white shadow dark:bg-gray-700 opacity-100 w-1/2 rounded mt-10 mb-10 px-6 min-w-[600px] max-h-[90vh] overflow-y-auto"
                onClick={(e) => e.stopPropagation()}
            >
                <div className="justify-center bg-warning-400 mt-6 mb-6 text-2xl border-b-4 border-gray-500">
                    📄 OCR 인식 결과
                </div>

                <div className="pt-4 pb-4">
                    {extraction ? (
                        <>
                            <div className="mb-4 text-sm">
                                <div className="text-gray-600 mb-2">인식 모델: {extraction.modelName || "-"}</div>
                                <div className="text-gray-600">신뢰도: {extraction.confidence
                                    ? `${(extraction.confidence * 100).toFixed(1)}%`
                                    : "-"}</div>
                            </div>

                            <div className="mb-4">
                                <h3 className="text-lg font-bold mb-2">추출된 정보 (수정 가능)</h3>
                                <div className="grid grid-cols-2 gap-4">
                                    <div>
                                        <label className="block text-sm font-medium mb-1">지출 일자</label>
                                        <input
                                            type="date"
                                            className="w-full p-2 border border-gray-300 rounded"
                                            value={editableData.extractedDate || ""}
                                            onChange={(e) => handleChange("extractedDate", e.target.value)}
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium mb-1">가맹점명</label>
                                        <input
                                            type="text"
                                            className="w-full p-2 border border-gray-300 rounded"
                                            value={editableData.extractedMerchant || ""}
                                            onChange={(e) => handleChange("extractedMerchant", e.target.value)}
                                            placeholder="가맹점명을 입력하세요"
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium mb-1">금액</label>
                                        <input
                                            type="number"
                                            className="w-full p-2 border border-gray-300 rounded"
                                            value={editableData.extractedAmount || ""}
                                            onChange={(e) => handleChange("extractedAmount", e.target.value)}
                                            placeholder="금액을 입력하세요"
                                            min="0"
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium mb-1">카테고리</label>
                                        <select
                                            className="w-full p-2 border border-gray-300 rounded"
                                            value={editableData.extractedCategory || ""}
                                            onChange={(e) => handleChange("extractedCategory", e.target.value)}
                                        >
                                            <option value="">선택하세요</option>
                                            <option value="식비">식비</option>
                                            <option value="교통비">교통비</option>
                                            <option value="비품">비품</option>
                                            <option value="기타">기타</option>
                                        </select>
                                    </div>
                                </div>
                                <div className="mt-4">
                                    <label className="block text-sm font-medium mb-1">상세내용</label>
                                    <textarea
                                        className="w-full p-2 border border-gray-300 rounded"
                                        value={editableData.extractedDescription || ""}
                                        onChange={(e) => handleChange("extractedDescription", e.target.value)}
                                        placeholder="구매한 상품명 목록을 입력하세요"
                                        rows={3}
                                    />
                                </div>
                            </div>

                            {extraction.confidence && extraction.confidence < 0.7 && (
                                <div className="mb-4 p-2 bg-yellow-100 text-yellow-800 rounded text-sm">
                                    ⚠️ 신뢰도가 낮습니다. 추출된 정보를 확인 후 수정해주세요.
                                </div>
                            )}

                            {/* AI 검증 결과 (준비 중) */}
                            <div className="mb-4 p-3 bg-gray-50 border border-gray-200 rounded text-sm">
                                <div className="text-gray-600 font-medium mb-2">🤖 AI 검증 결과 (준비 중)</div>
                                <div className="text-gray-500 text-xs">
                                    AI가 OCR 결과를 검증하여 오류나 누락된 정보를 자동으로 감지합니다.
                                </div>
                            </div>
                        </>
                    ) : (
                        <div className="text-gray-600">OCR 결과를 불러올 수 없습니다.</div>
                    )}
                </div>
                <div className="justify-end flex">
                    <button
                        className="rounded bg-gray-500 mt-4 mb-4 px-6 pt-4 pb-4 text-lg text-white mr-2"
                        onClick={handleCancel}
                    >
                        취소
                    </button>
                    <button
                        className="rounded bg-blue-500 mt-4 mb-4 px-6 pt-4 pb-4 text-lg text-white"
                        onClick={handleApply}
                        disabled={!extraction}
                    >
                        결과 적용
                    </button>
                </div>
            </div>
        </div>
    );
};

export default OcrResultModal;

