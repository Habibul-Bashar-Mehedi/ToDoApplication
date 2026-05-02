export interface DateRange {
  from: string;
  to: string;
}

export interface SummaryMetrics {
  totalTasks: number;
  completionRate: number;
  overdueCount: number;
  activeModuleCount: number;
}

export interface StatusDistributionItem {
  status: string;
  count: number;
  percentage: number;
}

export interface CompletionTrendItem {
  date: string;
  created: number;
  completed: number;
}

export interface ModulePerformanceItem {
  moduleId: number;
  moduleName: string;
  completionRate: number;
}

export interface PriorityAnalysisItem {
  priority: string;
  count: number;
  completionRate: number;
}

export interface OverdueTrendItem {
  date: string;
  overdueCount: number;
}

export interface ProductivityInsights {
  mostProductiveDayOfWeek: string;
  peakCompletionHour: number;
  completionVelocityPerWeek: number;
}

export interface TopCollaborator {
  userId: number;
  displayName: string;
  activityCount: number;
}

export interface SharingMetrics {
  sharedModuleCount: number;
  collaborationActivityCount: number;
  topCollaborators: TopCollaborator[];
}

export interface AnalyticsDashboard {
  dateRange: DateRange;
  summary: SummaryMetrics;
  statusDistribution: StatusDistributionItem[];
  completionTrend: CompletionTrendItem[];
  modulePerformance: ModulePerformanceItem[];
  priorityAnalysis: PriorityAnalysisItem[];
  avgCompletionDays: number;
  overdueTrend: OverdueTrendItem[];
  productivityInsights: ProductivityInsights;
  sharingMetrics: SharingMetrics;
}
