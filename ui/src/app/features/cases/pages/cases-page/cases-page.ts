import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';

import { CasesStore } from '../../store/cases.store';

@Component({
  selector: 'app-cases-page',
  imports: [ReactiveFormsModule],
  providers: [CasesStore],
  template: `
    <section class="page-shell">
      <header class="page-header">
        <div>
          <p class="eyebrow">Review queue</p>
          <h1>Review cases</h1>
        </div>
      </header>

      <form class="filter-grid" [formGroup]="form" (ngSubmit)="applyFilters()">
        <label>
          <span>Status</span>
          <select formControlName="status">
            <option value="">All persisted statuses</option>
            <option value="OPEN">OPEN</option>
            <option value="ESCALATED">ESCALATED</option>
            <option value="RESOLVED">RESOLVED</option>
          </select>
        </label>

        <label>
          <span>Min score</span>
          <input formControlName="minRiskScore" type="number" />
        </label>

        <label>
          <span>Payment lookup</span>
          <input formControlName="paymentId" type="text" />
        </label>

        <label class="toggle">
          <input formControlName="unassignedOnly" type="checkbox" />
          <span>Only unassigned</span>
        </label>

        <button class="button" type="submit">Apply queue filters</button>
      </form>

      @if (store.error()) {
        <p class="inline-error">{{ store.error() }}</p>
      }

      <section class="content-grid content-grid--wide">
        <article class="table-panel">
          <table class="data-table data-table--interactive">
            <thead>
              <tr>
                <th>Payment</th>
                <th>Customer</th>
                <th>Decision</th>
                <th>Status</th>
                <th>Score</th>
              </tr>
            </thead>
            <tbody>
              @for (item of store.cases(); track item.caseId) {
                <tr
                  [class.is-selected]="item.caseId === store.selectedCase()?.caseId"
                  (click)="selectCase(item.caseId)"
                >
                  <td>{{ item.paymentId }}</td>
                  <td>{{ item.customerId }}</td>
                  <td>{{ item.decision }}</td>
                  <td>{{ item.status }}</td>
                  <td>{{ item.riskScore }}</td>
                </tr>
              }
            </tbody>
          </table>
        </article>

        <aside class="detail-panel">
          @if (store.selectedCase(); as item) {
            <div class="section-heading">
              <p class="eyebrow">Selected case</p>
              <h2>{{ item.paymentId }}</h2>
            </div>

            <dl class="definition-grid">
              <div>
                <dt>Status</dt>
                <dd>{{ item.status }}</dd>
              </div>
              <div>
                <dt>Decision</dt>
                <dd>{{ item.decision }}</dd>
              </div>
              <div>
                <dt>Risk score</dt>
                <dd>{{ item.riskScore }}</dd>
              </div>
              <div>
                <dt>Assignee</dt>
                <dd>{{ item.currentAssignee || 'Unassigned' }}</dd>
              </div>
            </dl>

            <p class="detail-copy">{{ item.summary }}</p>

            <div class="timeline">
              <h3>Timeline</h3>
              @for (entry of item.timelineEntries; track entry.entryId) {
                <div class="timeline__item">
                  <strong>{{ entry.actionType }}</strong>
                  <span>{{ entry.actor }}</span>
                  <p>{{ entry.detail }}</p>
                </div>
              }
            </div>

            <div class="action-stack">
              <div class="section-heading">
                <p class="eyebrow">Case actions</p>
                <h3>Actions</h3>
              </div>

              @if (store.actionMessage()) {
                <p class="inline-success">{{ store.actionMessage() }}</p>
              }

              @if (store.actionError()) {
                <p class="inline-error">{{ store.actionError() }}</p>
              }

              <form class="form-grid" [formGroup]="assignForm" (ngSubmit)="assignSelectedCase()">
                <label><span>Assign to</span><input formControlName="assignee" type="text" /></label>
                <label><span>Assignment note</span><input formControlName="note" type="text" /></label>
                <button class="button" type="submit" [disabled]="store.actionBusy()">Assign case</button>
              </form>

              <form class="form-grid" [formGroup]="noteForm" (ngSubmit)="addNote()">
                <label><span>Analyst note</span><input formControlName="note" type="text" /></label>
                <button class="button button--ghost" type="submit" [disabled]="store.actionBusy()">Add note</button>
              </form>

              <form class="form-grid" [formGroup]="escalationForm" (ngSubmit)="escalate()">
                <label><span>Escalation reason</span><input formControlName="reason" type="text" /></label>
                <button class="button button--ghost" type="submit" [disabled]="store.actionBusy()">Escalate case</button>
              </form>

              <form class="form-grid" [formGroup]="resolutionForm">
                <label><span>Resolution summary</span><input formControlName="resolutionSummary" type="text" /></label>
                <div class="button-row button-row--inline">
                  <button class="button" type="button" (click)="release()" [disabled]="store.actionBusy()">Release payment</button>
                  <button class="button button--ghost" type="button" (click)="confirmDecline()" [disabled]="store.actionBusy()">Confirm decline</button>
                </div>
              </form>
            </div>
          } @else {
            <p class="detail-copy">Select a case to inspect it.</p>
          }
        </aside>
      </section>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CasesPage {
  private readonly formBuilder = inject(FormBuilder);
  protected readonly store = inject(CasesStore);

  protected readonly form = this.formBuilder.nonNullable.group({
    status: 'OPEN',
    minRiskScore: 65,
    paymentId: '',
    unassignedOnly: false
  });

  protected readonly assignForm = this.formBuilder.nonNullable.group({
    assignee: 'analyst.one',
    note: 'Taking ownership for first-line review.'
  });

  protected readonly noteForm = this.formBuilder.nonNullable.group({
    note: 'Customer verified device ownership during outbound callback.'
  });

  protected readonly escalationForm = this.formBuilder.nonNullable.group({
    reason: 'Needs a senior investigator because the beneficiary risk is still unclear.'
  });

  protected readonly resolutionForm = this.formBuilder.nonNullable.group({
    resolutionSummary: 'Resolved after investigator review.'
  });

  constructor() {
    this.applyFilters();
  }

  protected applyFilters(): void {
    const value = this.form.getRawValue();
    this.store.load({
      status: value.status || undefined,
      minRiskScore: value.minRiskScore || null,
      paymentId: value.paymentId || undefined,
      unassignedOnly: value.unassignedOnly
    });
  }

  protected selectCase(caseId: string): void {
    this.store.loadDetail(caseId);
  }

  protected assignSelectedCase(): void {
    const selectedCase = this.store.selectedCase();
    if (!selectedCase) {
      return;
    }

    const value = this.assignForm.getRawValue();
    this.store.assign(selectedCase.caseId, value.assignee, value.note);
  }

  protected addNote(): void {
    const selectedCase = this.store.selectedCase();
    if (!selectedCase) {
      return;
    }

    this.store.addNote(selectedCase.caseId, this.noteForm.getRawValue().note);
  }

  protected escalate(): void {
    const selectedCase = this.store.selectedCase();
    if (!selectedCase) {
      return;
    }

    this.store.escalate(selectedCase.caseId, this.escalationForm.getRawValue().reason);
  }

  protected release(): void {
    const selectedCase = this.store.selectedCase();
    if (!selectedCase) {
      return;
    }

    this.store.release(
      selectedCase.caseId,
      this.resolutionForm.getRawValue().resolutionSummary
    );
  }

  protected confirmDecline(): void {
    const selectedCase = this.store.selectedCase();
    if (!selectedCase) {
      return;
    }

    this.store.confirmDecline(
      selectedCase.caseId,
      this.resolutionForm.getRawValue().resolutionSummary
    );
  }
}
