import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-status-message',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './status-message.component.html'
})
export class StatusMessageComponent {
  @Input() loading = false;
  @Input() loadingText = 'Loading...';
  @Input() errorMessage: string | null = null;
}
