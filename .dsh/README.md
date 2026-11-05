# Agent Skills cho FITOS (`.dsh/skills`)

Bộ **18 skill context-engineering** được chép vào repo này để cả nhóm dùng chung khi làm đồ án FITOS (backend Spring Boot + frontend Next.js + 4 AI service FastAPI).

- **Nguồn:** [muratcankoylan/Agent-Skills-for-Context-Engineering](https://github.com/muratcankoylan/Agent-Skills-for-Context-Engineering) (nhánh `main`, thư mục `skills/`)
- **License:** MIT — xem [`skills/LICENSE`](skills/LICENSE)
- **Định dạng:** mỗi skill là một thư mục `<name>/SKILL.md` kèm `references/` và `scripts/`; DSH đọc frontmatter (`name`, `description`) để đưa vào catalog.

## DSH tự nạp

`.dsh/skills` là root skill chuẩn của DSH ở cấp project (rank 100). Chỉ cần mở repo FITOS bằng DSH, các skill sẽ xuất hiện trong catalog phiên làm việc, không cần cấu hình gì thêm. Sửa nội dung `SKILL.md` thì lần gọi kế tiếp đã thấy bản mới.

## Dùng với Cursor / Kiro / Copilot

Các công cụ đó không đọc `.dsh/skills`. Có 2 cách:

| Cách | Làm gì |
|---|---|
| Copy | Chép cả thư mục skill vào nơi công cụ quét (Claude Code: `.claude/skills/`, Cursor: rules/agent skills theo hướng dẫn của công cụ) |
| Symlink | Trỏ `.claude/skills` (hoặc thư mục tương ứng) về `.dsh/skills` để chỉ có một nguồn sự thật |

Trong prompt chỉ cần gọi tên skill, ví dụ: "dùng `context-compression` để viết handoff cho ai-workout".

## Danh sách skill và chỗ dùng trong FITOS

| Skill | Dùng khi |
|---|---|
| `context-fundamentals` | Giải thích khái niệm nền: context window, attention budget, U-shaped curve |
| `context-degradation` | Agent trả lời tệ dần khi chat dài, lost-in-middle, context bị nhiễu/mâu thuẫn |
| `context-optimization` | Giảm token/cost cho AI service, mask output, prefix cache, chia partition |
| `context-compression` | Nén phiên chat dài (AI coach, CRM follow-up) thành summary bàn giao |
| `filesystem-context` | Offload output lớn ra file, scratchpad bền, handoff file giữa các agent |
| `memory-systems` | Bộ nhớ dài hạn: hồ sơ hội viên, lịch sử tập, temporal validity |
| `multi-agent-patterns` | Quyết định tách 4 AI service hay gộp, supervisor/swarm, handoff |
| `project-development` | Chọn shape cho pipeline LLM, ước lượng token/cost, single vs multi-agent |
| `evaluation` | Bộ test đánh giá agent, rubric, regression suite, quality gate CI |
| `advanced-evaluation` | LLM-as-judge, pairwise comparison, hiệu chỉnh evaluator |
| `tool-design` | Viết mô tả/schema tool cho AI service & MCP server, gộp tool trùng |
| `harness-engineering` | Thiết kế harness tự chạy: research loop, eval scaffold, rollback, approval |
| `self-improvement-loops` | Vòng tự cải thiện harness/scaffold, gate chấp nhận chỉnh sửa |
| `self-managed-context` | Model tự sửa context window của mình (edit gate, receipts, rollback) |
| `latent-briefing` | Chia sẻ memory giữa agent không qua summary (orchestrator → worker) |
| `long-horizon-prompting` | Viết prompt khởi động cho agent chạy dài/đa nhánh trên bài toán khó |
| `hosted-agents` | Hạ tầng agent chạy nền: sandbox, warm pool, session persistence |
| `bdi-mental-states` | Mô hình hoá mental state (belief/desire/intention) cho agent lý luận |

## Thêm skill mới

Tạo thư mục `.dsh/skills/<ten-kebab-case>/SKILL.md` với frontmatter:

```yaml
---
name: ten-kebab-case
description: "Mô tả ngắn gọn khi nào dùng skill này."
---

# Nội dung hướng dẫn
```

`name` phải kebab-case và trùng tên thư mục, `description` là bắt buộc. File nội dung nên < 500 dòng, chi tiết đẩy vào `references/`.

## Cập nhật từ repo gốc

Repo chưa có cơ chế sync tự động. Khi upstream có bản mới: chép lại `skills/<name>/` từ GitHub vào đây rồi commit. Nếu lệch nhiều, ghi lại commit SHA của upstream trong PR để truy vết.
