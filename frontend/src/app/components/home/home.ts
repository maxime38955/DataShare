import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common'; // Indispensable pour utiliser *ngIf
import { UserService } from '../../services/user.service'; // Ajuste le chemin selon ton projet

@Component({
  selector: 'app-home', // ou 'app-hero' selon ton projet
  standalone: true,
  imports: [CommonModule], 
  templateUrl: './home.html',
  styleUrls: ['./home.scss']
})
export class HomeComponent {

  constructor(
    private router: Router,
    private userService: UserService // Injection du service
  ) {}

  // Ce "getter" interroge le service en temps réel
  get isLoggedIn(): boolean {
    return this.userService.isLoggedIn();
  }

  navigateTo(path: string): void {
    this.router.navigate([`/${path}`]);
  }

  logout(): void {
    this.userService.logout(); 
 
  }
}