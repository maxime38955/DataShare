import { TestBed, ComponentFixture } from '@angular/core/testing';
import { ProfilComponent } from './profil';
import { FileService } from '../../services/file.service';
import { UserService } from '../../services/user.service';
import { Router } from '@angular/router';
import { ChangeDetectorRef } from '@angular/core';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
describe('ProfilComponent', () => {
  let component: ProfilComponent;
  let fixture: ComponentFixture<ProfilComponent>;

  let mockFileService: any;
  let mockUserService: any;
  let mockRouter: any;
  let mockCdr: any;

  const mockFiles = [
    { fileId: 1, name: 'photo.jpg', expirationDate: '2099-01-01T00:00:00Z' },
    { fileId: 2, name: 'vieux.txt', expirationDate: '2020-01-01T00:00:00Z' } // Déjà expiré
  ];

  beforeEach(async () => {
    mockFileService = { getUserFiles: vi.fn().mockReturnValue(of(mockFiles)), deleteFile: vi.fn().mockReturnValue(of({})) };
    mockUserService = { logout: vi.fn() };
    mockRouter = { navigate: vi.fn() };
    mockCdr = { detectChanges: vi.fn() };

    // Mock du localStorage
    Storage.prototype.getItem = vi.fn(() => 'header.eyJzdWIiOiJ0ZXN0QG1haWwuY29tIn0.signature');

    await TestBed.configureTestingModule({
      imports: [ProfilComponent],
      providers: [
        { provide: FileService, useValue: mockFileService },
        { provide: UserService, useValue: mockUserService },
        { provide: Router, useValue: mockRouter },
        { provide: ChangeDetectorRef, useValue: mockCdr }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProfilComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // Au lieu de la ligne globale dans profil.spec.ts :
it('devrait extraire le bon email du token au chargement', () => {
  // Mock local uniquement pour ce test
  vi.spyOn(Storage.prototype, 'getItem').mockReturnValue('header.eyJzdWIiOiJ0ZXN0QG1haWwuY29tIn0.signature');
  
  // Force le rechargement pour prendre en compte le nouveau mock
  component.ngOnInit(); 
  expect(component.userEmail).toBe('test@mail.com');
  
  // Nettoyage après le test
  vi.restoreAllMocks();
});

  it('devrait charger les fichiers au chargement', () => {
    expect(mockFileService.getUserFiles).toHaveBeenCalled();
    expect(component.files.length).toBe(2);
  });

  it('devrait filtrer les fichiers actifs', () => {
    component.filterStatus = 'ACTIVE';
    expect(component.filteredFiles.length).toBe(1);
    expect(component.filteredFiles[0].name).toBe('photo.jpg');
  });

  it('devrait filtrer les fichiers expirés', () => {
    component.filterStatus = 'EXPIRED';
    expect(component.filteredFiles.length).toBe(1);
    expect(component.filteredFiles[0].name).toBe('vieux.txt');
  });

  it('devrait retourner l\'émoji correspondant à l\'extension', () => {
    expect(component.getFileEmoji('image.jpg')).toBe('🖼️');
    expect(component.getFileEmoji('document.pdf')).toBe('📕');
    expect(component.getFileEmoji('archive.zip')).toBe('📦');
    expect(component.getFileEmoji('test')).toBe('📄');
  });

  it('devrait gérer la déconnexion', () => {
    component.onLogout();
    expect(mockUserService.logout).toHaveBeenCalled();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/home']);
  });
});