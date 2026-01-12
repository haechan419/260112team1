import { useDispatch, useSelector } from "react-redux";
import { useNavigate, Navigate, createSearchParams } from "react-router-dom";
import { loginPostAsync, logout } from "../slices/loginSlice";
import { getCookie } from "../util/cookieUtil";

const useCustomLogin = () => {
    const navigate = useNavigate();
    const dispatch = useDispatch();

    const loginState = useSelector((state) => state.loginSlice);

    // ✅ 쿠키 기반 로그인 판정
    const member = getCookie("member");
    const isLogin = !!member?.accessToken;

    const doLogin = async (loginParam) => {
        const action = await dispatch(loginPostAsync(loginParam));
        return action.payload;
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
        console.log("Exception:", ex);

        const errorMsg = ex.response?.data?.error || ex.response?.data?.message;
        const errorStr = createSearchParams({ error: errorMsg }).toString();

        // ✅ 너 백엔드는 지금 {"success":false,"message":"UNAUTHORIZED"} 형태도 씀
        if (errorMsg === "REQUIRE_LOGIN" || errorMsg === "UNAUTHORIZED") {
            alert("로그인이 필요합니다.");
            navigate({ pathname: "/login", search: errorStr });
            return;
        }

        if (errorMsg === "ERROR_ACCESSDENIED" || ex.response?.status === 403) {
            alert("해당 메뉴를 사용할 수 있는 권한이 없습니다.");
            navigate({ pathname: "/login", search: errorStr });
            return;
        }
    };

    return {
        loginState,
        isLogin,
        doLogin,
        doLogout,
        moveToPath,
        moveToLogin,
        moveToLoginReturn,
        exceptionHandle,
    };
};

export default useCustomLogin;
