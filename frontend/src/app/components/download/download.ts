import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FileService, FileResponseDTO } from '../../services/file.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-download',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './download.html',
  styleUrls: ['./download.scss']
})
export class DownloadComponent implements OnInit {
  token: string = '';
  file: FileResponseDTO | null = null;
  password: string = '';
  loading = true;

  constructor(
    private route: ActivatedRoute,
    private fileService: FileService,
    private router: Router,
     private cdr: ChangeDetectorRef,
       private userService: UserService 
  ) {}

   

  ngOnInit(): void {
    // Récupère le token depuis l'URL /download/:token
    this.token = this.route.snapshot.paramMap.get('token') || '';
    
    if (this.token) {
      this.fileService.getMetadata(this.token).subscribe({
        next: (data) => {
          this.file = data;
          this.loading = false;
          this.cdr.detectChanges(); 
        },
        error: (err) => {
          console.error("Erreur métadonnées", err);
          this.loading = false;
        }
      });
    }
  }

  onDownload(): void {
    // 1. Sécurité côté front : on bloque si le mot de passe manque
    if (this.file?.password  && !this.password) {
      alert("Veuillez saisir le mot de passe.");
      return;
    }

    // 2. On envoie la requête avec le mot de passe (s'il y en a un)
    this.fileService.downloadFile(this.token, this.password).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = this.file?.name || 'fichier_telecharge';
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        a.remove();
        
        // Optionnel : vider le champ après succès
        this.password = ''; 
      },
      error: (err) => {
        // Gérer spécifiquement l'erreur 403/401 (Mauvais mot de passe)
        if (err.status === 403 || err.status === 401) {
          alert("Erreur : Le mot de passe est incorrect.");
        } else {
          alert("Erreur : Fichier introuvable ou expiré.");
        }
      }
    });
  }
  // --- HELPERS ---
  formatSize(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  isExpired(dateStr: string): boolean {
    return new Date(dateStr) < new Date();
  }

  getDaysRemaining(dateStr: string): number {
    const diff = new Date(dateStr).getTime() - new Date().getTime();
    return Math.max(0, Math.ceil(diff / (1000 * 3600 * 24)));
  }

  navigateTo(path: string) {
    this.router.navigate([`/${path}`]);
  }

  get isLoggedIn(): boolean {
    return this.userService.isLoggedIn();
  }
  
}