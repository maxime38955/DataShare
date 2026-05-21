import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { UserService } from '../../services/user.service'; // Ajuste le chemin selon ton arborescence

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './register.html',
  styleUrls: ['./register.scss']

  })
export class RegisterComponent {
  email = '';
  password = '';
  errorMessage = '';
  successMessage = '';

  constructor(
    private userService: UserService,
    private router: Router
  ) {}

  onSubmit(): void {
    this.errorMessage = '';
    this.successMessage = '';

    console.log("Tentative d'inscription pour :", this.email);

    // Appel à ton service connecté au DTO JSON
    this.userService.register(this.email, this.password).subscribe({
      next: (response) => {
        console.log('Inscription réussie !', response);
        this.successMessage = response.message || 'Compte créé avec succès !';
        
        // Puisque le backend renvoie le token dès le register, on peut connecter l'utilisateur immédiatement
        if (response.token) {
          localStorage.setItem('auth_token', response.token);
        }

        // Petite pause de 1.5s pour laisser l'utilisateur voir le message de succès avant redirection
        setTimeout(() => {
          this.router.navigate(['/profil']);
        }, 1500);
      },
      error: (err) => {
        console.error("Erreur lors de l'inscription :", err);
        // Si ton backend renvoie une erreur brute (ex: "Cet email est déjà utilisé")
        if (err.error && typeof err.error === 'string') {
          this.errorMessage = err.error;
        } else {
          this.errorMessage = "Une erreur est survenue lors de l'inscription. Veuillez réessayer.";
        }
      }
    });
  }

  navigateTo(path: string): void {
    this.router.navigate([`/${path}`]);
  }
}