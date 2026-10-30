import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Contact, ContactRequest } from '../../features/contacts/models/contact.model';

@Injectable({
  providedIn: 'root'
})
export class ContactService {
  private readonly apiUrl = 'http://localhost:8080/api/contacts';

  constructor(private readonly http: HttpClient) {}

  getContacts(search?: string): Observable<Contact[]> {
    const term = search?.trim();
    if (!term) {
      return this.http.get<Contact[]>(this.apiUrl);
    }
    return this.http.get<Contact[]>(this.apiUrl, { params: { search: term } });
  }

  getContact(id: number): Observable<Contact> {
    return this.http.get<Contact>(`${this.apiUrl}/${id}`);
  }

  createContact(request: ContactRequest): Observable<Contact> {
    return this.http.post<Contact>(this.apiUrl, request);
  }

  updateContact(id: number, request: ContactRequest): Observable<Contact> {
    return this.http.put<Contact>(`${this.apiUrl}/${id}`, request);
  }

  deleteContact(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
