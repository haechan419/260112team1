import { useDispatch, useSelector } from "react-redux";
import { useNavigate, Navigate, createSearchParams } from "react-router-dom";
import { loginPostAsync, logout, loginAction } from "../slices/loginSlice";
import { getCookie, setCookie } from "../util/cookieUtil";
import axios from "axios";
import { API_SERVER_HOST } from "../util/jwtUtil";

const useCustomLogin = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const loginState = useSelector((state) => state.loginSlice);
  const member = getCookie("member");
  const isLogin = !!member?.accessToken;

  const doLogin = async (loginParam) => {
    const action = await dispatch(loginPostAsync(loginParam));
    return action.payload;
  };

  // 👇 [최종] 변수명 대통합 버전
  const doFaceLogin = async (userInfo) => {
    console.log("🚀 [Face ID] 서버로 요청 보냄...");
    const userId = userInfo.userId || userInfo.employeeNo || userInfo;

    try {
      const res = await axios.get(`${API_SERVER_HOST}/api/face/login`, {
        params: { userId: userId },
      });
      const serverData = res.data;

      // 1. 이름 / 부서 데이터 정제
      const realName = serverData.name || serverData.user?.name || "이름없음";
      const realDept =
        serverData.departmentName || serverData.user?.departmentName || "";

      // 2. 권한(Role) 정제
      let finalRole = "USER";
      let finalRoleNames = ["USER"]; // 배열 형태도 준비

      const roles = serverData.roleNames || serverData.roles || [];

      // 배열이든 문자열이든 ADMIN이 보이면 격상
      const hasAdmin =
        (Array.isArray(roles) &&
          (roles.includes("ADMIN") || roles.includes("ROLE_ADMIN"))) ||
        (typeof roles === "string" && roles.includes("ADMIN"));

      if (hasAdmin) {
        finalRole = "ADMIN";
        finalRoleNames = ["ADMIN", "USER"];
      }

      // 3. 🚨 [핵심] 모든 변수명 다 넣어주기 (걸려라!)
      const loginData = {
        // ID 관련
        employeeNo: serverData.employeeNo || serverData.id,
        id: serverData.employeeNo || serverData.id, // id로 찾는 경우 대비

        // 이름 관련
        name: realName,
        userName: realName, // userName으로 찾는 경우 대비

        // 부서 관련 (가장 의심스러움)
        departmentName: realDept,
        deptName: realDept, // deptName으로 찾는 경우 대비
        dept: realDept, // dept로 찾는 경우 대비

        // 권한 관련
        role: finalRole, // 문자열로 찾는 경우 (ADMIN)
        roleNames: finalRoleNames, // 배열로 찾는 경우 ([ADMIN, USER])
        roles: finalRoleNames, // roles로 찾는 경우

        // 토큰
        accessToken: serverData.accessToken,
        refreshToken: serverData.refreshToken,
      };

      console.log("📦 [Face ID] 최종 저장 데이터:", loginData);

      // 쿠키 저장 & 리덕스 갱신
      setCookie("member", JSON.stringify(loginData), 1);
      dispatch(loginAction(loginData));

      return loginData;
    } catch (err) {
      console.error(err);
      throw err;
    }
  };

  const doLogout = () => {
    dispatch(logout());
  };
  const moveToPath = (path) => {
    navigate({ pathname: path }, { replace: true });
  };
  const moveToLogin = () => {
    navigate({ pathname: "/login" }, { replace: true });
  };
  const moveToLoginReturn = () => <Navigate replace to="/login" />;
  const exceptionHandle = (ex) => {
    /* 기존 동일 */
  };

  return {
    loginState,
    isLogin,
    doLogin,
    doFaceLogin,
    doLogout,
    moveToPath,
    moveToLogin,
    moveToLoginReturn,
    exceptionHandle,
  };
};

export default useCustomLogin;
