import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface AppToast {
  id: number;
  kind: 'success' | 'error';
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private nextId = 0;
  private readonly toastsSubject = new BehaviorSubject<AppToast[]>([]);
  readonly toasts$ = this.toastsSubject.asObservable();

  success(message: string): void {
    this.show('success', message);
  }

  error(message: string): void {
    this.show('error', message);
  }

  dismiss(id: number): void {
    this.toastsSubject.next(this.toastsSubject.value.filter(toast => toast.id !== id));
  }

  private show(kind: AppToast['kind'], message: string): void {
    const toast: AppToast = { id: ++this.nextId, kind, message };
    this.toastsSubject.next([...this.toastsSubject.value, toast]);
    window.setTimeout(() => this.dismiss(toast.id), 4000);
  }
}
