import { Injectable } from '@angular/core';
import { HttpClient, HttpParams} from '@angular/common/http';
import { Observable } from 'rxjs';
 
export interface FileResponseDTO {
  fileId: number;
  name: string;
  size: number;
  mimeType: string;
  token: string;
  uploadDate: string;      
  expirationDate: string;  
  active: boolean;
  password: string;
  tags: string[];
}

@Injectable({
  providedIn: 'root'
})
export class FileService {
  
  // URL alignée avec le context path du backend (/api/v1)
  private apiUrl = 'http://localhost:8080/api/v1/files';

  constructor(private http: HttpClient) {}

  // ==========================================
  // 1. POST : UPLOAD UN FICHIER (Reste en FormData car il y a un fichier binaire)
  // ==========================================
  uploadFile(file: File, password?: string, expirationDays?: number, tags?: string[]): Observable<FileResponseDTO> {
    const formData = new FormData();
    formData.append('file', file);
    
    if (password) {
      formData.append('password', password);
    }
    
    if (expirationDays) {
      formData.append('expirationDays', expirationDays.toString());
    }
    
    if (tags && tags.length > 0) {
      tags.forEach(tag => formData.append('tags', tag));
    }

    // Le retour est maintenant typé avec le DTO unique
    return this.http.post<FileResponseDTO>(`${this.apiUrl}/upload`, formData);
  }

  // ==========================================
  // 2. GET : HISTORIQUE DE L'UTILISATEUR
  // ==========================================
  getUserFiles(): Observable<FileResponseDTO[]> {
    // Le tableau renvoie une liste propre de DTOs nettoyés (sans le path serveur)
    return this.http.get<FileResponseDTO[]>(`${this.apiUrl}/user/files`);
  }

  // ==========================================
  // 3. GET : MÉTADONNÉES D'UN FICHIER (Page publique)
  // ==========================================
  getMetadata(token: string): Observable<FileResponseDTO> {
    return this.http.get<FileResponseDTO>(`${this.apiUrl}/metadata/${token}`);
  }

 

  // ==========================================
  // 4. GET : TÉLÉCHARGEMENT BINAIRE
  // ==========================================
  downloadFile(token: string, password?: string): Observable<Blob> {
    let params = new HttpParams();
    
    // Si un mot de passe est fourni, on l'ajoute à l'URL (?password=xxx)
    if (password) {
      params = params.set('password', password);
    }

    return this.http.get(`${this.apiUrl}/download/${token}`, {
      params: params,
      responseType: 'blob'
    });
  }
  // ==========================================
  // 5. DELETE : SUPPRESSION D'UN FICHIER
  // ==========================================
  deleteFile(fileId: number): Observable<string> {
    return this.http.delete(`${this.apiUrl}/user/${fileId}`, { 
      responseType: 'text' 
    });
  }
}