import React, { useState } from "react";

const ReceiptVerification = ({ receiptId, onVerify }) => {
  const [action, setAction] = useState("APPROVE");
  const [verifiedMerchant, setVerifiedMerchant] = useState("");
  const [verifiedAmount, setVerifiedAmount] = useState(0);
  const [verifiedCategory, setVerifiedCategory] = useState("");
  const [reason, setReason] = useState("");

  const handleSubmit = (e) => {
    e.preventDefault();
    onVerify({
      action,
      verifiedMerchant: verifiedMerchant || undefined,
      verifiedAmount: verifiedAmount || undefined,
      verifiedCategory: verifiedCategory || undefined,
      reason: reason || undefined,
    });
  };

  return (
    <form onSubmit={handleSubmit} className="receipt-verification-form">
      <div className="form-group">
        <label>처리</label>
        <select value={action} onChange={(e) => setAction(e.target.value)} required>
          <option value="APPROVE">승인</option>
          <option value="REJECT">반려</option>
        </select>
      </div>

      {action === "APPROVE" && (
        <>
          <div className="form-group">
            <label>검증된 상점명</label>
            <input
              type="text"
              value={verifiedMerchant}
              onChange={(e) => setVerifiedMerchant(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label>검증된 금액</label>
            <input
              type="number"
              value={verifiedAmount}
              onChange={(e) => setVerifiedAmount(parseInt(e.target.value))}
              min="0"
            />
          </div>

          <div className="form-group">
            <label>검증된 카테고리</label>
            <select value={verifiedCategory} onChange={(e) => setVerifiedCategory(e.target.value)}>
              <option value="">선택하세요</option>
              <option value="식비">식비</option>
              <option value="교통비">교통비</option>
              <option value="비품">비품</option>
              <option value="기타">기타</option>
            </select>
          </div>
        </>
      )}

      <div className="form-group">
        <label>사유</label>
        <textarea
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          rows={3}
          required={action !== "APPROVE"}
        />
      </div>

      <div className="form-actions">
        <button type="submit">확인</button>
      </div>
    </form>
  );
};

export default ReceiptVerification;

