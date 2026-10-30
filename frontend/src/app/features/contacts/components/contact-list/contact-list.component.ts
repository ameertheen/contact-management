import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';

import { ContactService } from '../../../../core/services/contact.service';
import { ToastService } from '../../../../shared/toast/toast.service';
import { Contact } from '../../models/contact.model';
import { StatusMessageComponent } from '../../../../shared/status-message/status-message.component';

@Component({
  selector: 'app-contact-list',
  standalone: true,
  imports: [CommonModule, RouterLink, StatusMessageComponent],
  templateUrl: './contact-list.component.html',
  styleUrls: ['./contact-list.component.css']
})
export class ContactListComponent implements OnInit, OnDestroy {
  readonly pageSizeOptions: readonly number[] = [5, 10, 20];

  contacts: Contact[] = [];
  loading = false;
  errorMessage: string | null = null;
  deletingId: number | null = null;
  searchTerm = '';
  pageSize = 5;
  currentPage = 1;

  private contactsSubscription?: Subscription;
  private deleteSubscription?: Subscription;

  constructor(
    private readonly contactService: ContactService,
    private readonly toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.loadContacts(true);
  }

  ngOnDestroy(): void {
    this.contactsSubscription?.unsubscribe();
    this.deleteSubscription?.unsubscribe();
  }

  get totalContacts(): number {
    return this.contacts.length;
  }

  get totalPages(): number {
    if (this.totalContacts === 0) {
      return 1;
    }
    return Math.ceil(this.totalContacts / this.pageSize);
  }

  get pageContacts(): Contact[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.contacts.slice(start, start + this.pageSize);
  }

  get rangeStart(): number {
    if (this.totalContacts === 0) {
      return 0;
    }
    return (this.currentPage - 1) * this.pageSize + 1;
  }

  get rangeEnd(): number {
    return Math.min(this.currentPage * this.pageSize, this.totalContacts);
  }

  get pageNumbers(): number[] {
    return Array.from({ length: this.totalPages }, (_, index) => index + 1);
  }

  get isFirstPage(): boolean {
    return this.currentPage <= 1;
  }

  get isLastPage(): boolean {
    return this.currentPage >= this.totalPages;
  }

  get hasSearch(): boolean {
    return this.searchTerm.trim().length > 0;
  }

  trackById(_index: number, contact: Contact): number {
    return contact.id;
  }

  displayValue(value: string | null | undefined): string {
    const trimmed = value?.trim() ?? '';
    return trimmed.length === 0 ? '—' : trimmed;
  }

  setPage(page: number): void {
    if (page < 1 || page > this.totalPages) {
      return;
    }
    this.currentPage = page;
  }

  onPageSizeChange(event: Event): void {
    const select = event.target;
    if (!(select instanceof HTMLSelectElement)) {
      return;
    }
    const size = Number(select.value);
    if (!this.pageSizeOptions.includes(size)) {
      return;
    }
    this.pageSize = size;
    this.currentPage = 1;
  }

  onSearchInput(event: Event): void {
    const input = event.target;
    if (input instanceof HTMLInputElement) {
      this.searchTerm = input.value;
    }
  }

  search(event: Event): void {
    const form = event.target;
    if (form instanceof HTMLFormElement) {
      const input = form.querySelector('input');
      this.searchTerm = input?.value ?? '';
    }
    this.currentPage = 1;
    this.loadContacts(true);
  }

  deleteContact(contact: Contact): void {
    if (this.deletingId !== null) {
      return;
    }

    const confirmed = window.confirm(`Delete ${contact.firstName} ${contact.lastName}? This cannot be undone.`);
    if (!confirmed) {
      return;
    }

    this.deletingId = contact.id;
    this.errorMessage = null;
    this.deleteSubscription?.unsubscribe();
    this.deleteSubscription = this.contactService.deleteContact(contact.id).subscribe({
      next: () => {
        this.deletingId = null;
        this.toastService.success('Contact deleted.');
        this.loadContacts(false);
      },
      error: () => {
        this.deletingId = null;
        this.errorMessage = 'Unable to delete this contact. Please try again.';
        this.toastService.error(this.errorMessage);
      }
    });
  }

  private loadContacts(showLoader: boolean): void {
    if (showLoader) {
      this.loading = true;
    }
    this.errorMessage = null;
    this.contactsSubscription?.unsubscribe();
    this.contactsSubscription = this.contactService.getContacts(this.searchTerm).subscribe({
      next: (contacts: Contact[]) => {
        this.contacts = contacts;
        this.loading = false;
        this.clampPage();
      },
      error: () => {
        this.contacts = [];
        this.loading = false;
        this.currentPage = 1;
        this.errorMessage = 'Unable to load contacts. Make sure the backend is running and try again.';
      }
    });
  }

  private clampPage(): void {
    if (this.currentPage > this.totalPages) {
      this.currentPage = this.totalPages;
    }
    if (this.currentPage < 1) {
      this.currentPage = 1;
    }
  }
}
