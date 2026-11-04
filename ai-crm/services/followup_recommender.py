from typing import Dict, Any, List
from schemas import FollowUpResponse, ConversionTipsResponse, ConversionTip

class FollowUpRecommender:
    """
    Persona & Follow-up Script Generator:
    Generates targeted phone scripts, Zalo/SMS follow-up messages and objection handling.
    """

    @classmethod
    def generate_followup(cls, context: Dict[str, Any], target_package: str, tone: str) -> FollowUpResponse:
        name = context.get("name", "Quý khách")
        goal = context.get("goal", "sức khỏe")
        budget = context.get("budget", "MEDIUM")
        schedule = context.get("preferred_schedule", "buổi tối")
        trial_attended = context.get("trial_attended", False)
        price_objection = context.get("price_objection", False)
        time_objection = context.get("time_objection", False)

        # 1. Determine Persona
        if trial_attended and budget in ["HIGH", "PREMIUM"]:
            persona = "High-Value Achiever (Người chú trọng kết quả & dịch vụ cao cấp)"
            pain_point = "Cần PT chuyên sâu đẩy tiến độ nhanh và phòng tập không gian chất lượng"
            angle = "Tập trung vào lộ trình cá nhân hóa 1-1, cam kết chỉ số InBody rõ ràng"
            channel = "Gọi điện thoại trực tiếp hoặc hẹn cà phê tư vấn tại phòng tập"
            best_time = "11:30 - 13:00 hoặc sau 18:30"
            call_script = (
                f"Chào anh/chị {name}, em là tư vấn viên FITOS. Sau buổi tập thử hôm trước, HLV bên em đã đo chỉ số InBody và "
                f"lên sẵn phân tích cơ xương khớp cho mục tiêu {goal} của anh/chị. Em muốn mời anh/chị ghé xem lộ trình "
                f"3 tháng cam kết cải thiện chỉ số, hiện phòng tập đang tặng thêm 3 buổi PT kèm riêng khi kích hoạt trong tuần này ạ."
            )
            zalo_script = (
                f"Dạ FITOS chào anh/chị {name}! HLV đã hoàn thành bản đánh giá thể trạng sau buổi tập thử của mình. "
                f"Em gửi anh/chị tham khảo kết quả và bảng lộ trình tăng cơ/giảm mỡ chi tiết tại đây nhé ạ: [Link xem lộ trình]. "
                f"Chúc anh/chị một ngày nhiều năng lượng!"
            )
        elif price_objection:
            persona = "Value-Conscious Fitness Seeker (Người cân nhắc chi phí & so sánh giá)"
            pain_point = "Lo ngại chi phí ban đầu cao hoặc chưa thấy sự khác biệt so với gym bình dân"
            angle = "Đưa ra giải pháp chia nhỏ chi phí theo ngày (chỉ từ 15k/ngày) hoặc ưu đãi trả góp 0%"
            channel = "Nhắn tin Zalo kèm voucher hình ảnh trực quan"
            best_time = "12:00 - 13:30 (Giờ nghỉ trưa)"
            call_script = (
                f"Dạ em chào anh/chị {name}, bên em vừa mở cổng ưu đãi Flash Sale đầu tháng cho học viên mới. "
                f"Gói tập 6 tháng tính ra chỉ khoảng 18.000đ/ngày, đầy đủ tiện ích tủ locker, phòng xông hơi và đo InBody hàng tháng. "
                f"Em giữ cho anh/chị 1 suất ưu đãi giảm 25% trong hôm nay nhé ạ?"
            )
            zalo_script = (
                f"Chào anh/chị {name}, FITOS gửi tặng anh/chị Voucher giảm ngay 500k áp dụng cho gói tập 3-6 tháng, "
                f"chia nhỏ chỉ từ 18k/ngày thoải mái giờ tập. Anh/chị bấm vào đây để giữ mã ưu đãi có hạn trong hôm nay nhé: [Link nhận Voucher]!"
            )
        elif time_objection:
            persona = "Busy Professional (Dân văn phòng bận rộn, áp lực thời gian)"
            pain_point = "Không có nhiều thời gian cố định, sợ mua gói rồi bận không đi tập được"
            angle = "Linh hoạt giờ tập từ 5h30 sáng - 22h đêm, có các lớp HIIT 45 phút đốt mỡ cấp tốc"
            channel = "Nhắn tin Zalo ngắn gọn, không spam cuộc gọi"
            best_time = "12:15 trưa hoặc 19:30 tối"
            call_script = (
                f"Chào anh/chị {name}, em biết lịch làm việc của anh/chị khá bận rộn. FITOS có khung giờ linh hoạt "
                f"từ sáng sớm đến 22h và các giáo án 45 phút tối ưu thời gian. Anh/chị có thể bắt đầu với gói linh hoạt hoặc thẻ bảo lưu khi bận công tác ạ."
            )
            zalo_script = (
                f"Dạ anh/chị {name}, FITOS có chính sách bảo lưu ngày tập miễn phí khi anh/chị bận công tác và "
                f"lớp tập 45 phút ngắn dành riêng cho người bận rộn. Khi nào rảnh, mời anh/chị ghé xả stress nhé ạ!"
            )
        else:
            persona = "Explorer / Beginner (Người mới bắt đầu tìm hiểu gym)"
            pain_point = "Chưa biết bắt đầu từ đâu, sợ vào phòng tập bỡ ngỡ không biết dùng máy"
            angle = "Tặng 1 buổi định hướng miễn phí cùng HLV hướng dẫn toàn bộ máy móc"
            channel = "Cuộc gọi thân thiện kết hợp tin nhắn Zalo"
            best_time = "14:00 - 16:30"
            call_script = (
                f"Dạ em chào anh/chị {name}, FITOS thấy anh/chị đang có nhu cầu tìm hiểu cải thiện vóc dáng ({goal}). "
                f"Đối với người mới, bên em có HLV hướng dẫn chỉnh dáng và cách sử dụng máy chuẩn an toàn hoàn toàn miễn phí trong buổi đầu. "
                f"Chiều mai tầm 17h anh/chị có tiện ghé qua tập trải nghiệm thử một buổi không ạ?"
            )
            zalo_script = (
                f"Chào anh/chị {name}, FITOS gửi anh/chị vé mời trải nghiệm tập thử 1 ngày miễn phí có HLV hướng dẫn máy móc. "
                f"Anh/chị chọn ngày tiện nhất tại đây nhé: [Link vé tập thử FITOS]."
            )

        return FollowUpResponse(
            lead_id=context.get("lead_id", 0),
            persona=persona,
            primary_pain_point=pain_point,
            suggested_angle=angle,
            recommended_channel=channel,
            call_script=call_script,
            zalo_sms_script=zalo_script,
            best_contact_time=best_time
        )

    @classmethod
    def generate_conversion_tips(cls, lead_id: int, objections: List[str], fitness_goal: str, budget_range: str) -> ConversionTipsResponse:
        tips: List[ConversionTip] = []

        objection_map = {
            "giá": {
                "handling": "Phương pháp Chia nhỏ Chi phí (Cost Reframe)",
                "script": "Quy đổi chi phí gói tập ra mỗi ngày (chỉ bằng 1 ly cà phê 20k) nhưng mang lại năng lượng làm việc và sức khỏe dài hạn."
            },
            "thời gian": {
                "handling": "Phương pháp Lịch linh hoạt & Giáo án rút gọn (Time Flexibility)",
                "script": "Nhấn mạnh giáo án 45 phút đốt mỡ/tăng cơ chuẩn khoa học, phòng mở cửa đến 22:00 và chính sách bảo lưu bảo vệ quyền lợi."
            },
            "suy nghĩ": {
                "handling": "Tạo Động lực Quyết định (Loss Aversion & Scarcity)",
                "script": "Cung cấp ưu đãi giữ chỗ có hạn chỉ áp dụng trong ngày, tặng thêm 2 tuần tập khi đăng ký sớm."
            },
            "gia đình": {
                "handling": "Gói Hội viên Kép / Gia đình (Family Referral)",
                "script": "Tư vấn gói tập đôi hoặc voucher mời người thân cùng đi tập để tạo động lực đồng hành."
            }
        }

        for obj in (objections or ["Giá hơi cao", "Chưa có thời gian"]):
            matched = False
            for key, val in objection_map.items():
                if key in obj.lower():
                    tips.append(ConversionTip(
                        objection=obj,
                        handling_technique=val["handling"],
                        script=val["script"]
                    ))
                    matched = True
                    break
            if not matched:
                tips.append(ConversionTip(
                    objection=obj,
                    handling_technique="Lắng nghe & Đặt câu hỏi thấu cảm",
                    script=f"Hỏi sâu hơn về rào cản cụ thể của khách: 'Dạ anh/chị đang băn khoăn nhất ở điểm nào để em hỗ trợ phương án tốt nhất ạ?'"
                ))

        closing_hook = (
            f"Cam kết kết quả cho mục tiêu {fitness_goal}: Hoàn tiền hoặc tặng thêm buổi tập nếu không đạt chỉ số mục tiêu sau 60 ngày tuân thủ lộ trình!"
        )

        return ConversionTipsResponse(
            lead_id=lead_id,
            tips=tips,
            closing_hook=closing_hook
        )

