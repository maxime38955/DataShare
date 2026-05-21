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
    
    const token = localStorage.getItem('auth_token'); 
    
    if (token) {
      try {
         
        const payload = token.split('.')[1];
        
         
        const decodedPayload = JSON.parse(atob(payload));
        
         
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

isMenuOpen = false;  

toggleMenu(): void {
  this.isMenuOpen = !this.isMenuOpen;  
}

 
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
    this.router.navigate(['/home']);
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

  // --- Fonction pour déterminer l'émoji selon l'extension ---
  getFileEmoji(fileName: string): string {
    if (!fileName) return '📄'; // Fichier par défaut

    // On récupère l'extension en minuscules
    const parts = fileName.split('.');
    if (parts.length === 1) return '📄'; // Pas d'extension trouvée
    
    const extension = parts.pop()?.toLowerCase();

    switch (extension) {
      // Images
      case 'jpg':
      case 'jpeg':
      case 'png':
      case 'gif':
      case 'svg':
      case 'webp':
        return '🖼️';
      
      // Vidéos
      case 'mp4':
      case 'avi':
      case 'mov':
      case 'mkv':
        return '🎥';
      
      // Audios
      case 'mp3':
      case 'wav':
      case 'ogg':
        return '🎵';
      
      // Documents
      case 'pdf':
        return '📕';
      case 'doc':
      case 'docx':
      case 'txt':
        return '📝';
      case 'xls':
      case 'xlsx':
      case 'csv':
        return '📊';
      case 'ppt':
      case 'pptx':
        return '📽️';
      
      // Archives
      case 'zip':
      case 'rar':
      case '7z':
      case 'tar':
      case 'gz':
        return '📦';
      
      // Code / Dev
      case 'html':
      case 'css':
      case 'js':
      case 'ts':
      case 'json':
      case 'java':
        return '👨‍💻';

      // Par défaut
      default:
        return '📄';
    }
  }
}