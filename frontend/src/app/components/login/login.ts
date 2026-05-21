import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
// 1. On importe FormsModule (pour [(ngModel)]) et CommonModule (pour *ngIf)
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-login',
  standalone: true, // 2. Indique que c'est un composant indépendant
  imports: [CommonModule, FormsModule], // 3. On déclare les modules utilisés ici
  templateUrl: './login.html',
  styleUrls: ['./login.scss']
})
export class LoginComponent {
  private userService = inject(UserService);

  email = '';
  password = '';
  passwordConf = '';
  errorMessage = '';

  constructor(private router: Router) {}

 onSubmit(): void {
    this.errorMessage = '';
    console.log('Tentative de connexion avec :', this.email);

    // On déclenche la requête HTTP en utilisant .subscribe()
    this.userService.login(this.email, this.password).subscribe({
      
      // 'next' s'exécute si le serveur répond avec un Succès (ex: 200 OK)
      next: (response) => {
        console.log('Connexion réussie ! Token reçu.');
        // Le token est déjà sauvegardé par le tap() dans ton service, 
        // on a juste à rediriger l'utilisateur !
        this.router.navigate(['/profil']);
      },

      // 'error' s'exécute si le serveur renvoie une erreur (ex: 401 Unauthorized)
      error: (err) => {
        console.error('Erreur lors de la connexion :', err);
        this.errorMessage = 'Identifiants incorrects. Veuillez réessayer.';
      }
      
    });
  }

  navigateTo(path: string): void {
    this.router.navigate([`/${path}`]);
  }
}