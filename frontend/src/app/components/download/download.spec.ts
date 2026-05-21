import { TestBed, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { DownloadComponent } from './download';
import { Router, ActivatedRoute } from '@angular/router';
import { FileService, FileResponseDTO } from '../../services/file.service';
import { UserService } from '../../services/user.service';
import { of, throwError } from 'rxjs';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
 import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
 
describe('DownloadComponent', () => {
  let component: DownloadComponent;
  let fixture: ComponentFixture<DownloadComponent>;
  
  // Déclaration de nos mocks
  let routerMock: any;
  let fileServiceMock: any;
  let userServiceMock: any;
  let activatedRouteMock: any;

  // Faux fichier retourné par le backend pour nos tests
  const mockFile: FileResponseDTO = {
    name: 'secret.pdf',
    size: 5000000,
    expirationDate: new Date(new Date().getTime() + 86400000).toISOString(), // Expire demain
    password: true, // Fichier protégé par défaut
  } as any;

  beforeEach(async () => {
    // Initialisation des mocks façon vi
    routerMock = {
      navigate: vi.fn() // Ton composant utilise navigate(), pas navigateByUrl()
    };

    fileServiceMock = {
      // getMetadata retourne un Observable contenant notre mockFile
      getMetadata: vi.fn().mockReturnValue(of(mockFile)), 
      downloadFile: vi.fn()
    };

    userServiceMock = {
      isLoggedIn: vi.fn().mockReturnValue(true)
    };

    activatedRouteMock = {
      snapshot: {
        paramMap: {
          get: vi.fn().mockReturnValue('fake-token-123') // Simule l'URL /download/fake-token-123
        }
      }
    };

    // On mock (intercepte) les alertes et les fonctions URL du navigateur pour éviter les crashs vi
    vi.spyOn(window, 'alert').mockImplementation(() => {});
      window.URL.createObjectURL = vi.fn() as any;
      window.URL.revokeObjectURL = vi.fn() as any;

    await TestBed.configureTestingModule({
      imports: [DownloadComponent, CommonModule, FormsModule], // Import du composant standalone
      providers: [
        { provide: Router, useValue: routerMock },
        { provide: FileService, useValue: fileServiceMock },
        { provide: UserService, useValue: userServiceMock },
        { provide: ActivatedRoute, useValue: activatedRouteMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DownloadComponent);
    component = fixture.componentInstance;
    
    // Le premier detectChanges déclenche le ngOnInit
    fixture.detectChanges(); 
  });

  afterEach(() => {
    vi.clearAllMocks(); // Nettoie les compteurs vi après chaque test
  });

  it('devrait créer le composant', () => {
    expect(component).toBeTruthy();
  });

  it('devrait charger les métadonnées au démarrage (ngOnInit)', () => {
    // Assert : Vérifie que le service a été appelé avec le bon token
    expect(fileServiceMock.getMetadata).toHaveBeenCalledWith('fake-token-123');
    
    // Assert : Vérifie que les données sont bien affectées au composant
    expect(component.file).toEqual(mockFile);
expect(component.loading).toBe(false);
 });

  it('devrait naviguer vers une autre page lors de l\'appel de navigateTo', () => {
    // Act
    component.navigateTo('login');

    // Assert
    expect(routerMock.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('devrait bloquer le téléchargement si le mot de passe est manquant', () => {
    // Arrange
    component.file = mockFile; // Fichier protégé
    component.password = ''; // Utilisateur n'a rien saisi

    // Act
    component.onDownload();

    // Assert
    expect(window.alert).toHaveBeenCalledWith("Veuillez saisir le mot de passe.");
    expect(fileServiceMock.downloadFile).not.toHaveBeenCalled(); // Le service ne doit pas être appelé
  });

  it('devrait lancer le téléchargement si le mot de passe est saisi', async() => {
    // Arrange
    component.file = mockFile;
    component.password = 'super_secret';
    
    // On simule le service renvoyant un Blob (un fichier binaire)
    const mockBlob = new Blob(['contenu'], { type: 'application/pdf' });
    fileServiceMock.downloadFile.mockReturnValue(of(mockBlob));

    // Act
    component.onDownload();
   await new Promise(resolve => setTimeout(resolve, 1500));

    // Assert
    expect(fileServiceMock.downloadFile).toHaveBeenCalledWith('fake-token-123', 'super_secret');
    expect(window.URL.createObjectURL).toHaveBeenCalledWith(mockBlob);
    expect(component.password).toBe(''); // Vérifie que le champ de mot de passe est réinitialisé
  });

  it('devrait afficher une alerte 403 si le mot de passe est incorrect', () => {
    // Arrange
    component.file = mockFile;
    component.password = 'mauvais_pass';
    
    // On simule une erreur 403 (Forbidden) renvoyée par le backend
    fileServiceMock.downloadFile.mockReturnValue(throwError(() => ({ status: 403 })));

    // Act
    component.onDownload();

    // Assert
    expect(window.alert).toHaveBeenCalledWith("Erreur : Le mot de passe est incorrect.");
  });

  it('devrait formatter correctement la taille du fichier', () => {
    // Act & Assert
    expect(component.formatSize(1048576)).toBe('1 MB'); // 1024 * 1024 = 1 MB
    expect(component.formatSize(0)).toBe('0 B');
  });
});