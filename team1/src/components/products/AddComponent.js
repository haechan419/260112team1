import { useRef, useState } from "react";
import { postAdd } from "../../api/productApi"; // API 함수 import
import FetchingModal from "../common/FetchingModal";
import useCustomMove from "../../hooks/useCustomMove";

const initState = {
  pname: "",
  pdesc: "",
  price: 0,
  files: [], // 파일 상태
};

const AddComponent = () => {
  const [product, setProduct] = useState({ ...initState });
  const [fetching, setFetching] = useState(false); // 로딩 모달용
  const uploadRef = useRef(); // 파일 입력창 제어용
  const { moveToList } = useCustomMove(); // 등록 후 이동용

  // 입력값 변경 시 실행
  const handleChangeProduct = (e) => {
    product[e.target.name] = e.target.value;
    setProduct({ ...product });
  };

  // 등록 버튼 클릭 시 실행
  const handleClickAdd = () => {
    const formData = new FormData();
    const files = uploadRef.current.files;

    // 1. 이미지 파일 담기
    for (let i = 0; i < files.length; i++) {
      formData.append("files", files[i]);
    }

    // 2. 나머지 텍스트 데이터 담기
    formData.append("pname", product.pname);
    formData.append("pdesc", product.pdesc);
    formData.append("price", product.price);

    setFetching(true);

    // 3. 서버로 전송 (API 호출)
    postAdd(formData)
      .then((data) => {
        setFetching(false);
        alert("상품이 등록되었습니다!");
        moveToList(); // 목록 페이지로 이동
      })
      .catch((err) => {
        console.error(err);
        setFetching(false);
        alert("등록 실패! (콘솔 확인 필요)");
      });
  };

  return (
    <div className="border-2 border-sky-200 mt-10 m-2 p-4">
      {fetching ? <FetchingModal /> : <></>}

      <h1 className="text-3xl font-bold">상품 등록</h1>

      {/* 상품명 입력 */}
      <div className="flex justify-center mt-5">
        <div className="w-1/5 p-6 font-bold">상품명</div>
        <input
          className="w-4/5 p-6 rounded border border-neutral-300"
          name="pname"
          type="text"
          value={product.pname}
          onChange={handleChangeProduct}
        />
      </div>

      {/* 설명 입력 */}
      <div className="flex justify-center mt-5">
        <div className="w-1/5 p-6 font-bold">상세설명</div>
        <input
          className="w-4/5 p-6 rounded border border-neutral-300"
          name="pdesc"
          type="text"
          value={product.pdesc}
          onChange={handleChangeProduct}
        />
      </div>

      {/* 가격 입력 */}
      <div className="flex justify-center mt-5">
        <div className="w-1/5 p-6 font-bold">가격</div>
        <input
          className="w-4/5 p-6 rounded border border-neutral-300"
          name="price"
          type="number"
          value={product.price}
          onChange={handleChangeProduct}
        />
      </div>

      {/* 파일 업로드 (핵심 ✨) */}
      <div className="flex justify-center mt-5">
        <div className="w-1/5 p-6 font-bold">이미지</div>
        <input
          ref={uploadRef}
          className="w-4/5 p-6 rounded border border-neutral-300"
          type="file"
          multiple={true}
        />
      </div>

      {/* 버튼 */}
      <div className="flex justify-end mt-5">
        <button
          type="button"
          className="rounded p-4 w-36 bg-blue-500 text-xl text-white"
          onClick={handleClickAdd}
        >
          등록하기
        </button>
      </div>
    </div>
  );
};

export default AddComponent;
