import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { EMPTY } from 'rxjs';
import { catchError, distinctUntilChanged, map, switchMap } from 'rxjs/operators';

import { ContactService } from '../../../../core/services/contact.service';
import { Contact } from '../../models/contact.model';
import { StatusMessageComponent } from '../../../../shared/status-message/status-message.component';

@Component({
  selector: 'app-contact-details',
  standalone: true,
  imports: [CommonModule, RouterLink, StatusMessageComponent],
  templateUrl: './contact-details.component.html'
})
export class ContactDetailsComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);

  contact: Contact | null = null;
  loading = false;
  missing = false;
  errorMessage: string | null = null;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly contactService: ContactService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.pipe(
      map(params => params.get('id')),
      distinctUntilChanged(),
      switchMap(id => this.loadContact(id)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(contact => {
      this.contact = contact;
      this.loading = false;
    });
  }

  display(value: string | null | undefined): string {
    const trimmed = value?.trim() ?? '';
    return trimmed.length === 0 ? '—' : trimmed;
  }

  private loadContact(id: string | null) {
    this.contact = null;
    this.missing = false;
    this.errorMessage = null;

    const numericId = Number(id);
    if (!Number.isInteger(numericId) || numericId <= 0) {
      this.loading = false;
      this.missing = true;
      this.errorMessage = 'This contact could not be found.';
      return EMPTY;
    }

    this.loading = true;
    return this.contactService.getContact(numericId).pipe(
      catchError((error: HttpErrorResponse) => {
        this.loading = false;
        this.missing = error.status === 404;
        this.errorMessage = error.status === 404
          ? 'This contact could not be found.'
          : 'Unable to load this contact. Make sure the backend is running and try again.';
        return EMPTY;
      })
    );
  }
}
