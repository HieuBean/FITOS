export interface LeadDto {
  id: number;
  fullName: string;
  phone: string;
  email?: string;
  status: string;
  source: string;
  goal?: string;
  notes?: string;
  assignedToUserId?: number;
  assignedToName?: string;
  aiScore?: number;
  createdAt: string;
  updatedAt: string;
}

export interface LeadView extends LeadDto {
  firstName: string;
  lastName: string;
  interest: string;
  assignedStaffId?: number;
  assignedStaffName?: string;
  opportunityScore: number;
  scoreReason?: string;
}

export function mapLead(dto: LeadDto): LeadView {
  const name = (dto.fullName || '').trim();
  const parts = name.split(/\s+/);
  return {
    ...dto,
    firstName: parts.pop() || '',
    lastName: parts.join(' '),
    interest: dto.goal || 'Chưa xác định',
    assignedStaffId: dto.assignedToUserId,
    assignedStaffName: dto.assignedToName,
    opportunityScore: dto.aiScore ?? 0,
  };
}

export function mapLeadPage(data: { content?: LeadDto[] } | LeadDto[]): LeadView[] {
  const content = Array.isArray(data) ? data : data.content;
  if (!Array.isArray(content)) throw new Error('Invalid lead page response');
  return content.map(mapLead);
}

export interface LeadActivityDto {
  id: number;
  leadId: number;
  userId?: number;
  userName?: string;
  activityType: string;
  notes: string;
  activityTime: string;
}

export function mapLeadActivity(dto: LeadActivityDto) {
  return {
    id: dto.id,
    leadId: dto.leadId,
    staffId: dto.userId,
    staffName: dto.userName,
    activityType: dto.activityType,
    content: dto.notes,
    createdAt: dto.activityTime,
  };
}

export interface DashboardStatsDto {
  revenueThisMonth: number;
  activeMembers: number;
  classesToday: number;
  bookedSpotsToday: number;
  totalCapacityToday: number;
  classFillRate: number;
  newLeadsThisMonth: number;
  leadConversionRate: number;
}

export interface RevenueChartDto {
  points: Array<{ month: string; amount: number }>;
}

export interface CrmFunnelDto {
  stages: Array<{ stage: string; count: number; percentage: number }>;
}

export function mapRevenueChart(data: RevenueChartDto) {
  if (!Array.isArray(data?.points)) throw new Error('Invalid revenue chart response');
  return data.points.map((point) => ({ month: point.month, revenue: Number(point.amount) || 0 }));
}

export function mapCrmFunnel(data: CrmFunnelDto) {
  if (!Array.isArray(data?.stages)) throw new Error('Invalid CRM funnel response');
  const count = (stage: string) => data.stages.find((item) => item.stage === stage)?.count || 0;
  const total = data.stages.reduce((sum, item) => sum + item.count, 0);
  return {
    leadsCount: total,
    trialsCount: count('TRIAL'),
    convertedCount: count('CONVERTED'),
    conversionRate: total ? Math.round((count('CONVERTED') / total) * 1000) / 10 : 0,
  };
}

/* -------------------------------------------------------------------------
 * Commerce & vận hành (layer C)
 * ---------------------------------------------------------------------- */

export type MembershipStatus = 'PENDING' | 'ACTIVE' | 'PAUSED' | 'EXPIRED' | 'CANCELLED';
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';

export interface MembershipDto {
  id: number;
  customerId: number;
  customerName: string;
  planId: number;
  planName: string;
  startDate: string;
  endDate: string;
  status: MembershipStatus;
  daysRemaining: number;
  pausedAt?: string | null;
  pauseDaysUsed: number;
  autoRenew: boolean;
  riskLevel: RiskLevel;
  riskReason: string;
  createdAt: string;
}

export interface PlanDto {
  id: number;
  name: string;
  planType: 'MEMBERSHIP' | 'PT_PACKAGE';
  price: number;
  durationDays: number;
  sessionCount?: number | null;
  description?: string | null;
  features: string[];
  isActive: boolean;
}

export interface PaymentDto {
  id: number;
  orderId: number;
  customerId: number;
  transactionRef: string;
  paymentMethod: string;
  amount: number;
  status: 'PENDING' | 'SUCCESS' | 'FAILED' | 'REFUNDED';
  paidAt?: string | null;
  createdAt: string;
}

export interface ClassSessionDto {
  id: number;
  classId: number;
  className: string;
  category: string;
  colorCode: string;
  trainerId?: number | null;
  trainerName?: string | null;
  sessionDate: string;
  startTime: string;
  endTime: string;
  room: string;
  capacity: number;
  bookedCount: number;
  availableSpots: number;
  status: string;
}

export interface BookingDto {
  id: number;
  customerId: number;
  customerName: string;
  sessionId: number;
  className: string;
  sessionDate: string;
  startTime: string;
  status: string;
  bookedAt: string;
  checkedIn: boolean;
}

export interface StudioClassDto {
  id: number;
  name: string;
  category: string;
  description?: string | null;
  defaultCapacity: number;
  colorCode: string;
  isActive: boolean;
}

export interface SlotSuggestionDto {
  slot: string;
  classId?: number | null;
  className: string;
  reason: string;
  priority: number;
}

export interface ScheduleSuggestionDto {
  date: string;
  suggestions: SlotSuggestionDto[];
  source: string;
}

export function formatCurrency(value: number): string {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(value);
}

export function formatDate(value?: string | null): string {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

export function extractPageContent<T>(data: unknown): T[] {
  if (Array.isArray(data)) return data as T[];
  const content = (data as { content?: T[] } | null)?.content;
  return Array.isArray(content) ? content : [];
}
