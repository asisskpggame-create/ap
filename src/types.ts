export type TabType = 'dashboard' | 'tasks' | 'expenses' | 'health' | 'vault' | 'assistant' | 'settings';

export type TaskCategory = 'work' | 'personal' | 'finance' | 'health' | 'study';
export type TaskPriority = 'low' | 'medium' | 'high' | 'urgent';

export interface TaskItem {
  id: string;
  title: string;
  category: TaskCategory;
  priority: TaskPriority;
  completed: boolean;
  dueDate: string; // Jalali or ISO string
  createdAt: string;
}

export interface HabitItem {
  id: string;
  title: string;
  streak: number;
  completedToday: boolean;
  category: TaskCategory;
}

export interface ExpenseItem {
  id: string;
  title: string;
  amount: number; // in Tomans
  category: string;
  date: string;
  isSmsParsed?: boolean;
  bankName?: string;
}

export interface HealthLog {
  id: string;
  date: string;
  sleepHours: number;
  sleepQuality: 'poor' | 'fair' | 'good' | 'excellent';
  waterGlasses: number;
  exerciseMinutes: number;
  exerciseType: string;
  caloriesBurned: number;
}

export interface VaultNote {
  id: string;
  title: string;
  content: string;
  category: string;
  updatedAt: string;
  isEncrypted?: boolean;
}

export interface ChatMessage {
  id: string;
  sender: 'user' | 'gemini';
  text: string;
  timestamp: string;
}
