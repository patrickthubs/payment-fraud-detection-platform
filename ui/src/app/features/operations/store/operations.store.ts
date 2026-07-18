import { inject, Injectable, signal } from '@angular/core';
import { forkJoin } from 'rxjs';

import { FraudApiService } from '../../../core/services/fraud-api.service';
import {
  FraudOutboundDispatchResponse,
  FraudOutboundEvent,
  FraudOperationsSummary,
  FraudOutboundRetryBatchResponse,
  FraudReplayBatch,
  FraudReplayBatchCreateRequest,
  StepUpDeliveryResponse,
  StepUpRevokeResponse,
  StepUpTokenResponse,
  StepUpVerificationResponse
} from '../../../shared/models/api.models';

@Injectable()
export class OperationsStore {
  private readonly api = inject(FraudApiService);

  readonly loading = signal(false);
  readonly error = signal('');
  readonly actionBusy = signal(false);
  readonly actionMessage = signal('');
  readonly actionError = signal('');
  readonly summary = signal<FraudOperationsSummary | null>(null);
  readonly outboundEvents = signal<FraudOutboundEvent[]>([]);
  readonly selectedOutboundEvent = signal<FraudOutboundEvent | null>(null);
  readonly replayBatches = signal<FraudReplayBatch[]>([]);
  readonly selectedReplay = signal<FraudReplayBatch | null>(null);
  readonly stepUpToken = signal<StepUpTokenResponse | null>(null);
  readonly stepUpVerification = signal<StepUpVerificationResponse | null>(null);
  readonly stepUpRevoke = signal<StepUpRevokeResponse | null>(null);
  readonly stepUpDeliveries = signal<StepUpDeliveryResponse[]>([]);

  load(): void {
    this.loading.set(true);
    this.error.set('');

    forkJoin({
      summary: this.api.getSummary(),
      outboundEvents: this.api.listOutboundEvents({ status: 'FAILED', limit: 10 }),
      replayBatches: this.api.listReplays(),
      stepUpDeliveries: this.api.listStepUpDeliveries({ limit: 10 })
    }).subscribe({
      next: ({ summary, outboundEvents, replayBatches, stepUpDeliveries }) => {
        this.summary.set(summary);
        this.outboundEvents.set(outboundEvents);
        const currentEvent = this.selectedOutboundEvent()?.eventId;
        this.selectedOutboundEvent.set(
          outboundEvents.find((item) => item.eventId === currentEvent) ?? outboundEvents[0] ?? null
        );
        this.replayBatches.set(replayBatches.slice(0, 8));
        this.stepUpDeliveries.set(stepUpDeliveries);
        const currentReplay = this.selectedReplay()?.batchId;
        this.selectedReplay.set(
          replayBatches.find((item) => item.batchId === currentReplay) ?? replayBatches[0] ?? null
        );
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Unable to load the outbound operations workspace.');
        this.loading.set(false);
      }
    });
  }

  loadReplay(batchId: string): void {
    this.api.getReplay(batchId).subscribe({
      next: (replay) => this.selectedReplay.set(replay)
    });
  }

  selectOutboundEvent(eventId: string): void {
    const found = this.outboundEvents().find((item) => item.eventId === eventId) ?? null;
    this.selectedOutboundEvent.set(found);
  }

  addIncidentNote(eventId: string, note: string): void {
    this.runAction(
      this.api.addOutboundIncidentNote(eventId, { note }),
      'Incident note saved.',
      () => this.load()
    );
  }

  retryEvent(eventId: string): void {
    this.runAction(
      this.api.retryOutboundEvent(eventId),
      'Outbound event queued for retry.',
      () => this.load()
    );
  }

  retryFailedBatch(limit: number): void {
    this.runAction(
      this.api.retryFailedOutboundEvents(limit),
      'Failed outbound batch queued for retry.',
      () => this.load()
    );
  }

  dispatchNow(): void {
    this.runAction(
      this.api.dispatchOutboundEventsNow(),
      'Dispatch sweep triggered successfully.',
      () => this.load()
    );
  }

  createReplay(request: FraudReplayBatchCreateRequest): void {
    this.runAction(
      this.api.createReplay(request),
      'Replay batch created successfully.',
      () => this.load()
    );
  }

  generateStepUpToken(): void {
    this.runAction(
      this.api.generateStepUpToken(),
      'Step-up token generated successfully.',
      (result) => {
        this.stepUpToken.set(result as StepUpTokenResponse);
        this.load();
      }
    );
  }

  resendStepUpToken(): void {
    this.runAction(
      this.api.resendStepUpToken(),
      'Step-up token resent successfully.',
      (result) => {
        this.stepUpToken.set(result as StepUpTokenResponse);
        this.load();
      }
    );
  }

  verifyStepUpToken(token: string): void {
    this.runAction(
      this.api.verifyStepUpToken(token),
      'Step-up verification completed successfully.',
      (result) => {
        this.stepUpVerification.set(result as StepUpVerificationResponse);
        this.load();
      }
    );
  }

  revokeStepUp(): void {
    this.runAction(
      this.api.revokeStepUp(),
      'Outstanding step-up tokens revoked.',
      (result) => {
        this.stepUpRevoke.set(result as StepUpRevokeResponse);
        this.load();
      }
    );
  }

  private runAction(
    request$: { subscribe: Function },
    successMessage: string,
    afterSuccess?: (result: unknown) => void
  ): void {
    this.actionBusy.set(true);
    this.actionMessage.set('');
    this.actionError.set('');

    request$.subscribe({
      next: (result: unknown) => {
        afterSuccess?.(result);
        this.actionMessage.set(successMessage);
        this.actionBusy.set(false);
      },
      error: () => {
        this.actionError.set('That operations command could not be completed with the current operator session.');
        this.actionBusy.set(false);
      }
    });
  }
}
