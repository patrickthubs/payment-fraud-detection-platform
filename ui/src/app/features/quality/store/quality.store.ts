import { inject, Injectable, signal } from '@angular/core';
import { finalize } from 'rxjs';

import { FraudApiService } from '../../../core/services/fraud-api.service';
import { FraudOutcomeRequest, FraudQualityMetrics } from '../../../shared/models/api.models';

@Injectable()
export class QualityStore {
  private readonly api = inject(FraudApiService);

  readonly metrics = signal<FraudQualityMetrics | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly message = signal('');

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.api.getQualityMetrics().pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (metrics) => this.metrics.set(metrics),
      error: () => this.error.set('Decision quality metrics could not be loaded.')
    });
  }

  record(assessmentId: string, request: FraudOutcomeRequest): void {
    this.saving.set(true);
    this.error.set('');
    this.message.set('');
    this.api.recordOutcome(assessmentId, request).pipe(finalize(() => this.saving.set(false))).subscribe({
      next: () => {
        this.message.set('Outcome recorded and decision quality recalculated.');
        this.load();
      },
      error: () => this.error.set('The outcome could not be recorded. Supervisor access is required.')
    });
  }
}
