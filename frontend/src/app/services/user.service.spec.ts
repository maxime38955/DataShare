
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthResponse, UserService } from './user.service';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';

describe('UserService', () => {
  let service: UserService;
  let httpMock: HttpTestingController;
  const apiUrl = 'http://localhost:8080/api/v1/user';
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [UserService]
    });
    service = TestBed.inject(UserService);
    httpMock = TestBed.inject(HttpTestingController);
    
    // Nettoyage TOTAL avant chaque test
    localStorage.clear();
    vi.clearAllMocks();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear(); // Nettoyage TOTAL après chaque test
  });
  it('devrait être créé', () => {
    expect(service).toBeTruthy();
  });

  it('devrait envoyer une requête POST à /register', () => {
    const mockResponse: AuthResponse = { token: 'tok123', email: 't@t.com', message: 'Ok' };

    service.register('t@t.com', 'pass123').subscribe(res => {
      expect(res).toEqual(mockResponse);
    });

    const req = httpMock.expectOne(`${apiUrl}/register`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 't@t.com', password: 'pass123' });
    req.flush(mockResponse);
  });

  it('devrait stocker le token et l\'email lors du login réussi', () => {
    const mockResponse: AuthResponse = { token: 'tok-abc', email: 'test@mail.com', message: 'Connexion réussie' };

    service.login('test@mail.com', 'password123').subscribe(res => {
      expect(res.token).toBe('tok-abc');
      expect(localStorage.getItem('auth_token')).toBe('tok-abc');
      expect(localStorage.getItem('user_email')).toBe('test@mail.com');
    });

    const req = httpMock.expectOne(`${apiUrl}/login`);
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);
  });

  it('devrait gérer la déconnexion en supprimant les items du localStorage', () => {
    localStorage.setItem('auth_token', 'xyz');
    localStorage.setItem('user_email', 'test@mail.com');

    service.logout();

    expect(localStorage.getItem('auth_token')).toBeNull();
    expect(localStorage.getItem('user_email')).toBeNull();
  });

  it('devrait retourner true si isLoggedIn est appelé avec un token', () => {
    localStorage.setItem('auth_token', 'fake-token');
    expect(service.isLoggedIn()).toBe(true);
  });

  it('devrait retourner false si isLoggedIn est appelé sans token', () => {
    expect(service.isLoggedIn()).toBe(false);
  });
});