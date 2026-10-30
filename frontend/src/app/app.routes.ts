import { Routes } from '@angular/router';

import { ContactDetailsComponent } from './features/contacts/components/contact-details/contact-details.component';
import { ContactFormComponent } from './features/contacts/components/contact-form/contact-form.component';
import { ContactListComponent } from './features/contacts/components/contact-list/contact-list.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'contacts' },
  { path: 'contacts/new', component: ContactFormComponent, data: { mode: 'create' } },
  { path: 'contacts/:id/edit', component: ContactFormComponent, data: { mode: 'edit' } },
  { path: 'contacts/:id', component: ContactDetailsComponent },
  { path: 'contacts', component: ContactListComponent },
  { path: '**', redirectTo: 'contacts' }
];
