# Tách trách nhiệm Quản lý và Lễ tân

## Mục tiêu

- Quản lý tập trung vào dashboard, sản phẩm/kho, gói dịch vụ, nhân sự lễ tân/HLV và báo cáo thực.
- Lễ tân tập trung vào CRM, bán hàng, POS, hội viên, đặt lịch và điểm danh.
- Admin tiếp tục độc quyền tài khoản/phân quyền; không chuyển quyền tài khoản sang Quản lý khi chưa được xác nhận.
- Menu, điều hướng trực tiếp và API phải cùng một ma trận quyền.

## Thực hiện

1. Tạo ma trận quyền tuyến frontend theo vai trò và dùng chung cho menu và route guard; thêm kiểm thử truy cập trực tiếp.
2. Tách API ghi tác nghiệp tại quầy khỏi vai trò MANAGER, giữ các API đọc cần cho báo cáo/giám sát.
3. Thêm màn hình nhân sự cho Quản lý, ban đầu xem danh sách theo studio; cập nhật chức năng theo câu trả lời về phạm vi “quản lý lễ tân”.
4. Thay báo cáo dữ liệu cố định bằng số liệu từ backend, chỉ xuất CSV đúng dữ liệu đang hiển thị.
5. Chạy build/test backend, frontend và kiểm thử trình duyệt với hai vai trò.
