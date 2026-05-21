import { TestBed, ComponentFixture } from '@angular/core/testing';
import { UploadComponent } from './upload';
import { FileService } from '../../services/file.service';
import { UserService } from '../../services/user.service';
import { Router } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
 import { describe, it, expect, beforeEach, vi } from 'vitest';

describe('UploadComponent', () => {
  let component: UploadComponent;
  let fixture: ComponentFixture<UploadComponent>;

  let mockFileService: any;
  let mockUserService: any;
  let mockRouter: any;

  beforeEach(async () => {
    mockFileService = { uploadFile: vi.fn() };
    mockUserService = { isLoggedIn: vi.fn().mockReturnValue(true) };
    mockRouter = { navigate: vi.fn() };

    await TestBed.configureTestingModule({
      // On importe le composant et les modules nécessaires
      imports: [UploadComponent, ReactiveFormsModule, NoopAnimationsModule],
      providers: [
        { provide: FileService, useValue: mockFileService },
        { provide: UserService, useValue: mockUserService },
        { provide: Router, useValue: mockRouter }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(UploadComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('devrait créer le composant', () => {
    expect(component).toBeTruthy();
  });

  it('devrait initialiser le formulaire correctement', () => {
    expect(component.uploadForm).toBeDefined();
    expect(component.uploadForm.get('expirationDays')?.value).toBe(7);
  });

  it('devrait ajouter un tag à la liste', () => {
    component.uploadForm.get('tagInput')?.setValue('document');
    component.addTag();
    expect(component.tagsList).toContain('document');
    expect(component.uploadForm.get('tagInput')?.value).toBe('');
  });

  it('devrait supprimer un tag de la liste', () => {
    component.tagsList = ['photo', 'vacances'];
    component.removeTag('photo');
    expect(component.tagsList).not.toContain('photo');
    expect(component.tagsList).toContain('vacances');
  });

  it('devrait bloquer un fichier trop lourd', () => {
    const bigFile = { size: component.MAX_FILE_SIZE + 1 } as File;
    const event = { target: { files: [bigFile] } };

    component.onFileSelected(event);

    expect(component.selectedFile).toBeNull();
    expect(component.errorMessage).toContain('dépasse la taille maximale');
  });

  it('devrait appeler le service d\'upload lors de la soumission', () => {
    // Arrange
    const mockFile = new File(['content'], 'test.pdf', { type: 'application/pdf' });
    component.selectedFile = mockFile;
    component.uploadForm.get('password')?.setValue('pass123');
    mockFileService.uploadFile.mockReturnValue(of({ token: 'xyz-123' }));

    // Act
    component.onSubmit();

    // Assert
    expect(mockFileService.uploadFile).toHaveBeenCalled();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/download/xyz-123']);
  });

  it('devrait gérer l\'erreur lors de l\'upload', () => {
    component.selectedFile = new File(['content'], 'test.pdf');
    mockFileService.uploadFile.mockReturnValue(throwError(() => new Error('Upload error')));

    component.onSubmit();

    expect(component.errorMessage).toBe("Une erreur est survenue lors de la communication avec le serveur.");
  });
});