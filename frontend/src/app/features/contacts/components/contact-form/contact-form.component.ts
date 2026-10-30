import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { EMPTY, combineLatest } from 'rxjs';
import { catchError, distinctUntilChanged, map, switchMap } from 'rxjs/operators';

import { ContactService } from '../../../../core/services/contact.service';
import { ToastService } from '../../../../shared/toast/toast.service';
import { Contact, ContactRequest } from '../../models/contact.model';
import { StatusMessageComponent } from '../../../../shared/status-message/status-message.component';

function requiredTrimmed(control: AbstractControl): ValidationErrors | null {
  const value = control.value;
  if (typeof value !== 'string' || value.trim().length === 0) {
    return { required: true };
  }
  return null;
}

@Component({
  selector: 'app-contact-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, StatusMessageComponent],
  templateUrl: './contact-form.component.html'
})
export class ContactFormComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);

  loading = false;
  saving = false;
  submitted = false;
  missing = false;
  errorMessage: string | null = null;
  serverMessages: string[] = [];
  contactId: number | null = null;

  readonly contactForm = this.formBuilder.group({
    firstName: ['', [requiredTrimmed, Validators.maxLength(100)]],
    lastName: ['', [requiredTrimmed, Validators.maxLength(100)]],
    email: ['', [requiredTrimmed, Validators.email, Validators.maxLength(255)]],
    phone: ['', [requiredTrimmed, Validators.pattern(/^[6-9]\d{9}$/)]],
    company: ['', Validators.maxLength(150)],
    jobTitle: ['', Validators.maxLength(150)],
    address: ['', Validators.maxLength(255)],
    notes: ['', Validators.maxLength(2000)]
  });

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly contactService: ContactService,
    private readonly router: Router,
    private readonly toastService: ToastService
  ) {}

  ngOnInit(): void {
    combineLatest([this.route.data, this.route.paramMap]).pipe(
      map(([data, params]) => ({
        edit: data['mode'] === 'edit',
        id: params.get('id')
      })),
      distinctUntilChanged((previous, current) => previous.edit === current.edit && previous.id === current.id),
      switchMap(({ edit, id }) => this.loadRoute(edit, id)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(contact => {
      this.contactForm.patchValue({
        firstName: contact.firstName,
        lastName: contact.lastName,
        email: contact.email,
        phone: contact.phone,
        company: contact.company ?? '',
        jobTitle: contact.jobTitle ?? '',
        address: contact.address ?? '',
        notes: contact.notes ?? ''
      });
      this.loading = false;
    });
  }

  get isEdit(): boolean {
    return this.contactId !== null;
  }

  showError(control: AbstractControl): boolean {
    return control.invalid && (control.touched || this.submitted);
  }

  fieldError(control: AbstractControl, requiredMessage: string, maxLengthMessage: string, emailMessage?: string): string | null {
    if (!this.showError(control)) {
      return null;
    }
    if (control.hasError('required')) {
      return requiredMessage;
    }
    if (emailMessage && control.hasError('email')) {
      return emailMessage;
    }
    if (control.hasError('pattern')) {
      return 'Enter a valid 10-digit mobile number starting with 6, 7, 8, or 9.';
    }
    if (control.hasError('maxlength')) {
      return maxLengthMessage;
    }
    return null;
  }

  onEnter(event: Event): void {
    if (event.target instanceof HTMLTextAreaElement) {
      return;
    }
    if (this.contactForm.invalid) {
      event.preventDefault();
      this.onSubmit();
    }
  }

  onSubmit(): void {
    this.submitted = true;
    this.errorMessage = null;
    this.serverMessages = [];

    if (this.contactForm.invalid) {
      this.contactForm.markAllAsTouched();
      return;
    }
    if (this.saving) {
      return;
    }

    const request = this.toRequest();
    this.saving = true;
    const save$ = this.contactId === null
      ? this.contactService.createContact(request)
      : this.contactService.updateContact(this.contactId, request);

    save$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.toastService.success(this.contactId === null ? 'Contact created.' : 'Contact updated.');
        void this.router.navigate(['/contacts']);
      },
      error: (error: HttpErrorResponse) => {
        this.saving = false;
        this.applyHttpError(error, 'save');
        const message = this.errorMessage ?? this.serverMessages[0];
        if (message) {
          this.toastService.error(message);
        }
      }
    });
  }

  private loadRoute(edit: boolean, id: string | null) {
    this.submitted = false;
    this.saving = false;
    this.missing = false;
    this.errorMessage = null;
    this.serverMessages = [];
    this.contactForm.reset({
      firstName: '',
      lastName: '',
      email: '',
      phone: '',
      company: '',
      jobTitle: '',
      address: '',
      notes: ''
    });

    if (!edit) {
      this.contactId = null;
      this.loading = false;
      return EMPTY;
    }

    const numericId = Number(id);
    if (!Number.isInteger(numericId) || numericId <= 0) {
      this.contactId = null;
      this.loading = false;
      this.missing = true;
      this.errorMessage = 'This contact could not be found.';
      return EMPTY;
    }

    this.contactId = numericId;
    this.loading = true;
    return this.contactService.getContact(numericId).pipe(
      catchError((error: HttpErrorResponse) => {
        this.loading = false;
        this.applyHttpError(error, 'load');
        return EMPTY;
      })
    );
  }

  private toRequest(): ContactRequest {
    const value = this.contactForm.getRawValue();
    return {
      firstName: this.requiredText(value.firstName),
      lastName: this.requiredText(value.lastName),
      email: this.requiredText(value.email),
      phone: this.requiredText(value.phone),
      company: this.optionalText(value.company),
      jobTitle: this.optionalText(value.jobTitle),
      address: this.optionalText(value.address),
      notes: this.optionalText(value.notes)
    };
  }

  private requiredText(value: string | null): string {
    return value?.trim() ?? '';
  }

  private optionalText(value: string | null): string | null {
    const trimmed = value?.trim() ?? '';
    return trimmed.length === 0 ? null : trimmed;
  }

  private applyHttpError(error: HttpErrorResponse, action: 'load' | 'save'): void {
    const messages = this.messagesFrom(error);
    this.serverMessages = [];

    if (error.status === 400 && messages.length > 0) {
      this.serverMessages = messages;
      this.errorMessage = null;
      return;
    }
    if (error.status === 400) {
      this.errorMessage = 'Some fields are invalid. Check the form and try again.';
      return;
    }
    if (error.status === 404) {
      this.missing = true;
      this.errorMessage = 'This contact could not be found.';
      return;
    }
    if (error.status === 409) {
      const message = messages[0] ?? '';
      if (message.toLowerCase().includes('mobile') || message.toLowerCase().includes('phone')) {
        this.errorMessage = 'This mobile number is already registered.';
        return;
      }
      this.errorMessage = message.length > 0 ? message : 'This email address is already in use.';
      return;
    }
    this.errorMessage = action === 'load'
      ? 'Unable to load this contact. Make sure the backend is running and try again.'
      : 'Unable to save this contact. Make sure the backend is running and try again.';
  }

  private messagesFrom(error: HttpErrorResponse): string[] {
    const body: unknown = error.error;
    if (typeof body !== 'object' || body === null || !('messages' in body)) {
      return [];
    }
    const messages = (body as { messages?: unknown }).messages;
    if (!Array.isArray(messages)) {
      return [];
    }
    return messages.filter((message): message is string => typeof message === 'string' && message.trim().length > 0);
  }
}
