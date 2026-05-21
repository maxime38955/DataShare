import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

// Optionnel mais recommandé : définir une interface pour la réponse
export interface AuthResponse {
  token: string;
  email: string;
  message: string;
}

@Injectable({
  providedIn: 'root',
})
export class UserService {

  // Assure-toi que cette URL correspond bien à ton @RequestMapping Java
  private apiUrl = 'http://localhost:8080/api/v1/user'; 

  constructor(private http: HttpClient) {}

  /**
   * Inscription : On envoie un objet JSON { email, password }
   */
  register(email: string, password: string): Observable<AuthResponse> {
    const registerRequest = { email, password }; // C'est l'équivalent de ton RegisterRequest.java
    
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, registerRequest);
  }

  /**
   * Connexion : On envoie un objet JSON { email, password }
   */
  login(email: string, password: string): Observable<AuthResponse> {
    const loginRequest = { email, password }; // C'est l'équivalent de ton LoginRequest.java

    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, loginRequest).pipe(
      tap(response => {
        if (response && response.token) {
         
          this.saveToken(response.token);
          // Tu peux aussi stocker l'email si tu en as besoin pour l'affichage
          localStorage.setItem('user_email', response.email);
        }
      })
    );
  }

  private saveToken(token: string): void {
    localStorage.setItem('auth_token', token);
  }

  getToken(): string | null {
    return localStorage.getItem('auth_token');
  }

  logout(): void {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('user_email');
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }
}