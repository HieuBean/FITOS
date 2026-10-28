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
