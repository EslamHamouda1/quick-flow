export * from './habits.service';
import { HabitsService } from './habits.service';
export * from './info.service';
import { InfoService } from './info.service';
export * from './tasks.service';
import { TasksService } from './tasks.service';
export const APIS = [HabitsService, InfoService, TasksService];
