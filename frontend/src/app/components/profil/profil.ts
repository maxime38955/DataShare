import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FileService, FileResponseDTO } from '../../services/file.service';
import { UserService } from '../../services/user.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-profil',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './profil.html',
  styleUrls: ['./profil.scss']
})
export class ProfilComponent implements OnInit {
  files: FileResponseDTO[] = [];
  userEmail: string = 'Utilisateur';
  constructor(
    private fileService: FileService,
    private userService: UserService,
    private router: Router,
    private cdr: ChangeDetectorRef 
  ) {}

  ngOnInit(): void {
    
    this.userEmail = this.extractEmailFromToken();
    this.loadFiles();
  }
  
extractEmailFromToken(): string {
    // ⚠️ Remplace 'auth_token' par le nom exact de ta clé dans le localStorage
    const token = localStorage.getItem('auth_token'); 
    
    if (token) {
      try {
        // Un token JWT est composé de 3 parties séparées par des points.
        // Le payload (les données) est la 2ème partie.
        const payload = token.split('.')[1];
        
        // On décode la base64 et on parse le JSON
        const decodedPayload = JSON.parse(atob(payload));
        
        // Spring Boot place généralement l'email/username dans "sub"
        return decodedPayload.sub || 'Utilisateur'; 
      } catch (e) {
        console.error('Erreur lors du décodage du token', e);
        return 'Utilisateur';
      }
    }
    return 'Utilisateur';
  }

  
loadFiles(): void {
    this.fileService.getUserFiles().subscribe({
      next: (data) => {
        this.files = data;
        this.cdr.detectChanges(); 
      },
      error: (err) => console.error(err)
    });
}

isMenuOpen = false; // Par défaut, le menu est fermé sur mobile

toggleMenu(): void {
  this.isMenuOpen = !this.isMenuOpen; // Alterne l'état ouvert/fermé
}

// Ajoute cette variable dans ta classe ProfilComponent
filterStatus: 'ALL' | 'ACTIVE' | 'EXPIRED' = 'ALL';

// Cette méthode retourne la liste filtrée
get filteredFiles(): FileResponseDTO[] {
  if (this.filterStatus === 'ACTIVE') {
    return this.files.filter(f => !this.isExpired(f.expirationDate));
  }
  if (this.filterStatus === 'EXPIRED') {
    return this.files.filter(f => this.isExpired(f.expirationDate));
  }
  return this.files; // Par défaut : 'ALL'
}

  onDelete(id: number): void {
    if (confirm('Voulez-vous vraiment supprimer ce fichier ?')) {
      this.fileService.deleteFile(id).subscribe(() => {
        this.files = this.files.filter(f => f.fileId !== id);
        this.ngOnInit();
      });
    }
  }

  onLogout(): void {
    this.userService.logout();
    this.router.navigate(['/login']);
  }

  openFile(token: string): void {
    this.router.navigate(['/download', token]);
  }

  navigateTo(path: string): void {
    this.router.navigate([`/${path}`]);
  }

  // --- Helpers pour l'affichage des dates ---

  isExpired(dateStr: string): boolean {
    return new Date(dateStr) < new Date();
  }

  getExpiryText(dateStr: string): string {
    const date = new Date(dateStr);
    if (date < new Date()) return 'Expiré';
    
    // Calcul simple de jours restants
    const diff = date.getTime() - new Date().getTime();
    const days = Math.ceil(diff / (1000 * 3600 * 24));
    return `Expire dans ${days} jour(s)`;
  }
}