export interface Contact {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  company?: string | null;
  jobTitle?: string | null;
  address?: string | null;
  notes?: string | null;
}

export type ContactRequest = Omit<Contact, 'id'>;
