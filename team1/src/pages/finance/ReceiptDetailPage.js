import {useParams} from "react-router-dom";
import ReceiptDetailComponent from "../../components/finance/ReceiptDetailComponent";
import AppLayout from "../../components/layout/AppLayout";

/**
 * 영수증 상세 페이지 컴포넌트
 *
 * @component
 */
const ReceiptDetailPage = () => {
    const {id} = useParams();

    // id가 없으면 에러 메시지 표시
    if (!id || id === "undefined") {
        return (
            <div className="p-4 w-full bg-white">
                <div className="text-3xl font-extrabold mb-4">영수증 상세</div>
                <div className="text-center p-8 text-red-500">
                    <p>영수증 ID가 필요합니다.</p>
                    <p className="text-sm text-gray-500 mt-2">지출 내역에서 영수증을 선택해주세요.</p>
                </div>
            </div>
        );
    }

    return (
        <AppLayout>
            <div className="p-4 w-full bg-white">
                <ReceiptDetailComponent id={id}></ReceiptDetailComponent>
            </div>
        </AppLayout>
    );
};

export default ReceiptDetailPage;
