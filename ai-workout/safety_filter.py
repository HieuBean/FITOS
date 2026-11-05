import re
import unicodedata
from typing import List, Tuple
from schemas import SafetyCheckResult

# Danh mục các chất cấm, Steroid đồng hóa, SARMs và Doping
BANNED_SUBSTANCES = [
    "tren", "trenbolone", "anavar", "oxandrolone", "dianabol", "dbol", 
    "winstrol", "stanozolol", "clenbuterol", "clen", "testosterone enanthate", 
    "sustanon", "deca-durabolin", "deca", "nandrolone", "sarm", "rad-140", "rad140", 
    "lgd-4033", "lgd4033", "mk-677", "mk677", "ostarine", "growth hormone", "hgh", 
    "tiem steroid", "chich steroid", "thuoc kich co", "thuoc tang co", "ephedrine", "doping"
]

# Danh mục các bệnh lý cấp tính & chỉ định y khoa khẩn cấp
MEDICAL_RISKS = [
    "nhoi mau co tim", "dau that nguc", "gay xuong", "vo sun", "dot quy", 
    "tai bien", "tran dich khop cap", "thoat vi dia dem chen ep tuy", "suy tim cap"
]

# Các hành vi tập luyện cực đoan, nguy hiểm đến tính mạng
UNSAFE_PRACTICES = [
    "khoa khop goi", "nhin uong nuoc cat nuoc cuc doan", "thuoc loi tieu thi dau"
]

def remove_accents(text: str) -> str:
    """Loại bỏ dấu tiếng Việt để đối soát từ khóa an toàn không phụ thuộc bộ gõ."""
    text = unicodedata.normalize('NFD', text)
    text = ''.join(c for c in text if unicodedata.category(c) != 'Mn')
    return text.replace('đ', 'd').replace('Đ', 'd').lower()

def check_safety(message: str) -> SafetyCheckResult:
    """
    Kiểm duyệt an toàn nội dung câu hỏi trước khi xử lý hoặc gọi LLM.
    Trả về SafetyCheckResult với cờ an toàn và thông điệp cảnh báo y khoa nếu phát hiện vi phạm.
    """
    if not message or not message.strip():
        return SafetyCheckResult(is_safe=True)

    normalized = remove_accents(message)
    
    # 1. Kiểm tra chất cấm / Doping
    found_substances = []
    for term in BANNED_SUBSTANCES:
        pattern = r'\b' + re.escape(term) + r'\b'
        if re.search(pattern, normalized):
            found_substances.append(term)

    if found_substances:
        return SafetyCheckResult(
            is_safe=False,
            violation_type="BANNED_SUBSTANCE",
            flagged_terms=found_substances,
            warning_message=(
                "⚠️ **CẢNH BÁO AN TOÀN Y TẾ FITOS**: Hệ thống phát hiện nội dung liên quan đến chất cấm, "
                "Steroid đồng hóa hoặc hợp chất Doping (" + ", ".join(found_substances) + "). "
                "FITOS tuân thủ tuyệt đối chuẩn mực thể thao trong sạch (Clean Fitness) và pháp luật y tế. "
                "AI Coach không tư vấn, hướng dẫn hay kê đơn các chất này do rủi ro nguy hiểm đến tính mạng, "
                "suy gan, suy thận và rối loạn tim mạch. Hãy kiên trì tập luyện tự nhiên (Natural Bodybuilding) "
                "kết hợp dinh dưỡng khoa học để đạt mục tiêu bền vững!"
            )
        )

    # 2. Kiểm tra bệnh lý nguy kịch
    found_medical = []
    for term in MEDICAL_RISKS:
        if term in normalized:
            found_medical.append(term)

    if found_medical:
        return SafetyCheckResult(
            is_safe=False,
            violation_type="MEDICAL_CONTRAINDICATION",
            flagged_terms=found_medical,
            warning_message=(
                "🚨 **KHUYẾN CÁO Y KHOA KHẨN CẤP**: Câu hỏi chứa các dấu hiệu bệnh lý hoặc tổn thương thể chất nghiêm trọng ("
                + ", ".join(found_medical) + "). Vui lòng **DỪNG TẬP NGAY LẬP TỨC** và đến cơ sở y tế gần nhất "
                "để được bác sĩ chuyên khoa thăm khám trực tiếp. FITOS AI Coach không thay thế chẩn đoán y khoa!"
            )
        )

    # 3. Kiểm tra thói quen tập luyện nguy hiểm
    found_unsafe = []
    for term in UNSAFE_PRACTICES:
        if term in normalized:
            found_unsafe.append(term)

    if found_unsafe:
        return SafetyCheckResult(
            is_safe=False,
            violation_type="UNSAFE_EXERCISE",
            flagged_terms=found_unsafe,
            warning_message=(
                "⚠️ **CẢNH BÁO KỸ THUẬT NGUY HIỂM**: Động tác hoặc kỹ thuật này tiềm ẩn nguy cơ chấn thương vĩnh viễn "
                "(như rách dây chằng gối, tổn thương cột sống). FITOS khuyến cáo bạn duy trì tư thế an toàn sinh học (biomechanics) "
                "và trao đổi trực tiếp với Huấn luyện viên thể hình trước khi thực hiện."
            )
        )

    return SafetyCheckResult(is_safe=True)
