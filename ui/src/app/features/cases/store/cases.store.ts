import { inject, Injectable, signal } from '@angular/core';

import { FraudApiService } from '../../../core/services/fraud-api.service';
import { FraudCase, FraudCaseFilters } from '../../../shared/models/api.models';

@Injectable()
export class CasesStore {
  private readonly api = inject(FraudApiService);
  private lastFilters: FraudCaseFilters = {};

  readonly loading = signal(false);
  readonly error = signal('');
  readonly actionBusy = signal(false);
  readonly actionMessage = signal('');
  readonly actionError = signal('');
  readonly cases = signal<FraudCase[]>([]);
  readonly selectedCase = signal<FraudCase | null>(null);

  load(filters: FraudCaseFilters): void {
    this.lastFilters = filters;
    this.loading.set(true);
    this.error.set('');

    this.api.listCases(filters).subscribe({
      next: (cases) => {
        this.cases.set(cases);
        this.loading.set(false);
        const current = this.selectedCase()?.caseId;
        const preserved = cases.find((item) => item.caseId === current);
        this.selectedCase.set(preserved ?? cases[0] ?? null);
        if (this.selectedCase()) {
          this.loadDetail(this.selectedCase()!.caseId);
        }
      },
      error: () => {
        this.error.set('Unable to load the fraud case queue.');
        this.loading.set(false);
      }
    });
  }

  loadDetail(caseId: string): void {
    this.api.getCase(caseId).subscribe({
      next: (item) => this.selectedCase.set(item)
    });
  }

  assign(caseId: string, assignee: string, note: string): void {
    this.runAction(
      this.api.assignCase(caseId, assignee, note),
      'Case assigned successfully.'
    );
  }

  escalate(caseId: string, reason: string): void {
    this.runAction(
      this.api.escalateCase(caseId, reason),
      'Case escalated successfully.'
    );
  }

  addNote(caseId: string, note: string): void {
    this.runAction(
      this.api.addCaseNote(caseId, note),
      'Case note added.'
    );
  }

  release(caseId: string, resolutionSummary: string): void {
    this.runAction(
      this.api.releaseCase(caseId, resolutionSummary),
      'Held payment released successfully.'
    );
  }

  confirmDecline(caseId: string, resolutionSummary: string): void {
    this.runAction(
      this.api.confirmDecline(caseId, resolutionSummary),
      'Decline confirmed successfully.'
    );
  }

  private runAction(request$: { subscribe: Function }, successMessage: string): void {
    this.actionBusy.set(true);
    this.actionMessage.set('');
    this.actionError.set('');

    request$.subscribe({
      next: (item: FraudCase) => {
        this.selectedCase.set(item);
        this.actionMessage.set(successMessage);
        this.actionBusy.set(false);
        this.load(this.lastFilters);
      },
      error: () => {
        this.actionError.set('That case action could not be completed with the current operator session.');
        this.actionBusy.set(false);
      }
    });
  }
}
