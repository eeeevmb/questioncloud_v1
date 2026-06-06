export interface DashboardWelcomeVO {
  username: string;
}

export interface DashboardStatsVO {
  questionTotal: number;
  questionYesterdayDelta: number;
  collectionTotal: number;
  collectionYesterdayDelta: number;
  paperTotal: number;
  paperYesterdayDelta: number;
  importTotal: number;
  importYesterdayDelta: number;
}

export interface DashboardRecentItemVO {
  id: string;
  name: string;
  type: string;
  updatedAt: string;
  targetPath: string;
}

export interface DashboardAnnouncementVO {
  title: string;
  date: string;
}

export interface DashboardActivityVO {
  type: string;
  username?: string | null;
  content: string;
  occurredAt: string;
  timeText: string;
}

export interface DashboardOverviewVO {
  welcome: DashboardWelcomeVO;
  stats: DashboardStatsVO;
  recentItems: DashboardRecentItemVO[];
  announcements: DashboardAnnouncementVO[];
  activities: DashboardActivityVO[];
}
