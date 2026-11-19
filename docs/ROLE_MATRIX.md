# Ma trận vai trò ở cổng FITOS

| Nhóm nghiệp vụ | Admin | Quản lý | Lễ tân | HLV | Hội viên |
| --- | --- | --- | --- | --- | --- |
| Tài khoản và phân quyền | Quản trị | — | — | — | — |
| Dashboard, báo cáo và thống kê | — | Xem và xuất báo cáo | — | — | — |
| Gói dịch vụ, sản phẩm và tồn kho | — | Quản lý | — | — | — |
| Nhân sự lễ tân và hồ sơ HLV | — | Xem lễ tân, phân công lead, tạo/sửa hồ sơ HLV | — | — | — |
| Khách tiềm năng, bán hàng, POS, thanh toán | — | Xem để giám sát | Thực hiện | — | — |
| Đặt lịch, điểm danh, gia hạn | — | Xem để giám sát | Thực hiện | Lịch tập liên quan | Tự phục vụ |
| Giáo án và buổi tập | — | — | — | Thực hiện | Xem dữ liệu của mình |

Admin tiếp tục là vai trò duy nhất được thêm, sửa, xóa tài khoản và đổi vai trò. Quản lý chỉ cập nhật **hồ sơ nghiệp vụ** của HLV, không tạo tài khoản đăng nhập cho họ. Trạng thái hoạt động của tài khoản lễ tân do Admin kiểm soát; Quản lý được xem danh sách và phân công khách tiềm năng cho lễ tân đang hoạt động.

Menu và tuyến `/dashboard` dùng chung `frontend/src/lib/portalAccess.ts`. API ghi bán hàng, thanh toán, gia hạn, điểm danh và CRM được tách khỏi quyền Quản lý; API cấu hình sản phẩm, gói, nhân sự HLV và báo cáo được tách khỏi Lễ tân. Một số API đọc vẫn dùng chung để Quản lý giám sát số liệu. Quyền ADMIN ở một số API nghiệp vụ cũ vẫn được giữ cho tương thích với công cụ nạp dữ liệu mẫu; cổng Admin trên frontend không hiển thị các nghiệp vụ này.
