import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { QualityStore } from '../../store/quality.store';

@Component({
  selector: 'app-quality-page',
  imports: [ReactiveFormsModule],
  providers: [QualityStore],
  template: `
    <section class="page-stack">
      <header class="page-header">
        <div>
          <p class="eyebrow">Ground truth loop</p>
          <h1>Decision quality</h1>
          <p>Measure whether alerts catch fraud without creating unnecessary customer friction.</p>
        </div>
      </header>

      @if (store.metrics(); as metrics) {
        <div class="metric-grid">
          <article class="metric"><span>Precision</span><strong>{{ percent(metrics.precision) }}</strong></article>
          <article class="metric"><span>Recall</span><strong>{{ percent(metrics.recall) }}</strong></article>
          <article class="metric"><span>False-positive rate</span><strong>{{ percent(metrics.falsePositiveRate) }}</strong></article>
          <article class="metric"><span>Labelled decisions</span><strong>{{ metrics.totalLabelled }}</strong></article>
          <article class="metric"><span>Net loss</span><strong>{{ money(metrics.netLoss) }}</strong></article>
        </div>
      }

      <section class="panel">
        <div class="section-heading">
          <p class="eyebrow">Supervisor workflow</p>
          <h2>Record confirmed outcome</h2>
        </div>
        <form class="form-grid" [formGroup]="form" (ngSubmit)="submit()">
          <label><span>Assessment ID</span><input formControlName="assessmentId" /></label>
          <label><span>Outcome</span><select formControlName="outcomeLabel">
            <option value="CONFIRMED_FRAUD">Confirmed fraud</option>
            <option value="ACCOUNT_TAKEOVER">Account takeover</option>
            <option value="CHARGEBACK">Chargeback</option>
            <option value="GENUINE">Genuine</option>
            <option value="CUSTOMER_AUTHORIZED">Customer authorized</option>
            <option value="INCONCLUSIVE">Inconclusive</option>
          </select></label>
          <label><span>Evidence source</span><input formControlName="source" /></label>
          <label><span>Actual loss</span><input type="number" min="0" formControlName="actualLoss" /></label>
          <label><span>Recovered amount</span><input type="number" min="0" formControlName="recoveredAmount" /></label>
          <label><span>Notes</span><textarea formControlName="notes"></textarea></label>
          @if (store.error()) { <p class="inline-error">{{ store.error() }}</p> }
          @if (store.message()) { <p>{{ store.message() }}</p> }
          <button class="button" type="submit" [disabled]="form.invalid || store.saving()">
            {{ store.saving() ? 'Recording...' : 'Record outcome' }}
          </button>
        </form>
      </section>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QualityPage implements OnInit {
  protected readonly store = inject(QualityStore);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly form = this.formBuilder.nonNullable.group({
    assessmentId: ['', Validators.required],
    outcomeLabel: ['CONFIRMED_FRAUD' as const, Validators.required],
    source: ['CUSTOMER_CONFIRMATION', Validators.required],
    actualLoss: [0, [Validators.required, Validators.min(0)]],
    recoveredAmount: [0, [Validators.required, Validators.min(0)]],
    notes: ['']
  });

  ngOnInit(): void { this.store.load(); }

  protected submit(): void {
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    this.store.record(value.assessmentId.trim(), {
      outcomeLabel: value.outcomeLabel,
      source: value.source.trim(),
      actualLoss: value.actualLoss,
      recoveredAmount: value.recoveredAmount,
      notes: value.notes.trim(),
      occurredAt: null
    });
  }

  protected percent(value: number): string { return `${(value * 100).toFixed(1)}%`; }
  protected money(value: number): string { return new Intl.NumberFormat('en-ZA', { style: 'currency', currency: 'ZAR' }).format(value); }
}
