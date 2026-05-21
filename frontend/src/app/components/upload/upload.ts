import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common'; // Pour *ngIf, *ngFor
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms'; // Pour [formGroup]
import { Router } from '@angular/router';

 
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSliderModule } from '@angular/material/slider';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';

import { FileService } from '../../services/file.service';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-upload',
  standalone: true, // Très important pour les composants modernes
  imports: [
    CommonModule,
    ReactiveFormsModule,
    // On déclare à notre composant qu'il a le droit d'utiliser ces balises HTML :
    MatCardModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSliderModule,
    MatChipsModule,
    MatButtonModule
  ],
  templateUrl: './upload.html',
  styleUrls: ['./upload.scss']
})
 
export class UploadComponent implements OnInit {
  uploadForm!: FormGroup;
  selectedFile: File | null = null;
  errorMessage: string = '';
  tagsList: string[] = [];
  
  // Limite fixée à 1 Go en octets (comme sur ton Spring Boot)
  readonly MAX_FILE_SIZE = 1024 * 1024 * 1024; 
 
  constructor(
    private fb: FormBuilder,
    private fileService: FileService,
    private router: Router,
    private userService: UserService 
  ) {}

  ngOnInit(): void {
    this.uploadForm = this.fb.group({
      password: ['', [Validators.minLength(6)]],
      expirationDays: [7], // Valeur par défaut de ton slider
      tagInput: ['']
    });
  }

  onFileSelected(event: any): void {
    const file: File = event.target.files[0];
    
    if (file) {
      if (file.size > this.MAX_FILE_SIZE) {
        this.errorMessage = 'Le fichier dépasse la taille maximale autorisée (1 Go).';
        this.selectedFile = null;
      } else {
        this.selectedFile = file;
        this.errorMessage = ''; // On réinitialise l'erreur
      }
    }
  }

  // Ajoute un tag quand l'utilisateur appuie sur Entrée
  addTag(): void {
    const tagControl = this.uploadForm.get('tagInput');
    const newTag = tagControl?.value?.trim();
    
    if (newTag && !this.tagsList.includes(newTag)) {
      this.tagsList.push(newTag);
    }
    
    // On vide le champ après l'ajout
    tagControl?.setValue('');
  }

  // Supprime un tag en cliquant sur la croix
  removeTag(tag: string): void {
    const index = this.tagsList.indexOf(tag);
    if (index >= 0) {
      this.tagsList.splice(index, 1);
    }
  }

  onSubmit(): void {
    if (!this.selectedFile || this.uploadForm.invalid) {
      return;
    }

    // Récupération des valeurs du formulaire
    const password = this.uploadForm.get('password')?.value;
    const expirationDays = this.uploadForm.get('expirationDays')?.value;

    // Appel au backend
    this.fileService.uploadFile(this.selectedFile, password, expirationDays, this.tagsList)
      .subscribe({
        next: (response : any) => {
          console.log('Upload réussi ! Réponse du serveur :', response);
         
          this.navigateTo([`download/${response.token}`]);
        },
        error: (error : any) => {
          console.error('Erreur lors de l\'upload :', error);
          this.errorMessage = 'Une erreur est survenue lors de la communication avec le serveur.';
        }
      });
  }

  navigateTo(path: any): void {
    this.router.navigate([`/${path}`]);
  }


  get isLoggedIn(): boolean {
    return this.userService.isLoggedIn();
  }
}