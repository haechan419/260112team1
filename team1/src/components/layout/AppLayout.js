import Sidebar from "./Sidebar";
import Topbar from "./Topbar";
import "../../styles/layout.css";
import { useState } from "react";

export default function AppLayout({ children }) {
    const [sidebarOpen, setSidebarOpen] = useState(false);

    const toggleSidebar = () => {
        setSidebarOpen(!sidebarOpen);
    };

    const closeSidebar = () => {
        setSidebarOpen(false);
    };

    return (
        <div className="app-root">
            <Sidebar isOpen={sidebarOpen} onClose={closeSidebar} />
            {sidebarOpen && <div className="sidebar-overlay" onClick={closeSidebar} />}
            <div className="app-main">
                <Topbar onMenuClick={toggleSidebar} />
                <div className="app-content">{children}</div>
            </div>
        </div>
    );
}
