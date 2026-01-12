import React, { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { getUsers } from "../../../api/adminUserApi";
import AppLayout from "../../../components/layout/AppLayout";
import "./UserListPage.css";

const UserListPage = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  // 상태 관리
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [pageInfo, setPageInfo] = useState({
    page: 1,
    totalPages: 0,
    totalElements: 0,
    hasNext: false,
    hasPrev: false,
  });
  const [departments, setDepartments] = useState([]);

  // 검색/필터 상태 (URL에서 초기값 읽기)
  const [searchType, setSearchType] = useState(
    () => searchParams.get("searchType") || "name"
  );
  const [keyword, setKeyword] = useState(
    () => searchParams.get("keyword") || ""
  );
  const [department, setDepartment] = useState(
    () => searchParams.get("department") || ""
  );
  const [isActive, setIsActive] = useState(
    () => searchParams.get("isActive") || ""
  );
  const [currentPage, setCurrentPage] = useState(() => {
    const page = parseInt(searchParams.get("page") || "1", 10);
    return isNaN(page) || page < 1 ? 1 : page;
  });

  // URL 쿼리 파라미터 동기화
  useEffect(() => {
    const params = new URLSearchParams();
    if (currentPage > 1) params.set("page", currentPage.toString());
    if (searchType !== "name") params.set("searchType", searchType);
    if (keyword) params.set("keyword", keyword);
    if (department) params.set("department", department);
    if (isActive) params.set("isActive", isActive);

    setSearchParams(params, { replace: true });
  }, [currentPage, searchType, keyword, department, isActive, setSearchParams]);

  // 데이터 로딩
  useEffect(() => {
    loadUsers();
  }, [currentPage]);

  const loadUsers = async () => {
    setLoading(true);
    try {
      const params = {
        page: currentPage,
        size: 10,
        searchType: searchType || undefined,
        keyword: keyword || undefined,
        department: department || undefined,
        isActive: isActive === "" ? undefined : isActive === "true",
      };

      const response = await getUsers(params);

      setUsers(response.content || []);
      setPageInfo({
        page: response.page,
        totalPages: response.totalPages,
        totalElements: response.totalElements,
        hasNext: response.hasNext,
        hasPrev: response.hasPrev,
      });
      setDepartments(response.departments || []);
    } catch (error) {
      console.error("사원 목록 조회 실패:", error);
      setUsers([]);
    } finally {
      setLoading(false);
    }
  };

  // 검색 버튼 클릭
  const handleSearch = () => {
    setCurrentPage(1);
    loadUsers();
  };

  // 엔터키로 검색
  const handleKeyPress = (e) => {
    if (e.key === "Enter") {
      handleSearch();
    }
  };

  // 초기화 버튼
  const handleReset = () => {
    setSearchType("name");
    setKeyword("");
    setDepartment("");
    setIsActive("");
    setCurrentPage(1);
  };

  // 페이지 변경
  const handlePageChange = (page) => {
    setCurrentPage(page);
  };

  // 행 클릭 → 상세 페이지
  const handleRowClick = (user) => {
    navigate(`/admin/hr/users/${user.id}`);
  };

  // 사원 등록 버튼
  const handleCreateClick = () => {
    navigate("/admin/hr/users/create");
  };

  // 날짜 포맷
  const formatDate = (dateString) => {
    if (!dateString) return "-";
    return dateString.split("T")[0];
  };

  // 페이지 번호 목록 생성
  const getPageNumbers = () => {
    const pages = [];
    const startPage = Math.max(1, currentPage - 2);
    const endPage = Math.min(pageInfo.totalPages, currentPage + 2);

    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }
    return pages;
  };

  return (
    <AppLayout>
      <div className="user-list-page">
        {/* 헤더 */}
        <div className="page-header">
          <div className="page-title-section">
            <h1 className="page-title">사원 관리</h1>
          </div>
          <p className="page-description">
            사원 정보를 조회하고 관리할 수 있습니다.
          </p>
        </div>

        {/* 검색/필터 영역 */}
        <div className="filter-section">
          <div className="filter-row">
            {/* 검색 */}
            <div className="filter-item">
              <label>검색</label>
              <div className="search-group">
                <select
                  className="form-select search-type"
                  value={searchType}
                  onChange={(e) => setSearchType(e.target.value)}
                >
                  <option value="name">이름</option>
                  <option value="employeeNo">사번</option>
                  <option value="email">이메일</option>
                </select>
                <input
                  type="text"
                  className="form-input search-input"
                  placeholder="검색어 입력"
                  value={keyword}
                  onChange={(e) => setKeyword(e.target.value)}
                  onKeyPress={handleKeyPress}
                />
              </div>
            </div>

            {/* 부서 필터 */}
            <div className="filter-item">
              <label>부서</label>
              <select
                className="form-select"
                value={department}
                onChange={(e) => setDepartment(e.target.value)}
              >
                <option value="">전체</option>
                {departments.map((dept) => (
                  <option key={dept} value={dept}>
                    {dept}
                  </option>
                ))}
              </select>
            </div>

            {/* 재직 상태 필터 */}
            <div className="filter-item">
              <label>재직 상태</label>
              <select
                className="form-select"
                value={isActive}
                onChange={(e) => setIsActive(e.target.value)}
              >
                <option value="">전체</option>
                <option value="true">재직중</option>
                <option value="false">퇴사</option>
              </select>
            </div>

            {/* 버튼 */}
            <div className="filter-actions">
              <button className="btn btn-secondary" onClick={handleReset}>
                초기화
              </button>
              <button className="btn btn-primary" onClick={handleSearch}>
                조회
              </button>
            </div>
          </div>
        </div>

        {/* 테이블 상단 정보 */}
        <div className="table-header">
          <span className="total-count">
            총 <strong>{pageInfo.totalElements}</strong>명
          </span>
          <button className="btn btn-success" onClick={handleCreateClick}>
            + 사원 등록
          </button>
        </div>

        {/* 테이블 */}
        <div className="table-container">
          {loading ? (
            <div className="loading">로딩 중...</div>
          ) : users.length === 0 ? (
            <div className="empty-state">사원이 없습니다.</div>
          ) : (
            <>
              <table className="user-table">
                <thead>
                  <tr>
                    <th>사번</th>
                    <th>이름</th>
                    <th>부서</th>
                    <th>이메일</th>
                    <th>연락처</th>
                    <th>입사일</th>
                    <th>상태</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((user) => (
                    <tr
                      key={user.id}
                      onClick={() => handleRowClick(user)}
                      className="table-row-clickable"
                    >
                      <td>{user.employeeNo}</td>
                      <td>{user.name}</td>
                      <td>{user.departmentName || "-"}</td>
                      <td>{user.email || "-"}</td>
                      <td>{user.phone || "-"}</td>
                      <td>{formatDate(user.createdUserAt)}</td>
                      <td>
                        {user.locked ? (
                          <span className="status-badge status-locked">
                            잠금
                          </span>
                        ) : user.active ? (
                          <span className="status-badge status-active">
                            재직중
                          </span>
                        ) : (
                          <span className="status-badge status-resigned">
                            퇴사
                          </span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {/* 페이지네이션 */}
              {pageInfo.totalPages > 1 && (
                <div className="pagination">
                  <button
                    className="pagination-btn"
                    onClick={() => handlePageChange(1)}
                    disabled={currentPage === 1}
                  >
                    «
                  </button>
                  <button
                    className="pagination-btn"
                    onClick={() => handlePageChange(currentPage - 1)}
                    disabled={!pageInfo.hasPrev}
                  >
                    ‹
                  </button>

                  {getPageNumbers().map((page) => (
                    <button
                      key={page}
                      className={`pagination-btn ${
                        page === currentPage ? "active" : ""
                      }`}
                      onClick={() => handlePageChange(page)}
                    >
                      {page}
                    </button>
                  ))}

                  <button
                    className="pagination-btn"
                    onClick={() => handlePageChange(currentPage + 1)}
                    disabled={!pageInfo.hasNext}
                  >
                    ›
                  </button>
                  <button
                    className="pagination-btn"
                    onClick={() => handlePageChange(pageInfo.totalPages)}
                    disabled={currentPage === pageInfo.totalPages}
                  >
                    »
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </AppLayout>
  );
};

export default UserListPage;
