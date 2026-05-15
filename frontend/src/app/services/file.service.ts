import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class FileService {
  
  // L'URL de base pour ton contrôleur (ajuste si tu n'utilises pas /api/v1)
  private apiUrl = 'http://localhost:8080/api/v1/files';

  constructor(private http: HttpClient) {}

  // ==========================================
  // 1. POST : UPLOAD UN FICHIER
  // ==========================================
  uploadFile(file: File, password?: string, expirationDays?: number, tags?: string[]): Observable<any> {
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

    return this.http.post(`${this.apiUrl}/upload`, formData);
  }

  // ==========================================
  // 2. GET : HISTORIQUE DE L'UTILISATEUR
  // ==========================================
  getUserFiles(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/user/files`);
  }

  // ==========================================
  // 3. GET : MÉTADONNÉES D'UN FICHIER (Page publique)
  // ==========================================
  getMetadata(token: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/metadata/${token}`);
  }

  // ==========================================
  // 4. GET : TÉLÉCHARGEMENT BINAIRE
  // ==========================================
  downloadFile(token: string): Observable<Blob> {
    // ⚠️ IMPORTANT : 'responseType: blob' est indispensable pour dire à Angular
    // qu'il ne va pas recevoir du JSON, mais un fichier binaire physique (PDF, JPG, etc.)
    return this.http.get(`${this.apiUrl}/download/${token}`, {
      responseType: 'blob'
    });
  }

  // ==========================================
  // 5. DELETE : SUPPRESSION D'UN FICHIER
  // ==========================================
  deleteFile(fileId: number): Observable<any> {
    // ⚠️ IMPORTANT : 'responseType: text' est nécessaire ici car ton backend Spring Boot 
    // renvoie un simple String ("Fichier supprimé avec succès.") et non un objet JSON {}.
    // Sans ça, Angular croira à une erreur de parsing.
    return this.http.delete(`${this.apiUrl}/user/${fileId}`, { 
      responseType: 'text' 
    });
  }
}