import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-home',
  imports: [],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class HomeComponent {

  // On injecte le routeur Angular pour gérer la navigation
  constructor(private router: Router) {}

  /**
   * Méthode déclenchée au clic sur les boutons
   * @param path Le chemin de la route (ex: 'login', 'register')
   */
  navigateTo(path: string): void {
    console.log(`Bouton cliqué ! Navigation vers : /${path}`);
    
    // Décommente la ligne ci-dessous quand tu auras configuré ton app-routing.module.ts
    this.router.navigate([`/${path}`]);
  }
}