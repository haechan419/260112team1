import { Cookies } from "react-cookie";

const cookies = new Cookies();

export const setCookie = (name, value, days = 1) => {
  const expires = new Date();
  expires.setDate(expires.getDate() + days);

  // ⭐ JSON 문자열로 저장 (안전)
  cookies.set(name, JSON.stringify(value), { path: "/", expires });
};

export const getCookie = (name) => {
  const raw = cookies.get(name);
  if (!raw) return null;

  // raw가 이미 객체로 나오면 그대로 반환
  if (typeof raw === "object") return raw;

  try {
    return JSON.parse(raw);
  } catch (e) {
    console.error("[getCookie parse fail]", name, raw, e);
    return null;
  }
};

export const removeCookie = (name) => {
  cookies.remove(name, { path: "/" });
};
