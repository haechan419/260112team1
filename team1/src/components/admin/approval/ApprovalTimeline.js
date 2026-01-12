import React, { useMemo } from "react";
import "./ApprovalTimeline.css";

const ApprovalTimeline = ({ logs, approvalRequest }) => {
  const getActionLabel = (action) => {
    const actionMap = {
      SUBMIT: "상신",
      APPROVE: "승인",
      REJECT: "반려",
    };
    return actionMap[action || ""] || action;
  };

  const getActionClass = (action) => {
    const classMap = {
      SUBMIT: "action-submit",
      APPROVE: "action-approve",
      REJECT: "action-reject",
    };
    return classMap[action || ""] || "";
  };

  // 제출 로그가 있는지 확인
  const hasSubmitLog = useMemo(() => {
    return logs && logs.some((log) => log.action === "SUBMIT");
  }, [logs]);

  // 제출 이력을 포함한 전체 로그 목록 생성
  const allLogs = useMemo(() => {
    const logsList = logs || [];
    
    // 제출 로그가 없고 approvalRequest가 있으면 제출 이력을 추가
    if (!hasSubmitLog && approvalRequest && approvalRequest.createdAt) {
      const submitLog = {
        id: null, // 제출 로그는 DB에 없을 수 있으므로 id 없음
        action: "SUBMIT",
        actorName: approvalRequest.requesterName || "-",
        createdAt: approvalRequest.createdAt,
        message: "지출 내역을 제출했습니다.",
      };
      
      // 제출 이력을 맨 앞에 추가하고, 나머지 로그는 시간순으로 정렬
      const otherLogs = [...logsList].sort((a, b) => {
        const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
        const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
        return dateA - dateB;
      });
      
      return [submitLog, ...otherLogs];
    }
    
    // 제출 로그가 있으면 기존 로그를 시간순으로 정렬
    return [...logsList].sort((a, b) => {
      const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
      const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
      return dateA - dateB;
    });
  }, [logs, hasSubmitLog, approvalRequest]);

  if (allLogs.length === 0) {
    return <div className="timeline-empty">결재 이력이 없습니다.</div>;
  }

  return (
    <div className="approval-timeline">
      {allLogs.map((log, index) => (
        <div key={log.id || `log-${index}`} className="timeline-item">
          <div className="timeline-marker">
            <div className={`timeline-dot ${getActionClass(log.action)}`}></div>
            {index < allLogs.length - 1 && <div className="timeline-line"></div>}
          </div>
          <div className="timeline-content">
            <div className="timeline-header">
              <span className={`timeline-action ${getActionClass(log.action)}`}>
                {getActionLabel(log.action)}
              </span>
              <span className="timeline-date">
                {log.createdAt
                  ? new Date(log.createdAt).toLocaleString("ko-KR", {
                      year: "numeric",
                      month: "2-digit",
                      day: "2-digit",
                      hour: "2-digit",
                      minute: "2-digit",
                    })
                  : "-"}
              </span>
            </div>
            <div className="timeline-actor">
              {log.actorName || "-"}
            </div>
            {log.message && (
              <div className="timeline-message">{log.message}</div>
            )}
          </div>
        </div>
      ))}
    </div>
  );
};

export default ApprovalTimeline;

