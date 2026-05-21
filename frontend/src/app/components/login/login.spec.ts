import { TestBed, ComponentFixture } from '@angular/core/testing';
import { LoginComponent } from './login'; // Ajuste si le fichier s'appelle login.ts
import { Router } from '@angular/router';
import { UserService } from '../../services/user.service';
import { of, throwError } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
 import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;

  // Déclaration de nos mocks
  let mockRouter: any;
  let mockUserService: any;

  beforeEach(async () => {
    // 1. Initialisation des Mocks avec vi
    mockRouter = {
      navigate: vi.fn()
    };

    mockUserService = {
      login: vi.fn()
    };

    // 2. Configuration du module de test
    await TestBed.configureTestingModule({
      imports: [LoginComponent, CommonModule, FormsModule], // Standalone + Modules nécessaires
      providers: [
        { provide: Router, useValue: mockRouter },
        { provide: UserService, useValue: mockUserService }
      ]
    }).compileComponents();

    // 3. Création du composant
    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.clearAllMocks(); // On nettoie les appels après chaque test
  });

  it('devrait créer le composant', () => {
    expect(component).toBeTruthy();
  });

  describe('Méthode navigateTo', () => {
    it('devrait naviguer vers la route spécifiée', () => {
      // Act
      component.navigateTo('register');

      // Assert
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/register']);
    });
  });

  describe('Méthode onSubmit', () => {
    it('devrait appeler login, vider les erreurs et rediriger vers le profil en cas de succès', () => {
      // Arrange
      component.email = 'test@mail.com';
      component.password = 'password123';
      component.errorMessage = 'Ancienne erreur'; // Pour vérifier qu'elle est bien vidée
      
      // On simule une réponse HTTP réussie
      mockUserService.login.mockReturnValue(of({ token: 'fake-jwt-token' }));

      // Act
      component.onSubmit();

      // Assert
      expect(component.errorMessage).toBe(''); // Vérifie que l'erreur est vidée au début
      expect(mockUserService.login).toHaveBeenCalledWith('test@mail.com', 'password123');
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/profil']); // Redirection ok
    });

    it('devrait afficher un message d\'erreur si la connexion échoue', () => {
      // Arrange
      component.email = 'test@mail.com';
      component.password = 'mauvais_pass';
      
      // On simule une erreur HTTP (ex: 401 Unauthorized)
      mockUserService.login.mockReturnValue(throwError(() => new Error('Unauthorized')));

      // Act
      component.onSubmit();

      // Assert
      expect(mockUserService.login).toHaveBeenCalledWith('test@mail.com', 'mauvais_pass');
      expect(component.errorMessage).toBe('Identifiants incorrects. Veuillez réessayer.');
      expect(mockRouter.navigate).not.toHaveBeenCalled(); // On ne doit PAS être redirigé
    });
  });
});