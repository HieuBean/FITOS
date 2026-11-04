from typing import Dict, Any, List
from schemas import LeadProfile, ActivityItem

class LeadContextLoader:
    """
    Transforms raw LeadProfile and activity history into structured context
    for rule-based scoring and LLM Persona Generation.
    """

    @staticmethod
    def load_context(lead: LeadProfile) -> Dict[str, Any]:
        activities = lead.activities or []
        
        # Categorize activities
        calls = [a for a in activities if a.activity_type.upper() == "CALL"]
        consultations = [a for a in activities if a.activity_type.upper() == "CONSULTATION"]
        trials = [a for a in activities if a.activity_type.upper() == "TRIAL"]
        messages = [a for a in activities if a.activity_type.upper() == "MESSAGE"]

        # Check positive outcomes
        trial_attended = any(a.outcome == "ATTENDED" for a in trials)
        trial_no_show = any(a.outcome == "NO_SHOW" for a in trials)
        price_objection = any(
            (a.outcome == "PRICE_SENSITIVE") or 
            (a.notes and any(w in a.notes.lower() for w in ["đắt", "giá cao", "chát", "ngân sách", "giảm giá"]))
            for a in activities
        )
        time_objection = any(
            (a.outcome == "BUSY") or 
            (a.notes and any(w in a.notes.lower() for w in ["bận", "chưa có thời gian", "công tác", "tăng ca"]))
            for a in activities
        )

        all_notes = " ".join([a.notes for a in activities if a.notes])

        return {
            "lead_id": lead.id,
            "name": lead.name,
            "goal": lead.fitness_goal or "GENERAL_FITNESS",
            "budget": lead.budget_range or "MEDIUM",
            "preferred_schedule": lead.preferred_schedule or "EVENING",
            "source": lead.source or "ORGANIC",
            "current_status": lead.status or "NEW",
            "total_touchpoints": len(activities),
            "call_count": len(calls),
            "consultation_count": len(consultations),
            "trial_attended": trial_attended,
            "trial_no_show": trial_no_show,
            "price_objection": price_objection,
            "time_objection": time_objection,
            "notes_combined": all_notes
        }

