import { TestBed, ComponentFixture } from '@angular/core/testing';
import { HomeComponent } from './home'; // Ajuste si ton fichier s'appelle home.ts
import { Router } from '@angular/router';
import { UserService } from '../../services/user.service';
 import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';

describe('HomeComponent', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;
  
  // Déclaration des Mocks
  let mockRouter: any;
  let mockUserService: any;

  beforeEach(async () => {
    // Initialisation des Mocks avec vi
    mockRouter = {
      navigate: vi.fn()
    };

    mockUserService = {
      isLoggedIn: vi.fn().mockReturnValue(false), // Par défaut, non connecté
      logout: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [HomeComponent], // Le composant est standalone
      providers: [
        { provide: Router, useValue: mockRouter },
        { provide: UserService, useValue: mockUserService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges(); // Déclenche le cycle de vie initial
  });

  afterEach(() => {
    vi.clearAllMocks(); // Nettoie les appels après chaque test
  });

  it('devrait créer le composant', () => {
    expect(component).toBeTruthy();
  });

  describe('Méthode navigateTo', () => {
    it('devrait naviguer vers la route demandée avec le bon chemin', () => {
      // Act
      component.navigateTo('login');

      // Assert
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/login']);
    });

    it('devrait naviguer vers la racine si une chaîne vide est passée', () => {
      // Act
      component.navigateTo('');

      // Assert
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/']);
    });
  });

  describe('Méthode logout', () => {
    it('devrait appeler la méthode logout du UserService', () => {
      // Act
      component.logout();

      // Assert
      expect(mockUserService.logout).toHaveBeenCalled();
    });
  });

  describe('Getter isLoggedIn', () => {
    it('devrait retourner true si le UserService indique que l\'utilisateur est connecté', () => {
      // Arrange : on force le mock à renvoyer true pour ce test spécifique
      mockUserService.isLoggedIn.mockReturnValue(true);

      // Act
      const result = component.isLoggedIn;

      // Assert
      expect(result).toBe(true);
      expect(mockUserService.isLoggedIn).toHaveBeenCalled();
    });

    it('devrait retourner false si le UserService indique que l\'utilisateur n\'est pas connecté', () => {
      // Arrange
      mockUserService.isLoggedIn.mockReturnValue(false);

      // Act
      const result = component.isLoggedIn;

      // Assert
      expect(result).toBe(false);
      expect(mockUserService.isLoggedIn).toHaveBeenCalled();
    });
  });
});