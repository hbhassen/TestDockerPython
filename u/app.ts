import { Component, OnInit, signal } from '@angular/core';
import { WhoAmIService } from './whoami.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.html',
  standalone: false,
  styleUrl: './app.css'
})
export class App implements OnInit {
  private static readonly INITIAL_SSO_KEY = 'demo2-initial-sso-done';
  protected readonly message = signal('Chargement...');
  protected readonly error = signal<string | null>(null);
  protected readonly apiMessage = signal<string | null>(null);
  protected readonly apiError = signal<string | null>(null);

  constructor(private readonly whoAmIService: WhoAmIService) {}

  ngOnInit(): void {
    if (this.shouldRunInitialSsoRedirect()) {
      this.markInitialSsoRedirectAsDone();
      window.location.assign(this.whoAmIService.getSamlLoginUrl());
      return;
    }

    this.loadWhoAmI();
  }

  private loadWhoAmI(): void {
    this.whoAmIService.getWhoAmI().subscribe({
      next: (response) => {
        if (!response) {
          this.message.set('Redirection vers IdP...');
          this.error.set(null);
          return;
        }
        const nameId = response?.nameId ?? '';
        this.message.set(`bienvenu ${nameId}`);
        this.error.set(null);
      },
      error: () => {
        this.message.set('bienvenu');
        this.error.set('Impossible de charger le profil.');
      }
    });
  }

  onFetchMessage(): void {
    this.apiError.set(null);
    this.apiMessage.set(null);
    this.whoAmIService.getMessage().subscribe({
      next: (response) => {
        this.apiMessage.set(response?.message ?? '');
      },
      error: () => {
        this.apiError.set('Erreur lors de l appel API.');
      }
    });
  }

  private shouldRunInitialSsoRedirect(): boolean {
    return sessionStorage.getItem(App.INITIAL_SSO_KEY) !== 'true';
  }

  private markInitialSsoRedirectAsDone(): void {
    sessionStorage.setItem(App.INITIAL_SSO_KEY, 'true');
  }
}
