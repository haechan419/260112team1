import Sidebar from "./Sidebar";
import Topbar from "./Topbar";
import "../../styles/layout.css";

export default function AppLayout({ children }) {
    return (
        <div className="app-root">
            <Sidebar />
            <div className="app-main">
                <Topbar />
                <div className="app-content">{children}</div>
            </div>
        </div>
    );
}
