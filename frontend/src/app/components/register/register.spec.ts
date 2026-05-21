import { TestBed, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { RegisterComponent } from './register';
import { Router } from '@angular/router';
import { UserService } from '../../services/user.service';
import { of, throwError } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
 import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;
  
  let mockRouter: any;
  let mockUserService: any;

  beforeEach(async () => {
    mockRouter = { navigate: vi.fn() };
    mockUserService = { register: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [RegisterComponent, CommonModule, FormsModule],
      providers: [
        { provide: Router, useValue: mockRouter },
        { provide: UserService, useValue: mockUserService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('devrait créer le composant', () => {
    expect(component).toBeTruthy();
  });

  it('devrait enregistrer le token et naviguer vers profil après une inscription réussie', async() => {
    // Arrange
    component.email = 'nouveau@test.com';
    component.password = 'password123';
    const mockResponse = { token: 'fake-token', message: 'Succès' };
    mockUserService.register.mockReturnValue(of(mockResponse));

    // Act
    component.onSubmit();

    // Assert immédiat
    expect(mockUserService.register).toHaveBeenCalledWith('nouveau@test.com', 'password123');
    expect(component.successMessage).toBe('Succès');
    expect(localStorage.getItem('auth_token')).toBe('fake-token');

    // Attendre la fin du setTimeout (1500ms)
   await new Promise(resolve => setTimeout(resolve, 1500));
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/profil']);
  });

  it('devrait afficher le message d\'erreur si le serveur renvoie une erreur', () => {
    // Arrange
    component.email = 'deja@pris.com';
    mockUserService.register.mockReturnValue(throwError(() => ({ error: 'Cet email est déjà utilisé.' })));

    // Act
    component.onSubmit();

    // Assert
    expect(component.errorMessage).toBe('Cet email est déjà utilisé.');
    expect(mockRouter.navigate).not.toHaveBeenCalled();
  });

  it('devrait afficher un message d\'erreur générique si l\'erreur est inconnue', () => {
    // Arrange
    mockUserService.register.mockReturnValue(throwError(() => ({ error: null })));

    // Act
    component.onSubmit();

    // Assert
    expect(component.errorMessage).toBe("Une erreur est survenue lors de l'inscription. Veuillez réessayer.");
  });
});